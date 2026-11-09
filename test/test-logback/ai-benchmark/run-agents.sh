#!/usr/bin/env bash
# Runs the agents on the logs collected by generate-logs.sh, several runs in parallel.
#
# Usage: run-agents.sh <logs dir> [setups] [agents]
#
#   setups  any of: plain careful ai                       (default: all three)
#           plain    JUnit XML reports, only told where they are
#           careful  JUnit XML reports plus a tested grep cookbook (prompts/careful.txt)
#           ai       compact file and .full plus the AGENTS.md snippet from ../README.md
#   agents  tool:model:effort, tool is copilot or claude, model 'default' is the tool's default model
#           (default: "copilot:default:low claude:opus:low claude:sonnet:low claude:haiku:low")
#
# Both tools only get read-only shell commands (no writes). Output per run in <logs dir>/runs/:
# <setup>-<tool>-<model>-<effort>.prompt, .out (answer and usage), .meta (exit code, seconds).
# MAX_PARALLEL limits the number of concurrent runs (default 8), RUN_TIMEOUT stops a run after that many seconds
# (default 3600, exit=124 in .meta). MEMORY_MAX limits the memory of each run, the agent and the commands it runs, with
# systemd-run where available (default 4G, 0 = no limit; a command over the limit is killed): agents write runaway
# commands too, such as an awk loop that grew to 55 GB. DRY_RUN=1 only writes the prompts.
# Print a table of the runs with summarize.py.
set -euo pipefail

[ $# -ge 1 ] || { echo "usage: $0 <logs dir> [setups] [agents]" >&2; exit 2; }
HERE=$(cd "$(dirname "$0")" && pwd)
LOGS=$(cd "$1" && pwd)
SETUPS=${2:-"plain careful ai"}
AGENTS=${3:-"copilot:default:low claude:opus:low claude:sonnet:low claude:haiku:low"}
COMMANDS="grep wc head tail sort uniq cut tr ls sed awk echo printf cat nl paste test"
mkdir -p "$LOGS/runs"

LIMIT=()
if [ "${MEMORY_MAX:-4G}" != 0 ]; then
  if command -v systemd-run > /dev/null && systemd-run --user --scope --quiet -p MemoryMax=1G true 2> /dev/null; then
    LIMIT=(systemd-run --user --scope --quiet -p "MemoryMax=${MEMORY_MAX:-4G}" -p MemorySwapMax=0)
  else
    echo "warning: no systemd-run, the memory of the runs is not limited" >&2
  fi
fi

# The AI-mode instructions from the README, from the compact file bullet to the end of the snippet.
snippet() {
  awk '/^````markdown/ {b=1; next} b && /^````/ {exit} b && /^- `\/tmp\/test\.compact`/ {p=1} b && p' "$HERE/../README.md" \
    | sed "s|/tmp/test|$LOGS/ai/test|g"
}

prompt() { # setup
  if [ "$1" = ai ]; then
    SNIPPET=$(snippet) awk '$0 == "@SNIPPET@" {print ENVIRON["SNIPPET"]; next} {print}' "$HERE/prompts/ai.txt"
  else
    cat "$HERE/prompts/$1.txt"
  fi | sed "s|@LOGS@|$LOGS|g"
}

run() { # setup tool model effort
  local setup=$1 tool=$2 model=$3 effort=$4 o start status=0 args=()
  o=$LOGS/runs/$setup-$tool-$model-$effort
  prompt "$setup" > "$o.prompt"
  [ -z "${DRY_RUN:-}" ] || return 0
  [ "$model" = default ] || args+=(--model "$model")
  start=$(date +%s.%N)
  case $tool in
    copilot)
      for c in $COMMANDS; do args+=(--allow-tool "shell($c:*)"); done
      (cd "$LOGS" && "${LIMIT[@]}" timeout "${RUN_TIMEOUT:-3600}" copilot -p "$(cat "$o.prompt")" --reasoning-effort "$effort" "${args[@]}" \
        --deny-tool write --no-custom-instructions --no-color > "$o.out" 2>&1) || status=$?
      ;;
    claude)
      local allowed=Read,Grep,Glob
      for c in $COMMANDS; do allowed+=",Bash($c *)"; done
      (cd "$LOGS" && "${LIMIT[@]}" timeout "${RUN_TIMEOUT:-3600}" claude -p "$(cat "$o.prompt")" --effort "$effort" "${args[@]}" \
        --tools Bash,Read,Grep,Glob --allowedTools "$allowed" --strict-mcp-config --no-session-persistence \
        --output-format stream-json --verbose > "$o.out" 2> "$o.err") || status=$?
      ;;
    *) echo "unknown tool: $tool" >&2; return 1 ;;
  esac
  printf 'exit=%s\nseconds=%s\n' "$status" "$(awk -v a="$start" -v b="$(date +%s.%N)" 'BEGIN {printf "%.1f", b - a}')" > "$o.meta"
}

for setup in $SETUPS; do
  for agent in $AGENTS; do
    IFS=: read -r tool model effort <<< "$agent"
    while [ "$(jobs -rp | wc -l)" -ge "${MAX_PARALLEL:-8}" ]; do
      wait -n || true
    done
    run "$setup" "$tool" "$model" "$effort" &
  done
done
wait
echo "Done: $LOGS/runs (grade the answers against $LOGS/answer-key.txt, see README.md)"
