package com.zsilent.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AppSection {
    Dashboard,
    // Send y Receive dejaron de ser secciones: viven como pestañas dentro de
    // Wallet, igual que en el prototipo. Ver docs/GUIA_CAMBIO_VISUAL.md, D3.
    Wallet,
    Node,
    Mining,
    Stratum,
    Explorer,
    Privacy,
    Logs,
    Settings
}

enum class OnboardingStep {
    Welcome,
    SelfCustody,
    Wallet,
    Recovery,
    VerifySeed,
    Security,
    Account,
    Privacy,
    Tutorial,
    Dashboard
}

enum class WalletSetupMode {
    CreateNew,
    ImportSeed
}

enum class SeedWordCount(val words: Int) {
    Twelve(12),
    TwentyFour(24)
}

enum class ServiceStatus {
    Offline,
    Starting,
    Online,
    Stopping,
    Error
}

@Serializable
data class ChainHeightResponse(
    val height: Long = 0
)

// Local node's own view of its sync state (GET /sync/status). Unlike
// NodeStats from the bootstrap RPC, target_height/lag here are fed by the
// node's combined P2P-mesh + RPC sync machinery, so they stay meaningful
// even when the centralized bootstrap endpoint is saturated or offline.
@Serializable
data class NodeSyncStatus(
    val status: String = "",
    val mode: String = "",
    @SerialName("ready_to_mine")
    val readyToMine: Boolean = false,
    @SerialName("local_height")
    val localHeight: Long = 0,
    @SerialName("target_height")
    val targetHeight: Long = 0,
    val lag: Long = 0,
    @SerialName("connected_peers")
    val connectedPeers: Long = 0,
    @SerialName("min_peers")
    val minPeers: Long = 0,
    @SerialName("startup_phase")
    val startupPhase: String = "",
    @SerialName("last_sync_source")
    val lastSyncSource: String? = null,
    @SerialName("last_sync_event")
    val lastSyncEvent: String? = null,
    @SerialName("last_sync_error")
    val lastSyncError: String? = null,
    @SerialName("last_updated")
    val lastUpdated: Long = 0,
    @SerialName("gossip_rx_block_response")
    val gossipRxBlockResponse: Long = 0,
    @SerialName("gossip_publish_err")
    val gossipPublishErr: Long = 0
)

@Serializable
data class NodeStats(
    val height: Long = 0,
    val difficulty: Long = 0,
    @SerialName("hashrate_hs")
    val hashrateHs: Double = 0.0,
    @SerialName("emission_zka")
    val emissionZka: Double = 0.0,
    /**
     * ZKA emitidos según la propia cadena, en átomos.
     *
     * Se prefiere a [emissionZka] para mostrar la cifra: aquél es un decimal
     * recalculado desde la fórmula de halving —una estimación derivada de la
     * altura— y pasar por punto flotante redondea la última cifra. Este es el
     * total que la cadena lleva contabilizado, entero y exacto.
     *
     * Cero en nodos anteriores a 2.0, que no envían el campo.
     */
    @SerialName("total_supply_emitted_atoms")
    val totalSupplyEmittedAtoms: Long = 0,
    @SerialName("max_supply_atoms")
    val maxSupplyAtoms: Long = 0,
    @SerialName("remaining_supply_atoms")
    val remainingSupplyAtoms: Long = 0,
    @SerialName("last_block_time")
    val lastBlockTime: Long = 0,
    @SerialName("connected_peers")
    val connectedPeers: Long = 0,
    @SerialName("miners_online")
    val minersOnline: Long = 0,
    @SerialName("remote_miners_online")
    val remoteMinersOnline: Long = 0,
    @SerialName("reported_hashrate_hps")
    val reportedHashrateHps: Long = 0,
    @SerialName("local_mining")
    val localMining: Boolean = false
)

@Serializable
data class PeerInfo(
    @SerialName("peer_id")
    val peerId: String = "",
    val transport: String = "unknown",
    val address: String? = null
)

@Serializable
data class PeerTransportCounts(
    val tor: Long = 0,
    val direct: Long = 0,
    val other: Long = 0
)

@Serializable
data class PeersResponse(
    @SerialName("connected_peers")
    val connectedPeers: Long = 0,
    @SerialName("by_transport")
    val byTransport: PeerTransportCounts = PeerTransportCounts(),
    val peers: List<PeerInfo> = emptyList()
)

@Serializable
data class WalletSyncStatus(
    val wallet: String = "",
    @SerialName("chain_height")
    val chainHeight: Long = 0,
    @SerialName("scanned_height")
    val scannedHeight: Long = 0,
    @SerialName("progress_percent")
    val progressPercent: Double = 0.0
)

