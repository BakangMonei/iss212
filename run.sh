#!/usr/bin/env bash
# Run from repository root so data/menu.json resolves.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
exec java -cp "$ROOT/classes:$ROOT/lib/json-20231013.jar" Main
