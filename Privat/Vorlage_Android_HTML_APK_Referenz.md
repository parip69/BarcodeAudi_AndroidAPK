# Referenz- und Checklisten-Vorlage fuer neue Android-HTML-APK-Projekte

Diese Datei beschreibt den technischen Soll-Zustand fuer neue Projekte, die nach dem Muster von `BarcodeAudi_AndroidAPK` aufgebaut werden sollen. Sie ist als Begleitdokument zum Master-Prompt gedacht und macht die Standards, Platzhalter und Abnahmekriterien eindeutig.

## Zweck

Neue Projekte sollen standardmaessig:

- eine lokale `index.html` als Kern haben
- direkt im Browser funktionieren
- als PWA/Standalone funktionieren
- als Android-APK mit `WebView` laufen
- eine Android-Bridge fuer Download, Share, HTML-Export und Vollbild haben
- eine Footer-Versionsanzeige besitzen
- ueber `Privat/sync_version_and_build.ps1` und `Privat/sync_version_and_build.bat` Version, HTML und APK synchronisieren
- Build-Artefakte nach `Privat/` archivieren
- eine lokale VS-Code-Entwicklerumgebung mit Tasks und Buttons mitbringen

## Platzhalter

- `{{PROJECT_NAME}}`: Projekt- oder Repository-Name
- `{{ROOT_PROJECT_NAME}}`: Name aus `settings.gradle.kts`
- `{{APP_NAME}}`: sichtbarer Name in Android und Web
- `{{PACKAGE_NAME}}`: Android-Paketname
- `{{APK_BASENAME}}`: APK-Basisname im Build-Ausgabeordner
- `{{HTML_ARCHIVE_BASENAME}}`: HTML-Basisname fuer Archivkopien in `Privat/`
- `{{APK_ARCHIVE_BASENAME}}`: APK-Basisname fuer Archivkopien in `Privat/`
- `{{APP_STORAGE_PREFIX}}`: Prefix fuer `localStorage`/`appStorage`

## Soll-Struktur

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
|-- Privat/
|   |-- sync_version_and_build.ps1
|   |-- sync_version_and_build.bat
|   |-- VSCode_Vorlage/
|   |   |-- settings.json
|   |   |-- tasks.json
|   |   `-- launch.json
|   |-- Prompt_Android_HTML_APK_Projektvorlage.md
|   |-- Prompt_WebApp_PWA_Update_und_Cache.md
|   `-- Vorlage_Android_HTML_APK_Referenz.md
|-- .vscode/
|   |-- settings.json
|   |-- tasks.json
|   `-- launch.json
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

## Feste technische Standards

### Android-Wrapper

- `MainActivity.kt` laedt `file:///android_asset/index.html`.
- `WebView` ist JavaScript-, DOM-Storage- und dateifaehig konfiguriert.
- `SwipeRefreshLayout` umschliesst die `WebView`.
- Die Activity nutzt immersives Vollbild.
- Portrait ist Standard; Rotation wird nur bei Bedarf ueber `setBarcodeFullscreenRotationEnabled(...)` freigegeben.
- `WebChromeClient` unterstuetzt Dateiauswahl fuer Upload-Faelle.
- `WebViewClient.shouldInterceptRequest(...)` liefert `manifest.webmanifest`, `sw.js` und Icon-Dateien sauber aus den Assets aus.

### Standardisierte Android-Bridge

Die Bridge heisst immer `AndroidInterface`.

Pflichtmethoden:

- `saveTextFile(fileName, content)`
- `shareTextFile(fileName, content)`
- `getBundledIndexHtml()`
- `getAppDisplayName()`
- `getAppVersionName()`
- `setBarcodeFullscreenRotationEnabled(enabled)`

Pflichtverhalten:

- Dateityp wird ueber die Dateiendung ermittelt.
- Speichern erfolgt auf Android 10+ ueber `MediaStore.Downloads`.
- Aeltere Android-Versionen erhalten einen praktikablen Fallback.
- Teilen laeuft ueber `FileProvider`.

### Manifest und Provider

Pflicht:

- `android.permission.INTERNET`
- `FileProvider` mit `${applicationId}.provider`
- `@xml/provider_paths`
- Launcher-Activity
- `singleTask`
- `hardwareAccelerated`
- `portrait`
- `adjustResize`

`provider_paths.xml` enthaelt:

- `external-path`
- `files-path`
- `cache-path`

### Web-/PWA-Seite

