# BarcodeAudi Android APK

Work only in this repository; other projects in `/workspace` are independent.

## Commit-Nachrichten und Veroeffentlichung

Commit-Nachrichten auf Deutsch schreiben. Titel und beschreibende Zeilen mit
thematisch passenden Icons beginnen, z. B. 📤 fuer Veroeffentlichung, 🔄 fuer
Update-Ablauf, 🔒 fuer Signierung, 💡 fuer neue Funktionen oder ✅ fuer belegte
Pruefergebnisse. Icons passend zum Inhalt waehlen, nicht zufaellig abwechseln.
Im Titel die Version nennen, wenn eine Version veroeffentlicht wird. Kurz und
konkret die tatsaechlichen Aenderungen beschreiben; nicht nur "Publish version"
schreiben und keine ungetesteten Ergebnisse als erfolgreich darstellen.
Diese Nachricht vor dem Push erstellen und mit dem Commit hochladen.

Der Button "Version +1 & GitHub" veroeffentlicht ausschliesslich den lokalen
Arbeitsstand: Version erhoehen, kompilieren, APK pruefen, APK/HTML archivieren,
committen und pushen. Kein automatisches Fetch, Pull, Merge oder Rebase in diesem
Ablauf. Zum ausdruecklichen Holen von GitHub dient der separate "Git Pull"-Button.
Die fertige APK unter `Privat/BarcodeAudiScanner_ver<Version>.apk` und die von
Gradle erzeugten `docs/`-Dateien mit hochladen, damit der vorhandene Update-Button
die passende APK auf GitHub findet.

The editable web source is `app/src/main/assets/`. Do not edit `docs/` directly:
Gradle synchronizes it from the assets during every build.

Cloud commands:

- Setup: `bash scripts/setup-cloud.sh`
- Rebuild current version: `bash scripts/build-apk.sh`
- Explicitly requested next version: `python3 scripts/next-version.py`

Before publishing, verify the APK using SDK `apksigner` and check its version
with `aapt dump badging`. Compare the signing certificate with the previous APK
and report any change, because Android requires matching certificates for updates.
Use the shared development key `signing/barcodeaudi-debug.keystore` for every debug build.
Gradle configures it for Linux/cloud and Windows/VS Code automatically; never replace
or regenerate it. Its intentionally public development credentials are documented
in `signing/README.md`. Keep any separate production signing keys outside Git.

Archive APK and HTML in `Privat/` as the version script does. Include the generated
`docs/` changes when uploading to GitHub. Never force-push. Do not claim a separate
Codex UI environment was created merely because this repository was cloned.

## Required signing identity for every debug APK

The authoritative key is the existing GitHub file:
https://github.com/parip69/BarcodeAudi_AndroidAPK/blob/main/signing/barcodeaudi-debug.keystore

Its checkout path is `signing/barcodeaudi-debug.keystore`; Gradle selects it in
`app/build.gradle.kts` for VS Code/Codex, Windows, Android Studio and cloud builds.
Certificate SHA-256 (not the file hash):
`b46b439e36ed2de248b9085cb4b457b74ba70b5beb823962e9c931ce79104d28`.
Use alias `androiddebugkey` and the documented development passwords in
`signing/README.md`. Never generate, replace or rotate this key; never fall back
to a machine-local debug keystore. If it is missing, recover this exact file from
GitHub and verify the certificate before building. If the fingerprint differs,
stop and report the mismatch rather than changing signing identities.

Before publishing any APK (including versions built in VS Code), verify the APK
certificate matches this fingerprint. Do not infer an APK's signature from its
filename or version. Fetch current GitHub state before choosing a new version;
respect versions already built locally by the user and do not overwrite their
APK or bump a version merely to update these instructions.
