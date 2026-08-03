param(
    [string]$SourceBitmap = ""
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot
$brandRoot = Join-Path $repoRoot "assets\branding"
$packagingRoot = Join-Path $repoRoot "packaging\windows"
$resourceRoot = Join-Path $repoRoot "src\main\resources"
$masterBitmap = Join-Path $brandRoot "logomsi.bmp"

if ([string]::IsNullOrWhiteSpace($SourceBitmap)) {
    $SourceBitmap = $masterBitmap
}
if (-not (Test-Path -LiteralPath $SourceBitmap -PathType Leaf)) {
    throw "Brand bitmap not found: $SourceBitmap"
}

New-Item -ItemType Directory -Path $brandRoot, $packagingRoot, $resourceRoot -Force | Out-Null
$resolvedSource = (Resolve-Path -LiteralPath $SourceBitmap).Path
if ($resolvedSource -ne $masterBitmap) {
    Copy-Item -LiteralPath $resolvedSource -Destination $masterBitmap -Force
}

function Test-BrandPixel([System.Drawing.Color]$Color) {
    return $Color.R -gt 32 -or $Color.G -gt 32 -or $Color.B -gt 32
}

function Get-BrandBounds(
    [System.Drawing.Bitmap]$Bitmap,
    [int]$StartY,
    [int]$EndY
) {
    $minX = $Bitmap.Width
    $minY = $Bitmap.Height
    $maxX = -1
    $maxY = -1
    for ($y = $StartY; $y -le $EndY; $y++) {
        for ($x = 0; $x -lt $Bitmap.Width; $x++) {
            if (Test-BrandPixel $Bitmap.GetPixel($x, $y)) {
                $minX = [Math]::Min($minX, $x)
                $minY = [Math]::Min($minY, $y)
                $maxX = [Math]::Max($maxX, $x)
                $maxY = [Math]::Max($maxY, $y)
            }
        }
    }
    if ($maxX -lt $minX -or $maxY -lt $minY) {
        throw "The source bitmap does not contain a visible white brand mark."
    }
    return [System.Drawing.Rectangle]::FromLTRB($minX, $minY, $maxX + 1, $maxY + 1)
}

function Get-BrandSegments([System.Drawing.Bitmap]$Bitmap) {
    $activeRows = New-Object System.Collections.Generic.List[int]
    for ($y = 0; $y -lt $Bitmap.Height; $y++) {
        $active = $false
        for ($x = 0; $x -lt $Bitmap.Width; $x++) {
            if (Test-BrandPixel $Bitmap.GetPixel($x, $y)) {
                $active = $true
                break
            }
        }
        if ($active) { $activeRows.Add($y) }
    }
    if ($activeRows.Count -eq 0) { throw "The source bitmap is empty." }

    $segments = New-Object System.Collections.Generic.List[System.Drawing.Rectangle]
    $segmentStart = $activeRows[0]
    $previous = $activeRows[0]
    foreach ($row in $activeRows | Select-Object -Skip 1) {
        if ($row - $previous -gt 1) {
            $segments.Add((Get-BrandBounds $Bitmap $segmentStart $previous))
            $segmentStart = $row
        }
        $previous = $row
    }
    $segments.Add((Get-BrandBounds $Bitmap $segmentStart $previous))
    return $segments
}

function New-BrandBitmap([int]$Width, [int]$Height) {
    $bitmap = New-Object System.Drawing.Bitmap(
        $Width,
        $Height,
        [System.Drawing.Imaging.PixelFormat]::Format24bppRgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Black)
    $graphics.Dispose()
    return $bitmap
}

function New-RoundedRectanglePath([System.Drawing.RectangleF]$Bounds, [float]$Radius) {
    $diameter = [Math]::Max(1.0, $Radius * 2.0)
    $arc = New-Object System.Drawing.RectangleF($Bounds.X, $Bounds.Y, $diameter, $diameter)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc($arc, 180, 90)
    $arc.X = $Bounds.Right - $diameter
    $path.AddArc($arc, 270, 90)
    $arc.Y = $Bounds.Bottom - $diameter
    $path.AddArc($arc, 0, 90)
    $arc.X = $Bounds.Left
    $path.AddArc($arc, 90, 90)
    $path.CloseFigure()
    return $path
}

function Set-HighQuality([System.Drawing.Graphics]$Graphics) {
    $Graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $Graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
}

function Draw-BrandFit(
    [System.Drawing.Graphics]$Graphics,
    [System.Drawing.Bitmap]$Source,
    [System.Drawing.Rectangle]$SourceBounds,
    [System.Drawing.Rectangle]$DestinationBounds
) {
    $scale = [Math]::Min(
        $DestinationBounds.Width / [double]$SourceBounds.Width,
        $DestinationBounds.Height / [double]$SourceBounds.Height
    )
    $width = [Math]::Max(1, [int][Math]::Round($SourceBounds.Width * $scale))
    $height = [Math]::Max(1, [int][Math]::Round($SourceBounds.Height * $scale))
    $x = $DestinationBounds.X + [int](($DestinationBounds.Width - $width) / 2)
    $y = $DestinationBounds.Y + [int](($DestinationBounds.Height - $height) / 2)
    $destination = New-Object System.Drawing.Rectangle($x, $y, $width, $height)
    $Graphics.DrawImage(
        $Source,
        $destination,
        $SourceBounds.X,
        $SourceBounds.Y,
        $SourceBounds.Width,
        $SourceBounds.Height,
        [System.Drawing.GraphicsUnit]::Pixel
    )
}

function Save-BrandPng(
    [System.Drawing.Bitmap]$Source,
    [System.Drawing.Rectangle]$SymbolBounds,
    [string]$Destination
) {
    $bitmap = New-Object System.Drawing.Bitmap(
        512,
        512,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    Set-HighQuality $graphics
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $plateBounds = New-Object System.Drawing.RectangleF(12, 12, 488, 488)
    $platePath = New-RoundedRectanglePath $plateBounds 92
    $plateBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Black)
    $graphics.FillPath($plateBrush, $platePath)
    $plateBrush.Dispose()
    $platePath.Dispose()
    Draw-BrandFit $graphics $Source $SymbolBounds (New-Object System.Drawing.Rectangle(72, 58, 368, 396))
    $graphics.Dispose()
    $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Save-BrandIcon(
    [System.Drawing.Bitmap]$Source,
    [System.Drawing.Rectangle]$SymbolBounds,
    [string]$Destination
) {
    $sizes = @(16, 20, 24, 32, 40, 48, 64, 128, 256)
    $frames = New-Object System.Collections.Generic.List[object]
    foreach ($size in $sizes) {
        $bitmap = New-Object System.Drawing.Bitmap(
            $size,
            $size,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        Set-HighQuality $graphics
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $margin = [Math]::Max(1.0, $size * 0.025)
        $plateBounds = [System.Drawing.RectangleF]::new(
            [float]$margin,
            [float]$margin,
            [float]($size - 2 * $margin),
            [float]($size - 2 * $margin)
        )
        $platePath = New-RoundedRectanglePath $plateBounds ([Math]::Max(1.0, $size * 0.18))
        $plateBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Black)
        $graphics.FillPath($plateBrush, $platePath)
        $plateBrush.Dispose()
        $platePath.Dispose()
        $horizontalPadding = [int][Math]::Round($size * 0.14)
        $verticalPadding = [int][Math]::Round($size * 0.105)
        Draw-BrandFit $graphics $Source $SymbolBounds (
            [System.Drawing.Rectangle]::new(
                $horizontalPadding,
                $verticalPadding,
                $size - 2 * $horizontalPadding,
                $size - 2 * $verticalPadding
            )
        )
        $graphics.Dispose()
        $stream = New-Object System.IO.MemoryStream
        $bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
        $frames.Add([pscustomobject]@{ Size = $size; Bytes = $stream.ToArray() })
        $stream.Dispose()
        $bitmap.Dispose()
    }

    $fileStream = [System.IO.File]::Open($Destination, [System.IO.FileMode]::Create)
    $writer = New-Object System.IO.BinaryWriter($fileStream)
    try {
        $writer.Write([uint16]0)
        $writer.Write([uint16]1)
        $writer.Write([uint16]$frames.Count)
        $offset = 6 + 16 * $frames.Count
        foreach ($frame in $frames) {
            $dimension = if ($frame.Size -eq 256) { 0 } else { $frame.Size }
            $writer.Write([byte]$dimension)
            $writer.Write([byte]$dimension)
            $writer.Write([byte]0)
            $writer.Write([byte]0)
            $writer.Write([uint16]1)
            $writer.Write([uint16]32)
            $writer.Write([uint32]$frame.Bytes.Length)
            $writer.Write([uint32]$offset)
            $offset += $frame.Bytes.Length
        }
        foreach ($frame in $frames) {
            $writer.Write([byte[]]$frame.Bytes)
        }
    } finally {
        $writer.Dispose()
        $fileStream.Dispose()
    }
}

function Save-InstallerDialog(
    [System.Drawing.Bitmap]$Source,
    [System.Drawing.Rectangle]$FullBounds,
    [string]$Destination
) {
    $bitmap = New-BrandBitmap 493 312
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    Set-HighQuality $graphics
    Draw-BrandFit $graphics $Source $FullBounds (New-Object System.Drawing.Rectangle(24, 27, 128, 258))
    $divider = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(48, 48, 48), 1)
    $graphics.DrawLine($divider, 174, 24, 174, 288)
    $divider.Dispose()
    $graphics.Dispose()
    $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Bmp)
    $bitmap.Dispose()
}

