[CmdletBinding()]
param(
    [string]$NodeRepoRoot = "",
    [string]$TorBundleDir = "",
    [string]$OutputDir = "",
    [switch]$Package
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Resolve-AbsolutePath {
    param([Parameter(Mandatory = $true)][string]$Path, [string]$Base = (Get-Location).Path)
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }
    return [System.IO.Path]::GetFullPath((Join-Path $Base $Path))
}

function Read-VersionMatch {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Pattern,
        [Parameter(Mandatory = $true)][string]$Label
    )
    $match = [regex]::Match((Get-Content -LiteralPath $Path -Raw), $Pattern, "Multiline")
    if (-not $match.Success) { throw "Could not read $Label from $Path" }
    return $match.Groups[1].Value
}

function Get-RequiredHash {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing required file: $Path" }
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Get-BinaryVersion {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$CommandName
    )
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing release binary: $Path" }
    $output = (& $Path --version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw "$CommandName --version failed with exit code $LASTEXITCODE" }
    $match = [regex]::Match($output, "^$([regex]::Escape($CommandName))\s+(\d+\.\d+\.\d+)$")
    if (-not $match.Success) { throw "Unexpected version output from $CommandName`: $output" }
    return $match.Groups[1].Value
}

$zsilentRoot = Resolve-AbsolutePath -Path ".." -Base $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($NodeRepoRoot)) {
    $NodeRepoRoot = Resolve-AbsolutePath -Path ".." -Base $zsilentRoot
} else {
    $NodeRepoRoot = Resolve-AbsolutePath -Path $NodeRepoRoot
}
if ([string]::IsNullOrWhiteSpace($TorBundleDir)) {
    $TorBundleDir = Join-Path $NodeRepoRoot "third_party\tor\windows-x86_64"
} else {
    $TorBundleDir = Resolve-AbsolutePath -Path $TorBundleDir
}

$gradleBuild = Join-Path $zsilentRoot "build.gradle.kts"
$cargoToml = Join-Path $NodeRepoRoot "Cargo.toml"
$appVersion = Read-VersionMatch -Path $gradleBuild -Pattern '^[\s]*val[\s]+releaseVersion[\s]*=[\s]*"([^"]+)"' -Label "ZSilent releaseVersion"
$nodeSourceVersion = Read-VersionMatch -Path $cargoToml -Pattern '^[\s]*version[\s]*=[\s]*"([^"]+)"' -Label "Cargo package version"
if ($appVersion -ne $nodeSourceVersion) {
    throw "Version mismatch: ZSilent=$appVersion, node source=$nodeSourceVersion"
}

$zokaExe = Join-Path $NodeRepoRoot "target\release\zoka.exe"
$zokahdExe = Join-Path $NodeRepoRoot "target\release\zokahd.exe"
$zokaVersion = Get-BinaryVersion -Path $zokaExe -CommandName "zoka"
$zokahdVersion = Get-BinaryVersion -Path $zokahdExe -CommandName "zokahd"
if ($zokaVersion -ne $appVersion -or $zokahdVersion -ne $appVersion) {
    throw "Binary version mismatch: app=$appVersion, zoka=$zokaVersion, zokahd=$zokahdVersion. Rebuild both release binaries."
}

$networkManifest = Join-Path $NodeRepoRoot "docs\mainnet-network.v1.json"
$trustedSetupDir = Join-Path $NodeRepoRoot "trusted-setup"
$requiredSetup = @("DIGEST", "balance.params", "merkle.params", "nullifier.params", "private_tx.params")
foreach ($name in $requiredSetup) {
    $null = Get-RequiredHash -Path (Join-Path $trustedSetupDir $name)
}

$expectedTorBundleVersion = "15.0.18"
$expectedTorVersion = "0.4.9.11"
$expectedTorUrl = "https://archive.torproject.org/tor-package-archive/torbrowser/15.0.18/tor-expert-bundle-windows-x86_64-15.0.18.tar.gz"
$expectedTorArchiveSha256 = "6ac067402c7b4a3dc37887ed3754b3914b67fdc220c966190683e9ccf91abf0f"
$torMetadataPath = Join-Path $TorBundleDir ".zsilent-bundle.json"
if (-not (Test-Path -LiteralPath $torMetadataPath -PathType Leaf)) {
    throw "Missing verified Tor metadata: $torMetadataPath. Re-run prepare-tor-expert-bundle-windows.ps1 for Expert Bundle $expectedTorBundleVersion."
}
$torMetadata = Get-Content -LiteralPath $torMetadataPath -Raw | ConvertFrom-Json
if ($torMetadata.bundle_version -ne $expectedTorBundleVersion -or
    $torMetadata.source_url -ne $expectedTorUrl -or
    $torMetadata.archive_sha256 -ne $expectedTorArchiveSha256) {
    throw "Tor bundle provenance mismatch; expected Expert Bundle $expectedTorBundleVersion with archive SHA256 $expectedTorArchiveSha256"
}

