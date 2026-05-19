# ZSilent Core Roadmap

ZSilent Core is being built as the private desktop control center for the Zoka network: wallet, full node, mining, privacy, logs, and local operations from one native app.

## Current Status

The app is in an early technical MVP stage. It compiles, opens as a Compose Desktop app, detects the local Zoka binaries, reads local wallet metadata, unlocks wallet data through the CLI bridge, displays balances/history, shows receive addresses, renders QR codes, polls node RPC stats, starts/stops the local node, starts/stops mining, monitors the local private transfer queue, and keeps personal wallet data separate from global network/miner state in the main screens.

## Phase 1 - Desktop Foundation

Status: mostly complete

- Kotlin/JVM 17 project with Gradle Kotlin DSL.
- Compose Desktop and Material 3 shell.
- Dark/light theme support.
- Sidebar navigation.
- Dashboard, Wallet, Send, Receive, Node, Mining, Privacy, Logs, and Settings screens.
- Windows MSI packaging configuration.

## Phase 2 - First-Run Onboarding

Status: mostly complete

- Create wallet from the desktop app.
- Restore wallet from seed.
- Choose 12 or 24 seed words.
- Welcome -> self-custody warning -> create/import wallet -> show seed -> verify seed -> create password -> create account -> privacy setup -> mini tutorial -> dashboard.
- Generate a recovery phrase before account creation, then create the encrypted wallet by restoring that verified seed locally.
- Configure master password.
- Configure local node/mining preferences.
- Configure balance hiding during setup.
- Persist setup metadata locally.
- Clear seed/password setup fields after wallet creation or restore.

Remaining:

- Improve validation/error messages.
- Add stronger “backup confirmed” UX.

## Phase 3 - Wallet Read Operations

Status: in progress

- List real local Zoka wallets.
- Select active wallet.
- Unlock wallet through `--password-env`.
- Load private balance.
- Load locked balance.
- Load local private transaction history.
- Lock wallet from UI.
- Hide balances on screen.
- Receive screen with real address and QR.
- Create additional wallets after onboarding.
- Keep ZSilent-managed wallets visible after setup instead of forcing only the first onboarding wallet.
- Export encrypted wallet backup from UI.
- Restore encrypted wallet backup from UI.
- Restore encrypted backups through the Zoka backend with password validation and rollback on failed unlock.

Remaining:

- Import additional wallets after onboarding.
- Address book.
- Auto-lock by inactivity.
- Better multi-wallet account management.

## Phase 4 - Send Flow

Status: in progress

- Validate recipient address before sending.
- Enter amount.
- Estimate fee.
- Build transaction preview.
- Show confirmation modal before signing.
- Sign locally.
- Submit transaction through local node/CLI.
- Show pending/confirmed/failed state.
- Never expose private keys, seed, or spending key outside the device.

Implemented:

- Basic recipient validation for `zka1...` addresses.
- Fee estimation through the Zoka CLI.
- Transaction preview before signing.
- Local signing/submission through `tx-send --password-env`.
- Send result with transaction hash and local node DB queue path.
- Split transaction hashes are shown individually.
- Send status is correlated with the local node queue and local wallet history when available.
- Desktop bridge preserves password handling through environment variables.

Remaining:

- Stronger address validation before calling the backend.
- More explicit confirmation modal.
- Better confirmed/failed tracking directly from block inclusion data.
- Optional priority selector for fee estimation.

## Phase 5 - Node Control

Status: in progress

- Detect Zoka binaries.
- Start local node.
- Stop local node.
- Stream node logs with sensitive-line redaction.
- Poll local RPC stats.
- Show height, difficulty, hashrate, and emission.

Remaining:

- Detect already-running node.
- Restart node.
- Configure RPC/P2P ports.
- Configure data directory.
- Show peers.
- Show CPU/RAM/disk usage.
- Support explicit remote-node mode later.

## Phase 6 - Mining Control

Status: in progress

- Select reward wallet/address.
- Configure threads.
- Start mining from UI.
- Stop mining from UI.
- Detect external/node-controlled mining and monitor it instead of launching another miner.
- Show hashrate.
- Show network height and difficulty.
- Show the local node private-transfer queue.
- Stream mining logs with sensitive-line redaction.
- Show active-wallet available/locked/total funds instead of presenting global mined totals as personal data.
- Show local mining accounts with each wallet's available and locked funds.

Remaining:

- Persist auto-start mining preference.
- Add per-wallet mined-lifetime accounting if the backend exposes it.
- Add clearer maturity/next-unlock data per wallet address instead of global miner-state unlocks.
- Add mining pause/resume and power presets from the desktop app.

## Phase 7 - Privacy And Security Hardening

Status: partial

- No telemetry by default.
- Passwords/seeds are not sent through command-line arguments in new desktop bridge flows.
- Logs redact obvious sensitive lines.
- Balance hiding exists in UI.
- Login gate unlocks the active wallet with its wallet password.
- Auto-lock timer clears visible wallet/send data after inactivity.
- Manual lock is available from Privacy.
- Login reset returns the app to onboarding without deleting wallet files.

Remaining:

- Encrypt all local app settings that can reveal sensitive usage patterns.
- Add stronger memory cleanup around password fields where possible.
- Add privacy audit checklist before release.
- Ensure exported JSON is always encrypted.
- Add guardrails against accidental secret display.

## Phase 8 - Packaging And Release

Status: initial

- MSI target configured.
- Package name is `ZSilent Core`.
- Windows shortcuts enabled.

Remaining:

- Add application icon.
- Add installer branding.
- Validate MSI generation on clean Windows environment.
- Add release verification checklist.
- Prepare future Linux/macOS packaging.
- Plan update mechanism.

## Next Recommended Milestones

1. Add encrypted backup export/import UI.
2. Add node configuration, restart, and richer already-running-node detection.
3. Add auto-lock and privacy hardening pass.
4. Add per-wallet mining reward accounting from backend data.
5. Polish the Wallet, Send, Node, and Mining UX sections.
6. Polish MSI packaging.
