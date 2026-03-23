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

## Entwicklerumgebung in VS Code

Die eigentliche App braucht VS Code nicht, aber fuer den Entwicklungsablauf ist lokal eine kleine VS-Code-Struktur sinnvoll. Dieses Muster kannst du auch in andere Projekte uebernehmen.

### Lokale VS-Code-Struktur

```text
.vscode/
  settings.json
  tasks.json
  launch.json
sync_version_and_build.bat
sync_version_and_build.ps1
```

### Rolle der Dateien

- `.vscode/tasks.json` definiert die ausfuehrbaren Aufgaben in VS Code.
- `.vscode/settings.json` enthaelt die lokalen Editor-Einstellungen und die Statusleisten-Buttons.
- `.vscode/launch.json` ist aktuell nur ein Platzhalter.
- `sync_version_and_build.bat` ist der einfache Einstiegspunkt fuer Windows und ruft das PowerShell-Skript auf.
- `sync_version_and_build.ps1` macht die eigentliche Arbeit: Version erhoehen, `index.html` synchronisieren, APK bauen und danach die Archivkopien nach `Privat/` schreiben.

### Eingerichtete Tasks

In `tasks.json` sind aktuell diese Tasks hinterlegt:

- `Build APK` fuehrt `.\gradlew.bat assembleDebug` aus
- `Sync Version & Build APK` fuehrt `.\sync_version_and_build.bat` aus

### Statusleisten-Buttons

In `settings.json` sind Buttons ueber `statusbar_command.commands` hinterlegt. Dadurch erscheinen unten in VS Code diese Schnellstarter:

- `Build APK`
- `Sync & Build APK`

Die Buttons starten intern einfach die beiden VS-Code-Tasks.

### Wenn du das in ein anderes Projekt uebernehmen willst

Kopiere oder baue dort dieselben Bausteine nach:

1. `sync_version_and_build.ps1`
2. `sync_version_and_build.bat`
3. `.vscode/tasks.json`
4. die relevanten Teile aus `.vscode/settings.json`, vor allem `statusbar_command.commands`

Danach musst du nur noch diese projektspezifischen Stellen anpassen:

- Pfad zu `app\build.gradle.kts`
- Pfad zu `app\src\main\assets\index.html`
- erwarteter APK-Ausgabeordner
- APK-Dateiname
- Zielnamen fuer die Archivkopien in `Privat/`

### Wichtig fuer dieses Repository

`.vscode/` ist in `.gitignore` absichtlich ignoriert. Die Entwicklerumgebung ist also lokal dokumentiert und nutzbar, gehoert aber nicht zwingend zum eigentlichen App-Code. Darum steht das Setup hier in der README, damit du es fuer andere Projekte trotzdem sauber nachbauen kannst.

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