// Block metadata as served by GET /blocks/:height, /blocks/hash/:hash and
// /blocks/latest. Mirrors the node's block_metadata_json exactly.
//
// Note what is deliberately absent: private transactions contribute only
// counts here. The node never serves nullifier owners or private amounts on
// this endpoint, so the explorer cannot leak them either.
@Serializable
data class BlockSummary(
    val hash: String = "",
    val height: Long = 0,
    val timestamp: Long = 0,
    @SerialName("state_root")
    val stateRoot: String = "",
    @SerialName("previous_hash")
    val previousHash: String = "",
    val nonce: Long = 0,
    // Genesis (height 0) has no recorded difficulty — the node sends null.
    @SerialName("difficulty_bits")
    val difficultyBits: Int? = null,
    @SerialName("size_bytes")
    val sizeBytes: Long = 0,
    @SerialName("reward_atoms")
    val rewardAtoms: Long = 0,
    @SerialName("reward_zka_display")
    val rewardZkaDisplay: String = "",
    @SerialName("transactions_count")
    val transactionsCount: Int = 0,
    @SerialName("private_tx_count")
    val privateTxCount: Int = 0,
    @SerialName("private_nullifier_count")
    val privateNullifierCount: Int = 0,
    @SerialName("private_coinbase")
    val privateCoinbase: Boolean = false
)

// GET /blocks/:height/transactions. The node exposes only the hash of each
// public transaction — sender, receiver and amount are private by protocol.
@Serializable
data class BlockTransactionRef(
    val hash: String = ""
)

@Serializable
data class BlockTransactions(
    @SerialName("block_height")
    val blockHeight: Long = 0,
    @SerialName("transactions_count")
    val transactionsCount: Int = 0,
    val transactions: List<BlockTransactionRef> = emptyList()
)

// GET /accounts/:address — existence only. The node deliberately withholds
// balance and nonce here; those are private.
@Serializable
data class AccountCheck(
    val address: String = "",
    val valid: Boolean = false
)

/** `GET /quantic/address/<corta>`: la completa que el nodo tiene para una corta. */
@Serializable
data class ShortAddressLookup(
    @SerialName("short_address")
    val shortAddress: String = "",
    val address: String = ""
)

// POST /private/view-scan-key. Finds the private outputs belonging to a scan
// key. The key itself is a secret that reveals every incoming private payment
// to its owner, so it is only ever sent to a node running on this machine.
@Serializable
data class ViewScanMatch(
    @SerialName("tx_hash")
    val txHash: String = "",
    @SerialName("output_id")
    val outputId: String = "",
    @SerialName("output_index")
    val outputIndex: Int = 0,
    @SerialName("asset_tag")
    val assetTag: String = "",
    val amount: Long = 0,
    @SerialName("is_change")
    val isChange: Boolean = false,
    val timestamp: Long = 0
)

@Serializable
data class ViewScanResponse(
    val address: String = "",
    @SerialName("scanned_transactions")
    val scannedTransactions: Int = 0,
    @SerialName("matching_outputs")
    val matchingOutputs: Int = 0,
    @SerialName("total_amount_atoms")
    val totalAmountAtoms: Long = 0,
    val matches: List<ViewScanMatch> = emptyList()
)

@Serializable
data class ViewScanKeyRequest(
    @SerialName("scan_key_hex")
    val scanKeyHex: String,
    val limit: Int? = null
)

// `zoka wallet-receive --hybrid --json`. La zkq1 vale para recibir solo si
// `hybrid_activation_height` no es nulo; antes nadie puede pagarle. Un binario
// sin `pq-experimental` no la informa (el comando falla) y la app la oculta.
@Serializable
data class WalletReceiveInfo(
    val wallet: String = "",
    val address: String = "",
    @SerialName("hybrid_address")
    val hybridAddress: String? = null,
    @SerialName("hybrid_activation_height")
    val hybridActivationHeight: Long? = null
)

// GET /mempool/tx/:hash — only ever describes an unconfirmed transaction.
// The node has no tx-hash index over confirmed blocks, so a hash that is
// already mined 404s here rather than resolving.
@Serializable
data class MempoolTransaction(
    val hash: String = "",
    val timestamp: Long = 0,
    val status: String = ""
)

// GET /private/tx/:hash, reducido a lo que muestra el explorador. El cuerpo
// completo trae pruebas y anillos (unos 350 KB) que acá no se usan. `status` y
// `block_height` los calcula el nodo contra la cadena: el estado que viaja
// dentro de la transacción lo estampa quien la crea y no cambia nunca.
// Valores de `status`: Confirmed, Pending, Rejected, Spent o Unknown.
//
// `fee` queda afuera a propósito: en las pruebas nuevas la comisión va oculta y
// el campo viene en 0, que en pantalla se leería como "sin comisión".
@Serializable
data class PrivateTxLookup(
    @SerialName("tx_hash")
    val txHash: String = "",
    val status: String = "",
    @SerialName("block_height")
    val blockHeight: Long? = null,
    val timestamp: Long = 0
)

