@echo off
REM Vorlage fuer neue Projekte.
REM Datei spaeter als Privat\sync_version_and_build.bat ins Zielprojekt kopieren.
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync_version_and_build.ps1"
set "EXITCODE=%ERRORLEVEL%"

endlocal & exit /b %EXITCODE%
