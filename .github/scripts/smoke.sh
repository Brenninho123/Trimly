#!/usr/bin/env bash
set -uo pipefail

PACKAGE="com.brenninho.trimly"

adb install -r dist/Trimly-debug.apk || exit 1
adb logcat -c
adb shell am start -W -n "$PACKAGE/.MainActivity" || exit 1
sleep 8

adb exec-out screencap -p > smoke.png
adb logcat -d > logcat.txt

if ! adb shell pidof -s "$PACKAGE" > /dev/null; then
  echo "App process is not running"
  exit 1
fi

if grep -q "FATAL EXCEPTION" logcat.txt; then
  echo "Crash detected"
  grep -A 20 "FATAL EXCEPTION" logcat.txt
  exit 1
fi

echo "Smoke test passed"
