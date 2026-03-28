# START HIER

Diese Datei ist die Schnellanleitung fuer neue Projekte nach dem Muster von `BarcodeAudi_AndroidAPK`.

## Ziel

Du willst ein neues Projekt aufsetzen, das:

- eine lokale `index.html` hat
- im Browser funktioniert
- als Android-APK mit `WebView` laeuft
- Vollbild, Android-Bridge, Footer-Version und Build-Skripte hat
- seine Skripte und Vorlagen aus dem `Privat`-Ordner nutzt

## Wichtig

Kopiere **nicht einfach den ganzen `Privat`-Ordner blind** in ein neues Projekt.

Warum:

- dort liegen auch alte APKs
- dort liegen alte HTML-Archive
- dort liegen projektspezifische Exportdateien

Nutze `Privat` stattdessen als **Vorlagenquelle**.

## Schnellablauf

1. Neuen Projektordner anlegen.
2. In Android Studio dort ein einfaches Android-Projekt erstellen.
3. Danach die Vorlagen aus diesem Projekt in das neue Projekt kopieren.
4. Platzhalter anpassen.
5. Den Master-Prompt im neuen Projekt verwenden.
6. Build und Versionssync testen.

## Was du in das neue Projekt kopierst

### In den neuen Projektordner nach `Privat/`

- `Privat/sync_version_and_build.template.ps1`
- `Privat/sync_version_and_build.template.bat`
- `Privat/Prompt_Android_HTML_APK_Projektvorlage.md`
- `Privat/Vorlage_Android_HTML_APK_Referenz.md`

### In den neuen Projektordner nach `.vscode/`

Kopiere diese Vorlagen aus `Privat/VSCode_Vorlage/`:

- `tasks.json`
- `settings.json`
- `launch.json`

## Was du im neuen Projekt umbenennst

In `Privat/` des neuen Projekts:

- `sync_version_and_build.template.ps1` -> `sync_version_and_build.ps1`
- `sync_version_and_build.template.bat` -> `sync_version_and_build.bat`

## Was du im neuen Projekt anpassen musst

### In `Privat/sync_version_and_build.ps1`

Ersetze diese Platzhalter:

- `{{APK_BASENAME}}`
- `{{HTML_ARCHIVE_BASENAME}}`
- `{{APK_ARCHIVE_BASENAME}}`

### Im Master-Prompt

Ersetze diese Platzhalter:

- `{{PROJECT_NAME}}`
- `{{ROOT_PROJECT_NAME}}`
- `{{APP_NAME}}`
- `{{PACKAGE_NAME}}`
- `{{APK_BASENAME}}`
- `{{HTML_ARCHIVE_BASENAME}}`
- `{{APK_ARCHIVE_BASENAME}}`
- `{{APP_STORAGE_PREFIX}}`

## Was im neuen Projekt vorhanden sein muss

Diese Dinge muessen im neuen Projekt existieren oder von Codex/ChatGPT angelegt werden:

- `gradlew.bat`
- `app/build.gradle.kts`
- `app/src/main/assets/index.html`
- `app/build/outputs/apk/debug/`
- `Privat/`
- `.vscode/`

## So benutzt du den Prompt

1. Oeffne im neuen Projekt die Datei `Privat/Prompt_Android_HTML_APK_Projektvorlage.md`.
2. Ersetze alle Platzhalter.
3. Gib den kompletten Inhalt an Codex oder ChatGPT.
4. Sage zusaetzlich:

```text
Nutze in diesem Projekt den Privat-Ordner als Vorlage und Quelle fuer Build-Skripte und VS-Code-Setup. Die Build-Skripte sollen in Privat/ liegen und ueber .\Privat\sync_version_and_build.bat gestartet werden.
```

## Wichtige Regeln fuer das neue Projekt

- Die Skripte liegen immer in `Privat/`
- VS-Code-Dateien kommen aus `Privat/VSCode_Vorlage/`
- Der Start fuer Versionssync und Build ist immer:

```powershell
.\Privat\sync_version_and_build.bat
```

- Normales APK-Bauen geht ueber:

```powershell
.\gradlew.bat assembleDebug
```

## Was das Sync-Skript macht

`Privat/sync_version_and_build.bat` startet die PowerShell-Version.

`Privat/sync_version_and_build.ps1` macht dann:

- `versionCode` hochzaehlen
- `versionName` hochzaehlen
- `data-app-version` in `index.html` synchronisieren
- `assembleDebug` ausfuehren
- HTML nach `Privat/` kopieren
- APK nach `Privat/` kopieren

## Reihenfolge fuer ein neues Projekt

1. Neues Android-Projekt anlegen
2. `Privat/` im neuen Projekt anlegen
3. Vorlagen hineinkopieren
4. `.vscode/` aus `Privat/VSCode_Vorlage/` befuellen
5. Template-Skripte umbenennen
6. Platzhalter ersetzen
7. Prompt ausfuehren lassen
8. Testen:

```powershell
.\gradlew.bat assembleDebug
.\Privat\sync_version_and_build.bat
```

## Android-Voraussetzungen

Auf dem Rechner sollten installiert sein:

- Android Studio
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools
- Android SDK Platform-Tools

Falls noetig, braucht das Projekt auch `local.properties` mit `sdk.dir=...`.

## Mini-Checkliste am Ende

Pruefe nach der Einrichtung:

- Baut `.\gradlew.bat assembleDebug`?
- Laeuft `.\Privat\sync_version_and_build.bat`?
- Wird `data-app-version` in `index.html` aktualisiert?
- Wird die Footer-Version angezeigt?
- Wird HTML nach `Privat/` archiviert?
- Wird APK nach `Privat/` archiviert?
- Laeuft `index.html` im Browser?
- Laeuft die App in der APK?

## Merksatz

Fuer neue Projekte gilt:

- `Privat/` ist die Vorlagenzentrale
- `.vscode/` wird daraus befuellt
- Build-Skripte liegen in `Privat/`
- gestartet wird mit `.\Privat\sync_version_and_build.bat`
