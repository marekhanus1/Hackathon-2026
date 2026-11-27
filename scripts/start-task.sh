#!/usr/bin/env bash
# Použití: scripts/start-task.sh <jméno>/<agent>/<NNN-nazev>
# Vytvoří větev z aktuálního main a samostatný worktree vedle repa.
set -euo pipefail
branch="$1"
root=$(git rev-parse --show-toplevel)
wt="$(dirname "$root")/$(basename "$root")-wt/${branch//\//-}"
git remote | grep -q . && git fetch --quiet origin main && base=origin/main || base=main
git worktree add --no-track -b "$branch" "$wt" "$base"
echo
echo "Worktree: $wt"
echo "Spusť agenta tam:  cd \"$wt\" && claude   # nebo: gemini"
