$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$source = [Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'barcode-gloss-master.png'))
function Export-Icon([string]$relativePath, [int]$size, [double]$scale = 1) {
    $bitmap = New-Object Drawing.Bitmap $size, $size
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([Drawing.Color]::Black)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $inner = [int]($size * $scale)
    $offset = [int](($size - $inner) / 2)
    $graphics.DrawImage($source, $offset, $offset, $inner, $inner)
    $bitmap.Save((Join-Path $repoRoot $relativePath), [Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}
Export-Icon 'app/src/main/assets/icons/icon-192.png' 192
Export-Icon 'app/src/main/assets/icons/icon-512.png' 512
Export-Icon 'app/src/main/assets/icons/apple-touch-icon.png' 180
Export-Icon 'app/src/main/assets/icons/icon-maskable-192.png' 192 0.82
Export-Icon 'app/src/main/assets/icons/icon-maskable-512.png' 512 0.82
# Older Android versions use the complete icon instead of adaptive layers.
foreach ($density in @(@('mdpi',48), @('hdpi',72), @('xhdpi',96), @('xxhdpi',144), @('xxxhdpi',192))) {
    $directory = "app/src/main/res/mipmap-$($density[0])"
    New-Item -ItemType Directory -Force (Join-Path $repoRoot $directory) | Out-Null
    Export-Icon "$directory/ic_launcher.png" $density[1]
    Export-Icon "$directory/ic_launcher_round.png" $density[1] 0.82
}
# Adaptive icons expose approximately the central 72 of 108 dp.
Export-Icon 'app/src/main/res/drawable/ic_launcher_foreground.png' 512 0.60

$preview = New-Object Drawing.Bitmap 760, 430
$g = [Drawing.Graphics]::FromImage($preview)
$g.Clear([Drawing.Color]::FromArgb(35, 35, 39))
$g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$font = New-Object Drawing.Font 'Segoe UI', 18
foreach ($x in @(55, 435)) {
    $path = New-Object Drawing.Drawing2D.GraphicsPath
    if ($x -eq 435) {
        $path.AddEllipse($x, 40, 270, 270)
    } else {
        $radius = 62
        $diameter = $radius * 2
        $path.AddArc($x, 40, $diameter, $diameter, 180, 90)
        $path.AddArc($x + 270 - $diameter, 40, $diameter, $diameter, 270, 90)
        $path.AddArc($x + 270 - $diameter, 310 - $diameter, $diameter, $diameter, 0, 90)
        $path.AddArc($x, 310 - $diameter, $diameter, $diameter, 90, 90)
        $path.CloseFigure()
    }
    $state = $g.Save()
    $g.SetClip($path)
    if ($x -eq 435) {
        $android = [Drawing.Image]::FromFile((Join-Path $repoRoot 'app/src/main/res/drawable/ic_launcher_foreground.png'))
        $visible = [single](512 * 72 / 108)
        $inset = [single]((512 - $visible) / 2)
        $g.DrawImage($android, [Drawing.Rectangle]::new($x, 40, 270, 270), $inset, $inset, $visible, $visible, [Drawing.GraphicsUnit]::Pixel)
        $android.Dispose()
    } else {
        $g.DrawImage($source, $x, 40, 270, 270)
    }
    $g.Restore($state)
    $label = if ($x -eq 55) { 'Web-App' } else { 'Android (rund)' }
    $g.DrawString($label, $font, [Drawing.Brushes]::White, $x + 37, 335)
    $path.Dispose()
}
$preview.Save((Join-Path $PSScriptRoot 'icon-vorschau.png'), [Drawing.Imaging.ImageFormat]::Png)
$font.Dispose()
$g.Dispose()
$preview.Dispose()
$source.Dispose()
