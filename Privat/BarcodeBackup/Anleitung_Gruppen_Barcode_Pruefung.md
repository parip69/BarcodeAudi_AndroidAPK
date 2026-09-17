# Gruppen, Barcode-Klammern und Prüfung deiner beiden Listen

Stand: 17.09.2026. Grundlage ist der aktuelle App-Quellstand Version 60 einschließlich eingebauter Hilfe, Bearbeitungsdialog, Parser, Import, Gruppenfilter und Vollbild-Paarung. Die HTML-Dateien der App, der Webversion und des Archivs Version 60 sind inhaltsgleich. Gruppenparser, Filter und Gruppenumschalter wurden auch mit dem Archiv Version 59 verglichen und sind in den untersuchten Abschnitten identisch.

Die App und die beiden Barcode-Dateien wurden nicht verändert. Dies ist eine Quellcode- und Dateiprüfung mit ausgeführten Prüfungen des originalen Parsers, kein Funktionstest auf einem Handy oder am betrieblichen Scanner. Ob Artikelnummern, Mengen, Anmeldedaten und Arbeitsplatzcodes betrieblich richtig sind, lässt sich ohne deren Stammdaten nicht bestätigen. Ein bereits auf deinem Handy ausgeführter Import ist aus den Dateien allein ebenfalls nicht rekonstruierbar.

## 1. Grundprinzip: Was kommt wohin?

Jeder Barcode-Eintrag in der Textdatei (TXT) muss **exakt auf einer Zeile** stehen und ist in drei Bereiche aufgeteilt. Das Trennzeichen ist der Schrägstrich `/` und die eckigen Klammern `[...]`.

**Aufbau in der Textdatei:**
```text
BARCODEWERT/Lesbare Bezeichnung [Befehl1, Befehl2, Befehl3]
```

- **Teil 1: Vor dem `/` (Der eigentliche Barcode)**
  Genau dieser Wert wird vom Scanner gelesen. Leerzeichen, Tabulatoren und Groß-/Kleinschreibung (wie `a` vs. `A`) sind hier extrem wichtig!
- **Teil 2: Nach dem `/` (Deine Bezeichnung)**
  Dieser Text wird dir in der App als Name angezeigt. Verwende hier **keine** weiteren Schrägstriche (`/`), sonst verschluckt sich die App!
- **Teil 3: In den `[...]` (Die Steuerbefehle)**
  Hier legst du fest, was die App mit dem Barcode machen soll (welche Gruppe, ob er gepaart ist etc.). Benutze immer nur **einen** Klammerblock am Ende der Zeile. Trenne die einzelnen Befehle mit einem Komma und einem Leerzeichen.

### Wie trage ich das in der App ein?

Wenn du einen Barcode in der App über den Stift-Button **✎** bearbeitest, siehst du **drei getrennte Felder**. Die App teilt die Textzeile für dich auf.

1. **Feld „Code vor /“:** Hier steht nur der Barcodewert. (z. B. `123456`)
2. **Feld „Bezeichnung nach /“:** Hier steht nur dein Name. (z. B. `Mein Material`)
3. **Feld „Inhalt der eckigen Klammern“:** Hier trägst du die Befehle ein – aber **ACHTUNG: OHNE die äußeren eckigen Klammern `[` und `]`!**

**Tipp für das 3. Feld in der App:** Du kannst hier für die Übersichtlichkeit jeden Befehl in eine neue Zeile schreiben. Die App macht beim Speichern automatisch Kommas daraus.

**Beispiel für die Eingabe im 3. Feld in der App:**
```text
P:M:EQ6L:@
&ZSB10:L
*:-
```
Wird in der Textdatei beim Export automatisch zu: `[P:M:EQ6L:@, &ZSB10:L, *:-]`

## 2. Alle relevanten Klammerbefehle

