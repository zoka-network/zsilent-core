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

// ── P2P exchange ─────────────────────────────────────────────────────────
//
// Mirrors the node's `p2p_exchange::Offer`. Amounts stay integers on this side
// too: ZKA in atoms, USDT in micro-USDT (TRC-20 carries six decimals). Parsing
// them into Double for display is fine; doing arithmetic on that Double is not,
// because the node decides payment matching on the integer value.
@Serializable
data class OfferSummary(
    val id: String = "",
    @SerialName("seller_address")
    val sellerAddress: String = "",
    @SerialName("zka_atoms")
    val zkaAtoms: Long = 0,
    @SerialName("micro_usdt")
    val microUsdt: Long = 0,
    @SerialName("usdt_receive_address")
    val usdtReceiveAddress: String = "",
    val status: String = "Open",
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("claim_expires_at")
    val claimExpiresAt: Long? = null,
    @SerialName("claimed_by")
    val claimedBy: String? = null
)

@Serializable
data class OfferList(
    val offers: List<OfferSummary> = emptyList(),
    val verification: String = "unknown"
)

@Serializable
data class ClaimOfferResponse(
    @SerialName("offer_id")
    val offerId: String = "",
    @SerialName("seller_address")
    val sellerAddress: String = "",
    val status: String = "claim_requested"
)

@Serializable
data class CreateOfferRequest(
    @SerialName("zka_atoms")
    val zkaAtoms: Long,
    @SerialName("micro_usdt")
    val microUsdt: Long,
    @SerialName("usdt_receive_address")
    val usdtReceiveAddress: String
)

@Serializable
data class ReportPaymentFailureRequest(
    @SerialName("tron_tx_hash")
    val tronTxHash: String,
    @SerialName("acknowledge_public_disclosure")
    val acknowledgePublicDisclosure: Boolean
)

@Serializable
data class PaymentFailureSummary(
    @SerialName("offer_id")
    val offerId: String = "",
    @SerialName("seller_address")
    val sellerAddress: String = "",
    @SerialName("buyer_address")
    val buyerAddress: String = "",
    @SerialName("tron_tx_hash")
    val tronTxHash: String = "",
    val timestamp: Long = 0
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
data class TeamMiningControlRequest(
    val threads: Int = 1
)

@Serializable
data class JoinMiningTeamRequest(
    @SerialName("invite_code")
    val inviteCode: String
)

@Serializable
data class TeamMiningMember(
    val address: String = "",
    val active: Boolean = false,
    val expelled: Boolean = false,
    @SerialName("total_work")
    val totalWork: Long = 0,
    @SerialName("accepted_shares")
    val acceptedShares: Long = 0,
    @SerialName("blocks_paid")
    val blocksPaid: Long = 0,
    @SerialName("share_difficulty_bits")
    val shareDifficultyBits: Int = 0,
    @SerialName("estimated_hashrate_hps")
    val estimatedHashrateHps: Long = 0
)

@Serializable
data class TeamMiningTemplate(
    val id: String = "",
    val height: Long = 0,
    @SerialName("recipient_address")
    val recipientAddress: String = "",
    @SerialName("network_difficulty_bits")
    val networkDifficultyBits: Int = 0
)

@Serializable
data class MiningTeam(
    @SerialName("team_id")
    val teamId: String = "",
    @SerialName("invite_code")
    val inviteCode: String = "",
    val coordinator: String = "",
    @SerialName("backup_coordinator")
    val backupCoordinator: String? = null,
    val epoch: Long = 0,
    val revision: Long = 0,
    val members: Map<String, TeamMiningMember> = emptyMap(),
    @SerialName("total_work")
    val totalWork: Long = 0,
    @SerialName("confirmed_blocks")
    val confirmedBlocks: Long = 0,
    @SerialName("active_template")
    val activeTemplate: TeamMiningTemplate? = null,
    @SerialName("last_hijacked_recipient")
    val lastHijackedRecipient: String? = null
)

@Serializable
data class TeamMiningStatus(
    val enabled: Boolean = false,
    val hashing: Boolean = false,
    val threads: Long = 1,
    @SerialName("join_status")
    val joinStatus: String = "not_joined",
    @SerialName("local_address")
    val localAddress: String = "",
    val team: MiningTeam? = null,
    @SerialName("combined_hashrate_hps")
    val combinedHashrateHps: Long = 0,
    @SerialName("local_accepted_shares")
    val localAcceptedShares: Long = 0,
    @SerialName("team_accepted_shares")
    val teamAcceptedShares: Long = 0,
    @SerialName("estimated_turn")
    val estimatedTurn: Int? = null
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
    // Absent in setup files written before the explorer existed. Those users
    // saw an Explorer entry in 1.9.1, so defaulting to true keeps it where
    // they expect rather than making the section disappear on upgrade.
    val explorerEnabled: Boolean = true,
    // Moves real funds, so legacy setup files and fresh installs stay opted
    // out until the user enables the experimental section deliberately.
    val p2pExchangeEnabled: Boolean = false,
    // Team mining is experimental and can produce uneven short-term payouts,
    // so it is absent from the sidebar until explicitly enabled.
    val teamMiningEnabled: Boolean = false,
    val p2pExchangeRisksAccepted: Boolean = false,
    val tronApiUrl: String = "https://api.trongrid.io",
    val tronApiKey: String = "",
    val tronFallbackUrls: String = "",
    val torEnabled: Boolean = true,
    val torOnly: Boolean = false,
    val torSocksHost: String = "127.0.0.1",
    val torSocksPort: Int = 19050,
    val managedTorEnabled: Boolean = true,
    val onionSeedEnabled: Boolean = false,
    val managedTorSocksPort: Int = 19050
)
