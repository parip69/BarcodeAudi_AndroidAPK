# Prompt fuer gemeinsame Installations-/Startbildschirm-Hinweise in HTML + APK + PWA

Diese Vorlage ist fuer Projekte gedacht, bei denen **dieselbe `index.html`** gleichzeitig in mehreren Einsatzarten funktionieren soll:

- Android-APK mit `WebView`
- GitHub Pages / normaler Browser
- installierbare PWA
- iPhone/iPad mit Safari-Startbildschirm

Die Vorlage bildet genau das Muster ab, das wir in diesem Projekt umgesetzt haben:

- **eine gemeinsame HTML-Quelle**
- **keine stoerenden PWA-Hinweise in der APK**
- **echter Install-Button nur dort, wo der Browser ihn wirklich unterstuetzt**
- **iPhone/iPad mit klarer Home-Bildschirm-Anleitung**
- **In-App-Browser mit Hinweis, erst im normalen Browser zu oeffnen**
- **bestehende Overlay-/Startup-Logik nur minimal erweitern**

## Wann du diesen Prompt nutzen solltest

- wenn ein Projekt **eine gemeinsame Web-Oberflaeche fuer APK und Browser** nutzt
- wenn PWA-Installationshinweise bisher nur halb funktionieren
- wenn Android, iPhone und In-App-Browser unterschiedlich behandelt werden muessen
- wenn vorhandene Start-/Fullscreen-/Overlay-Logik **nicht neu gebaut**, sondern nur sauber erweitert werden soll

## Grundprinzip

1. **Nur eine Quelle pflegen**
   - Wenn das Projekt eine Android-App mit lokaler HTML-Datei hat, ist die Quelle normalerweise:
     - `app/src/main/assets/index.html`
   - `docs/` ist dann nur die synchronisierte Auslieferung fuer GitHub Pages/PWA.
   - Nicht nur `docs/index.html` bearbeiten, wenn die eigentliche Quelle unter `app/src/main/assets/` liegt.

2. **APK/WebView darf nicht gestoert werden**
   - Wenn die Seite in der Android-App/WebView laeuft, muessen Install-/Homescreen-Hinweise komplett verschwinden.
   - Kein Button `App installieren`
   - Kein Button `Zum Startbildschirm`
   - Kein PWA-Hinweis-Overlay nur fuer Browser-Faelle in der APK anzeigen

3. **Browser-Faelle sauber unterscheiden**
   - Android-Browser:
     - `beforeinstallprompt` verwenden
     - Install-Button nur anzeigen, wenn wirklich verfuegbar
   - iPhone/iPad:
     - kein echter PWA-Install-Prompt
     - stattdessen klarer Hinweis:
       - `In Safari oeffnen -> Teilen -> Zum Home-Bildschirm`
   - In-App-Browser:
     - kein Installieren
     - deutlicher Hinweis:
       - `Im normalen Browser oeffnen`
   - Desktop:
     - nur dann etwas anzeigen, wenn wirklich ein Prompt vorhanden ist

4. **Vorhandene Struktur behalten**
   - vorhandene Fullscreen-Logik nicht zerstoeren
   - vorhandene Startup-Overlays nicht doppelt aufbauen
   - vorhandene Buttons nicht unnoetig duplizieren
   - bestehende IDs und Texte weiterverwenden, wenn moeglich

## Prompt-Vorlage

