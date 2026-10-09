# BarcodeAudi Android APK

Android-Studio-Projekt fuer einen nativen Android-Wrapper um eine lokale HTML-/JavaScript-App. Die App laedt `app/src/main/assets/index.html` in einer `WebView` und bringt die Android-spezifischen Dateifunktionen ueber `MainActivity.kt` mit.

## Relevante Projektbestandteile

- `app/` enthaelt den eigentlichen Android-App-Code und die Web-App-Assets.
- `gradle/`, `gradlew`, `gradlew.bat`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` gehoeren zum Build.
- `Privat/sync_version_and_build.ps1` und `Privat/sync_version_and_build.bat` erhoehen Version und bauen die Debug-APK.
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

### Codex / Linux-Cloud

Das Projekt wird in einem eigenen Ordner bearbeitet, getrennt von anderen Projekten.
Voraussetzungen: Java 17 oder 21, Python 3, Bash, curl und unzip.
Das Android-SDK liegt standardmaessig in `/workspace/android-sdk`; alternativ
kann `ANDROID_HOME` vor dem Setup gesetzt werden. Lokale SDK-Pfade und
Zugangsdaten werden nicht ins Repository aufgenommen.

Einmalig die Android-Werkzeuge installieren (akzeptiert die SDK-Lizenzen):

```bash
bash scripts/setup-cloud.sh
```

Aktuelle Version ohne Erhoehung bauen:

```bash
bash scripts/build-apk.sh
```

Naechste Version bauen und HTML/APK in `Privat/` archivieren:

```bash
python3 scripts/next-version.py
```

Dieses Skript erhoeht `versionCode`, `versionName`, die HTML-Versionsangaben
und beide Service-Worker-Caches gemeinsam. Gradle synchronisiert dabei `docs/`.
Bei einem fehlgeschlagenen Build werden die Versionsdateien zurueckgesetzt.
Fuer einen erneuten Build derselben Version `build-apk.sh` verwenden, damit
die Versionsnummer nicht erneut erhoeht wird.

Nach Pruefung der Aenderungen lassen sich Quellcode und die archivierte APK
mit `git add`, `git commit` und `git push origin main` hochladen.
Dafuer wird GitHub-Schreibzugriff benoetigt. Eine Debug-APK kann eine bereits
installierte App nur ersetzen, wenn beide mit demselben Schluessel signiert sind.
Der lokale Debug-Schluessel muss fuer spaetere Builds aufbewahrt werden;
er gehoert nicht ins Repository.

Debug-APK bauen:

```powershell
.\gradlew.bat assembleDebug
```

Dabei wird vor dem eigentlichen Android-Build die komplette Web-App aus
`app/src/main/assets/` automatisch nach `docs/` synchronisiert. Dadurch
verwenden APK/WebView und GitHub-Pages-/PWA-Version denselben Stand aller
Web-App-Dateien aus dem Assets-Ordner.

Wichtige Regel fuer Web-Aenderungen:

- Die bearbeitbare Quelle ist immer `app/src/main/assets/`.
- `docs/` ist die synchronisierte Auslieferung fuer GitHub Pages und installierte PWAs.
- `docs/` nie direkt bearbeiten, weil diese Dateien beim Sync aus `app/src/main/assets/` wieder ueberschrieben werden.
- Wenn sich gecachte Web-Dateien aendern, muss auch die Cache-Version in `app/src/main/assets/sw.js` wechseln. Das erledigt `Privat/sync_version_and_build.ps1` automatisch zusammen mit der Versionsnummer.

Die APK liegt danach typischerweise hier:

```text
app/build/outputs/apk/debug/BarcodeAudiScanner_ver<Version>.apk
```

## Version erhoehen und direkt bauen

Mit diesen Skripten wird:

- `versionCode` erhoeht
- `versionName` angepasst
- `data-app-version` in `app/src/main/assets/index.html` synchronisiert
- `APP_SHELL_CACHE` und `RUNTIME_CACHE` in `app/src/main/assets/sw.js` auf die neue Versionsnummer gesetzt
- anschliessend `assembleDebug` gestartet
- nach erfolgreichem Build eine Archivkopie in `Privat/` erstellt

Archiviert werden automatisch:

- `Privat/BarcodeAudiScanner_ver<Version>.html`
- `Privat/BarcodeAudiScanner_ver<Version>.apk`

Windows Batch:

```bat
.\Privat\sync_version_and_build.bat
```

PowerShell direkt:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Privat\sync_version_and_build.ps1
```

## Prompt fuer spaetere Web-/PWA-Updates

Wenn spaeter nur Web-App, PWA, Manifest, Vollbild oder Installations-Icons angepasst werden sollen, verwende zusaetzlich diese Vorlage:

- `Privat/Prompt_WebApp_PWA_Update_und_Cache.md`

Sie haelt den festen Ablauf fuer `app/src/main/assets`, `docs/`, `sw.js`, PWA-Cache und iPhone-Installationsicon fest.

## Entwicklerumgebung in VS Code

Die eigentliche App braucht VS Code nicht, aber fuer den Entwicklungsablauf ist lokal eine kleine VS-Code-Struktur sinnvoll. Dieses Muster kannst du auch in andere Projekte uebernehmen.

### Lokale VS-Code-Struktur

```text
.vscode/
  settings.json
  tasks.json
  launch.json
Privat/
  sync_version_and_build.bat
  sync_version_and_build.ps1
  VSCode_Vorlage/
    settings.json
    tasks.json
    launch.json
```

### Rolle der Dateien

