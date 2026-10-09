# BarcodeAudi Android APK

Work only in this repository; other projects in `/workspace` are independent.

The editable web source is `app/src/main/assets/`. Do not edit `docs/` directly:
Gradle synchronizes it from the assets during every build.

Cloud commands:

- Setup: `bash scripts/setup-cloud.sh`
- Rebuild current version: `bash scripts/build-apk.sh`
- Explicitly requested next version: `python3 scripts/next-version.py`

Before publishing, verify the APK using SDK `apksigner` and check its version
with `aapt dump badging`. Compare the signing certificate with the previous APK
and report any change, because Android requires matching certificates for updates.
Keep signing keys outside Git and preserve them across environment replacements.

Archive APK and HTML in `Privat/` as the version script does. Include the generated
`docs/` changes when uploading to GitHub. Never force-push. Do not claim a separate
Codex UI environment was created merely because this repository was cloned.
