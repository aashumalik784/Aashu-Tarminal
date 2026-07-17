#!/usr/bin/env bash
# Aashu Tarminal — run unit + instrumentation tests.
set -e
cd "$(dirname "$0")/.."
gradle test
echo "Unit tests passed. Run 'gradle connectedAndroidTest' with a device attached for UI tests."
