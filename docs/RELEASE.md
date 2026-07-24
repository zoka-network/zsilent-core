# ZSilent Core release runbook

This process prepares and verifies artifacts. It does not publish a GitHub
release or send a Discord announcement unless a maintainer makes a separate,
explicit choice to do so.

## 1. Align and build the node

The following four versions must be identical:

- `Cargo.toml` package version
- `build.gradle.kts` `releaseVersion`
- `zoka --version`
- `zokahd --version`

For protocol 1.9, all four must report `1.9.0`. Protocol 1.9 is intentionally
incompatible with 1.8; do not package a 1.8 node in a 1.9 installer.

Build the node from the reviewed release source before running the ZSilent
preflight:

```powershell
cargo build --release --features randomx --bin zoka --bin zokahd
```

## 2. Require managed Tor

Prepare the checksum-pinned Tor Expert Bundle as described in the node
repository's `docs/zsilent-managed-tor-onion-seed.md`. Windows packaging now
fails if `tor.exe` is absent; a release must not silently lose onion coverage.

## 3. Preflight, then package

From the node repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\zsilent-core\scripts\prepare-release.ps1
```

This read-only preflight checks version alignment, required trusted-setup and
network files, the managed Tor executable, and hashes every embedded binary.
After reviewing its output, build the MSI explicitly:

```powershell
powershell -ExecutionPolicy Bypass -File .\zsilent-core\scripts\prepare-release.ps1 -Package
```

The result is staged under `build/release/<version>/` with
`release-manifest.json` and `SHA256SUMS.txt`. Verify that the MSI hash is the
same value inserted into release notes and the Discord draft.

## 4. Recovery release checks for 1.9.0

- Confirm the canonical recovery checkpoint is block **10,887**.
- Exercise migration from a representative 1.8 data directory.
- Confirm an automatic backup is created before recovery/migration.
- Confirm wallet files are preserved; recovery must not delete a wallet.
- Confirm a 1.8 node is rejected as protocol-incompatible rather than allowed
  to follow or advertise the 1.9 chain.
- Install the MSI on a clean Windows VM and verify direct plus managed-Tor
  startup.

## 5. CI and publication gates

The manual GitHub workflow requires the official `zoka.exe` and `zokahd.exe`
SHA-256 values. It verifies app, tag, source and binary versions, prepares the
pinned Tor bundle, and uploads build artifacts. `publish_release` defaults to
false; leave it false for verification-only runs.

Only after independent installer testing:

1. publish the GitHub release with its final checksums;
2. replace the hash placeholder in the Discord draft and set `draft` to
   `false`;
3. preview the notice again;
4. send it with the explicit confirmation guard documented in the bot README.
