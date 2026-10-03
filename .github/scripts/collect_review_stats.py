"""Copies AI Review run records into the `stats` branch checkout so they outlive the
90-day artifact retention, and appends one summary line per run to runs.jsonl.

Usage: collect_review_stats.py <stats-dir> [run-id ...]
With no run ids, backfills every AI Review run not yet collected. Idempotent: a run
attempt already in runs.jsonl is skipped. Runs without an artifact (cancelled, skipped,
failed early) get a row without stats, so they're never retried. Standard library only; needs `gh` with GH_TOKEN.
"""
import datetime
import json
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile

WORKFLOW = "AI Review"
# Artifacts older than this are deleted by GitHub, so there is nothing left to collect.
RETENTION_DAYS = 90
LIST_LIMIT = 500
# Runs handled per invocation, oldest first, so a big backlog finishes in steps and
# each step gets pushed instead of timing out with nothing saved.
BATCH_SIZE = 100
ARTIFACT = re.compile(r"^ai-review-pr-(\d+)-(\d+)$")


def gh(*args: str) -> str:
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True).stdout


def collected_keys(index: pathlib.Path) -> set[tuple[int, int]]:
    if not index.exists():
        return set()
    keys = set()
    for line in index.read_text().splitlines():
        if line.strip():
            row = json.loads(line)
            keys.add((row["run_id"], row["attempt"]))
    return keys


def uncollected_runs(done: set[tuple[int, int]]) -> list[tuple[int, int]]:
    """(run id, latest attempt) for completed runs within the retention window that aren't
    recorded yet. Decided from the run list alone, so collected runs cost no further API calls."""
    runs = json.loads(gh("run", "list", "--workflow", WORKFLOW, "--limit", str(LIST_LIMIT),
                         "--json", "databaseId,attempt,status,createdAt"))
    if len(runs) >= LIST_LIMIT:
        print(f"Warning: listed the newest {LIST_LIMIT} runs only; older runs within "
              f"{RETENTION_DAYS} days may be missed.", file=sys.stderr)
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=RETENTION_DAYS)
    pending = [
        r for r in runs
        if r["status"] == "completed"
        and datetime.datetime.fromisoformat(r["createdAt"].replace("Z", "+00:00")) >= cutoff
        and (r["databaseId"], r["attempt"]) not in done
    ]
    pending.sort(key=lambda r: r["createdAt"])  # oldest first: closest to expiring
    return [(r["databaseId"], r["attempt"]) for r in pending[:BATCH_SIZE]]


def attempt_meta(run_id: int, attempt: int) -> dict:
    """Details of one attempt. `gh run view` only describes the latest attempt, which would
    give earlier attempts the wrong conclusion and time."""
    return json.loads(gh("api", f"repos/{{owner}}/{{repo}}/actions/runs/{run_id}/attempts/{attempt}"))


def base_row(run_id: int, attempt: int, pr: int | None, meta: dict) -> dict:
    return {
        "run_id": run_id,
        "attempt": attempt,
        "pr": pr,
        "head_sha": meta["head_sha"],
        "branch": meta["head_branch"],
        "event": meta["event"],
        "conclusion": meta["conclusion"],
        "created_at": meta["run_started_at"] or meta["created_at"],
        "url": meta["html_url"],
        "record_path": None,
        "trace_path": None,
    }


def collect_run(run_id: int, latest_attempt: int, stats: pathlib.Path, done: set[tuple[int, int]]) -> list[dict]:
    rows = []
    with tempfile.TemporaryDirectory() as tmp:
        download = subprocess.run(["gh", "run", "download", str(run_id), "-D", tmp], capture_output=True, text=True)
        if download.returncode != 0:
            if "no valid artifacts" not in (download.stderr + download.stdout).lower():
                raise RuntimeError(f"download failed: {download.stderr.strip()}")
            # Cancelled, skipped or early-failing runs have no artifact. Record them anyway,
            # so they're marked as collected and never retried.
            if (run_id, latest_attempt) not in done:
                rows.append(row_without_artifact(run_id, latest_attempt))
            return rows

        for artifact in sorted(pathlib.Path(tmp).iterdir()):
            match = ARTIFACT.match(artifact.name)
            if not match:
                continue
            pr, attempt = int(match.group(1)), int(match.group(2))
            if (run_id, attempt) in done:
                continue
            dest = stats / "runs" / f"pr-{pr}" / f"{run_id}-{attempt}"
            dest.mkdir(parents=True, exist_ok=True)
            record, trace = None, None
            for f in artifact.glob("*.json"):
                shutil.copy2(f, dest / f.name)
                if f.name.endswith(".trace.json"):
                    trace = dest / f.name
                else:
                    record = dest / f.name
            row = base_row(run_id, attempt, pr, attempt_meta(run_id, attempt))
            row["record_path"] = str(record.relative_to(stats)) if record else None
            row["trace_path"] = str(trace.relative_to(stats)) if trace else None
            if record:
                r = json.loads(record.read_text())
                row.update(
                    model=r.get("model"),
                    outcome=r.get("outcome"),
                    turns=r.get("turns"),
                    input_tokens=r.get("inputTokens"),
                    output_tokens=r.get("outputTokens"),
                    estimated_cost_usd=r.get("estimatedCostUsd"),
                    accepted=len(r.get("accepted") or []),
                    rejected=len(r.get("rejected") or []),
                    posted=r.get("posted"),
                )
            rows.append(row)

    # A re-run whose latest attempt left no artifact: earlier attempts' artifacts still
    # downloaded, so record the latest attempt too, or it would be fetched on every trigger.
    if (run_id, latest_attempt) not in done and all(r["attempt"] != latest_attempt for r in rows):
        rows.append(row_without_artifact(run_id, latest_attempt))
    return rows


def row_without_artifact(run_id: int, attempt: int) -> dict:
    meta = attempt_meta(run_id, attempt)
    # GitHub doesn't link manually started (workflow_dispatch) runs to a PR, so pr is null for those.
    prs = meta.get("pull_requests") or []
    return base_row(run_id, attempt, prs[0]["number"] if prs else None, meta)


def main() -> None:
    stats = pathlib.Path(sys.argv[1])
    index = stats / "runs.jsonl"
    done = collected_keys(index)
    explicit = [int(x) for x in sys.argv[2:]]
    runs = [(i, json.loads(gh("run", "view", str(i), "--json", "attempt"))["attempt"]) for i in explicit] \
        or uncollected_runs(done)

    new_rows, failed = [], []
    for run_id, latest_attempt in runs:
        # One broken run (deleted, API error, truncated record) must not stop the rest.
        try:
            new_rows += collect_run(run_id, latest_attempt, stats, done)
        except Exception as e:  # noqa: BLE001 - log and move on
            failed.append(run_id)
            print(f"Skipping run {run_id}: {e}", file=sys.stderr)

    if new_rows:
        with index.open("a") as out:
            for row in sorted(new_rows, key=lambda r: (r["created_at"], r["attempt"])):
                out.write(json.dumps(row, sort_keys=True) + "\n")
    print(f"Collected {len(new_rows)} new run(s); {len(done) + len(new_rows)} in total.")
    if failed:
        # Rows collected above are still written and pushed; the next run retries the failures.
        print(f"{len(failed)} run(s) failed and will be retried: {failed}", file=sys.stderr)


if __name__ == "__main__":
    main()
