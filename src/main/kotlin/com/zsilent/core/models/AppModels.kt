package com.zsilent.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AppSection {
    Dashboard,
    Wallet,
    Send,
    Receive,
    Node,
    Mining,
    MiningPool,
    P2PExchange,
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
    val minPeers: Long = 0
)

@Serializable
data class NodeStats(
    val height: Long = 0,
    val difficulty: Long = 0,
    @SerialName("hashrate_hs")
    val hashrateHs: Double = 0.0,
    @SerialName("emission_zka")
    val emissionZka: Double = 0.0,
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
    val viewOnly: Boolean = false
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
    val feeHidden: Boolean = true
)

data class FeeEstimate(
    val amount: String,
    val priority: String,
    val fee: String
)

data class SendResult(
    val hash: String,
    val transactionHashes: List<String> = listOf(hash).filter { it.isNotBlank() },
    val splitCount: Int = transactionHashes.size,
    val from: String,
    val to: String,
    val queuedDb: String?
)

data class RuntimeLog(
    val source: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis()
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
    val darkTheme: Boolean = true,
    val torEnabled: Boolean = true,
    val torOnly: Boolean = false,
    val torSocksHost: String = "127.0.0.1",
    val torSocksPort: Int = 19050,
    val managedTorEnabled: Boolean = true,
    val onionSeedEnabled: Boolean = false,
    val managedTorSocksPort: Int = 19050
)
