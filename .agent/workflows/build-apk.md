---
description: Baut die aktuelle Android-Debug-APK des Projekts
---

### APK Build

1. Starte den Gradle-Build:
```powershell
.\gradlew.bat assembleDebug
```

2. Die fertige APK findest du dann hier:
`app/build/outputs/apk/debug/BarcodeAudi_ver_<Version>.apk`

3. Wenn gleichzeitig die Versionsnummer erhoeht werden soll:
```powershell
.\sync_version_and_build.bat
```
