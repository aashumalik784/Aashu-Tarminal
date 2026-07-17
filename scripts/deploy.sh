#!/usr/bin/env bash
# Aashu Tarminal — install the debug APK to a connected device/emulator.
set -e
cd "$(dirname "$0")/.."
gradle installDebug
adb shell am start -n com.aashutarminal.debug/com.aashutarminal.MainActivity
