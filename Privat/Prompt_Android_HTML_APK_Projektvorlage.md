# Master-Prompt fuer neue Android-HTML-APK-Projekte

Diese Vorlage ist fuer ein neues Projekt gedacht, das nach demselben Grundmuster wie `BarcodeAudi_AndroidAPK` aufgebaut werden soll: lokale `index.html`, Android-WebView-Wrapper, Vollbildmodus, Android-JavaScript-Bridge, PWA-/Standalone-Betrieb, Footer-Versionsanzeige, Versionssync ueber zwei Build-Skripte, Archivkopien nach `Privat/` und lokale VS-Code-Entwicklerumgebung.

## Verwendung

1. Ersetze die Platzhalter vor der Nutzung.
2. Fuege den kompletten Prompt in Codex oder ChatGPT ein.
3. Lasse die Umsetzung direkt im Zielprojekt durchfuehren, nicht nur beschreiben.

## Platzhalter

- `{{PROJECT_NAME}}`: Anzeigename der Projektvorlage oder des Repositories
- `{{ROOT_PROJECT_NAME}}`: `rootProject.name` in `settings.gradle.kts`
- `{{APP_NAME}}`: sichtbarer App-Name in Android und Web
- `{{PACKAGE_NAME}}`: Android-Paketname, z. B. `de.example.meineapp`
- `{{APK_BASENAME}}`: Basisname der APK im Build-Ordner
- `{{HTML_ARCHIVE_BASENAME}}`: Basisname der archivierten HTML-Datei in `Privat/`
- `{{APK_ARCHIVE_BASENAME}}`: Basisname der archivierten APK in `Privat/`
- `{{APP_STORAGE_PREFIX}}`: Prefix fuer Web-Storage-Schluessel

## Master-Prompt

Ich moechte, dass du in meinem neuen Projekt `{{PROJECT_NAME}}` eine Android-HTML-APK-Struktur nach dem Muster meines Referenzprojekts `BarcodeAudi_AndroidAPK` vollstaendig implementierst.

Bitte fuehre die Aenderungen direkt im Projekt aus und liefere nicht nur eine Beschreibung. Wenn Dateien fehlen, lege sie an. Wenn bereits passende Dateien existieren, passe sie sauber an, statt ein zweites Parallel-Setup zu bauen.

### Projektwerte

- Projektname: `{{PROJECT_NAME}}`
- Root-Projektname: `{{ROOT_PROJECT_NAME}}`
- App-Name: `{{APP_NAME}}`
- Paketname: `{{PACKAGE_NAME}}`
- APK-Basisname: `{{APK_BASENAME}}`
- HTML-Archiv-Basisname: `{{HTML_ARCHIVE_BASENAME}}`
- APK-Archiv-Basisname: `{{APK_ARCHIVE_BASENAME}}`
- Storage-Prefix: `{{APP_STORAGE_PREFIX}}`

### Zielbild

Baue eine lokale HTML-/JavaScript-App, die:

- direkt als `index.html` im Browser lauffaehig ist
- als PWA/Standalone installierbar ist
- in einer Android-APK ueber `WebView` geladen wird
- im Vollbildmodus sauber nutzbar ist
- eine native Android-Bridge fuer Speichern, Teilen, HTML-Export und Vollbild-Rotation besitzt
- in der HTML-Fusszeile immer die aktuelle Versionsnummer anzeigt
- `docs/` als synchronisierte Web-/GitHub-Pages-Auslieferung pflegt
- ueber zwei Build-Skripte Version, HTML und APK synchronisiert und archiviert

### Pflichtarchitektur

Lege oder pflege diese Struktur:

```text
.
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- local.properties
|-- docs/
|   |-- index.html
|   |-- manifest.webmanifest
|   |-- sw.js
|   `-- icons/
|-- .vscode/
|   |-- settings.json
|   |-- tasks.json
|   `-- launch.json
|-- Privat/
|   |-- sync_version_and_build.ps1
|   |-- sync_version_and_build.bat
|   `-- VSCode_Vorlage/
|       |-- settings.json
|       |-- tasks.json
|       `-- launch.json
`-- app/
    |-- build.gradle.kts
    `-- src/main/
        |-- AndroidManifest.xml
        |-- java/{{PACKAGE_NAME als Pfad}}/MainActivity.kt
        |-- assets/
        |   |-- index.html
        |   |-- manifest.webmanifest
        |   |-- sw.js
        |   `-- icons/
        |-- res/layout/activity_main.xml
        |-- res/xml/provider_paths.xml
        `-- res/values/strings.xml
```

### Android-Implementierung

Implementiere in `MainActivity.kt` mindestens diese Punkte:

- `WebView` mit aktivem JavaScript
- `domStorageEnabled = true`
- `databaseEnabled = true`
- `allowFileAccess = true`
- `allowContentAccess = true`
- `allowFileAccessFromFileURLs = true`
- `allowUniversalAccessFromFileURLs = true`
- `mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW`
- `SwipeRefreshLayout` um die `WebView`
- Laden von `file:///android_asset/index.html`
- `JavascriptInterface` mit dem Namen `AndroidInterface`
- native Methoden:
  - `saveTextFile(fileName, content)`
  - `shareTextFile(fileName, content)`
  - `getBundledIndexHtml()`
  - `getAppDisplayName()`
  - `getAppVersionName()`
  - `setBarcodeFullscreenRotationEnabled(enabled)`
- MIME-Type-Erkennung ueber Dateiendung
- Speichern in Downloads ueber `MediaStore.Downloads` auf Android 10+
- Fallback fuer aeltere Android-Versionen ohne veraltete Pflichtlogik fuer `WRITE_EXTERNAL_STORAGE`
- `FileProvider`-basierter Share-Intent
- Datei-Upload ueber `WebChromeClient` und Dateiauswahl
- Intercept fuer `manifest.webmanifest`, `sw.js` und Icons aus den Assets
- immersiven Vollbildmodus mit ausgeblendeten Systemleisten
- Portrait als Standard, Rotation nur ueber die Bridge steuerbar

### Manifest und Android-Ressourcen

Richte in `AndroidManifest.xml` mindestens ein:

- `android.permission.INTERNET`
- `FileProvider` mit `android:authorities="${applicationId}.provider"`
- `android:grantUriPermissions="true"`
- Verweis auf `@xml/provider_paths`
- `MainActivity` als Launcher-Activity
- `launchMode="singleTask"`
- `hardwareAccelerated="true"`
- `screenOrientation="portrait"`
- `windowSoftInputMode="adjustResize"`
- sinnvolle `configChanges` fuer stabile WebView-/Fullscreen-Nutzung

Lege `res/xml/provider_paths.xml` mit diesen Pfaden an:

- `external-path`
- `files-path`
- `cache-path`

Lege `res/layout/activity_main.xml` als `SwipeRefreshLayout` mit voller `WebView` an.

### HTML-/JavaScript-/PWA-Implementierung

Die `app/src/main/assets/index.html` muss sowohl im Browser als auch in der APK funktionieren.

Pflicht:

- Wurzel-Tag mit `data-app-version`, z. B. `<html lang="de" data-app-version="1">`
- Footer mit Versionsanzeige ueber `<span id="footerVersion"></span>`
- JavaScript, das beim Laden die Footer-Version aus `data-app-version` setzt
- `getAndroidBridge()`
- `downloadTextFileRobust(...)`
- `getBundledIndexHtmlText()`
- `supportsBundledIndexHtmlExport()`
- `updateBundledIndexHtmlExportVisibility()`
- `exportBundledIndexHtml()`
- Browser-Fallback fuer Downloads
- Browser-Fallback fuer Teilen, wenn sinnvoll
- PWA-Install-Erkennung
- Standalone-Erkennung
- Install-Hinweis oder Install-Button
- Vollbildverhalten fuer Browser/PWA und APK
- `apple-touch-icon` fuer iPhone mit `sizes="180x180"`

Quellenregel:

- Die editierbare Web-Quelle liegt in `app/src/main/assets/`.
- `docs/` ist nur die synchronisierte Auslieferung fuer GitHub Pages und installierte PWAs.

Bridge-bezogenes Verhalten:

- Wenn `window.AndroidInterface` verfuegbar ist, sollen Speichern, Teilen und HTML-Export bevorzugt nativ laufen.
- Wenn keine Android-Bridge vorhanden ist, muss die Seite im Browser trotzdem sinnvoll funktionieren.

Portable-HTML-Export:

- Beim HTML-Export soll die originale gebuendelte `index.html` gelesen werden, nicht nur der aktuelle DOM-Zustand.
- Optional soll ein Snapshot des relevanten `localStorage` in die exportierte HTML eingebettet werden.
- Nutze dafuer einen sicheren Bootstrap-Ansatz mit serialisiertem JSON und Marker-Key gegen Mehrfachanwendung.
- Verwende einen Projekt-Prefix fuer Storage-Schluessel: `{{APP_STORAGE_PREFIX}}`

### Versionsfuehrung

Die Version muss an genau diesen Stellen gefuehrt und synchronisiert werden:

- `app/build.gradle.kts`: numerische `versionCode`
- `app/build.gradle.kts`: numerische `versionName` als String
- `app/src/main/assets/index.html`: `data-app-version`
- `app/src/main/assets/sw.js`: Versionssuffix fuer `APP_SHELL_CACHE` und `RUNTIME_CACHE`
- HTML-Footer: Anzeige ueber `footerVersion`

Wichtig:

- `versionCode` und `versionName` muessen als einfache numerische Werte vorliegen, damit das Skript sie robust per Regex erhoehen kann.
- Der `<html>`-Tag muss genau einen `data-app-version`-Marker enthalten.

### Build- und Archiv-Skripte

Erstelle diese zwei Skripte:

- `Privat/sync_version_and_build.ps1`
- `Privat/sync_version_and_build.bat`