function Save-InstallerBanner(
    [System.Drawing.Bitmap]$Source,
    [System.Drawing.Rectangle]$SymbolBounds,
    [string]$Destination
) {
    $bitmap = New-BrandBitmap 493 58
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    Set-HighQuality $graphics
    Draw-BrandFit $graphics $Source $SymbolBounds (New-Object System.Drawing.Rectangle(445, 9, 30, 40))
    $divider = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(48, 48, 48), 1)
    $graphics.DrawLine($divider, 0, 57, 493, 57)
    $divider.Dispose()
    $graphics.Dispose()
    $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Bmp)
    $bitmap.Dispose()
}

function Save-WelcomeBanner(
    [System.Drawing.Bitmap]$Source,
    [System.Drawing.Rectangle]$FullBounds,
    [string]$Destination
) {
    $bitmap = New-BrandBitmap 2172 724
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    Set-HighQuality $graphics
    Draw-BrandFit $graphics $Source $FullBounds (New-Object System.Drawing.Rectangle(887, 56, 398, 612))
    $graphics.Dispose()
    $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

$source = [System.Drawing.Bitmap]::FromFile($masterBitmap)
try {
    $segments = @(Get-BrandSegments $source)
    $symbolBounds = $segments[0]
    $fullBounds = Get-BrandBounds $source 0 ($source.Height - 1)

    Save-BrandPng $source $symbolBounds (Join-Path $resourceRoot "zsilent-logo.png")
    Save-BrandPng $source $symbolBounds (Join-Path $resourceRoot "zsilent-icon.png")
    Save-BrandIcon $source $symbolBounds (Join-Path $resourceRoot "zsilent-icon.ico")
    Save-InstallerDialog $source $fullBounds (Join-Path $packagingRoot "zsilent-installer-dialog.bmp")
    Save-InstallerBanner $source $symbolBounds (Join-Path $packagingRoot "zsilent-installer-banner.bmp")
    Save-WelcomeBanner $source $fullBounds (Join-Path $resourceRoot "welcome-banner.png")
} finally {
    $source.Dispose()
}

Write-Host "Prepared ZSilent brand assets from $masterBitmap"