- `.vscode/tasks.json` definiert die ausfuehrbaren Aufgaben in VS Code.
- `.vscode/settings.json` enthaelt die lokalen Editor-Einstellungen und die Statusleisten-Buttons.
- `.vscode/launch.json` ist aktuell nur ein Platzhalter.
- `Privat/sync_version_and_build.bat` ist der einfache Einstiegspunkt fuer Windows und ruft das PowerShell-Skript auf.
- `Privat/sync_version_and_build.ps1` macht die eigentliche Arbeit: Version erhoehen, `index.html` synchronisieren, APK bauen und danach die Archivkopien nach `Privat/` schreiben.
- `Privat/VSCode_Vorlage/` enthaelt die kopierbaren Vorlagen fuer neue Projekte.

### Eingerichtete Tasks

In `tasks.json` sind aktuell diese Tasks hinterlegt:

- `Build APK` fuehrt `.\gradlew.bat assembleDebug` aus
- `Sync Version & Build APK` fuehrt `.\Privat\sync_version_and_build.bat` aus

### Statusleisten-Buttons

In `settings.json` sind Buttons ueber `statusbar_command.commands` hinterlegt. Dadurch erscheinen unten in VS Code diese Schnellstarter:

- `Build APK`
- `Sync & Build APK`

Die Buttons starten intern einfach die beiden VS-Code-Tasks.

### Wenn du das in ein anderes Projekt uebernehmen willst

Kopiere oder baue dort dieselben Bausteine nach:

1. `Privat/sync_version_and_build.ps1`
2. `Privat/sync_version_and_build.bat`
3. `Privat/VSCode_Vorlage/tasks.json`
4. `Privat/VSCode_Vorlage/settings.json`
5. `Privat/VSCode_Vorlage/launch.json`

Danach musst du nur noch diese projektspezifischen Stellen anpassen:

- Pfad zu `app\build.gradle.kts`
- Pfad zu `app\src\main\assets\index.html`
- erwarteter APK-Ausgabeordner
- APK-Dateiname
- Zielnamen fuer die Archivkopien in `Privat/`

### Wichtig fuer dieses Repository

`.vscode/` ist in `.gitignore` absichtlich ignoriert. Die Entwicklerumgebung ist also lokal dokumentiert und nutzbar, gehoert aber nicht zwingend zum eigentlichen App-Code. Darum steht das Setup hier in der README, damit du es fuer andere Projekte trotzdem sauber nachbauen kannst.

Die versionierbare Quelle fuer neue Projekte liegt deshalb zusaetzlich in `Privat/VSCode_Vorlage/`. Von dort kannst du die Dateien in ein neues Projekt nach `.vscode/` kopieren.

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

## Updates ab Version 116

Der bestehende GitHub-Pages-Link und die Android-Package-ID bleiben erhalten.
Der Update-Button aktualisiert eine installierte Web-App im eigenen Fenster:
Web-App-Dateien und Icons werden erneuert, lokale Barcode-Daten bleiben erhalten.
Auf Android im Browser wird Installation oder weitere Nutzung in Chrome angeboten;
der echte Installationsdialog ist nur verfügbar, wenn Chrome ihn bereitstellt.

In der nativen APK ab Version 116 lädt der Update-Button die passende APK aus
`Privat/` und öffnet den Android-Installationsdialog. Falls erforderlich, muss die
Installation aus dieser Quelle erlaubt werden. Android prüft die Signatur; ein
Update benötigt denselben Signaturschlüssel wie die installierte APK. Abbrechen
oder ein fehlgeschlagener Download wird nicht als erfolgreiches Update gemeldet.
Ältere APKs benötigen einmalig den manuellen APK-Download, um diese native
Update-Funktion zu erhalten. Ein Wechsel zur Website aktualisiert keine APK.
Der ursprüngliche Schlüssel archivierter APKs ist in der Cloud nicht vorhanden;
Cloud-APKs ab Version 115 verwenden einen anderen, außerhalb von Git gespeicherten
Debug-Schlüssel und können ursprüngliche Installationen nicht direkt ersetzen.

## Teilen und Erstinstallation ab Version 125

Ein geteilter Link zeigt zuerst `teilen.html` mit der Visitenkarte. Mitgesendete
Daten hängen als URL-Fragment am selben Link; sie werden nicht als zweite Nachricht
versendet. Alte Links mit `?share=` bleiben lesbar. Der Messenger entscheidet, ob
und wie er die Open-Graph-Vorschau anzeigt.

Auf Android gibt es eine optionale Chrome-Weiterleitung mit Browser-Fallback und
einen APK-Download. Eine Webseite kann Chrome nicht erzwingen oder installieren.
Nach dem Öffnen der Web-App kommt die Installationsauswahl vor dem Datenimport.
Nicht gelesene Daten bleiben lokal für einen erneuten Versuch gespeichert. Ein
abgelehnter Import verwirft die vorgemerkten Daten.

Chrome und eine native APK haben getrennte Datenspeicher. Für eine neu installierte
APK kehrt der Empfänger zum ursprünglichen Karten-Link zurück und drückt
„Nach APK-Installation: Daten übernehmen“. Die APK ab Version 125 verarbeitet den
Import-Link und fragt vor der Übernahme nach Bestätigung. Ältere APKs unterstützen
diesen neuen Import-Link noch nicht. Der kleine Downloadpfeil neben dem Teilen-Button
lädt die APK auf Android auch ohne Web-App-Installationsangebot herunter.

Cloud-Testbuilds sind nicht die mit dem PC-Schlüssel signierten Veröffentlichungen.
Die veröffentlichte APK weiterhin auf dem PC bauen und vor dem Hochladen die
Signatur mit dem bisherigen PC-Zertifikat vergleichen.
