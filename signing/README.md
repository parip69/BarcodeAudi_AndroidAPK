# Gemeinsamer Entwicklungsschlüssel

Alle Debug-APKs verwenden automatisch `signing/barcodeaudi-debug.keystore`.
Die Konfiguration steht in `app/build.gradle.kts` und gilt für Android Studio,
VS Code/Codex, Windows (`gradlew.bat assembleDebug`) und die Cloud
(`bash scripts/build-apk.sh`). Es ist keine lokale Schlüsselkopie erforderlich.
Fehlt diese Datei, muss der Build fehlschlagen; keinen Ersatzschlüssel erzeugen.

Dieser Entwicklungsschlüssel ist auf ausdrücklichen Wunsch im Git-Repository
verfügbar. Passwort und Schlüsselpasswort: `android`; Alias: `androiddebugkey`.
Er ist öffentlich und eignet sich nicht als vertraulicher Produktionsschlüssel.

Zertifikat SHA-256:
`b46b439e36ed2de248b9085cb4b457b74ba70b5beb823962e9c931ce79104d28`

Er wurde am 25.10.2026 für diese Umgebung erzeugt. Er ist **nicht** der frühere
Cloud-/PC-Schlüssel. Die archivierte APK Version 128 hat den Fingerabdruck
`465acf7f29044ef722f92ecf0b9386bdf20ef638002ba52704093045962e7468`.
Ein direktes Android-Update von dieser alten Signatur auf die neue ist nicht
möglich. Vor einer Neuinstallation vorhandene Nutzerdaten exportieren/sichern.
Spätere Debug-Builds mit diesem gemeinsamen Schlüssel haben dieselbe Signatur.

Die APK-Signatur vor einer Veröffentlichung mit Android SDK `apksigner verify
--verbose --print-certs <apk>` prüfen. Diese Einrichtung veröffentlicht keine
neue APK und erhöht keine Versionsnummer.
