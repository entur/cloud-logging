#!/usr/bin/env bash
# Runs the two example test suites twice, without and with AI mode, and collects what the agents get to see.
# The AI-mode run uses the scripts/test-ai script of ../README.md, writing to <logs dir>/ai instead of a new directory in /tmp.
#
# Usage: generate-logs.sh <logs dir>
#
#   <logs dir>/xml/<module>/*.xml     JUnit XML reports without AI mode (plain and careful baseline)
#   <logs dir>/xml-console.out        console output of that run
#   <logs dir>/test-ai                the scripts/test-ai script from ../README.md, as run here
#   <logs dir>/test-ai.out            what it printed (AI mode), given to the agents
#   <logs dir>/ai/test.<pid>.compact  compact file of each test JVM (AI mode)
#   <logs dir>/ai/test.<pid>.full     full file of each test JVM (AI mode)
#   <logs dir>/ai/test.summary        failed tests and number of tests per test JVM (AI mode)
#   <logs dir>/ai/test.out            console output of the AI-mode run (written by scripts/test-ai)
#   <logs dir>/answer-key.txt         expected answers, from answer-key.py
set -euo pipefail

[ $# -eq 1 ] || { echo "usage: $0 <logs dir>" >&2; exit 2; }
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE" && git rev-parse --show-toplevel)
mkdir -p "$1"
LOGS=$(cd "$1" && pwd)
MODULES=(gcp-web-example gcp-grpc-spring-example)
TASKS=()
for m in "${MODULES[@]}"; do
  TASKS+=(":examples:$m:cleanTest" ":examples:$m:test")
done

rm -rf "$LOGS/xml" "$LOGS/ai" "$LOGS/runs"
mkdir -p "$LOGS/xml" "$LOGS/ai"

# scripts/test-ai from the README, with the log directory replaced
awk '/^```bash$/ {b=1; s=""; next} b && /^```$/ {if (s ~ /entur\.logging\.ai\.enabled/) {printf "%s", s; exit} b=0; next} b {s = s $0 "\n"}' \
  "$HERE/../README.md" | sed 's|^L=$(mktemp -d .*|L=$BENCHMARK_AI_LOGS|' > "$LOGS/test-ai"
grep -q '^L=\$BENCHMARK_AI_LOGS$' "$LOGS/test-ai" || { echo "scripts/test-ai not found in ../README.md" >&2; exit 1; }
chmod +x "$LOGS/test-ai"
cd "$ROOT"

echo "Running tests without AI mode ..."
status=0
./gradlew "${TASKS[@]}" -q --console=plain > "$LOGS/xml-console.out" 2>&1 || status=$?
[ $status -eq 0 ] || echo "  tests failed (exit $status), see $LOGS/xml-console.out"
for m in "${MODULES[@]}"; do
  mkdir -p "$LOGS/xml/$m"
  cp "examples/$m/build/test-results/test/"*.xml "$LOGS/xml/$m/"
done

echo "Running tests with AI mode (scripts/test-ai) ..."
status=0
BENCHMARK_AI_LOGS=$LOGS/ai "$LOGS/test-ai" "${TASKS[@]}" > "$LOGS/test-ai.out" 2>&1 || status=$?
[ $status -eq 0 ] || echo "  tests failed (exit $status), see $LOGS/test-ai.out"

python3 -I "$HERE/answer-key.py" "$LOGS"/ai/*.full > "$LOGS/answer-key.txt"

echo
echo "Console output:  without AI mode $(wc -c < "$LOGS/xml-console.out") bytes, with AI mode $(wc -c < "$LOGS/ai/test.out") bytes (scripts/test-ai prints $(wc -c < "$LOGS/test-ai.out") bytes)"
echo "Logs to search:  XML reports $(cat "$LOGS"/xml/*/*.xml | wc -c) bytes, compact files $(cat "$LOGS"/ai/*.compact | wc -c) bytes (.full $(cat "$LOGS"/ai/*.full | wc -c) bytes, summary $(wc -c < "$LOGS/ai/test.summary") bytes)"
echo "Answer key:      $LOGS/answer-key.txt"
