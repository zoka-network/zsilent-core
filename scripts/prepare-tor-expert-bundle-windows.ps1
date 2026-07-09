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

$resolvedOut = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutDir))
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

    tar -xzf $archive -C $extract
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
    Write-Host "Prepared Tor bundle at $resolvedOut"
}
finally {
    if (Test-Path $work) {
        Remove-Item -LiteralPath $work -Recurse -Force
    }
}
