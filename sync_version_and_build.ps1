$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleFile = Join-Path $scriptRoot "app\build.gradle.kts"
$indexFile = Join-Path $scriptRoot "app\src\main\assets\index.html"
$gradlewBat = Join-Path $scriptRoot "gradlew.bat"

function Read-TextFilePreserveEncoding {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $bytes = [System.IO.File]::ReadAllBytes($Path)

    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $encoding = New-Object System.Text.UTF8Encoding($true)
        $text = $encoding.GetString($bytes, 3, $bytes.Length - 3)
        return [pscustomobject]@{
            Text = $text
            Encoding = $encoding
        }
    }

    if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
        $encoding = [System.Text.Encoding]::Unicode
        $text = $encoding.GetString($bytes, 2, $bytes.Length - 2)
        return [pscustomobject]@{
            Text = $text
            Encoding = $encoding
        }
    }

    if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) {
        $encoding = [System.Text.Encoding]::BigEndianUnicode
        $text = $encoding.GetString($bytes, 2, $bytes.Length - 2)
        return [pscustomobject]@{
            Text = $text
            Encoding = $encoding
        }
    }

    try {
        $encoding = New-Object System.Text.UTF8Encoding($false, $true)
        $text = $encoding.GetString($bytes)
        return [pscustomobject]@{
            Text = $text
            Encoding = New-Object System.Text.UTF8Encoding($false)
        }
    } catch {
        $encoding = [System.Text.Encoding]::Default
        $text = $encoding.GetString($bytes)
        return [pscustomobject]@{
            Text = $text
            Encoding = $encoding
        }
    }
}

function Write-TextFilePreserveEncoding {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Text,
        [Parameter(Mandatory = $true)]
        [System.Text.Encoding]$Encoding
    )

    $preamble = $Encoding.GetPreamble()
    $contentBytes = $Encoding.GetBytes($Text)

    if ($preamble.Length -gt 0) {
        $allBytes = New-Object byte[] ($preamble.Length + $contentBytes.Length)
        [System.Buffer]::BlockCopy($preamble, 0, $allBytes, 0, $preamble.Length)
        [System.Buffer]::BlockCopy($contentBytes, 0, $allBytes, $preamble.Length, $contentBytes.Length)
        [System.IO.File]::WriteAllBytes($Path, $allBytes)
        return
    }

    [System.IO.File]::WriteAllBytes($Path, $contentBytes)
}

if (-not (Test-Path -LiteralPath $gradleFile)) {
    Write-Host "[FEHLER] build.gradle.kts nicht gefunden: $gradleFile"
    exit 1
}

$gradleContent = Get-Content -Raw -LiteralPath $gradleFile
$versionMatch = [regex]::Match($gradleContent, '(?m)^\s*versionName\s*=\s*"([^"]*)"')
$version = if ($versionMatch.Success) { $versionMatch.Groups[1].Value.Trim() } else { "" }

Write-Host "[INFO] Gelesene Version aus build.gradle.kts: ""$version"""
if ([string]::IsNullOrWhiteSpace($version)) {
    Write-Host "[FEHLER] Konnte keine Version aus build.gradle.kts lesen! Abbruch."
    exit 1
}

if (Test-Path -LiteralPath $indexFile) {
    $indexData = Read-TextFilePreserveEncoding -Path $indexFile
    $pattern = '<html lang="de" data-app-version="[^"]*"'
    $replacement = '<html lang="de" data-app-version="' + $version + '"'

    if ([regex]::IsMatch($indexData.Text, $pattern)) {
        $updatedText = [regex]::Replace($indexData.Text, $pattern, $replacement, 1)
        if ($updatedText -ne $indexData.Text) {
            Write-TextFilePreserveEncoding -Path $indexFile -Text $updatedText -Encoding $indexData.Encoding
        }
        Write-Host "[INFO] data-app-version in index.html auf ""$version"" gesetzt."
    } else {
        Write-Host "[WARNUNG] data-app-version Marker in index.html nicht gefunden."
    }
} else {
    Write-Host "[WARNUNG] index.html nicht gefunden: $indexFile"
}

if (-not (Test-Path -LiteralPath $gradlewBat)) {
    Write-Host "[FEHLER] gradlew.bat nicht gefunden: $gradlewBat"
    exit 1
}

& $gradlewBat assembleDebug
$buildExitCode = $LASTEXITCODE

if ($buildExitCode -eq 0) {
    Write-Host "[SUCCESS] Build und Versionssync erfolgreich! Version: $version"
    exit 0
}

Write-Host "[FEHLER] Fehler beim Build!"
exit $buildExitCode
