# Master-Prompt fuer aehnliche Android-Projekte

Diesen Prompt kannst du in einem anderen Projekt oder in einem neuen Chat verwenden, wenn eine App dieselbe Grundidee, Struktur und Build-Logik wie dieses Repository bekommen soll.

## Copy-Paste-Prompt

```text
Erstelle oder refaktoriere mein Projekt nach dem Vorbild eines Android-APK-Wrappers fuer eine lokale HTML/JavaScript-App. Orientiere dich an folgendem Zielbild und setze es sauber, konsistent und produktionsnah um.

Ziel:
Ich moechte eine Android-App, die im Kern eine lokale Web-App aus dem assets-Ordner ausliefert. Die App soll dieselbe technische Idee, Ordnerstruktur und Build-Philosophie haben wie mein Referenzprojekt:
- Android Studio / Gradle Kotlin DSL
- eine `app`-Modulstruktur
- lokale Web-App in `app/src/main/assets/index.html`
- Start ueber eine native `MainActivity` mit `WebView`
- Export-/Import-Funktionen zwischen Web-App und Android
- Versionserhoehung ueber Skript
- automatische APK-Dateinamen mit Versionsnummer
- moeglichst offline-faehig

Arbeite bitte so, als ob du ein bestehendes Projekt auf dieses Muster bringst. Wenn etwas fehlt, lege es an. Wenn etwas schon existiert, passe es daran an, statt unnötig neu zu bauen.

Technisches Zielbild:

1. Android-Grundstruktur
- Verwende Gradle Kotlin DSL.
- Lege ein Android-App-Modul `app` an oder passe das vorhandene an.
- Nutze aktuelle, stabile AndroidX-/Material-Abhaengigkeiten, passend zum vorhandenen Projekt.
- Verwende `compileSdk` und `targetSdk` auf aktuellem Niveau.
- Nutze `minSdk`, das fuer WebView, Dateiauswahl und moderne Android-Speicherung sinnvoll ist.
- Aktiviere `viewBinding`.
- Verwende Java/Kotlin Target 17, wenn nichts im Projekt dagegen spricht.

2. Native App-Idee
- Die App ist ein nativer Android-Wrapper fuer eine HTML-App.
- `MainActivity` laedt `file:///android_asset/index.html`.
- Die App soll standardmaessig hochkant laufen.
- Ziehe eine `SwipeRefreshLayout`-Huelle um die WebView, damit die HTML-App neu geladen werden kann.
- Die Zurueck-Taste soll innerhalb der WebView zurueck navigieren, sonst die App schliessen.

3. WebView-Konfiguration
- Aktiviere JavaScript, DOM Storage, Datenbanknutzung, Datei- und Content-Zugriff.
- Erlaube sinnvolle Einstellungen fuer lokale Assets, Medien und gemischte Inhalte, wenn fuer das Projekt noetig.
- Implementiere einen `WebChromeClient` fuer Dateiauswahl.
- Implementiere einen `WebViewClient`, der lokale Zusatzdateien wie `manifest.webmanifest`, `sw.js` und Icons aus dem Assets-Ordner korrekt ausliefert.
- Achte darauf, dass die Web-App auch offline in der App stabil funktioniert.

4. Android-JavaScript-Bridge
- Stelle eine Android-Bridge bereit, zum Beispiel als `AndroidInterface`.
- Diese Bridge soll mindestens folgende Aufgaben abdecken, angepasst an mein Projekt:
  - Textdateien aus der Web-App in den Android-Download-Ordner speichern
  - Dateien aus der Web-App teilen/versenden
  - den nativen App-Namen an JavaScript liefern
  - die native App-Version an JavaScript liefern
  - die gebuendelte `index.html` bei Bedarf exportieren
  - optional native Steuerung fuer Bildschirmrotation oder Vollbildmodus
- Nutze `@JavascriptInterface` sauber und gezielt.

5. HTML-/Assets-Struktur
- Die Hauptlogik liegt in `app/src/main/assets/index.html`.
- Zusaetzlich koennen `manifest.webmanifest`, `sw.js` und ein `icons/`-Ordner im Assets-Bereich liegen.
- Die HTML-App soll so gebaut sein, dass sie sowohl im Browser als auch im Android-Wrapper sinnvoll funktioniert.
- Wenn Export-/Import-Funktionen existieren, sollen diese zuerst die Android-Bridge nutzen und sonst auf Browser-Fallbacks zurueckgreifen.

6. Persistenz und App-Zustand
- Verwende eine klare localStorage-Namensraum-Strategie, damit mehrere Projekte oder App-Varianten sich nicht gegenseitig in die Quere kommen.
- Trenne logisch zwischen:
  - eigentlichen Nutzdaten
  - UI-/Settings-Zustaenden
  - temporären oder internen Markern
- Export- und Importformate sollen die App-Version, den Namespace, Zeitpunkt und die enthaltenen Schluessel dokumentieren.

7. Versionierung
- Halte `versionCode` und `versionName` in `app/build.gradle.kts` synchron.
- Fuehre in `index.html` einen zentralen Marker wie `data-app-version="..."`.
- Lies die Versionsnummer in der Web-App bevorzugt nativ aus Android aus; verwende HTML-/Fallback-Werte nur als Reserve.
- Zeige die Version in der UI zentral an, zum Beispiel im Footer.
- Sorge dafuer, dass Dateinamen fuer Exportdateien und APKs die Versionsnummer automatisch enthalten.

