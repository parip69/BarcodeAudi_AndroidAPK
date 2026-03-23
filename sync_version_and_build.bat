@echo off
REM Dieses Skript synchronisiert die Version aus build.gradle.kts in die HTML-Datei und baut dann die APK

REM 1. Versionsnummer aus build.gradle.kts extrahieren (beliebige Strings, auch "Audi Barcode 18")
set VERSION=
for /f "tokens=2 delims== " %%A in ('findstr /R /C:"^[ \t]*versionName = \".*\"" app\build.gradle.kts') do set RAWVERSION=%%~A
if defined RAWVERSION set VERSION=%RAWVERSION:~1,-1%

echo [INFO] Gelesene Version aus build.gradle.kts: "%VERSION%"
if "%VERSION%"=="" (
  echo [FEHLER] Konnte keine Version aus build.gradle.kts lesen! Abbruch.
  exit /b 1
)

REM 2. Versionsnummer in index.html ersetzen (egal ob leer, Zahl oder String)
powershell -Command "(Get-Content app\src\main\assets\index.html) -replace '<html lang=\"de\" data-app-version=\"[^\"]*\"', '<html lang=\"de\" data-app-version=\"%VERSION%\"' | Set-Content app\src\main\assets\index.html"
echo [INFO] data-app-version in index.html auf "%VERSION%" gesetzt.

REM 3. APK bauen
call .\gradlew.bat assembleDebug

REM 4. Fertigmeldung
if %ERRORLEVEL%==0 (
  echo [SUCCESS] Build und Versionssync erfolgreich! Version: %VERSION%
) else (
  echo [FEHLER] Fehler beim Build!
)
