# ZSilent Core

ZSilent Core is the official desktop wallet of Zoka Quantic, for Windows. It
runs its own full node next to the wallet and adds a Stratum server for XMRig,
a block explorer that reads that node, and an optional Tor mode, in one
Material 3 interface. It runs on Zoka Quantic Testnet, whose coins have no
value.

Download the MSI from [Releases](https://github.com/zoka-network/zsilent-core/releases/latest)
and check it against `SHA256SUMS.txt` and its signature, `SHA256SUMS.txt.sig`
([how](https://github.com/zoka-network/zokanetwork/blob/main/docs/release-verification.md)).

## Stack

- Kotlin/JVM 17+
- Compose Desktop
- Material 3
- Gradle Kotlin DSL
- Kotlinx Serialization
- Coroutines and Flows
- Ktor HTTP/WebSocket client
- Windows MSI packaging

## Local Development

Common commands:

```powershell
.\gradlew.bat run
.\gradlew.bat build
.\gradlew.bat packageMsi -Pzsilent.requireTorBundle=true
```

Release installers must be prepared through
[`docs/RELEASE.md`](docs/RELEASE.md). The preflight verifies that the app,
`zoka`, and `zokahd` versions match and emits checksums without publishing.

The app looks for Zoka binaries in:

- `../target/release/zoka.exe`
- `../target/release/zokahd.exe`
- `../target/debug/zoka.exe`
- `../target/debug/zokahd.exe`

The first implementation controls the existing Rust binaries through a local process bridge and reads node state through the local RPC API.

## Privacy Defaults

- No telemetry.
- No private keys in logs.
- JSON exports must be encrypted before being enabled.
- Wallet history is local-first.
- Remote node support is planned but disabled by default.

## First Run Flow

ZSilent Core starts in onboarding mode:

1. Choose create wallet or restore seed. Restoring takes the 24 words of a
   wallet from ZSilent, ZKAPriv or the web wallet.
2. Set the master password and auto-lock policy.
3. Create or restore the wallet through the node's `quantic-pq-wallet` command.
4. Save the generated 24-word recovery phrase when creating a new wallet.
5. Enter the dashboard only after setup is complete. The node starts and
   synchronizes; to mine, turn on the Stratum server and run XMRig.

The wallet bridge passes secrets through the environment of the process it
starts, never on the command line:

- `--password-env ZSILENT_WALLET_PASSWORD`
- `--phrase-env ZSILENT_RECOVERY_PHRASE`
- `--new-password-env ZSILENT_NEW_WALLET_PASSWORD`

The Zoka CLI also supports `--password-stdin` for terminal and automation
flows. Zoka Quantic wallets always have 24 words (256 bits).
