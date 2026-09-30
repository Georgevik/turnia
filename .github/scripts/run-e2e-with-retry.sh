#!/usr/bin/env bash
# Runs the E2E suite; if any test fails, retries exactly the failed ones once before giving up.
# A test still failing after that retry blocks the run, unless every one of them is on
# .github/known-flaky-e2e-tests.txt — a short, explicit, reviewed allowlist, not a blanket switch.
set -euo pipefail

RESULTS=app/androidApp/build/outputs/androidTest-results/connected
KNOWN_FLAKY=.github/known-flaky-e2e-tests.txt

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
if ./gradlew :app:androidApp:connectedDebugAndroidTest --rerun-tasks "-Pandroid.testInstrumentationRunnerArguments.class=$FAILED"; then
  exit 0
fi

STILL_FAILED=$(python3 .github/scripts/list-failed-tests.py "$RESULTS")
UNKNOWN=$(python3 .github/scripts/check-known-flaky.py "$STILL_FAILED" "$KNOWN_FLAKY")
if [ -z "$UNKNOWN" ]; then
  echo "::warning::Still failing after retry, but every failure ($STILL_FAILED) is on the known-flaky list — not blocking."
  exit 0
fi

echo "::error::Failing after retry, and not on the known-flaky list: $UNKNOWN"
exit 1
