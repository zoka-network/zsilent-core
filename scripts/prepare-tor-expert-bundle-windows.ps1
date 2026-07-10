param(
    [Parameter(Mandatory = $true)]
    [string]$Url,

    [Parameter(Mandatory = $true)]
    [string]$Sha256,

    [string]$AscUrl = "",

    [string]$OutDir = "",

    [switch]$VerifySignature
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = Join-Path $PSScriptRoot "..\..\third_party\tor\windows-x86_64"
}

# Resolve OutDir whether it was passed absolute or relative. Joining an
# already-absolute path onto the CWD produces an invalid path (e.g.
# C:\cwd\C:\dest), so anchor on the CWD only for relative inputs.
if ([System.IO.Path]::IsPathRooted($OutDir)) {
    $resolvedOut = [System.IO.Path]::GetFullPath($OutDir)
} else {
    $resolvedOut = [System.IO.Path]::GetFullPath((Join-Path (Get-Location).Path $OutDir))
}
$work = Join-Path ([System.IO.Path]::GetTempPath()) ("zsilent-tor-" + [Guid]::NewGuid().ToString("N"))
$archive = Join-Path $work "tor-expert-bundle.tar.gz"
$extract = Join-Path $work "extract"

New-Item -ItemType Directory -Force -Path $work, $extract | Out-Null

try {
    Write-Host "Downloading Tor Expert Bundle..."
    Invoke-WebRequest -Uri $Url -OutFile $archive

    $actual = (Get-FileHash -Algorithm SHA256 -Path $archive).Hash.ToLowerInvariant()
    $expected = $Sha256.Trim().ToLowerInvariant()
    if ($actual -ne $expected) {
        throw "SHA256 mismatch. Expected $expected, got $actual"
    }
    Write-Host "SHA256 verified: $actual"

    if ($VerifySignature) {
        if ([string]::IsNullOrWhiteSpace($AscUrl)) {
            throw "-VerifySignature requires -AscUrl"
        }
        $gpg = Get-Command gpg -ErrorAction SilentlyContinue
        if ($null -eq $gpg) {
            throw "gpg was not found; install GnuPG or run with checksum-only verification."
        }
        $asc = Join-Path $work "tor-expert-bundle.tar.gz.asc"
        Invoke-WebRequest -Uri $AscUrl -OutFile $asc
        & $gpg.Source --verify $asc $archive
        if ($LASTEXITCODE -ne 0) {
            throw "GPG signature verification failed"
        }
        Write-Host "GPG signature verified."
    }

    # Use Windows' native bsdtar (System32\tar.exe) explicitly. A Git-Bash /
    # MSYS `tar` earlier on PATH chokes on Windows-style extract paths, so
    # don't rely on bare `tar` resolution.
    $tarExe = Join-Path $env:SystemRoot "System32\tar.exe"
    if (-not (Test-Path $tarExe)) { $tarExe = "tar" }
    & $tarExe -xzf $archive -C $extract
    if ($LASTEXITCODE -ne 0) {
        throw "tar extraction failed"
    }

    $torExe = Get-ChildItem -Path $extract -Recurse -Filter tor.exe | Select-Object -First 1
    if ($null -eq $torExe) {
        throw "Extracted bundle does not contain tor.exe"
    }

    if (Test-Path $resolvedOut) {
        Remove-Item -LiteralPath $resolvedOut -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $resolvedOut | Out-Null

    $bundleRoot = $torExe.Directory.FullName
    Copy-Item -Path (Join-Path $bundleRoot "*") -Destination $resolvedOut -Recurse -Force

    # The Expert Bundle ships geoip/geoip6 in a sibling `data/` folder next to
    # `tor/`. Tor runs without them but logs "Unable to open GEOIP file" and
    # disables geoip features; copy them alongside tor.exe so the managed
    # instance starts clean. torrc-defaults is intentionally skipped -- the
    # supervisor passes its own -f torrc.
    $dataDir = Join-Path (Split-Path $bundleRoot -Parent) "data"
    foreach ($geo in @("geoip", "geoip6")) {
        $src = Join-Path $dataDir $geo
        if (Test-Path $src) {
            Copy-Item -Path $src -Destination (Join-Path $resolvedOut $geo) -Force
        }
    }
    Write-Host "Prepared Tor bundle at $resolvedOut"
}
finally {
    if (Test-Path $work) {
        Remove-Item -LiteralPath $work -Recurse -Force
    }
}