| Eingabe | Bedeutung und Anwendung |
|---|---|
| `&Andy:A` | Anzeigename Andy, interne Gruppenkennung `A`. |
| `&ZSB10:L` | Anzeigename ZSB10, interne Gruppenkennung `L`. |
| `&ZSB11:R` | Anzeigename ZSB11, interne Gruppenkennung `R`. |
| `&Porsche:P` | Anzeigename Porsche, interne Gruppenkennung `P`. |
| `&ZSB10:L, &ZSB11:R` | Ein Barcode gehört gleichzeitig zu beiden Gruppen. |
| `P:M:EQ6L` | Master/Hauptbarcode der Paarung `EQ6L`. |
| `P:M:EQ6L:@` | Derselbe Master, zusätzlich mit dem Haupt-Verfallsdatum im Vollbild. |
| `P:S:EQ6L` | Slave/Zusatzbarcode derselben Paarung, beispielsweise der Behälterbarcode. |
| `#` | Karte in der normalen Liste verstecken; über den Augen-Button wieder einblendbar. |
| `*:+` | Karte gehört zur Plus-Seite des Gruppenumschalters, beispielsweise Anmeldung/Arbeitsplatz. |
| `*:-` | Karte gehört zur Minus-Seite, beispielsweise Material und Behälter. |
| `pw:16.07.2026` | Startdatum einer festen 90-Tage-Frist für den Passwort-Hinweis. |
| `[]` | Sonderfall: Ein leerer Klammerblock wird ebenfalls als versteckbar behandelt. Besser ausdrücklich `[#]` verwenden. |

**Es gibt keinen eigenen Befehl `Part`.** Falls damit „Pair“/Paaren gemeint ist, sind dafür `P:M:...` und `P:S:...` zuständig. `M` bedeutet Master, `S` Slave. Die Paar-ID kannst du selbst wählen, beispielsweise `EQ6L` oder `PorscheR`.

### Was du nicht frei erfinden solltest

Unbekannter Text in Klammern wird nicht einfach immer ignoriert: Die App hat eine alte Paarungssyntax und kann freien Text als Paar-ID eines Slaves interpretieren. Schreibe Bemerkungen daher **außerhalb** der Klammern.

Die alte Syntax erkennt beispielsweise `[*123]` als Master und `[123]` beziehungsweise `[123:v]` als Slave zur ID `123`. `:v` wird dabei aus der Paar-ID entfernt; es ist im aktuellen Parser kein eigener Ausblendbefehl. Für neue Einträge die eindeutige `P:M:123`-/`P:S:123`-Schreibweise verwenden.

`@` ist kein frei stehender Datumsbefehl. Richtig ist `P:M:123:@`, nicht `P:M:123, @`. Das Beispiel `[P:M:123,@,#]` in der eingebauten Hilfe aktiviert die Datumsanzeige tatsächlich nicht. Keine Anführungszeichen oder weitere eckige Klammern um einzelne Befehle setzen.

## 3. Gruppen anlegen, zuweisen und entfernen

Eine Gruppe wird automatisch angelegt, sobald ein Barcode ihren `&...`-Eintrag enthält. Es gibt dafür keine notwendige separate Gruppenanlage.

1. Barcode mit ✎ öffnen.
2. Im Klammerfeld beispielsweise `&ZSB10:L` hinzufügen; vorhandene andere Befehle behalten.
3. Speichern.
4. Im Menü ☰ das Gruppen-Dropdown öffnen und „ZSB10 (L)“ wählen.

Für mehrere Gruppen mehrere `&`-Befehle eintragen:

```text
DEMO/Anmeldung [&Andy:A, &ZSB10:L, &ZSB11:R, &Porsche:P, *:+]
```

Um einen Barcode aus einer Gruppe zu entfernen, nur den betreffenden `&...`-Befehl löschen. Die anderen Gruppenzuweisungen und Paarungsbefehle bleiben erhalten. Wenn kein Barcode mehr zur Gruppe gehört, verschwindet sie aus dem Dropdown.

### Entscheidend: Die Kennung ist nur EIN Zeichen

Der Parser verwendet nach dem Doppelpunkt ausschließlich das erste Zeichen als Gruppenkennung:

| Eingabe | Tatsächliche Gruppe |
|---|---|
| `&ZSB10:L` | `L` |
| `&ZSB11:R` | `R` |
| `&Andy:A` | `A` |
| `&Syncrotess:Syncrotess` | `S`, keine lange ID „Syncrotess“ |
| `&Auto:Alpha1` | `A`, keine eigenständige Kennung „Alpha1“ |
| `&Gruppe:L1` und `&Gruppe:L2` | Beide Gruppe `L` |

