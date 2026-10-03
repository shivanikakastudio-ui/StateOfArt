"""Copies AI Review run records into the `stats` branch checkout so they outlive the
90-day artifact retention, and appends one summary line per run to runs.jsonl.

Usage: collect_review_stats.py <stats-dir> [run-id ...]
With no run ids, backfills every AI Review run not yet collected. Idempotent: a run
attempt already in runs.jsonl is skipped. Standard library only; needs `gh` with GH_TOKEN.
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


def uncollected_run_ids(done: set[tuple[int, int]]) -> list[int]:
    """Completed runs within the retention window whose latest attempt isn't recorded yet.
    Decided from the run list alone, so already-collected runs cost no further API calls."""
    runs = json.loads(gh("run", "list", "--workflow", WORKFLOW, "--limit", "500",
                         "--json", "databaseId,attempt,status,createdAt"))
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=RETENTION_DAYS)
    return [
        r["databaseId"] for r in runs
        if r["status"] == "completed"
        and datetime.datetime.fromisoformat(r["createdAt"].replace("Z", "+00:00")) >= cutoff
        and (r["databaseId"], r["attempt"]) not in done
    ]


def collect_run(run_id: int, stats: pathlib.Path, done: set[tuple[int, int]]) -> list[dict]:
    meta = json.loads(gh("run", "view", str(run_id), "--json", "headSha,headBranch,event,conclusion,createdAt,url"))
    rows = []
    with tempfile.TemporaryDirectory() as tmp:
        # Fails when a run has no artifacts (e.g. it stopped before the agent ran); record nothing then.
        if subprocess.run(["gh", "run", "download", str(run_id), "-D", tmp], capture_output=True).returncode != 0:
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
            row = {
                "run_id": run_id,
                "attempt": attempt,
                "pr": pr,
                "head_sha": meta["headSha"],
                "branch": meta["headBranch"],
                "event": meta["event"],
                "conclusion": meta["conclusion"],
                "created_at": meta["createdAt"],
                "url": meta["url"],
                "record_path": str(record.relative_to(stats)) if record else None,
                "trace_path": str(trace.relative_to(stats)) if trace else None,
            }
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
    return rows


def main() -> None:
    stats = pathlib.Path(sys.argv[1])
    index = stats / "runs.jsonl"
    done = collected_keys(index)
    run_ids = [int(x) for x in sys.argv[2:]] or uncollected_run_ids(done)

    new_rows, failed = [], []
    for run_id in run_ids:
        # One broken run (deleted, API error, truncated record) must not stop the rest.
        try:
            new_rows += collect_run(run_id, stats, done)
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