$torCandidates = @(
    (Join-Path $TorBundleDir "tor.exe"),
    (Join-Path $TorBundleDir "Tor\tor.exe"),
    (Join-Path $TorBundleDir "tor\tor.exe"),
    (Join-Path $TorBundleDir "bin\tor.exe")
)
$torExe = $torCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($torExe)) {
    throw "Managed Tor is required for a Windows release but tor.exe was not found under $TorBundleDir"
}
$torVersionOutput = (& $torExe --version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0) { throw "tor.exe --version failed with exit code $LASTEXITCODE" }
$torVersionMatch = [regex]::Match($torVersionOutput, 'Tor version ([0-9.]+)')
$torVersion = if ($torVersionMatch.Success) { $torVersionMatch.Groups[1].Value.TrimEnd('.') } else { $torVersionOutput }
$torExeSha256 = Get-RequiredHash -Path $torExe
if ($torVersion -ne $expectedTorVersion -or $torMetadata.tor_version -ne $expectedTorVersion) {
    throw "Unexpected Tor version '$torVersion'; expected $expectedTorVersion"
}
if ($torMetadata.tor_exe_sha256 -ne $torExeSha256) {
    throw "tor.exe has changed since the verified bundle was prepared"
}

$preflight = [ordered]@{
    app_version = $appVersion
    node_version = $zokaVersion
    zokahd_version = $zokahdVersion
    tor_bundle_version = $expectedTorBundleVersion
    tor_version = $torVersion
    zoka_sha256 = Get-RequiredHash -Path $zokaExe
    zokahd_sha256 = Get-RequiredHash -Path $zokahdExe
    tor_archive_sha256 = $expectedTorArchiveSha256
    tor_sha256 = $torExeSha256
    network_manifest_sha256 = Get-RequiredHash -Path $networkManifest
    trusted_setup_digest_sha256 = Get-RequiredHash -Path (Join-Path $trustedSetupDir "DIGEST")
}

Write-Host "Release preflight passed for ZSilent Core $appVersion"
$preflight | ConvertTo-Json | Write-Output

if (-not $Package) {
    Write-Host "No package was built. Re-run with -Package after reviewing the preflight."
    return
}

$gradlew = Join-Path $zsilentRoot "gradlew.bat"
& $gradlew packageMsi "-Pzoka.nodeRepoRoot=$NodeRepoRoot" "-Pzsilent.torBundleDir=$TorBundleDir" "-Pzsilent.requireTorBundle=true" --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { throw "Gradle packageMsi failed with exit code $LASTEXITCODE" }

$msi = Join-Path $zsilentRoot "build\compose\binaries\main\msi\ZSilent Core-$appVersion.msi"
if (-not (Test-Path -LiteralPath $msi -PathType Leaf)) { throw "Expected MSI was not generated: $msi" }

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $zsilentRoot "build\release\$appVersion"
} else {
    $OutputDir = Resolve-AbsolutePath -Path $OutputDir
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$publishedMsi = Join-Path $OutputDir (Split-Path $msi -Leaf)
Copy-Item -LiteralPath $msi -Destination $publishedMsi -Force
$msiItem = Get-Item -LiteralPath $publishedMsi
$msiHash = Get-RequiredHash -Path $publishedMsi
$manifest = [ordered]@{
    schema_version = 1
    generated_at_utc = [DateTime]::UtcNow.ToString("o")
    app_version = $appVersion
    node = $preflight
    artifact = [ordered]@{
        name = $msiItem.Name
        size_bytes = $msiItem.Length
        sha256 = $msiHash
    }
}

$manifestPath = Join-Path $OutputDir "release-manifest.json"
$sumsPath = Join-Path $OutputDir "SHA256SUMS.txt"
$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
"$msiHash  $($msiItem.Name)" | Set-Content -LiteralPath $sumsPath -Encoding ASCII

Write-Host "Prepared (not published): $OutputDir"
Write-Host "MSI SHA-256: $msiHash"
