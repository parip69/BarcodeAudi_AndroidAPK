$ErrorActionPreference = "Stop"

# Vorlage fuer neue Projekte.
# Vor dem Einsatz diese Platzhalter ersetzen:
# - {{APK_BASENAME}}
# - {{HTML_ARCHIVE_BASENAME}}
# - {{APK_ARCHIVE_BASENAME}}
#
# Standardstruktur:
# - Privat\sync_version_and_build.ps1
# - app\build.gradle.kts
# - app\src\main\assets\index.html
# - app\build\outputs\apk\debug
# - Privat\

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$candidateProjectRoots = @(
    $scriptDirectory
    (Split-Path -Parent $scriptDirectory)
) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
$projectRoot = $candidateProjectRoots |
    Where-Object { Test-Path -LiteralPath (Join-Path $_ "app\build.gradle.kts") } |
    Select-Object -First 1

if (-not $projectRoot) {
    $projectRoot = $scriptDirectory
}

$gradleFile = Join-Path $projectRoot "app\build.gradle.kts"
$indexFile = Join-Path $projectRoot "app\src\main\assets\index.html"
$gradlewBat = Join-Path $projectRoot "gradlew.bat"
$privatDir = Join-Path $projectRoot "Privat"
$apkOutputDir = Join-Path $projectRoot "app\build\outputs\apk\debug"

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

function Copy-BuildArtifactsToPrivat {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    if (-not (Test-Path -LiteralPath $privatDir)) {
        New-Item -ItemType Directory -Path $privatDir | Out-Null
    }

    if (-not (Test-Path -LiteralPath $indexFile)) {
        throw "index.html nicht gefunden: $indexFile"
    }

    if (-not (Test-Path -LiteralPath $apkOutputDir)) {
        throw "APK-Ausgabeordner nicht gefunden: $apkOutputDir"
    }

    $expectedApkFileName = "{{APK_BASENAME}}_ver_${Version}.apk"
    $apkSourcePath = Join-Path $apkOutputDir $expectedApkFileName

    if (-not (Test-Path -LiteralPath $apkSourcePath)) {
        $latestApk = Get-ChildItem -LiteralPath $apkOutputDir -Filter "*.apk" -File -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

        if ($null -eq $latestApk) {
            throw "Keine gebaute APK im Ordner gefunden: $apkOutputDir"
        }

        $apkSourcePath = $latestApk.FullName
    }

    $htmlArchivePath = Join-Path $privatDir ("{{HTML_ARCHIVE_BASENAME}}_ver_{0}.html" -f $Version)
    $apkArchivePath = Join-Path $privatDir ("{{APK_ARCHIVE_BASENAME}}-v{0}.apk" -f $Version)

    Copy-Item -LiteralPath $indexFile -Destination $htmlArchivePath -Force
    Copy-Item -LiteralPath $apkSourcePath -Destination $apkArchivePath -Force

    Write-Host "[INFO] HTML nach Privat kopiert: $htmlArchivePath"
    Write-Host "[INFO] APK nach Privat kopiert: $apkArchivePath"
}

if (-not (Test-Path -LiteralPath $gradleFile)) {
    Write-Host "[FEHLER] build.gradle.kts nicht gefunden: $gradleFile"
    exit 1
}

$gradleData = Read-TextFilePreserveEncoding -Path $gradleFile
$gradleText = $gradleData.Text

$versionNameNumericMatch = [regex]::Match($gradleText, '(?m)^\s*versionName\s*=\s*"(\d+)"')
$versionCodeNumericMatch = [regex]::Match($gradleText, '(?m)^\s*versionCode\s*=\s*(\d+)')

$currentVersion = ""
if ($versionNameNumericMatch.Success) {
    $currentVersion = $versionNameNumericMatch.Groups[1].Value.Trim()
} elseif ($versionCodeNumericMatch.Success) {
    $currentVersion = $versionCodeNumericMatch.Groups[1].Value.Trim()
}

Write-Host "[INFO] Gelesene Version aus build.gradle.kts: ""$currentVersion"""
if ([string]::IsNullOrWhiteSpace($currentVersion)) {
    Write-Host "[FEHLER] Konnte keine numerische Version aus build.gradle.kts lesen. Abbruch."
    exit 1
}

$newVersionNumber = [int64]$currentVersion + 1
$newVersion = $newVersionNumber.ToString()
$updatedGradleText = $gradleText

if ([regex]::IsMatch($updatedGradleText, '(?m)^\s*versionCode\s*=')) {
    $updatedGradleText = [regex]::Replace(
        $updatedGradleText,
        '(?m)^(\s*versionCode\s*=\s*)\d+([^\r\n]*)',
        ('${1}' + $newVersion + '${2}'),
        1
    )
} else {
    Write-Host "[WARNUNG] versionCode in build.gradle.kts nicht gefunden."
}

if ([regex]::IsMatch($updatedGradleText, '(?m)^\s*versionName\s*=')) {
    $updatedGradleText = [regex]::Replace(
        $updatedGradleText,
        '(?m)^(\s*versionName\s*=\s*")([^"]*)(")([^\r\n]*)',
        ('${1}' + $newVersion + '${3}${4}'),
        1
    )
} else {
    Write-Host "[WARNUNG] versionName in build.gradle.kts nicht gefunden."
}

if ($updatedGradleText -ne $gradleText) {
    Write-TextFilePreserveEncoding -Path $gradleFile -Text $updatedGradleText -Encoding $gradleData.Encoding
}

Write-Host "[INFO] Neue Version fuer Build und index.html: ""$newVersion"""

if (Test-Path -LiteralPath $indexFile) {
    $indexData = Read-TextFilePreserveEncoding -Path $indexFile
    $pattern = '<html lang="de" data-app-version="[^"]*"'
    $replacement = '<html lang="de" data-app-version="' + $newVersion + '"'

    if ([regex]::IsMatch($indexData.Text, $pattern)) {
        $updatedText = [regex]::Replace($indexData.Text, $pattern, $replacement, 1)
        if ($updatedText -ne $indexData.Text) {
            Write-TextFilePreserveEncoding -Path $indexFile -Text $updatedText -Encoding $indexData.Encoding
        }
        Write-Host "[INFO] data-app-version in index.html auf ""$newVersion"" gesetzt."
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
    try {
        Copy-BuildArtifactsToPrivat -Version $newVersion
    } catch {
        Write-Host "[FEHLER] Build war erfolgreich, aber die Archivkopien konnten nicht erstellt werden: $($_.Exception.Message)"
        exit 1
    }

    Write-Host "[SUCCESS] Build und Versionssync erfolgreich. Version: $newVersion"
    exit 0
}

Write-Host "[FEHLER] Fehler beim Build."
exit $buildExitCode
