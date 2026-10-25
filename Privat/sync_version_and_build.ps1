$ErrorActionPreference = "Stop"

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
$swFile = Join-Path $projectRoot "app\src\main\assets\sw.js"
$gradlewBat = Join-Path $projectRoot "gradlew.bat"
$privatDir = Join-Path $projectRoot "Privat"
$apkOutputDir = Join-Path $projectRoot "app\build\outputs\apk\debug"

function Invoke-GitChecked {
    param([string[]]$GitArguments)
    & git -C $projectRoot @GitArguments | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Git fehlgeschlagen: git $($GitArguments -join ' ')" }
}

# Stop before changing versions if Git or the verification tools are unavailable.
Invoke-GitChecked @("fetch", "origin")
$branch = & git -C $projectRoot branch --show-current
if ($LASTEXITCODE -ne 0 -or $branch -ne "main") { throw "Bitte zuerst main auschecken." }
Invoke-GitChecked @("merge", "--ff-only", "origin/main")
$untracked = & git -C $projectRoot ls-files --others --exclude-standard
if ($LASTEXITCODE -ne 0 -or $untracked) { throw "Neue Dateien zuerst gezielt zu Git hinzufuegen, dann erneut starten." }

$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot) {
    $localProperties = Get-Content -LiteralPath (Join-Path $projectRoot "local.properties")
    $sdkLine = $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($sdkLine) { $sdkRoot = $sdkLine.Substring(8).Replace('\\', '\').Replace('\:', ':') }
}
$buildTools = Get-ChildItem -LiteralPath (Join-Path $sdkRoot "build-tools") -Directory |
    Where-Object { (Test-Path (Join-Path $_.FullName "apksigner.bat")) -and (Test-Path (Join-Path $_.FullName "aapt.exe")) } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { throw "Android SDK apksigner/aapt fehlen." }
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
$aapt = Join-Path $buildTools.FullName "aapt.exe"
$expectedCertificate = "b46b439e36ed2de248b9085cb4b457b74ba70b5beb823962e9c931ce79104d28"

function Test-ApkIdentity {
    param([string]$Path, [string]$Version)
    $certificates = & $apksigner verify --print-certs $Path
    if ($LASTEXITCODE -ne 0) { throw "APK-Signatur ungueltig: $Path" }
    $fingerprints = @($certificates | Select-String 'certificate SHA-256 digest: (\w+)' | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if ($fingerprints.Count -ne 1 -or $fingerprints[0] -ne $expectedCertificate) { throw "Abweichender Signierungsschluessel: $Path" }
    $badging = & $aapt dump badging $Path
    if ($LASTEXITCODE -ne 0 -or -not ($badging -match "^package: name='de.parip69.barcodeaudiscanner' versionCode='$Version' versionName='$Version' ")) {
        throw "APK-Paket oder Version stimmt nicht: $Path"
    }
}

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

function Update-ServiceWorkerCacheVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "[WARNUNG] sw.js nicht gefunden: $Path"
        return
    }

    $swData = Read-TextFilePreserveEncoding -Path $Path
    $updatedSwText = $swData.Text
    $changed = $false

    foreach ($constantName in @("APP_SHELL_CACHE", "RUNTIME_CACHE")) {
        $pattern = '(?m)^(\s*const\s+' + [regex]::Escape($constantName) + '\s*=\s*")([^"]+)(";)'
        $match = [regex]::Match($updatedSwText, $pattern)

        if (-not $match.Success) {
            Write-Host "[WARNUNG] $constantName in sw.js nicht gefunden."
            continue
        }

        $currentValue = $match.Groups[2].Value
        $prefix = $currentValue
        $prefixMatch = [regex]::Match($currentValue, '^(.*?)-v[^"]+$')

        if ($prefixMatch.Success) {
            $prefix = $prefixMatch.Groups[1].Value
        }

        $newValue = "$prefix-v$Version"
        if ($newValue -eq $currentValue) {
            continue
        }

        $updatedSwText = [regex]::Replace(
            $updatedSwText,
            $pattern,
            ('${1}' + $newValue + '${3}'),
            1
        )
        $changed = $true
        Write-Host "[INFO] $constantName in sw.js auf ""$newValue"" gesetzt."
    }

    if ($changed) {
        Write-TextFilePreserveEncoding -Path $Path -Text $updatedSwText -Encoding $swData.Encoding
    }
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

    $expectedApkFileName = "BarcodeAudiScanner_ver${Version}.apk"
    $apkSourcePath = Join-Path $apkOutputDir $expectedApkFileName

    if (-not (Test-Path -LiteralPath $apkSourcePath)) {
        throw "Erwartete APK fehlt: $apkSourcePath"
    }

    Test-ApkIdentity -Path $apkSourcePath -Version $Version

    $htmlArchivePath = Join-Path $privatDir ("BarcodeAudiScanner_ver{0}.html" -f $Version)
    $apkArchivePath = Join-Path $privatDir ("BarcodeAudiScanner_ver{0}.apk" -f $Version)

    if ((Test-Path $htmlArchivePath) -or (Test-Path $apkArchivePath)) { throw "Archivversion $Version existiert bereits." }

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
    Write-Host "[FEHLER] Konnte keine numerische Version aus build.gradle.kts lesen! Abbruch."
    exit 1
}

$newVersionNumber = [int64]$currentVersion + 1
$newVersion = $newVersionNumber.ToString()
if ((Test-Path (Join-Path $privatDir "BarcodeAudiScanner_ver$newVersion.apk")) -or
    (Test-Path (Join-Path $privatDir "BarcodeAudiScanner_ver$newVersion.html"))) {
    throw "Version $newVersion ist bereits archiviert. Abbruch ohne Ueberschreiben."
}
$previousApk = Join-Path $privatDir "BarcodeAudiScanner_ver$currentVersion.apk"
if (Test-Path $previousApk) { Test-ApkIdentity -Path $previousApk -Version $currentVersion }
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
    $htmlPattern = '<html lang="de" data-app-version="[^"]*"'
    $htmlReplacement = '<html lang="de" data-app-version="' + $newVersion + '"'

    if ([regex]::IsMatch($indexData.Text, $htmlPattern)) {
        $updatedText = [regex]::Replace($indexData.Text, $htmlPattern, $htmlReplacement, 1)
        $updatedText = [regex]::Replace(
            $updatedText,
            '(?m)^(<!-- Barcode Audi Scanner - Ver\. )[^\r\n ]+( -->)\r?$',
            ('${1}' + $newVersion + '${2}'),
            1
        )
        $updatedText = [regex]::Replace(
            $updatedText,
            '(const APP_VERSION_FALLBACK = ")[^"]+(";)',
            ('${1}' + $newVersion + '${2}'),
            1
        )
        $updatedText = [regex]::Replace(
            $updatedText,
            '(<footer id="appFooter" data-app-version=")[^"]+("[^>]*>)',
            ('${1}' + $newVersion + '${2}'),
            1
        )
        $updatedText = [regex]::Replace(
            $updatedText,
            '(<span id="footerVersion">)[^<]+(</span>)',
            ('${1}' + $newVersion + '${2}'),
            1
        )
        if ($updatedText -ne $indexData.Text) {
            Write-TextFilePreserveEncoding -Path $indexFile -Text $updatedText -Encoding $indexData.Encoding
        }
        Write-Host "[INFO] Versionsangaben in index.html auf ""$newVersion"" gesetzt."
    } else {
        Write-Host "[WARNUNG] data-app-version Marker in index.html nicht gefunden."
    }
} else {
    Write-Host "[WARNUNG] index.html nicht gefunden: $indexFile"
}