Deshalb würden `&ZSB10` und `&ZSB11` ohne ausdrückliches `:L`/`:R` beide zur Gruppe `Z` gehören. Deine Unterscheidung `&ZSB10:L` und `&ZSB11:R` ist richtig und wichtig.

Verwende möglichst große Buchstaben A–Z, einen eindeutigen Buchstaben je Gruppe und denselben Namen für alle Einträge derselben Gruppe. Bei verschiedenen Namen mit gleicher Kennung gibt es im Dropdown trotzdem nur eine Gruppe; der zuerst gefundene Name wird angezeigt.

## 4. Gruppen benennen und umbenennen

### Nur den Namen ändern, Kennung behalten

Für deine ausdrücklich gesetzten Kennungen ist das gezielte Bearbeiten der betreffenden Einträge am zuverlässigsten:

```text
Vorher: &ZSB10:L
Nachher: &Audi links:L

Vorher: &ZSB11:R
Nachher: &Audi rechts:R
```

Die Änderung bei **allen** Barcodes der jeweiligen Gruppe durchführen, auch bei gemeinsam verwendeten Anmeldezeilen. `L` beziehungsweise `R` bleibt erhalten. Die Paar-IDs wie `EQ6L` nicht ändern, wenn nur der Gruppenname geändert werden soll.

### Menüfunktion „Gruppe umbenennen“

Die vorhandene Bedienung ist:

1. ☰ → „✏️ Gruppe umbenennen“.
2. Den bisherigen Gruppenbuchstaben eingeben, beispielsweise `L`.
3. Den neuen Namen eingeben.
4. Die angezeigte Änderung bestätigen.

**Wichtige Einschränkung im aktuellen Code:** Diese Funktion leitet den neuen Gruppenbuchstaben aus dem ersten Buchstaben des neuen Namens ab. Zusätzlich verliert sie bei normalen `&Name:Kennung`-Einträgen die explizite Kennung beim Neuschreiben. Aus `&ZSB10:L` kann bei einem neuen Namen „Links“ beispielsweise `&Links` werden. Damit gehört die Gruppe anschließend zu `L`; bei „Montage“ dagegen zu `M`.

Für „Audi links“ würde die Menüfunktion `A` wählen und bei deiner vorhandenen Gruppe Andy (`A`) einen Kennungskonflikt melden. Sie ist also keine reine Umbenennung bei unveränderlicher Kennung. Für deine ZSB-Gruppen die oben beschriebene Änderung im Klammerfeld verwenden. Vor umfangreichen Änderungen die aktuelle Liste exportieren.

## 5. Paaren: Material, Datum und Behälter zusammen anzeigen

Beispiel mit absichtlich erfundenen Barcode-Werten:

```text
MATERIAL-DEMO/Material links [P:M:EQ6L:@, &ZSB10:L, *:-]
BEHAELTER-DEMO/Behälter links [P:S:EQ6L, &ZSB10:L, #, *:-]
```

Beim Öffnen des Masters im Vollbild sucht die App in der gesamten Barcode-Liste den passenden Slave und zeigt ihn zusätzlich an. `#` versteckt dessen eigene Listenkarte; die Paarung bleibt nutzbar.

- Die Paar-ID hinter `P:M:` und `P:S:` muss exakt gleich sein. `EQ6L` und `eq6l` sind unterschiedliche Paarungen.
- Gruppenkennung und Paar-ID sind unabhängig: `L` ist die Gruppe; `EQ6L` ist die Verbindung zweier Barcodes.
- Die Suche nach dem Slave erfolgt über die gesamte Liste, nicht nur innerhalb der ausgewählten Gruppe.
- Pro Master wird nur der **erste passende Slave** angezeigt. Mehrere Slaves mit gleicher ID werden nicht alle zusammen angezeigt.
- Mehrere Master dürfen dieselbe ID verwenden. Dann zeigen alle denselben ersten Slave. Wird ein Slave direkt geöffnet, sucht die App den ersten passenden Master; dieser muss in der aktuellen Vollbild-Navigationsliste verfügbar sein.
- Für neue Daten möglichst eine klare Master-/Slave-Zuordnung je Paar-ID verwenden.
- Nur einen `P:...`-Befehl pro Barcode verwenden. Mehrere solche Befehle ergeben keine Mehrfachpaarung; spätere Angaben überschreiben die vorherige Auswertung.
- Den `P:...`-Befehl zuerst schreiben, danach Gruppen, `#` und gegebenenfalls `*:-`, wie in deinen Dateien.

