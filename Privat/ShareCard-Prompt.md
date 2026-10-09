# Link-Kärtchen

`app/src/main/assets/icons/share-card.png` wurde mit dem eingebauten Imagegen-Werkzeug erstellt und auf 1200 × 630 Pixel skaliert.

Prompt: Das breite schwarze Visitenkärtchen aus dem Nutzerbild als Layoutvorlage verwenden. Glänzender schwarzer Rahmen mit silberner Kante und rotem Lichtpunkt. Links ein plastischer schwarzer Barcode-Button mit vier Audi-Ringen und rotem Scanstrahl. Rechts die Texte „Audi Barcode Scanner“, „Barcodes erstellen.“, „Scannen. Verwalten. Teilen.“ und „Jetzt öffnen & installieren“. Gut lesbar, ohne QR-Code. Verwendung als Vorschau eines anklickbaren App-Links, nicht als Bildanhang.

Beim Senden wird ausschließlich der anklickbare Link mit Beschreibung geteilt, kein Bildanhang. APK und Browser verwenden die kleine `teilen.html` mit statischen Open-Graph-Metadaten. WhatsApp und Telegram können daraus das Kärtchen als Link-Vorschau laden, auch ohne JavaScript auszuführen. `icons/share-card-preview.jpg` ist eine JPEG-Exportkopie des vorhandenen Kärtchens (1200 × 630, Qualität 82, etwa 88 KB). Die große PNG-Originaldatei bleibt erhalten.

Beim Öffnen leitet die Teilen-Seite per JavaScript zur App weiter und übernimmt Query-Parameter und Fragment unverändert, damit geteilte Barcodes und Einstellungen weiterhin importiert werden. Ohne JavaScript bleibt ein anklickbares Kärtchen mit „App öffnen“ sichtbar. Die QR-Vorschau bleibt ausschließlich im Teilen-Fenster. Die empfangende Anwendung entscheidet über Darstellung und Aktivierung der Link-Vorschau; bereits gesendete Nachrichten ändern sich nicht durch diese Reparatur.