@Serializable
data class WalletListResponse(
    val network: String = "",
    @SerialName("wallet_storage")
    val walletStorage: String = "",
    @SerialName("private_wallets")
    val privateWallets: List<LocalWallet> = emptyList()
)

@Serializable
data class LocalWallet(
    val name: String = "",
    val address: String = "",
    /** La dirección corta (unos 57 caracteres): la que se comparte y va en el QR. */
    @SerialName("short_address")
    val shortAddress: String = "",
    @SerialName("balance_atoms")
    val balanceAtoms: Long = 0,
    val balance: String = "0",
    @SerialName("locked_balance_atoms")
    val lockedBalanceAtoms: Long = 0,
    @SerialName("locked_balance")
    val lockedBalance: String = "0",
    @SerialName("total_balance_atoms")
    val totalBalanceAtoms: Long = 0,
    @SerialName("total_balance")
    val totalBalance: String = "0",
    @SerialName("tx_count")
    val txCount: Int = 0,
    val encrypted: Boolean = true,
    val unlocked: Boolean = false
)

@Serializable
data class WalletBalance(
    val wallet: String = "",
    val address: String = "",
    @SerialName("balance_atoms")
    val balanceAtoms: Long = 0,
    val balance: String = "0",
    @SerialName("locked_balance_atoms")
    val lockedBalanceAtoms: Long = 0,
    @SerialName("locked_balance")
    val lockedBalance: String = "0",
    @SerialName("total_balance_atoms")
    val totalBalanceAtoms: Long = 0,
    @SerialName("total_balance")
    val totalBalance: String = "0",
    @SerialName("view_only")
    val viewOnly: Boolean = false,
    /** Zoka Quantic: lo más que sale en un envío ahora (saldo menos comisión). */
    @SerialName("max_sendable_atoms")
    val maxSendableAtoms: Long = 0
)

@Serializable
data class WalletHistory(
    val wallet: String = "",
    val count: Int = 0,
    val transactions: List<WalletHistoryEntry> = emptyList()
)

@Serializable
data class WalletHistoryEntry(
    @SerialName("tx_hash")
    val txHash: String = "",
    val status: String = "",
    val timestamp: Long = 0,
    val outputs: Int = 0,
    @SerialName("amount_hidden")
    val amountHidden: Boolean = true,
    @SerialName("fee_hidden")
    val feeHidden: Boolean = true,
    // Zoka Quantic: la wallet conoce sus propios montos (para todos los demás
    // siguen ocultos en la cadena).
    val height: Long = 0,
    /** "received", "sent" o "coinbase". */
    val kind: String = "",
    @SerialName("amount_atoms")
    val amountAtoms: Long = 0,
    val confirmations: Long = 0
)

data class FeeEstimate(
    val amount: String,
    val priority: String,
    val fee: String,
    /** Zoka Quantic: lo más que se puede enviar ahora, en átomos. */
    val maxSendableAtoms: Long = 0,
    /** Por qué no alcanza, si no alcanza. */
    val problem: String? = null
)

data class SendResult(
    val hash: String,
    val transactionHashes: List<String> = listOf(hash).filter { it.isNotBlank() },
    val splitCount: Int = transactionHashes.size,
    val from: String,
    val to: String,
    val queuedDb: String?
)

/** Severidad de una línea de log. El prototipo colorea por esto, no por fuente. */
enum class LogLevel { Info, Success, Warning, Error }

data class RuntimeLog(
    val source: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    /**
     * Por defecto `Info` para que los 92 sitios que ya construían `RuntimeLog`
     * sigan compilando sin tocarse. Los que sepan su severidad pueden
     * declararla; el visor solo aplica su heurística cuando esto es `Info`.
     */
    val level: LogLevel = LogLevel.Info
)

@Serializable
data class LocalChainHeightResponse(
    val height: Long = 0
)

@Serializable
data class IrohIdResponse(
    @SerialName("iroh_endpoint_id")
    val irohEndpointId: String = ""
)

@Serializable
data class RpcSyncResponse(
    @SerialName("start_height")
    val startHeight: Long = 0,
    @SerialName("end_height")
    val endHeight: Long = 0,
    val imported: Long = 0,
    val events: List<String> = emptyList()
)

