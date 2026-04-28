#!/usr/bin/env bash
# Build from repository root (packages under src/). Run: ./compile.sh && ./run.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$ROOT/classes"
javac --release 11 -encoding UTF-8 -cp "$ROOT/lib/json-20231013.jar" \
  -d "$ROOT/classes" $(find "$ROOT/src" -name "*.java")
echo "OK: classes written to $ROOT/classes"