### Was `:@` und `pw:` unterscheidet

`P:M:EQ6L:@` zeigt das allgemeine Haupt-Verfallsdatum im Vollbild. Es folgt dem eingestellten Tagesabstand, standardmäßig heute plus 84 Tage. `84` wird dafür nicht in die Klammer geschrieben; der Wert ist eine separate App-Einstellung.

`pw:16.07.2026` setzt dagegen das **Startdatum** der Passwortfrist. Die App addiert fest 90 Tage: in diesem Beispiel ergibt das den 14.10.2026. Es ist weder der Passwortinhalt noch eine Zugangs- oder Sperreinstellung. Für eine Aktualisierung kann der Datums-/Resttage-Button an der Karte verwendet werden. Ein Datum ausdrücklich als `TT.MM.JJJJ` eintragen; auf ein leeres `pw:` beim TXT-Import nicht verlassen.

## 6. Verstecken, Anzeigen und Plus/Minus umschalten

### Dauerhaft als versteckt markieren: `#`

```text
DEMO/Versteckter Eintrag [&ZSB10:L, #]
```

Über ☰ und den Augen-Button „Ausgeblendete …“ werden solche Karten vorübergehend ein- oder ausgeblendet. Um die Markierung dauerhaft aufzuheben, `#` aus dem Eintrag entfernen und speichern. `P:S:...` allein versteckt eine Karte nicht automatisch; deshalb ist `#` bei deinen Behältern sinnvoll.

Das ist eine Anzeigeoption, kein Schutz vertraulicher Daten. Die Daten bleiben gespeichert und gegebenenfalls im Export enthalten.

### Zwei Ansichten innerhalb derselben Gruppe: `*:+` und `*:-`

```text
LOGIN-DEMO/Anmeldung [&ZSB10:L, *:+]
MATERIAL-DEMO/Material [P:M:EQ6L:@, &ZSB10:L, *:-]
BEHAELTER-DEMO/Behälter [P:S:EQ6L, &ZSB10:L, #, *:-]
```

Die Absicht ist: Plus-Seite für Anmeldung/Arbeitsplatz, Minus-Seite für Material. Wähle zuerst eine einzelne Gruppe im Dropdown. Wische dann auf einer Karte mit `*:+` oder `*:-` deutlich nach links oder rechts. Der Handler verlangt eine überwiegend waagerechte Bewegung von mehr als 50 Pixeln. Er wechselt den Zustand der ausgewählten Gruppe.

Ein Eintrag ohne `*:+`/`*:-` wird durch diesen Umschalter nicht verändert. Mit `#` bleibt eine Karte zusätzlich ausgeblendet, bis die Ausblendmarkierung vorübergehend oder dauerhaft aufgehoben wird. Plus/Minus löscht oder verändert keine Barcode-Werte und ist auch kein Paarungsbefehl.

### Grenzen der aktuellen Umsetzung

- Der interne Startzustand ist Plus; die Umschaltzustände werden nicht dauerhaft im lokalen Speicher gesichert.
- Der Gruppenfilter setzt die Sichtbarkeit beim Filtern neu und berücksichtigt dabei Plus/Minus nicht. Nach Gruppenwechsel oder neuem Laden können deshalb beide Seiten gleichzeitig erscheinen. Nicht mit einer dauerhaft strikt getrennten Ansicht rechnen.
- Der Augen-Button arbeitet über alle als versteckbar markierten Karten und prüft beim Einblenden nicht erneut Gruppe oder Plus/Minus. Dadurch können auch Karten anderer Gruppen erscheinen. Anschließend die gewünschte Gruppe erneut auswählen.
- Ohne ausgewählte Einzelgruppe schaltet die Wischgeste keine Gruppe um. In „Alle Gruppen“ verwendet die anfängliche Marker-Auswertung gegebenenfalls die erste Gruppe eines Eintrags.
- Ein Kommentar im Quellcode erwähnt Doppelklick. Ein entsprechender `dblclick`-Handler wurde nicht gefunden. Die belegte Bedienung ist waagerechtes Wischen.