```text
Ich moechte die gemeinsame index.html fuer Browser/PWA und Android-APK sauber auf denselben Install-/Startup-Standard bringen.

Bitte fuehre die Aenderungen direkt im Projekt aus und beachte zwingend diese Regeln:

1. Es gibt nur eine gemeinsame Quelle fuer die Web-Oberflaeche.
   - Wenn das Projekt `app/src/main/assets/index.html` nutzt, ist das die einzige editierbare Quelle.
   - `docs/` ist dann nur die synchronisierte Auslieferung fuer GitHub Pages/PWA.

2. Die bestehende Install-/PWA-/Startup-Logik soll NICHT neu erfunden werden.
   - Vorhandene Strukturen suchen und minimal-invasiv erweitern:
     - `beforeinstallprompt`
     - `deferredInstallPrompt`
     - `prompt...Install`
     - `has...InstallPrompt`
     - vorhandenes Startup-/Overlay-Element
     - vorhandener Install-Button
     - vorhandener Hinweistext

3. Es soll eine zentrale Umgebungs-Erkennung geben:
   - Android-App/WebView erkennen
   - iPhone/iPad erkennen
   - Android erkennen
   - In-App-Browser erkennen
   - Standalone/PWA-Modus erkennen

4. APK-/WebView-Fall:
   - In der Android-App/WebView duerfen keine PWA-/Install-Hinweise stoeren.
   - Install-/Homescreen-Hinweise komplett ausblenden.
   - Die restliche Seite muss unveraendert normal funktionieren.

5. Browser-/PWA-Fall:
   - Android-Browser:
     - echter Installieren-Button ueber `beforeinstallprompt`
     - aber nur anzeigen, wenn der Prompt wirklich verfuegbar ist
   - iPhone/iPad:
     - kein Install-Prompt
     - stattdessen klarer Hinweis:
       - `In Safari oeffnen -> Teilen -> Zum Home-Bildschirm`
   - In-App-Browser:
     - deutlicher Hinweis:
       - `Im normalen Browser oeffnen`
   - Desktop:
     - keine stoerenden Hinweise, ausser wenn wirklich ein Prompt vorhanden ist

6. Die bestehende Overlay-/Hint-Logik weiterverwenden.
   - Keine zweite Installations-UI daneben bauen.
   - Die vorhandenen Elemente dynamisch anpassen:
     - Text
     - Sichtbarkeit
     - Button-Beschriftung
     - Click-Verhalten

7. `beforeinstallprompt` so behandeln, dass APK/WebView nicht davon betroffen ist.
   - Nur fuer echte Browserfaelle relevant machen.

8. Einen zentralen UI-Status bauen, der mindestens diese Modi unterscheiden kann:
   - `apk`
   - `standalone`
   - `external-browser`
   - `ios`
   - `prompt`
   - optional `android-manual`
   - `unsupported`

9. Der Install-/Hint-Button soll je nach Modus passend reagieren:
   - `prompt` -> echten Install-Prompt anzeigen
   - `ios` -> Hinweis fuer Safari/Home-Bildschirm
   - `external-browser` -> Hinweis, erst im normalen Browser oeffnen
   - `apk`/`standalone` -> nichts stoerendes anzeigen

10. CSS nur minimal erweitern.
    - Kein Redesign
    - Nur z. B.:
      - `min-width`
      - `white-space: nowrap`
      - etwas robustere Buttonbreite fuer laengere Texte

11. Keine Funktion zerstoeren:
    - bestehende Fullscreen-Logik erhalten
    - bestehende Export-/Import-Logik erhalten
    - bestehende Android-Bridges erhalten
    - Manifest und PWA-Struktur nicht kaputtmachen

12. Wenn die Aenderung als neue verteilte Version gedacht ist:
    - HTML-Version / `data-app-version` erhoehen
    - wenn im Projekt ueblich, auch sichtbare Footer-Version mitziehen
    - `docs/` danach wieder aus der Quell-Datei synchronisieren

13. Am Ende bitte pruefen und berichten:
    - APK/WebView: kein Install-/Homescreen-Hinweis
    - Android Chrome: Installieren nur bei echtem Prompt
    - iPhone Safari: Home-Bildschirm-Hinweis
    - Telegram/WhatsApp/Facebook/Instagram In-App-Browser: Hinweis zum normalen Browser
    - bereits installierte PWA / Standalone: kein unnoetiger Install-Hinweis
    - Quelle und `docs/` synchron
```

## Praktische Suchbegriffe fuer bestehende Projekte

Mit diesen Begriffen findest du in anderen Projekten meist die richtigen Stellen:

- `beforeinstallprompt`
- `deferredInstallPrompt`
- `promptInstall`
- `hasInstallPrompt`
- `appinstalled`
- `startupHint`
- `fullscreenHint`
- `installButton`
- `installAppButton`
- `startupOverlay`
- `standalone`
- `display-mode`
- `AndroidInterface`
- `WebView`

## Empfohlene Erkennungslogik

Diese Muster haben sich bewaehrt und koennen je Projekt minimal angepasst werden:

```js
function isAndroidAppWebView() {
  try {
    if (window.AndroidInterface || window.AndroidDownload) return true;
  } catch (e) {}
  const currentUrl = String(window.location.href || "");
  const ua = navigator.userAgent || "";
  return (
    currentUrl.indexOf("file:///android_asset/") === 0 ||
    /\bwv\b/i.test(ua) ||
    /; wv\)/i.test(ua)
  );
}

function isIOSDevice() {
  const ua = navigator.userAgent || "";
  return /iphone|ipad|ipod/i.test(ua);
}

function isAndroidDevice() {
  const ua = navigator.userAgent || "";
  return /android/i.test(ua);
}

function isInAppBrowser() {
  const ua = navigator.userAgent || "";
  return /FBAN|FBAV|Instagram|Line|Telegram|TikTok|Messenger|WhatsApp|wv/i.test(ua);
}

function isStandaloneLikeMode() {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    window.matchMedia("(display-mode: minimal-ui)").matches ||
    window.navigator.standalone === true
  );
}
```

## Merksaetze

- **Eine HTML-Datei, mehrere Umgebungen**
- **APK/WebView nie mit PWA-Hinweisen nerven**
- **Installieren nur dort anbieten, wo der Browser es wirklich kann**
- **iPhone braucht Anleitung statt Prompt**
- **In-App-Browser braucht zuerst den Wechsel in den echten Browser**
- **Vorhandene Overlay-Struktur erweitern statt neu bauen**
- **Quelle zuerst, `docs/` nur synchronisieren**

