# Prompt fuer Web-App-, PWA- und Cache-Updates

Diese Vorlage ist fuer spaetere Aenderungen an der Web-App gedacht, wenn Browser, PWA, GitHub Pages, Vollbildmodus, Manifest oder Installations-Icons angepasst werden sollen.

## Wann du diesen Prompt nutzt

- wenn nur die Web-App geaendert werden soll
- wenn eine installierte PWA den neuen Stand sicher laden muss
- wenn Icon, Manifest oder Fullscreen geaendert werden
- wenn iPhone-/Safari-Installation mitgedacht werden muss

## Prompt

```text
Ich moechte in diesem Projekt die Web-App/PWA sauber aktualisieren.

Bitte fuehre die Aenderungen direkt im Projekt aus und beachte zwingend diese Regeln:

1. Die editierbare Quelle liegt in `app/src/main/assets/`.
2. `docs/` ist nur die synchronisierte Auslieferung fuer GitHub Pages und installierte PWAs. Bearbeite nicht nur `docs/`, wenn die eigentliche Quelle in `app/src/main/assets/` liegt.
3. Wenn sich eine gecachte Web-Datei aendert, muss auch die Cache-Version in `app/src/main/assets/sw.js` wechseln:
   - `APP_SHELL_CACHE`
   - `RUNTIME_CACHE`
4. Wenn die Aenderung als neue verteilte Version gedacht ist, bevorzuge `.\Privat\sync_version_and_build.bat`, damit `versionCode`, `versionName`, `data-app-version` und `sw.js` gemeinsam aktualisiert werden.
5. Bei Installations-Icons immer dieses Paket mitpruefen:
   - `app/src/main/assets/icons/icon-192.png`
   - `app/src/main/assets/icons/icon-512.png`
   - `app/src/main/assets/icons/apple-touch-icon.png`
6. Fuer iPhone/Safari muss `apple-touch-icon.png` in `180x180` vorhanden sein und in `index.html` mit `sizes="180x180"` verlinkt werden.
7. Bei Web-Fullscreen-/PWA-Aenderungen pruefe gemeinsam:
   - `app/src/main/assets/index.html`
   - `app/src/main/assets/manifest.webmanifest`
   - `app/src/main/assets/sw.js`
8. Fuehre nach den Aenderungen mindestens `.\gradlew.bat assembleDebug` aus, damit `docs/` aus den Assets synchronisiert wird.
9. Pruefe danach, dass der neue Stand auch in `docs/` angekommen ist.
10. Nenne am Ende die geaenderten Dateien, das Build-Ergebnis und ob die Web-App bei Icon-Aenderungen auf iPhone oder manchen Android-Launchern neu installiert werden sollte.
```

## Merksatz

- `app/src/main/assets/` ist die Quelle.
- `docs/` ist die Auslieferung.
- `sw.js` entscheidet, ob die PWA das Update wirklich zieht.