Das PowerShell-Skript muss:

- Dateikodierung robust lesen und beim Schreiben erhalten
- `app/build.gradle.kts` lesen
- `versionCode` und `versionName` um `1` erhoehen
- `data-app-version` in `app/src/main/assets/index.html` auf dieselbe Version setzen
- in `app/src/main/assets/sw.js` die Cache-Schluessel `APP_SHELL_CACHE` und `RUNTIME_CACHE` auf dieselbe Version hochziehen
- `.\gradlew.bat assembleDebug` starten
- `Privat/` bei Bedarf anlegen
- nach erfolgreichem Build die HTML-Datei nach `Privat/{{HTML_ARCHIVE_BASENAME}}_ver_<Version>.html` kopieren
- nach erfolgreichem Build die APK nach `Privat/{{APK_ARCHIVE_BASENAME}}-v<Version>.apk` kopieren
- bevorzugt die erwartete APK `app/build/outputs/apk/debug/{{APK_BASENAME}}_ver_<Version>.apk` verwenden
- falls diese APK nicht exakt gefunden wird, die neueste APK im Debug-Ausgabeordner als Fallback verwenden

Das Batch-Skript soll nur die PowerShell-Variante starten und den Exit-Code sauber durchreichen.
Die Skripte sollen standardmaessig aus `Privat/` heraus laufen und trotzdem das Projektroot sauber finden.

### Gradle- und Build-Setup

Verwende als Default:

- Android Gradle Plugin 8.x
- Kotlin-Android
- Java 17
- Kotlin JVM Target 17
- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 24`
- `viewBinding = true`

Lege in `app/build.gradle.kts` fest:

- APK-Dateiname im Build-Ordner: `{{APK_BASENAME}}_ver_${versionName}.apk`
- Task `syncDocsWebApp`, der `app/src/main/assets/` vor `preBuild` nach `docs/` synchronisiert

### Entwicklerumgebung

Richte lokale VS-Code-Dateien ein:

- `.vscode/tasks.json`
- `.vscode/settings.json`
- `.vscode/launch.json`
- fuehre diese am besten aus einer mitversionierten Vorlage unter `Privat/VSCode_Vorlage/` herbei

Pflicht-Tasks:

- `Build APK` -> `.\gradlew.bat assembleDebug`
- `Sync Version & Build APK` -> `.\Privat\sync_version_and_build.bat`

Pflicht-Buttons in `statusbar_command.commands`:

- `Build APK`
- `Sync & Build APK`

Dokumentiere oder beruecksichtige ausserdem:

- Android Studio
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools
- Android SDK Platform-Tools
- `local.properties` mit `sdk.dir=...`, falls Android Studio sie nicht selbst anlegt

### Namenskonventionen

Verwende diese Muster:

- APK im Build-Ordner: `{{APK_BASENAME}}_ver_<Version>.apk`
- HTML-Archiv in `Privat/`: `{{HTML_ARCHIVE_BASENAME}}_ver_<Version>.html`
- APK-Archiv in `Privat/`: `{{APK_ARCHIVE_BASENAME}}-v<Version>.apk`

### Vollbild-Standard

Das neue Projekt soll denselben Anspruch wie das Referenzprojekt haben:

- App startet im immersiven Vollbildmodus
- Browser/PWA soll Standalone/Vollbild bestmoeglich unterstuetzen
- Android-Seite darf die Rotation im Barcode-/Fullscreen-Modus gezielt aktivieren und danach wieder auf Portrait zurueckschalten
- PWA-/Browser-Modus soll einen klaren Install-/Vollbild-Hinweis haben
- installierte PWAs muessen ueber wechselnde Cache-Schluessel zuverlaessig neue Web-Staende laden

### Erwartete Lieferung

Bitte liefere am Ende:

1. die echten Datei-Aenderungen im Projekt
2. die Liste der geaenderten oder neu angelegten Dateien
3. eine kurze Erklaerung der wichtigsten Entscheidungen
4. eine kurze Testpruefung oder klare Angabe, was nicht getestet werden konnte

### Abnahmebedingungen

Das Ergebnis ist nur dann vollstaendig, wenn:

- `.\gradlew.bat assembleDebug` eine Debug-APK baut
- `.\Privat\sync_version_and_build.bat` Version und HTML synchronisiert
- HTML und APK nach erfolgreichem Build nach `Privat/` kopiert werden
- die Footer-Versionsnummer sichtbar aus `data-app-version` gelesen wird
- `index.html` im Browser ohne APK nutzbar bleibt
- die APK dieselbe `index.html` ueber `WebView` laedt
- Download, Share und HTML-Export nativ ueber die Bridge funktionieren
- Browser-Fallbacks fuer Nicht-APK-Betrieb vorhanden bleiben

Wenn im Zielprojekt bereits aehnliche Dateien vorhanden sind, integriere die Anforderungen sauber in die bestehende Struktur, statt eine zweite Loesung daneben zu legen.