@Serializable
data class MiningSnapshot(
    val running: Boolean = false,
    @SerialName("blocks_mined")
    val blocksMined: Long = 0,
    @SerialName("chain_height")
    val chainHeight: Long = 0,
    @SerialName("difficulty_bits")
    val difficultyBits: Int = 0,
    @SerialName("hashrate_hps")
    val hashrateHps: Long = 0,
    @SerialName("avg_solve_time_secs")
    val avgSolveTimeSecs: Double = 0.0,
    @SerialName("last_solve_time_secs")
    val lastSolveTimeSecs: Double = 0.0,
    @SerialName("total_rewards")
    val totalRewards: String = "0",
    @SerialName("total_rewards_atoms")
    val totalRewardsAtoms: Long = 0,
    @SerialName("locked_rewards")
    val lockedRewards: String = "0",
    @SerialName("locked_rewards_atoms")
    val lockedRewardsAtoms: Long = 0,
    @SerialName("locked_reward_count")
    val lockedRewardCount: Int = 0,
    @SerialName("next_reward_unlock_height")
    val nextRewardUnlockHeight: Long? = null,
    @SerialName("next_reward_unlock_in_blocks")
    val nextRewardUnlockInBlocks: Long? = null,
    @SerialName("coinbase_maturity_blocks")
    val coinbaseMaturityBlocks: Long = 60,
    @SerialName("total_supply_emitted")
    val totalSupplyEmitted: String = "0",
    val mode: String = "",
    val algorithm: String = "",
    @SerialName("available_cpu_threads")
    val availableCpuThreads: Int = 1,
    @SerialName("legacy_pow_sim_detected")
    val legacyPowSimDetected: Boolean = false
)

@Serializable
data class PendingPrivateTransfers(
    val count: Int = 0,
    val db: String = "",
    @SerialName("pending_private_tx_hashes")
    val pendingPrivateTxHashes: List<String> = emptyList()
)

// GET /quantic/stratum: el servidor Stratum del nodo (`--stratum`). Con
// `configured` y sin `listening` ni `error`, está arrancando (se abre al final
// del arranque del nodo). Un nodo 3.0.2 no manda `configured` ni `error`.
@Serializable
data class StratumStatus(
    val configured: String? = null,
    val listening: String? = null,
    val error: String? = null,
    val miners: Int = 0,
    @SerialName("accepted_shares")
    val acceptedShares: Long = 0,
    @SerialName("rejected_shares")
    val rejectedShares: Long = 0,
    @SerialName("blocks_found")
    val blocksFound: Long = 0
)

@Serializable
data class PersistedSetup(
    val onboardingComplete: Boolean = false,
    val walletName: String = "",
    val walletAddress: String = "",
    val managedWallets: List<String> = emptyList(),
    val managedWalletAddresses: List<String> = emptyList(),
    val nodeEnabled: Boolean = true,
    val miningEnabled: Boolean = false,
    val balancesHidden: Boolean = false,
    val autoLockMinutes: Int = 10,
    // El Intro se muestra una sola vez, en la primera corrida tras instalar.
    //
    // Default TRUE a proposito, al reves que en ZSilentUiState: un archivo de
    // setup que ya existe es de alguien que viene usando la app, y actualizar
    // no es instalar. Si el campo falta, esa persona no ve el Intro. En una
    // instalacion limpia no hay archivo, se arranca de los defaults de
    // ZSilentUiState -- donde es false -- y el Intro aparece una vez.
    val introSeen: Boolean = true,
    val darkTheme: Boolean = true,
    // Ajustes de apariencia (D4). Ausentes en archivos previos: cozy y
    // animaciones activas son lo que veian esos usuarios, asi que ese es el
    // default que no cambia nada al actualizar.
    val compactDensity: Boolean = false,
    val animationsEnabled: Boolean = true,
    // Absent in setup files written before the explorer existed. Those users
    // saw an Explorer entry in 1.9.1, so defaulting to true keeps it where
    // they expect rather than making the section disappear on upgrade.
    val explorerEnabled: Boolean = true,
    // El servidor Stratum del nodo para XMRig (3.0.2). Apagado hasta que el
    // usuario lo encienda, y solo para esta PC: abrirlo a la red local es otra
    // decisión, aparte.
    val stratumEnabled: Boolean = false,
    val stratumAllowLan: Boolean = false,
    /**
     * Direcciones onion que el usuario agregó a mano en Peers Tor.
     *
     * Se guardan acá y no en el peerstore del nodo porque son una decisión del
     * usuario, no un descubrimiento de la red: el peerstore descarta una
     * dirección tras suficientes fallos, y una que alguien agregó a propósito
     * tiene que sobrevivir a que el otro esté apagado una semana.
     */
    val manualPeers: List<String> = emptyList(),
    val torEnabled: Boolean = true,
    val torOnly: Boolean = false,
    val torSocksHost: String = "127.0.0.1",
    val torSocksPort: Int = 19050,
    val managedTorEnabled: Boolean = true,
    val onionSeedEnabled: Boolean = false,
    val managedTorSocksPort: Int = 19050
)
