#!/usr/bin/env python3
"""Increment all version markers, build and archive; roll back on failure."""
import re
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
gradle = root / 'app/build.gradle.kts'
html = root / 'app/src/main/assets/index.html'
sw = root / 'app/src/main/assets/sw.js'
paths = [gradle, html, sw, root / 'docs/index.html', root / 'docs/sw.js']
originals = {p: p.read_bytes() for p in paths}

def replace(text, pattern, replacement, expected=1):
    result, count = re.subn(pattern, replacement, text, flags=re.M)
    if count != expected:
        raise ValueError(f'Versionsmarker fehlt oder ist mehrdeutig: {pattern}')
    return result

try:
    text = gradle.read_text()
    current = int(re.search(r'^\s*versionCode\s*=\s*(\d+)', text, re.M)[1])
    name = re.search(r'^\s*versionName\s*=\s*"(\d+)"', text, re.M)[1]
    if int(name) != current:
        raise ValueError('versionCode und versionName stimmen nicht überein.')
    version = str(current + 1)
    text = replace(text, r'^(\s*versionCode\s*=\s*)\d+', lambda m: m[1] + version)
    text = replace(text, r'^(\s*versionName\s*=\s*")\d+(".*)$', lambda m: m[1] + version + m[2])
    gradle.write_text(text)
    text = html.read_text()
    # Preserve markup and update only the known version fields.
    for pattern in [r'(<html lang="de" data-app-version=")[^"]+(")',
                    r'(const APP_VERSION_FALLBACK = ")[^"]+(";)',
                    r'(<footer id="appFooter" data-app-version=")[^"]+("[^>]*>)',
                    r'(<span id="footerVersion">)[^<]+(</span>)']:
        text = replace(text, pattern, lambda m: m[1] + version + m[2])
    text = re.sub(r'(<!-- Barcode Audi Scanner - Ver\. )\S+( -->)', lambda m: m[1] + version + m[2], text)
    html.write_text(text)
    text = sw.read_text()
    for constant in ['APP_SHELL_CACHE', 'RUNTIME_CACHE']:
        text = replace(text, rf'^(const {constant} = ".*?-v)[^"]+(";)', lambda m: m[1] + version + m[2])
    sw.write_text(text)
    subprocess.run(['bash', 'scripts/build-apk.sh'], cwd=root, check=True)
    apk = root / f'app/build/outputs/apk/debug/BarcodeAudiScanner_ver{version}.apk'
    if not apk.is_file():
        raise FileNotFoundError(apk)
    shutil.copy2(apk, root / 'Privat' / apk.name)
    shutil.copy2(html, root / 'Privat' / f'BarcodeAudiScanner_ver{version}.html')
    print(f'Version {version} erfolgreich gebaut und in Privat archiviert.')
except BaseException:
    for path, content in originals.items():
        path.write_bytes(content)
    raise
