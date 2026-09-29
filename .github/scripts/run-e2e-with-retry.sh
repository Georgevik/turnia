#!/usr/bin/env bash
# Runs the E2E suite; if any test fails, retries exactly the failed ones once before giving up.
# A test that fails twice is a real failure, not a CI-runner flake — the script then exits non-zero.
set -euo pipefail

RESULTS=app/androidApp/build/outputs/androidTest-results/connected

if ./gradlew :app:androidApp:connectedDebugAndroidTest; then
  exit 0
fi

echo "::warning::Some E2E tests failed on the first attempt — retrying just those once."
mkdir -p "$RESULTS-attempt1"
cp -r "$RESULTS"/. "$RESULTS-attempt1/"

FAILED=$(python3 .github/scripts/list-failed-tests.py "$RESULTS")
if [ -z "$FAILED" ]; then
  echo "::error::The suite failed with no test results to retry (the build or the emulator failed first)."
  exit 1
fi

echo "Retrying: $FAILED"
./gradlew :app:androidApp:connectedDebugAndroidTest --rerun-tasks "-Pandroid.testInstrumentationRunnerArguments.class=$FAILED"
