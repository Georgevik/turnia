#!/usr/bin/env bash
# Writes app/androidApp/google-services.json from the GOOGLE_SERVICES_JSON_BASE64 secret, or from
# the committed example when the secret is not there (a pull request from a fork gets no secrets).
# The example is enough to compile; it only cannot talk to Firebase.
set -euo pipefail

target=app/androidApp/google-services.json

if [ -n "${GOOGLE_SERVICES_JSON_BASE64:-}" ]; then
  echo "$GOOGLE_SERVICES_JSON_BASE64" | base64 --decode > "$target"
  echo "Wrote $target from the secret"
else
  cp app/androidApp/google-services.json.example "$target"
  echo "No secret: wrote $target from the example"
fi
