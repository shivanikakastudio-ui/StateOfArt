# AI Review stats

Written by the **AI Review Stats** workflow; don't edit by hand.

- `runs.jsonl`: one line per AI Review run attempt (PR, commit, outcome, turns, tokens, estimated cost, findings accepted/rejected, posted, links)
- Runs without an artifact (cancelled, skipped, failed early) have a line with no stats. Manually started runs that have no artifact have `pr: null`, since GitHub doesn't link them to a PR
- `runs/pr-<n>/<run-id>-<attempt>/`: that attempt's run record and trace, copied from its artifact
