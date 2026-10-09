#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/cloud-env.sh
command -v python3 >/dev/null
if ! command -v javac >/dev/null || ! command -v jlink >/dev/null; then
  mkdir -p /workspace/java/jdk-17
  jdk_archive=$(mktemp)
  trap 'rm -f "$jdk_archive"' EXIT
  curl -fL --retry 2 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz' -o "$jdk_archive"
  tar -xzf "$jdk_archive" --strip-components=1 -C /workspace/java/jdk-17
  rm -f "$jdk_archive"
  source scripts/cloud-env.sh
fi
command -v java >/dev/null
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  sdk_zip=$(mktemp)
  trap 'rm -f "$sdk_zip"' EXIT
  curl -fL --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o "$sdk_zip"
  unzip -q "$sdk_zip" -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
proxy_args=()
if [[ -n "${HTTPS_PROXY:-}" ]]; then
  read -r proxy_host proxy_port < <(python3 - <<'PY'
import os
from urllib.parse import urlparse
p = urlparse(os.environ['HTTPS_PROXY'])
print(p.hostname, p.port or 80)
PY
)
  proxy_args=(--proxy=http "--proxy_host=$proxy_host" "--proxy_port=$proxy_port")
fi
# Running this setup explicitly accepts the Android SDK licenses for this build environment.
set +o pipefail
yes | sdkmanager "--sdk_root=$ANDROID_HOME" "${proxy_args[@]}" --licenses
license_status=${PIPESTATUS[1]}
set -o pipefail
[[ "$license_status" == 0 ]]
sdkmanager "--sdk_root=$ANDROID_HOME" "${proxy_args[@]}" 'platforms;android-35' 'build-tools;34.0.0' 'build-tools;35.0.0' 'platform-tools'
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
echo 'Android-Buildumgebung eingerichtet.'
