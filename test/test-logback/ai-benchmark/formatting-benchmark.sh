#!/usr/bin/env bash
# Microbenchmark of the AI-mode formatting: time and allocation per event for the compact and full line against the JSON encoder
# of the machine-readable output (AiFormattingBenchmark in test-logback's tests). Prints a Markdown table.
#
# Usage: formatting-benchmark.sh [output file]     (default: a temporary file)
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE" && git rev-parse --show-toplevel)
OUT=${1:-$(mktemp)}
OUT=$(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")
rm -f "$OUT"

LOG=$(mktemp)
cd "$ROOT"
if ! AI_FORMATTING_BENCHMARK="$OUT" ./gradlew :test:test-logback:test --tests '*AiFormattingBenchmark' --rerun -q --console=plain > "$LOG" 2>&1 \
    || [ ! -s "$OUT" ]; then
  cat "$LOG" >&2
  echo "benchmark failed, see the output above" >&2
  exit 1
fi
rm -f "$LOG"
cat "$OUT"
