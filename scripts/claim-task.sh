#!/usr/bin/env bash
# Použití: scripts/claim-task.sh tasks/003-login.md <jméno> <agent>
# Zamkne úkol: nastaví owner/agent/status, commitne a pushne na main.
# Kdo pushne první, má úkol. Spouštěj z hlavního checkoutu na větvi main.
set -euo pipefail
task="$1"; who="$2"; agent="$3"
[[ -f "$task" ]] || { echo "Úkol $task neexistuje"; exit 1; }
[[ "$(git branch --show-current)" == "main" ]] || { echo "Spusť na větvi main"; exit 1; }
has_remote=$(git remote | grep -c . || true)
[[ "$has_remote" -gt 0 ]] && git pull --rebase --quiet
grep -q '^status: todo' "$task" || { echo "Úkol není volný:"; grep -E '^(status|owner|agent):' "$task"; exit 1; }
name=$(basename "$task" .md)
branch="$who/$agent/$name"
sed -i -E "s|^status:.*|status: in-progress|; s|^owner:.*|owner: $who|; s|^agent:.*|agent: $agent|; s|^branch:.*|branch: $branch|" "$task"
git add "$task"
git commit --quiet -m "[$agent] claim $name ($who)"
if [[ "$has_remote" -gt 0 ]] && ! git push --quiet; then
  git reset --quiet --hard HEAD~1
  echo "Push selhal – úkol si nejspíš vzal někdo jiný. Udělej git pull a vyber jiný."; exit 1
fi
echo "Úkol $name je tvůj. Větev: $branch"
echo "Dál: scripts/start-task.sh $branch"
