#!/usr/bin/env bash
# Source from build scripts; local SDK and network settings are never committed.
if [[ -d /workspace/java/jdk-17 ]]; then
  export JAVA_HOME=/workspace/java/jdk-17
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if [[ -f /etc/ssl/certs/java/cacerts && "${JAVA_TOOL_OPTIONS:-}" != *-Djavax.net.ssl.trustStore=* ]]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
export ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
if [[ -n "${HTTPS_PROXY:-}" ]]; then
  proxy_options=$(python3 - <<'PY'
import os
from urllib.parse import urlparse
p = urlparse(os.environ['HTTPS_PROXY'])
if p.hostname and p.port:
    print(f'-Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port} -Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port}')
PY
)
  export GRADLE_OPTS="${GRADLE_OPTS:-} $proxy_options"
fi
