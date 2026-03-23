@echo off
REM Dieses Skript startet die robuste PowerShell-Variante fuer Versionssync und Build.
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync_version_and_build.ps1"
set "EXITCODE=%ERRORLEVEL%"

endlocal & exit /b %EXITCODE%