`index.html` muss in Browser, PWA und APK laufen.

Pflichtverhalten:

- ein eindeutiger `<html ... data-app-version="...">`-Marker
- Footer-Version wird daraus automatisch gesetzt
- Browser-/PWA-Installlogik ist vorhanden
- Android-Bridge wird bevorzugt genutzt, wenn verfuegbar
- Browser-Fallback bleibt funktionsfaehig, wenn keine Bridge existiert
- HTML-Export liest bevorzugt die gebuendelte Asset-Datei
- optionaler portabler Export schreibt projektbezogene Storage-Daten sicher in die exportierte HTML
- iPhone nutzt ein eigenes `apple-touch-icon.png` in `180x180`

### Web-Auslieferung und Cache-Standard

- Quelle der Web-App ist `app/src/main/assets/`.
- `docs/` ist die synchronisierte Auslieferung fuer GitHub Pages und installierte PWAs.
- `app/build.gradle.kts` enthaelt einen Sync-Task, der `app/src/main/assets/` vor `preBuild` nach `docs/` kopiert.
- `app/src/main/assets/sw.js` fuehrt `APP_SHELL_CACHE` und `RUNTIME_CACHE` mit einem Versionssuffix wie `-v47`.
- `Privat/sync_version_and_build.ps1` setzt diese Cache-Schluessel bei jeder neuen Version automatisch mit hoch.
- Installationsicons werden als Paket gepflegt:
  - `icons/icon-192.png`
  - `icons/icon-512.png`
  - `icons/apple-touch-icon.png`

### Standardisierte Web-Helfer

Die folgenden Funktionen sollen in der Web-App vorhanden oder aequivalent abgedeckt sein:

- `getAndroidBridge()`
- `downloadTextFileRobust(...)`
- `getBundledIndexHtmlText()`
- `supportsBundledIndexHtmlExport()`
- `updateBundledIndexHtmlExportVisibility()`
- `exportBundledIndexHtml()`

Kompatibilitaet:

- `getAndroidBridge()` darf optional auch ein Legacy-Objekt wie `window.AndroidDownload` mitberuecksichtigen.

## Versionsstandard

### Fuehrende Stellen

- `app/build.gradle.kts`: `versionCode = <Zahl>`
- `app/build.gradle.kts`: `versionName = "<Zahl>"`
- `app/src/main/assets/index.html`: `data-app-version="<Zahl>"`
- `app/src/main/assets/sw.js`: `APP_SHELL_CACHE` und `RUNTIME_CACHE` mit derselben Versionsnummer
- Footer-Anzeige ueber `footerVersion`

### Wichtige Regel fuer das Skript

Das PowerShell-Skript arbeitet regex-basiert. Deshalb muessen diese Bedingungen eingehalten werden:

- `versionCode` steht als direkte numerische Zuweisung in `app/build.gradle.kts`
- `versionName` steht als direkter numerischer String in `app/build.gradle.kts`
- `index.html` hat genau einen gut erkennbaren `data-app-version`-Marker im `<html>`-Tag

### Standard-Footer-Snippet

```html
<footer id="appFooter">
  Entwickelt von: <DEIN NAME ODER TEAM> - Ver.<span id="footerVersion"></span>
</footer>

<script>
  document.addEventListener("DOMContentLoaded", function () {
    var version = document.documentElement.getAttribute("data-app-version") || "?";
    var footerVersion = document.getElementById("footerVersion");
    if (footerVersion) footerVersion.textContent = version;
  });
</script>
```

## Build-, Naming- und Archivstandard

### Gradle-Defaults

