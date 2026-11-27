#!/usr/bin/env bash
# Použití (uvnitř worktree): scripts/finish-task.sh
# Rebasne na main, pushne větev a vypíše odkaz na PR.
set -euo pipefail
branch=$(git branch --show-current)
[[ "$branch" != "main" ]] || { echo "Jsi na main – spusť ve worktree úkolu"; exit 1; }
git fetch --quiet origin main
git rebase origin/main
git push -u origin "$branch"
url=$(git remote get-url origin | sed -E 's#git@github.com:#https://github.com/#; s#\.git$##')
echo
echo "Otevři PR: $url/compare/main...$branch?expand=1"
echo "Pak v tasks/ nastav status: review a pr: <odkaz> (commit na main)."
