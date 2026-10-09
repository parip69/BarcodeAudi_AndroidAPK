#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/cloud-env.sh
[[ -f "$ANDROID_HOME/platforms/android-35/android.jar" ]] || { echo 'Android SDK Platform 35 fehlt.' >&2; exit 1; }
exec bash ./gradlew --no-daemon --max-workers=2 assembleDebug "$@"