## 7. Reihenfolge und Gruppenbereich

Wähle eine einzelne Gruppe und nutze die Pfeile ↑/↓ an den Barcode-Karten. Die App speichert dafür eine eigene Reihenfolge je Gruppe. Ohne gespeicherte Gruppenreihenfolge nimmt sie die Reihenfolge aus der Hauptliste, auf die Gruppe eingeschränkt. Der Listen-/Cache-Editor dient unter anderem zum Bearbeiten der Hauptliste.

`&ZSB10:L1`, `&ZSB10:L2` usw. werden zwar mit einem Zahlenanteil eingelesen, aber der aktuelle Filter sortiert **nicht nach diesen Zahlen**. Benutze die Pfeile; die Zahlen sind kein verlässlicher Sortierbefehl.

„Gruppenbereich fixiert“ merkt sich, ob der betreffende Bereich offen oder geschlossen ist. Es ändert weder Gruppenzuweisungen noch Paarungen. Auch das Ein-/Ausblenden des gesamten Bereichs ersetzt kein `#` an einzelnen Barcodes.

## 8. Ergebnis der Prüfung deiner Dateien

Geprüft wurden:

- `Privat/BarcodeBackup/BarcodeAudi_ver_59_Andy.txt`: **38 Einträge**.
- `Privat/BarcodeBackup/BarcodeAudi_verAndy_38.txt`: **14 Einträge**.

Die Nummern in den Dateinamen sind nicht die Anzahl der Einträge. Beide Dateien lassen sich als UTF-8 lesen; Umlaute sind korrekt enthalten. Sichtbare fehlerhafte Umlaute bei einer anders eingestellten Konsole sind kein Beleg für beschädigte Dateien.

### Gruppen in der 59-Datei

| Gruppe | Kennung | Zahl der zugewiesenen Einträge |
|---|---|---:|
| Andy | A | 12 |
| ZSB10 | L | 9 |
| ZSB11 | R | 16 |
| Porsche | P | 12 |
| Syncrotess | S | 2 |

Mehrfachzuweisungen zählen in jeder betroffenen Gruppe. Zeile 37 hat keine Gruppe. Die 38-Datei verwendet nur Andy/A mit 12 Einträgen; Zeilen 1 und 3 haben keine Gruppe.

### Paarungen in der 59-Datei

| Paar-ID | Master-Zeilen | Slave-Zeile | Ergebnis |
|---|---|---|---|
| EQ6L | 15 | 17 | Passend vorhanden |
| A6L | 16 | 18 | Passend vorhanden |
| EQ6R | 20 | 24 | Passend vorhanden |
| A6R | 21 | 25 | Passend vorhanden |
| TorsREQ6 | 22 | 26 | Passend vorhanden |
| TorsRA6 | 23 | 27 | Passend vorhanden |
| PorscheL | 29 und 31 | 33 | LOW und HIGH teilen denselben Behälter |
| PorscheR | 30 und 32 | 34 | LOW und HIGH teilen denselben Behälter |

Alle ausdrücklich angegebenen Master-Paarungen haben einen Slave, alle Slaves einen Master. Die Datei 38 enthält keine ausdrücklichen `P:M:`-/`P:S:`-Paarungen.

### Punkte, die du fachlich prüfen solltest

