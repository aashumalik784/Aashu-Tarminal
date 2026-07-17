#!/usr/bin/env bash
# Aashu Tarminal — run unit + instrumentation tests.
set -e
cd "$(dirname "$0")/.."
./gradlew test
echo "Unit tests passed. Run './gradlew connectedAndroidTest' with a device attached for UI tests."
