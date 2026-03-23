@echo off
REM Dieses Skript synchronisiert die Version aus build.gradle.kts in die HTML-Datei und baut dann die APK

REM 1. Versionsnummer aus build.gradle.kts extrahieren
for /f "tokens=2 delims== " %%A in ('findstr /R /C:"versionName = \"[0-9]*\"" app\build.gradle.kts') do set VERSION=%%~A
set VERSION=%VERSION:~1,-1%

REM 2. Versionsnummer in index.html ersetzen
powershell -Command "(Get-Content app\src\main\assets\index.html) -replace '<html lang=\"de\" data-app-version=\"[0-9]+\"', '<html lang=\"de\" data-app-version=\"%VERSION%\"' | Set-Content app\src\main\assets\index.html"

REM 3. APK bauen
call .\gradlew.bat assembleDebug

REM 4. Fertigmeldung
if %ERRORLEVEL%==0 (
  echo Build und Versionssync erfolgreich! Version: %VERSION%
) else (
  echo Fehler beim Build!
)
