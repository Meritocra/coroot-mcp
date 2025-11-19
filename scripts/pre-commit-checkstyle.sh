#!/usr/bin/env bash
set -euo pipefail

# Run Checkstyle on this Maven project before committing.
# Only runs if there are staged Java files.

STAGED_JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACMR | grep '\.java$' || true)

if [[ -z "${STAGED_JAVA_FILES}" ]]; then
  exit 0
fi

echo "Running Checkstyle for staged Java files..."
./mvnw -q checkstyle:check

