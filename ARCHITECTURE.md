# ZSilent Core Architecture

## Goal

ZSilent Core is the desktop control center for Zoka. The UI must never become the owner of private cryptography. It orchestrates wallet, node, mining, encrypted storage, and network status through explicit service boundaries.

## Modules

- `core/wallet`: wallet lifecycle, import/export flows, address derivation facade, transaction signing facade.
- `core/node`: local fullnode process control, RPC state detection, log streaming.
- `core/mining`: mining controls, reward wallet selection, thread configuration, session state.
- `core/privacy`: redaction, display privacy, sensitive value handling, audit rules.
- `core/storage`: encrypted local settings, setup metadata, encrypted wallet metadata, backup policy.
- `rpc`: local node HTTP/WebSocket client.
- `services`: external binary discovery and command bridges.
- `models`: serialized DTOs and local UI/domain models.
- `ui/screens`: Compose screens.
- `ui/components`: reusable Material components.
- `ui/theme`: Material 3 color schemes.

## Privacy Rules

- Seeds, private keys, spending keys, view keys, and passwords must not be logged.
- Exported wallet JSON must be encrypted.
- Telemetry is disabled by default.
- Balance display can be hidden at UI level.
- Remote node mode must be explicit; local node is the default.
- Passwords move through stdin or environment variables instead of process arguments.
- Recovery phrases must not be sent as process arguments; restoration uses `--seed-env` from the desktop bridge.

## Integration Phases

1. First-run onboarding: create/restore wallet, recovery phrase, security, node, mining.
2. Desktop shell, navigation, process bridge, RPC polling.
3. Secure password/seed channel for node/wallet commands.
4. Wallet read flows through local CLI/RPC.
5. Send/receive signing flows with confirmation modals and QR.
6. Mining controls with live session metrics.
7. Encrypted settings and backup store.
8. MSI icon, metadata, update channel.
