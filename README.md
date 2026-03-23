# BarcodeAudi Android APK

Android-Studio-Projekt fuer einen nativen Android-Wrapper um eine lokale HTML-/JavaScript-App. Die App laedt `app/src/main/assets/index.html` in einer `WebView` und bringt die Android-spezifischen Dateifunktionen ueber `MainActivity.kt` mit.

## Relevante Projektbestandteile

- `app/` enthaelt den eigentlichen Android-App-Code und die Web-App-Assets.
- `gradle/`, `gradlew`, `gradlew.bat`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` gehoeren zum Build.
- `sync_version_and_build.ps1` und `sync_version_and_build.bat` erhoehen Version und bauen die Debug-APK.
- `.agent/` bleibt absichtlich im Repository, damit die Agenten-Workflows mitkommen.
- `Privat/` bleibt absichtlich im Repository, ist aber kein Teil des Gradle-Builds. Das ist eher Archiv-/Begleitmaterial.

## Was du nach dem Klonen brauchst

Du musst nur die Android-Build-Voraussetzungen installieren, nicht extra Gradle:

- Android Studio
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools
- Android SDK Platform-Tools

Gradle selbst musst du nicht separat installieren, weil der Gradle Wrapper schon im Projekt enthalten ist.

## Einmalig nach dem Klonen

### Variante A: Android Studio

1. Projektordner in Android Studio oeffnen
2. Gradle-Sync abwarten
3. Falls noetig im SDK Manager die fehlenden Android-SDK-Komponenten nachinstallieren

Android Studio legt `local.properties` normalerweise automatisch an.

### Variante B: Kommandozeile

Lege eine `local.properties` im Projektroot an, falls sie noch nicht existiert:

```properties
sdk.dir=C:\\AndroidSDK
```

Passe den Pfad an dein lokales Android-SDK an.

## Build

Debug-APK bauen:

```powershell
.\gradlew.bat assembleDebug
```

Die APK liegt danach typischerweise hier:

```text
app/build/outputs/apk/debug/BarcodeAudi_ver_<Version>.apk
```

## Version erhoehen und direkt bauen

Mit diesen Skripten wird:

- `versionCode` erhoeht
- `versionName` angepasst
- `data-app-version` in `app/src/main/assets/index.html` synchronisiert
- anschliessend `assembleDebug` gestartet
- nach erfolgreichem Build eine Archivkopie in `Privat/` erstellt

Archiviert werden automatisch:

- `Privat/BarcodeScannerAudi_ver_<Version>.html`
- `Privat/BarcodeAudiScanner-v<Version>.apk`

Windows Batch:

```bat
.\sync_version_and_build.bat
```

PowerShell direkt:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\sync_version_and_build.ps1
```

## Hinweis zur Repository-Struktur

Nicht mit ins Repository gehoeren und werden ignoriert:

- `.gradle/`
- `.kotlin/`
- `.idea/`
- `.vscode/`
- `build/`
- `app/build/`
- `app/.gradle/`
- `local.properties`

Damit bleibt das Repository beim Hochladen auf die wirklich relevanten Projektdateien reduziert.
