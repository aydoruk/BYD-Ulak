#!/usr/bin/env sh
set -eu

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not installed or not available on PATH. Install it first or use the GitHub Actions workflow." >&2
  exit 1
fi

exec gradle "$@"
