#!/usr/bin/env bash
# Runs the two example test suites twice, without and with AI mode, and collects what the agents get to see.
#
# Usage: generate-logs.sh <logs dir>
#
#   <logs dir>/xml/<module>/*.xml     JUnit XML reports without AI mode (plain and careful baseline)
#   <logs dir>/xml-console.out        console output of that run
#   <logs dir>/ai/test.compact        compact file (AI mode)
#   <logs dir>/ai/test.full           full file (AI mode)
#   <logs dir>/ai-console.out         console output of that run
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
cd "$ROOT"

echo "Running tests without AI mode ..."
status=0
./gradlew "${TASKS[@]}" -q --console=plain > "$LOGS/xml-console.out" 2>&1 || status=$?
[ $status -eq 0 ] || echo "  tests failed (exit $status), see $LOGS/xml-console.out"
for m in "${MODULES[@]}"; do
  mkdir -p "$LOGS/xml/$m"
  cp "examples/$m/build/test-results/test/"*.xml "$LOGS/xml/$m/"
done

echo "Running tests with AI mode ..."
status=0
./gradlew "${TASKS[@]}" -q --console=plain -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file="$LOGS/ai/test" > "$LOGS/ai-console.out" 2>&1 || status=$?
[ $status -eq 0 ] || echo "  tests failed (exit $status), see $LOGS/ai-console.out"

python3 -I "$HERE/answer-key.py" "$LOGS/ai/test.full" > "$LOGS/answer-key.txt"

echo
echo "Console output:  without AI mode $(wc -c < "$LOGS/xml-console.out") bytes, with AI mode $(wc -c < "$LOGS/ai-console.out") bytes"
echo "Logs to search:  XML reports $(cat "$LOGS"/xml/*/*.xml | wc -c) bytes, compact file $(wc -c < "$LOGS/ai/test.compact") bytes (.full $(wc -c < "$LOGS/ai/test.full") bytes)"
echo "Answer key:      $LOGS/answer-key.txt"