1. **Linke Audi-Materialien zusätzlich in der rechten Gruppe:** Zeilen 15 und 16 sind sowohl `L` als auch `R` zugeordnet. Ihre Behälter in Zeilen 17 und 18 gehören nur zu `L`. Das ist technisch erlaubt; im Vollbild kann der Slave trotzdem aus der Gesamtliste gefunden werden. Wenn rechts ausschließlich rechte Teile erscheinen sollen, wäre die zusätzliche R-Zuweisung bei den beiden linken Mastern nicht gewünscht.
2. **Porsche LOW/HIGH:** Beide Varianten einer Seite teilen sich denselben Slave. Das passt nur, wenn wirklich derselbe Behälterbarcode verwendet werden soll. Die Slave-Bezeichnungen nennen derzeit nur LOW. Falls HIGH andere Behälter braucht, benötigt HIGH eigene Paar-IDs und passende Slaves.
3. **Tabulatoren und Leerzeichen:** Die Behälterzeile 33 verwendet zwischen `1` und `10` einen Tabulator; vergleichbare Zeilen verwenden dort ein Leerzeichen. Das ist ein echter Unterschied im gescannten Wert. Nicht automatisch vereinheitlichen; am Zielsystem prüfen, ob ein Feldwechsel oder ein Leerzeichen benötigt wird.
4. **Groß-/Kleinschreibung:** Zeile 10 beginnt im Code mit `Ft.`, Zeile 37 mit `ft.`. Die App betrachtet sie als unterschiedliche Werte. Auch das kleine `a` am Ende des Materialcodes in Zeile 22 ist Bestandteil des Barcodes.
5. **Passwortdatum:** Zeile 3 der 59-Datei verwendet den 09.06.2026 → Fristende 07.09.2026. Die 38-Datei verwendet in Zeile 3 den 16.07.2026 → Fristende 14.10.2026. Am Prüfdatum 17.09.2026 liegt nur die erste Frist bereits zurück. Welches Startdatum sachlich stimmt, kann nur deine tatsächliche Passwortänderung klären.

### Gleiche Barcode-Werte innerhalb der 59-Datei

| Zeilen | Unterschied / Bedeutung |
|---|---|
| 7 und 28 | Derselbe Arbeitsplatzcode einmal in Andy, einmal in Porsche. |
| 8 und 13 | Dieselbe Einlagerungsfunktion mit unterschiedlichen Gruppenzuweisungen. |
| 24 und 34 | Derselbe Behälterwert für Audi rechts und Porsche rechts, aber unterschiedliche Paar-IDs. |
| 35 und 36 | Identischer Syncrotess-Code einschließlich abschließendem Tabulator; unterschiedliche Beschriftung. |

Das sind keine vollständig identischen Zeilen und nicht automatisch Fehler. Bei interaktivem Import findet die App bei gleichem Barcode-Wert jedoch nur den ersten vorhandenen Treffer. Deshalb bewusst entscheiden, ob ein weiterer Eintrag erhalten bleiben soll.

## 9. Passt die ergänzte 38-Datei zur ursprünglichen 59-Datei?

**Als ungeprüfte Ergänzung oder vollständiger Ersatz passt sie nicht sauber.** Sie enthält überwiegend bereits vorhandene Daten, zwei Einträge mit weniger Gruppenzuweisungen sowie Unterschiede bei Schreibweise und Tabulatoren.

| Zeile der 38-Datei | Vergleich zur 59-Datei | Empfehlung beim Ergänzen |
|---|---|---|
| 1 | `Cicsl` statt `cicsl` in 59/1; ohne Gruppe oder Plus-Marker. Kein exakter Code-Treffer. | Nicht blind hinzufügen. Prüfen, ob Großschreibung wirklich nötig ist; andernfalls bestehende gemeinsame Anmeldezeile behalten. |
| 2 | Gleicher Code wie 59/2, aber nur Andy statt A/L/R/P und ohne `*:+`. | Überspringen, wenn die bisherige gemeinsame Anmeldung bleiben soll. |
| 3 | Gleicher Code wie 59/3; anderes Passwortdatum, keine Gruppen und kein `*:+`. | Nicht unverändert ersetzen. Falls das Datum stimmt, nur das Datum im bestehenden Eintrag aktualisieren. |
| 4–12 | Neun Zeilen vollständig identisch zu 59/4–12. | Überspringen. |
| 13 | Syncrotess-Anmeldung ohne den abschließenden Tabulator aus 59/35–36, außerdem Andy statt Syncrotess. | Zuerst gewünschtes TAB-Verhalten und Gruppenzuordnung klären; technisch wird sie als neuer Wert erkannt. |
| 14 | Zweite Syncrotess-Anmeldung, deren Barcode-Wert in der 59-Datei fehlt. Gruppe Andy. | Hinzufügen, wenn dieser zweite Zugang tatsächlich gebraucht wird. |

