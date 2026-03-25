# Prompt fuer nativen HTML-Export aus einer Android-WebView-App

Ich habe eine Android-App, die eine lokale `index.html` in einer `WebView` laedt. Ich moechte dieselbe Export-/Share-Logik wie in meinem Projekt `BarcodeAudi_AndroidAPK` sauber in eine andere App uebernehmen.

Bitte implementiere mir das in meiner Ziel-App vollstaendig und nicht nur teilweise.

## Ziel

Ich brauche im Hamburger-Menue einen Menuepunkt `Index HTML exportieren`.

Beim Antippen soll:

1. die in der APK gebuendelte `index.html` gelesen werden
2. optional der aktuelle App-Zustand aus `localStorage` in diese HTML eingebettet werden
3. die fertige HTML-Datei nativ im Android-Download-Ordner gespeichert werden
4. bestehende Share-/Download-Funktionen fuer Text/JSON ebenfalls ueber dieselbe native Bridge laufen

## Wichtig

Bitte uebernimm nicht blind die komplette App, sondern nur die fuer diese Funktion wirklich noetigen Teile.

## Wirklich notwendige Android-Bestandteile

- `WebView` mit aktivem JavaScript und `domStorageEnabled = true`
- `addJavascriptInterface(...)` mit einer Bridge namens `AndroidInterface`
- eine native Methode `saveTextFile(fileName, content)` zum Speichern von Textdateien in Downloads
- eine native Methode `getBundledIndexHtml()` zum Lesen von `assets/index.html`
- fuer Share per Datei eine native Methode `shareTextFile(fileName, content)`
- `FileProvider` im `AndroidManifest.xml`
- `res/xml/provider_paths.xml`
- MIME-Type-Erkennung ueber Dateiendung
- Speichern ueber `MediaStore.Downloads` auf Android 10+ und Fallback fuer aeltere Android-Versionen
- moeglichst keine alte `WRITE_EXTERNAL_STORAGE`-Pflichtlogik verwenden

## Berechtigungen

Im Ausgangsprojekt ist fuer diese Exportfunktion keine zusaetzliche Speicherberechtigung eingetragen.

- vorhanden ist nur `android.permission.INTERNET`
- der Download nutzt `MediaStore` bzw. app-interne/externe App-Verzeichnisse
- fuer die hier umgesetzte Export-/Share-Logik ist daher keine separate klassische Schreibberechtigung vorgesehen

## Optional, aber sehr nuetzlich

- native Methoden `getAppDisplayName()` und `getAppVersionName()` fuer saubere Dateinamen
- automatische Sichtbarkeit des Menuepunkts nur dann, wenn die native Bridge verfuegbar ist
- Fallback im Browser ueber `fetch(currentUrl)` und normalen Download
- Einbetten von `localStorage` in die exportierte HTML, damit die Datei portabel ist

## Bereits vorhandene Projekt-Abhaengigkeiten

Wenn in meiner Ziel-App noch nichts davon vorhanden ist, nutze nur das Minimum.

Fuer diese Export-Funktion ist kein zusaetzliches Fremd-Framework notwendig. Im Ausgangsprojekt sind nur diese normalen Android-Abhaengigkeiten vorhanden:

- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.activity:activity-ktx`
- `androidx.swiperefreshlayout:swiperefreshlayout`

Davon ist fuer die Exportfunktion technisch hauptsaechlich relevant:

- `androidx.core` wegen `FileProvider`
- Standard-Android-APIs wie `WebView`, `MediaStore`, `Intent`, `MimeTypeMap`

## Technische Vorlage aus meinem bestehenden Projekt

Nutze diese Architektur als Vorbild:

### Android / Kotlin

In `MainActivity.kt` gibt es:

- `resolveMimeTypeForFileName(...)`
- `saveBytesToDownloads(...)`
- `AndroidInterface.saveTextFile(...)`
- `AndroidInterface.getBundledIndexHtml()`
- `AndroidInterface.getAppDisplayName()`
- `AndroidInterface.getAppVersionName()`
- `AndroidInterface.shareTextFile(...)`
- `webView.addJavascriptInterface(AndroidInterface(), "AndroidInterface")`

### Manifest

Im `AndroidManifest.xml` gibt es einen `FileProvider` mit:

- `android:authorities="${applicationId}.provider"`
- `android:grantUriPermissions="true"`
- Verweis auf `@xml/provider_paths`

### provider_paths.xml

Es werden diese Pfade bereitgestellt:

- `external-path`
- `files-path`
- `cache-path`

### JavaScript in index.html

Es gibt:

- einen Button `exportBundledIndexHtmlButton`
- `getAndroidBridge()`
- `supportsBundledIndexHtmlExport()`
- `updateBundledIndexHtmlExportVisibility()`
- `exportBundledIndexHtml()`
- `buildHtmlExportBaseName()`
- `buildHtmlExportFilename()`
- `buildPortableHtmlStatePayload()`
- `serializePortableHtmlPayload()`
- `embedPortableAppStateIntoHtml(htmlText)`
- `getBundledIndexHtmlText()`
- `downloadTextFileRobust(...)`

## Verhalten der Exportfunktion

Bitte setze das Verhalten genau so oder besser um:

1. Menuepunkt im Hamburger-Menue anlegen
2. Im Browser soll der Button ausgeblendet sein, in der Android-App sichtbar
3. Beim Export zuerst die originale gebuendelte `index.html` lesen, nicht den aktuell manipulierten DOM-Stand
4. Danach optional `localStorage`-Daten einbetten
5. Dateiname soll aus App-Name und Version aufgebaut werden, z. B. `<AppName>_ver_<Version>.html`
6. Speicherung soll bevorzugt nativ ueber die Android-Bridge laufen
7. Falls die native Bridge nicht verfuegbar ist, soll ein sauberer Browser-Fallback verwendet werden

## Portable-HTML-Logik

Wenn ich portable Exporte moechte, dann bitte folgenden Ansatz umsetzen:

- relevante `localStorage`-Keys einsammeln
- JSON-Payload erzeugen mit:
  - `app`
  - `version`
  - `namespace`
  - `exportedAt`
  - `entryCount`
  - `selectedKeys`
  - `storage`
- Payload sicher serialisieren, damit keine problematischen Zeichen das HTML kaputt machen
- ein kleines Bootstrap-`<script>` vor `</head>` oder `</body>` einfuegen
- dieses Script soll die gespeicherten Daten beim Oeffnen einmalig wieder in `localStorage` schreiben
- mit Marker-Key verhindern, dass derselbe Bootstrap mehrfach angewendet wird

## Typische Stolperstellen, die du bitte direkt beruecksichtigst

- `navigator.share` funktioniert in WebViews oft unzuverlaessig
- Dateidownload per `<a download>` klappt in Android-WebViews nicht immer sauber
- `FileProvider` fehlt haeufig oder ist falsch konfiguriert
- falscher MIME-Type fuehrt dazu, dass Mail/Share-Apps die Datei schlecht behandeln
- man darf nicht nur den DOM exportieren, sondern muss die echte Asset-HTML lesen
- `localStorage` darf beim Einbetten nicht unsicher per ungefiltertem Script eingeschleust werden
- Fallbacks fuer Browser und Android muessen getrennt behandelt werden
- Dateiname muss bereinigt werden

## Was ich als Ergebnis von dir erwarte

Bitte liefere:

1. die konkreten Codeaenderungen
2. die betroffenen Dateien
3. die komplette Android-Bridge
4. die Manifest-Ergaenzungen
5. die `provider_paths.xml`
6. die JavaScript-Funktionen fuer Export, Download und Share
7. eine kurze Erklaerung, welche Teile Pflicht sind und welche optional
8. eine kurze Test-Checkliste

## Referenz aus meinem aktuellen Projekt

Orientiere dich inhaltlich an diesen Stellen:

- `app/src/main/java/de/parip69/barcodeaudiscanner/MainActivity.kt`
- `app/src/main/assets/index.html`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/provider_paths.xml`
- `app/build.gradle.kts`

## Kurzfassung fuer die Umsetzung

Baue mir fuer eine Android-WebView-App einen nativen HTML-Export der gebuendelten `index.html` mit optional eingebettetem `localStorage`-Zustand. Verwende eine `JavascriptInterface`-Bridge namens `AndroidInterface`, `MediaStore` fuer Downloads, `FileProvider` fuer Share-Intents, MIME-Type-Erkennung anhand des Dateinamens und einen Browser-Fallback. Der Menuepunkt soll im Hamburger-Menue erscheinen und nur sichtbar sein, wenn die nativen Funktionen verfuegbar sind.