- Android Gradle Plugin 8.x
- Kotlin-Android
- Java 17
- JVM Target 17
- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 24`
- `viewBinding = true`

### APK-Ausgabe

In `app/build.gradle.kts` soll der APK-Dateiname aktiv gesetzt werden:

```kotlin
output.outputFileName = "{{APK_BASENAME}}_ver_${versionName}.apk"
```

### Skriptverhalten

`Privat/sync_version_and_build.ps1` muss:

- Dateikodierung lesen und erhalten
- Version automatisch hochzaehlen
- `data-app-version` synchronisieren
- Service-Worker-Cache-Version synchronisieren
- `assembleDebug` starten
- `Privat/` bei Bedarf anlegen
- HTML und APK archivieren

### Archivnamen

- Build-APK: `{{APK_BASENAME}}_ver_<Version>.apk`
- HTML-Archiv: `{{HTML_ARCHIVE_BASENAME}}_ver_<Version>.html`
- APK-Archiv: `{{APK_ARCHIVE_BASENAME}}-v<Version>.apk`

## Entwicklerumgebung

### Voraussetzung

- Android Studio
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools
- Android SDK Platform-Tools
- Gradle Wrapper im Repository

### VS-Code-Standard

Die mitversionierte Quelle fuer neue Projekte liegt unter `Privat/VSCode_Vorlage/`.
Von dort werden die Dateien in das jeweilige Projekt nach `.vscode/` uebernommen.

`.vscode/tasks.json` enthaelt mindestens:

- `Build APK` -> `.\gradlew.bat assembleDebug`
- `Sync Version & Build APK` -> `.\Privat\sync_version_and_build.bat`

`.vscode/settings.json` enthaelt mindestens Statusleisten-Buttons fuer:

- `Build APK`
- `Sync & Build APK`

`launch.json` darf leer oder Platzhalter sein, soll aber als Teil des lokalen Setups vorhanden sein.

## Was pro Projekt umbenannt werden darf

- App-Name
- Paketname
- Root-Projektname
- Dateinamenbasis von APK und Archivkopien
- Storage-Prefix
- Texte, Branding, Farben, Icons, Fachlogik der HTML-App

## Was funktional gleich bleiben soll

- Versionsfuehrung ueber `versionCode`, `versionName`, `data-app-version` und Footer-Anzeige
- Android-Bridge unter dem Namen `AndroidInterface`
- Download/Share ueber native Bridge, wenn vorhanden
- Browser-Fallbacks, wenn die Bridge nicht vorhanden ist
- Vollbild-/Standalone-Grundverhalten
- zwei Skripte fuer Versionssync und Build
- Archivkopien nach `Privat/`
- VS-Code-Tasks und Statusleisten-Buttons

## Abnahmecheckliste

### Build und Versionssync

- `.\gradlew.bat assembleDebug` baut erfolgreich
- `.\Privat\sync_version_and_build.bat` erhoeht `versionCode` und `versionName`
- `data-app-version` wird auf dieselbe Version gesetzt
- `APP_SHELL_CACHE` und `RUNTIME_CACHE` werden auf dieselbe Version gesetzt
- die Build-APK hat das erwartete Namensmuster
- HTML und APK werden nach `Privat/` kopiert

### Android-App

- APK startet und laedt `index.html`
- Vollbildmodus ist aktiv
- Pull-to-Refresh funktioniert
- Dateiauswahl fuer Upload funktioniert
- Speichern in Downloads funktioniert
- Teilen als Datei funktioniert
- App-Name und Version koennen nativ ausgelesen werden

### Browser/PWA

- `index.html` laeuft direkt im Browser
- `manifest.webmanifest` ist erreichbar
- `sw.js` ist vorhanden
- Standalone-/Install-Hinweis funktioniert
- Download-Fallback funktioniert ohne Android-Bridge
- HTML-Export blendet sich im Browser sinnvoll ein oder aus
- `docs/` enthaelt nach dem Build denselben Stand wie `app/src/main/assets/`

### HTML-Version

- Footer zeigt die aktuelle Version aus `data-app-version`
- nach einem Skriptlauf stimmt die Footer-Version mit `versionName` ueberein
- nach einem Skriptlauf stimmt die Cache-Version in `sw.js` mit `versionName` ueberein

## Schnellstart fuer neue Projekte

1. Neues Android-Projekt oder bestehendes Zielprojekt vorbereiten.
2. Platzhalter im Master-Prompt ersetzen.
3. Master-Prompt in Codex oder ChatGPT einfuegen.
4. Umsetzung direkt im Zielprojekt erstellen lassen.
5. Danach `.\gradlew.bat assembleDebug` und `.\Privat\sync_version_and_build.bat` pruefen.
6. Browser-, PWA- und APK-Verhalten gegen die Abnahmecheckliste testen.

## Referenz aus diesem Projekt

Wenn du ein bestehendes Projekt gegen diese Vorlage pruefen willst, sind im aktuellen Repository besonders relevant:

- `app/src/main/java/de/parip69/barcodeaudiscanner/MainActivity.kt`
- `app/src/main/assets/index.html`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/provider_paths.xml`
- `app/build.gradle.kts`
- `Privat/sync_version_and_build.ps1`
- `.vscode/tasks.json`
- `.vscode/settings.json`