Update-ServiceWorkerCacheVersion -Path $swFile -Version $newVersion

if (-not (Test-Path -LiteralPath $gradlewBat)) {
    Write-Host "[FEHLER] gradlew.bat nicht gefunden: $gradlewBat"
    exit 1
}

& $gradlewBat -p $projectRoot assembleDebug
$buildExitCode = $LASTEXITCODE

if ($buildExitCode -eq 0) {
    try {
        Copy-BuildArtifactsToPrivat -Version $newVersion
    } catch {
        Write-Host "[FEHLER] Build war erfolgreich, aber die Archivkopien konnten nicht erstellt werden: $($_.Exception.Message)"
        exit 1
    }

    Invoke-GitChecked @("add", "--update")
    Invoke-GitChecked @("add", "--", "Privat/BarcodeAudiScanner_ver$newVersion.apk", "Privat/BarcodeAudiScanner_ver$newVersion.html", "docs", "signing")
    Invoke-GitChecked @("commit", "-m", "Publish version $newVersion")
    Invoke-GitChecked @("push", "origin", "main")
    Write-Host "[SUCCESS] Version $newVersion gebaut, geprueft und auf GitHub hochgeladen."
    Write-Host "[INFO] APK: https://raw.githubusercontent.com/parip69/BarcodeAudi_AndroidAPK/main/Privat/BarcodeAudiScanner_ver$newVersion.apk"
    exit 0
}

Write-Host "[FEHLER] Fehler beim Build!"
exit $buildExitCode
