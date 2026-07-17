#!/usr/bin/env bash
# Aashu Tarminal — build helper.
set -e
cd "$(dirname "$0")/.."
echo "Regenerating tool registry..."
python3 tools-registry/generate_registry.py
echo "Building debug APK..."
gradle assembleDebug
echo "Done. APK at app/build/outputs/apk/debug/"