Die App würde beim interaktiven Vergleich gegen die unveränderte 59-Liste **11 vorhandene Barcode-Werte** und **3 neue Werte** erkennen. Von den 11 vorhandenen sind neun komplette Zeilen identisch. Die drei als neu erkannten Werte sind die Zeilen 1, 13 und 14 — „neu erkannt“ heißt hier nicht automatisch „inhaltlich neu benötigt“.

Bei einfachem Aneinanderhängen beider Dateien entstehen **52 Einträge**, darunter neun vollständige Zeilenduplikate. Wenn „Liste komplett übernehmen“ mit der 38-Datei verwendet wird, wird die Liste dagegen durch deren 14 Einträge ersetzt; die Material-/Behälterstruktur der 59-Datei ist darin nicht enthalten.

## 10. So ergänzst du gezielt, ohne die Gruppenstruktur zu verlieren

1. Die aktuell verwendete Barcode-Liste exportieren. Für eine Sicherung einschließlich Gruppenreihenfolgen und sonstiger lokaler Einstellungen zusätzlich die lokale Daten-/Settings-Sicherung verwenden; die reine TXT-Liste enthält nicht alle Anzeigeeinstellungen.
2. Für die 38-Datei beim Barcode-Import **„Einzeln prüfen“** wählen.
3. Die neun identischen Zeilen 4–12 überspringen.
4. Den vorhandenen Login in Zeile 2 behalten, wenn er weiterhin A/L/R/P zugewiesen sein soll.
5. Das Passwortdatum gegebenenfalls am bestehenden Eintrag ändern. Dessen Gruppen und Plus-Marker behalten. Beispiel für den Klammerinhalt, wenn der 16.07.2026 tatsächlich richtig ist:

   ```text
   &Andy:A, &ZSB10:L, &ZSB11:R, &Porsche:P, *:+, pw:16.07.2026
   ```

6. Die abweichende Großschreibung der ersten Zeile und den fehlenden Tabulator der Syncrotess-Zeile bewusst prüfen. Nicht allein aufgrund unterschiedlicher Beschriftungen neue Barcodes anlegen.
7. Den zweiten Syncrotess-Zugang bei Bedarf ergänzen. Soll eine Anmeldung in Andy und Syncrotess erscheinen, beide Gruppen am gewünschten Eintrag angeben:

   ```text
   &Andy:A, &Syncrotess:S
   ```

8. Danach nacheinander A, L, R, P und S auswählen und die Zuordnung prüfen. Je einen Materialmaster im Vollbild öffnen und den zugehörigen Behälter kontrollieren. Zum Schluss erneut exportieren.

Beim Import bedeutet **„Ersetzen“**: Die komplette gefundene Zeile wird durch die importierte Zeile ersetzt, einschließlich Klammern. Die App führt Gruppen nicht automatisch zusammen. **„Duplikat hinzufügen“** erhält beide Einträge. **„Abbrechen“** verwirft bereits bestätigte Änderungen dieses Importlaufs nicht zwingend: Der aktuelle Code speichert sie anschließend ausdrücklich.

## 11. Schnellvorlagen

Nur zuweisen:

```text
DEMO/Eintrag [&ZSB10:L]
```

Mehreren Gruppen zuweisen:

```text
DEMO/Eintrag [&ZSB10:L, &ZSB11:R]
```

In der Liste verstecken:

```text
DEMO/Eintrag [&ZSB10:L, #]
```

Gemeinsame Anmeldung auf der Plus-Seite:

```text
DEMO/Anmeldung [&Andy:A, &ZSB10:L, &ZSB11:R, &Porsche:P, *:+]
```

Material und versteckter Behälter auf der Minus-Seite:

```text
MATERIAL-DEMO/Material [P:M:MEINPAAR:@, &ZSB10:L, *:-]
BEHAELTER-DEMO/Behälter [P:S:MEINPAAR, &ZSB10:L, #, *:-]
```

Passwort-Frist mit Gruppenzuweisung:

```text
DEMO/Passwort [&Andy:A, pw:16.07.2026]
```

Die DEMO-Werte sind nur Syntaxbeispiele und keine betrieblichen Barcode-Werte.

## 12. Nachvollziehbare Prüfgrundlage

Zentrale Stellen in `app/src/main/assets/index.html` im untersuchten Stand:

- Eingebaute Hilfe: ab Zeile 4790; Klammerbefehle ab 4945.
- Bearbeitungsdialog: ab 5517.
- Verstecken/Augen-Button: `applyStoredToggleableBarcodeState`, ab 6048.
- Passwortfrist: `calculateRemainingDays`, ab 6423.
- Umbenennungsdialog: ab 6928; Umsetzung `performGroupRename`, ab 12096.
- Klammerparser: `parsePairInfo`, ab 7923.
- Gruppenfilter und Reihenfolge: `filterAndSortBarcodesByGroup`, ab 8043.
- Wischbedienung: ab 8516; Umschaltlogik ab 8816.
- Vollbild und Paarung: `showFullscreenBarcodeByIndex`, ab 9320; Slave-Suche ab 9521.
- TXT-Import und Konfliktbehandlung: ab 10613 bis 10760.

Der originale Parser wurde isoliert mit den beiden Listen und ausgewählten Syntaxbeispielen ausgeführt. Geprüft wurden Gruppenmitgliedschaften, Paar-IDs, Master-/Slave-Vollständigkeit, identische Zeilen und exakte Barcode-Wert-Konflikte. Die genannten Bedienungsgrenzen ergeben sich aus dem aktuellen Code; sie wurden nicht durch Änderungen an der App behoben.

## 13. Häufige Fragen & Eigene Fehler erkennen (Ist das ein Fehler?)

In der von dir geöffneten Datei `BarcodeAudi_verAndy_38.txt` und deiner 59er Datei gibt es Stellen, bei denen du unsicher warst. Hier die Auflösung:

**1. Fehlende eckige Klammern `[...]` am Ende der Zeile:**
- **Dein Code (Datei 38, Zeile 1):** `Cicsl/ Logis`
- **Ist das ein Fehler?** Nein, technisch ist das erlaubt. Die App zeigt diesen Barcode dann einfach in der Hauptliste ("Alle Gruppen") an. Er gehört aber zu keiner Untergruppe. Wenn er z.B. in der Gruppe "Andy" erscheinen soll, musst du das ergänzen: `Cicsl/ Logis [&Andy:A]`

**2. Keine Gruppe zugewiesen, aber ein Passwortdatum:**
- **Dein Code (Datei 38, Zeile 3):** `RAM10326/PASSWORT[pw:16.07.2026]`
- **Ist das ein Fehler?** Das funktioniert zwar, aber der Barcode wird in keiner deiner Gruppen (wie Andy oder ZSB) angezeigt, sondern nur unter "Alle". Meistens möchte man solche Barcodes auch einer Gruppe zuordnen. Die Lösung wäre: `RAM10326/PASSWORT[&Andy:A, pw:16.07.2026]`

**3. Zu lange Gruppenkennung nach dem Doppelpunkt:**
- **Dein Code (Datei 59, Zeile 35):** `andreas1.krabes	/ Syncrotess [&Syncrotess:Syncrotess]`
- **Ist das ein Fehler?** Die App nutzt nach dem Doppelpunkt `:` immer **nur den ersten Buchstaben**. Aus `:Syncrotess` macht die App intern also automatisch die Kennung `S`. Das funktioniert und ist kein Absturz-Fehler, kann aber verwirrend sein, wenn man später in den Code schaut. Besser und sauberer wäre: `[&Syncrotess:S]`

**Fazit:** Wirkliche „Fehler“, die die App zum Absturz bringen, machst du dort nicht. Es sind eher Feinheiten in der Zuweisung, damit die Barcodes auch in den richtigen Ansichten und Gruppen der App auftauchen.