8. Build- und Skriptlogik
- Lege ein PowerShell-Skript an, das:
  - `versionCode` hochzaehlt
  - `versionName` auf denselben Wert setzt
  - den HTML-Marker `data-app-version` aktualisiert
  - die urspruengliche Dateikodierung beim Lesen/Schreiben beibehält
  - danach `gradlew.bat assembleDebug` ausfuehrt
- Lege optional eine `.bat`-Datei an, die nur die PowerShell-Variante startet.
- Benenne die erzeugte Debug-APK automatisch nach dem Muster `<Projektname>_ver_<version>.apk`.

9. Branding und Wiederverwendbarkeit
- Das Projekt soll leicht auf andere Apps uebertragbar sein.
- Halte deshalb folgende Werte klar austauschbar:
  - App-Name
  - Paketname / Namespace
  - applicationId
  - Dateinamenschemata
  - localStorage-Namespace
  - Farben / Theme
  - Texte im Manifest und in der HTML
- Vermeide harte Altlasten oder verstreute Projektbezeichner.

10. Qualitaetsregeln
- Behalte vorhandene Dateien nur dann bei, wenn sie noch zum neuen Zielbild passen.
- Suche gezielt nach alten Versionsresten, Projektbezeichnern und Dateinamenschemata und bereinige sie konsistent.
- Achte besonders darauf, dass keine widerspruechlichen Versionswerte in:
  - Gradle
  - HTML
  - Service Worker
  - README
  - Export-Dateinamen
  - Fallback-Konstanten
  - privaten Hilfsordnern
  stehen bleiben.
- Wenn du Inkonsistenzen findest, vereinheitliche sie.

Bitte liefere am Ende:
1. eine kurze Zusammenfassung der Architektur,
2. die geaenderten Dateien,
3. den Build-/Versionsablauf,
4. Hinweise, was ich fuer andere Projekte nur noch anpassen muss.

Verwende fuer mein konkretes Projekt diese Platzhalter und ersetze sie passend:
- APP_NAME = <hier App-Name einsetzen>
- PACKAGE_NAME = <hier Paketname einsetzen>
- APPLICATION_ID = <hier applicationId einsetzen>
- APK_BASENAME = <hier APK-Dateibasis einsetzen>
- HTML_EXPORT_BASENAME = <hier HTML-Exportbasis einsetzen>
- DATA_EXPORT_BASENAME = <hier Daten-Exportbasis einsetzen>
- STORAGE_NAMESPACE = <hier localStorage-Prefix einsetzen>
- BRAND_PRIMARY = <hier Hauptfarbe einsetzen>
- BRAND_SECONDARY = <hier Sekundaerfarbe einsetzen>

Wenn bereits Dateien vorhanden sind, arbeite inkrementell und zerstoere keine funktionierenden Teile ohne Grund. Ziel ist ein sauberes, uebertragbares Projektmuster wie im Referenzprojekt: Android-Wrapper + lokale Web-App + Export/Import + Versionssync + automatisierter Build.
```

## Kurzbeschreibung des Referenzprojekts

Dieses Repository folgt im Kern diesem Muster:

- Android-App mit Kotlin und `WebView`, die lokal `index.html` aus den Assets laedt.
- `SwipeRefreshLayout` fuer Reload und einfache naturnahe Bedienung.
- JavaScript-Bridge zwischen HTML-App und Android fuer Speichern, Teilen, HTML-Export, App-Name und App-Version.
- Web-App als weitgehend eigenstaendige Single-File-App mit zusaetzlichen Offline-Dateien wie Manifest, Service Worker und Icons.
- Versionslogik in zwei Ebenen:
  - Android: `versionCode` und `versionName`
  - HTML: `data-app-version`
- PowerShell-Skript fuer robustes Hochzaehlen der Version mit Erhalt der Dateikodierung und anschliessendem Debug-Build.
- Automatischer APK-Dateiname mit Versionsnummer.
- Versionsbezogene Export-Dateinamen fuer Text, JSON und HTML.

## Was du bei anderen Projekten anpassen solltest

- App-Name, Package/Namespace und sichtbare Branding-Texte.
- Storage-Namespace, damit sich Projekte nicht gegenseitig Daten ueberschreiben.
- Dateinamenschemata fuer APK, HTML-Export und Nutzdaten-Export.
- Farben, Icons, Manifest-Daten und Footer-Texte.
- Eventuelle projektspezifische Bridge-Funktionen.
- Alle Fallback-Versionen oder Cache-Namen, damit keine alten Versionsreste bleiben.

## Wichtiger Hinweis

Im Referenzprojekt gibt es mehrere verstreute Versions- und Namensstellen. Beim Uebertragen auf neue Projekte sollte immer aktiv geprueft werden, ob wirklich alle davon angepasst wurden, vor allem:

- `app/build.gradle.kts`
- `app/src/main/assets/index.html`
- `app/src/main/assets/sw.js`
- `README_BUILD.txt`
- Export-Basisnamen in JavaScript
- statische Fallback-Konstanten in JavaScript
