package com.zsilent.core.ui

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.core.node.NodeController
import com.zsilent.core.core.mining.MiningController
import com.zsilent.core.core.privacy.PrivacyGuards
import com.zsilent.core.core.process.ZokaProcessKiller
import com.zsilent.core.core.storage.SetupStore
import com.zsilent.core.core.tor.TorLaunchConfig
import com.zsilent.core.core.tor.TorSupervisor
import com.zsilent.core.core.wallet.WalletService
import com.zsilent.core.core.wallet.WalletBackupResult
import com.zsilent.core.core.wallet.WalletCreationResult
import com.zsilent.core.core.wallet.WalletImportResult
import com.zsilent.core.core.wallet.WalletPassphraseChangeResult
import com.zsilent.core.models.AppSection
import com.zsilent.core.models.FeeEstimate
import com.zsilent.core.models.LocalChainHeightResponse
import com.zsilent.core.models.LocalWallet
import com.zsilent.core.models.MiningSnapshot
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.OnboardingStep
import com.zsilent.core.models.PendingPrivateTransfers
import com.zsilent.core.models.PersistedSetup
import com.zsilent.core.models.RpcSyncResponse
import com.zsilent.core.models.RuntimeLog
import com.zsilent.core.models.SeedWordCount
import com.zsilent.core.models.SendResult
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.models.WalletBalance
import com.zsilent.core.models.WalletHistory
import com.zsilent.core.models.WalletSetupMode
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

data class ZSilentUiState(
    val onboardingComplete: Boolean = false,
    val onboardingStep: OnboardingStep = OnboardingStep.Welcome,
    val walletSetupMode: WalletSetupMode = WalletSetupMode.CreateNew,
    val seedWordCount: SeedWordCount = SeedWordCount.TwentyFour,
    val onboardingWalletName: String = "zsilent",
    val onboardingSeedPhrase: String = "",
    val onboardingPassword: String = "",
    val onboardingPasswordConfirm: String = "",
    val onboardingNodeEnabled: Boolean = true,
    val onboardingMiningEnabled: Boolean = false,
    val onboardingAutoLockMinutes: Int = 10,
    val onboardingSelfCustodyConfirmed: Boolean = false,
    val onboardingSeedCheckFirst: String = "",
    val onboardingSeedCheckSecond: String = "",
    val onboardingPrivacyConfirmed: Boolean = false,
    val onboardingTutorialConfirmed: Boolean = false,
    val onboardingBusy: Boolean = false,
    val onboardingError: String? = null,
    val onboardingWalletAddress: String = "",
    val onboardingRecoveryPhrase: String? = null,
    val onboardingRecoveryConfirmed: Boolean = false,
    val selectedSection: AppSection = AppSection.Dashboard,
    val darkTheme: Boolean = true,
    val appLocked: Boolean = true,
    val sessionUnlocked: Boolean = false,
    val loginBusy: Boolean = false,
    val loginError: String? = null,
    val lockMessage: String? = null,
    val torEnabled: Boolean = true,
    val torOnly: Boolean = false,
    val torSocksHost: String = "127.0.0.1",
    val torSocksPort: Int = 19050,
    val managedTorEnabled: Boolean = true,
    val onionSeedEnabled: Boolean = false,
    val managedTorSocksPort: Int = 19050,
    val torStatus: ServiceStatus = ServiceStatus.Offline,
    val torOnionHost: String = "",
    val torOnionMultiaddr: String = "",
    val torSeedAddress: String = "",
    val torError: String? = null,
    val lastActivityMillis: Long = System.currentTimeMillis(),
    val nodeStatus: ServiceStatus = ServiceStatus.Offline,
    val miningStatus: ServiceStatus = ServiceStatus.Offline,
    val walletName: String = "zsilent",
    val managedWalletName: String = "",
    val managedWallets: List<String> = emptyList(),
    val managedWalletAddresses: List<String> = emptyList(),
    val walletLocked: Boolean = true,
    val balancesHidden: Boolean = false,
    val wallets: List<LocalWallet> = emptyList(),
    val activeWalletName: String = "",
    val activeWalletAddress: String = "",
    val walletBalance: WalletBalance? = null,
    val walletHistory: WalletHistory = WalletHistory(),
    val walletBusy: Boolean = false,
    val walletError: String? = null,
    val walletCreationBusy: Boolean = false,
    val walletCreationError: String? = null,
    val walletCreationResult: WalletCreationResult? = null,
    val walletBackupBusy: Boolean = false,
    val walletBackupError: String? = null,
    val walletBackupResult: WalletBackupResult? = null,
    val walletImportBusy: Boolean = false,
    val walletImportError: String? = null,
    val walletImportResult: WalletImportResult? = null,
    val secretRevealBusy: Boolean = false,
    val secretRevealError: String? = null,
    val revealedSecretWallet: String = "",
    val revealedRecoveryPhrase: String? = null,
    val revealedScanPrivateKey: String? = null,
    val revealedSpendPrivateKey: String? = null,
    val pinChangeBusy: Boolean = false,
    val pinChangeError: String? = null,
    val pinChangeResult: WalletPassphraseChangeResult? = null,
    val sendBusy: Boolean = false,
    val sendError: String? = null,
    val sendFeeEstimate: FeeEstimate? = null,
    val sendResult: SendResult? = null,
    val chainHeight: Long = 0,
    val networkHeight: Long = 0,
    val syncRemainingBlocks: Long = 0,
    val syncProgressPercent: Double = 0.0,
    val networkSynced: Boolean = false,
    // Mining gate: true when the node is close enough to the network head to
    // mine competitively. Looser than networkSynced (which stays exact for
    // sync display): allows a small block tolerance, and falls back to the
    // local node's own P2P-mesh sync view when the bootstrap RPC is
    // saturated or unreachable, so ZSilent miners aren't locked out while
    // CLI miners (which have no such gate) keep mining.
    val miningSyncReady: Boolean = false,
    val miningBlockReason: String? = null,
    val miningDiagnostic: String? = null,
    val localMiningEnabled: Boolean = false,
    val syncBusy: Boolean = false,
    val localRpcOnline: Boolean = false,
    // Peers of THIS node (from the local node's /stats), distinct from
    // `stats.connectedPeers` which is the bootstrap seed's peer count. The
    // Node section shows this so "Connected peers" reflects your own node.
    val localConnectedPeers: Long = 0,
    val networkRpcUrl: String = ZokaNetworkConfig.RemoteRpcUrl,
    val localRpcUrl: String = ZokaNetworkConfig.LocalRpcUrl,
    val bootstrapPeer: String = ZokaNetworkConfig.BootstrapMultiaddr,
    val syncStatusMessage: String = "Waiting for network status.",
    val stats: NodeStats = NodeStats(),
    val mining: MiningSnapshot = MiningSnapshot(),
    val pendingPrivateTransfers: PendingPrivateTransfers = PendingPrivateTransfers(),
    val miningControlledByApp: Boolean = false,
    val logs: List<RuntimeLog> = emptyList(),
    val rpcOnline: Boolean = false,
    val zokaReady: Boolean = false,
    val lastBalanceRefreshedAt: Long = 0L
)

// Mining may start while trailing the observed network head by up to this
// many blocks. Transient bootstrap saturation or gossip lag must not lock
// ZSilent miners out while CLI miners (no gate) keep mining; fork-choice by
// cumulative work resolves any short-lived side branch this can produce.
// Keep this small: mining far behind the head only wastes CPU on blocks
// that will be orphaned.
private const val MINING_SYNC_TOLERANCE_BLOCKS = 2L

class ZSilentAppState(
    val cli: ZokaCli,
    val rpc: ZokaRpcClient,
    val localRpc: ZokaRpcClient,
    val nodeController: NodeController,
    val miningController: MiningController,
    val torSupervisor: TorSupervisor = TorSupervisor(cli),
    val walletService: WalletService = WalletService(cli),
    private val setupStore: SetupStore = SetupStore.default()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val _uiState = MutableStateFlow(ZSilentUiState(zokaReady = cli.ready))
    val state = _uiState.asStateFlow()
    val uiState: ZSilentUiState
        get() = _uiState.value
    @Volatile private var rpcSyncInProgress = false
    private var lastAutoRpcSyncAttemptMillis = 0L
    @Volatile private var autoRpcSyncBackoffUntilMillis = 0L
    private var sessionPassword: CharArray? = null
    private var integratedMiningRequested = false
    private var lastMiningThreads = 0
    private var lastLocalHeightReadMillis = 0L
    @Volatile private var blockPushInProgress = false
    private var lastPushedRemoteHeight = 0L
    private var lastBalanceRefreshMillis = 0L

    fun start() {
        val staleProcesses = ZokaProcessKiller.terminateManagedZokaProcesses(cli)
        if (staleProcesses > 0) {
            appendLog(RuntimeLog("system", "Cleaned up $staleProcesses stale Zoka process(es) from a previous ZSilent session."))
        }
        startMiningDiagnosticLogTail()
        setupStore.load()?.takeIf { it.onboardingComplete }?.let { setup ->
            val defaultWallet = setup.walletName.ifBlank { "main" }
            val managedWallets = setup.managedWallets
                .ifEmpty { listOf(defaultWallet) }
                .filter { it.isNotBlank() }
                .distinct()
            val managedWalletAddresses = (setup.managedWalletAddresses + setup.walletAddress + setup.managedWallets)
                .filter { it.isZokaAddress() }
                .distinct()
            update {
                it.copy(
                    onboardingComplete = true,
                    walletName = defaultWallet,
                    managedWalletName = defaultWallet,
                    managedWallets = managedWallets,
                    managedWalletAddresses = managedWalletAddresses,
                    activeWalletName = defaultWallet,
                    onboardingWalletName = defaultWallet,
                    onboardingWalletAddress = setup.walletAddress,
                    activeWalletAddress = setup.walletAddress,
                    onboardingNodeEnabled = setup.nodeEnabled,
                    onboardingMiningEnabled = setup.miningEnabled,
                    onboardingAutoLockMinutes = setup.autoLockMinutes,
                    balancesHidden = setup.balancesHidden,
                    darkTheme = setup.darkTheme,
                    torEnabled = setup.torEnabled,
                    torOnly = setup.torOnly,
                    torSocksHost = setup.torSocksHost,
                    torSocksPort = setup.torSocksPort,
                    managedTorEnabled = setup.managedTorEnabled,
                    onionSeedEnabled = setup.onionSeedEnabled,
                    managedTorSocksPort = setup.managedTorSocksPort,
                    appLocked = true,
                    walletLocked = true,
                    lockMessage = "Unlock ${defaultWallet} to continue.",
                    selectedSection = AppSection.Dashboard
                )
            }
        }
        refreshWallets()
        scope.launch {
            nodeController.status.collectLatest { status ->
                update {
                    val miningStatus = if (integratedMiningRequested && status == ServiceStatus.Online) {
                        ServiceStatus.Starting
                    } else if (integratedMiningRequested && status == ServiceStatus.Error) {
                        ServiceStatus.Error
                    } else {
                        it.miningStatus
                    }
                    it.copy(
                        nodeStatus = status,
                        miningStatus = miningStatus
                    )
                }
            }
        }
        scope.launch {
            nodeController.logs.collect { log ->
                appendLog(log)
            }
        }
        scope.launch {
            torSupervisor.state.collectLatest { tor ->
                update {
                    val managedOrActive = it.managedTorEnabled || tor.status != ServiceStatus.Offline
                    it.copy(
                        torStatus = tor.status,
                        torSocksHost = if (managedOrActive) tor.socksHost else it.torSocksHost,
                        torSocksPort = if (managedOrActive) tor.socksPort else it.torSocksPort,
                        torOnionHost = tor.onionHost,
                        torOnionMultiaddr = tor.onionMultiaddr,
                        torError = tor.error
                    )
                }
            }
        }
        scope.launch {
            torSupervisor.logs.collect { log ->
                appendLog(log)
            }
        }
        miningController.startPolling()
        scope.launch {
            miningController.status.collectLatest { status ->
                update {
                    val reconciledStatus = when {
                        !it.miningDiagnostic.isNullOrBlank() -> ServiceStatus.Error
                        integratedMiningRequested &&
                            it.nodeStatus == ServiceStatus.Online &&
                            status == ServiceStatus.Offline -> ServiceStatus.Starting
                        else -> status
                    }
                    it.copy(miningStatus = reconciledStatus)
                }
            }
        }
        scope.launch {
            var lastPushedHeight = 0L
            miningController.snapshot.collectLatest { mining ->
                val newHeight = mining.chainHeight
                update {
                    it.copy(
                        mining = mining,
                        chainHeight = maxOf(it.chainHeight, newHeight)
                    )
                }
                // When a new block is mined locally, push it to remote nodes
                // so they don't need to pull via P2P (works through NAT).
                if (newHeight > lastPushedHeight && newHeight > 0 && mining.running) {
                    lastPushedHeight = newHeight
                    scope.launch { pushMinedBlockToRemotes(newHeight) }
                }
            }
        }
        scope.launch {
            miningController.pendingTransfers.collectLatest { pending ->
                update { it.copy(pendingPrivateTransfers = pending) }
            }
        }
        scope.launch {
            miningController.controlledByApp.collectLatest { controlled ->
                update { it.copy(miningControlledByApp = controlled) }
            }
        }
        scope.launch {
            miningController.logs.collect { log ->
                appendLog(log)
            }
        }
        scope.launch {
            while (true) {
                pollRpc()
                delay(3_000)
            }
        }
        scope.launch {
            while (true) {
                delay(4_000)
                reconcileMiningIfNeeded()
            }
        }
        scope.launch {
            while (true) {
                enforceAutoLock()
                delay(5_000)
            }
        }
    }

    // Self-heal the mining switch: if mining is requested (switch ON) and the
    // node is online + synced but the node isn't actually mining (e.g. the node
    // restarted and the in-memory mining control reset, or a transient stop),
    // re-apply the mining control. Without this the switch can read ON while the
    // node silently isn't mining.
    private suspend fun reconcileMiningIfNeeded() {
        if (!integratedMiningRequested) return
        val s = _uiState.value
        if (s.nodeStatus != ServiceStatus.Online || !s.miningSyncReady) return
        val stats = runCatching { localRpc.stats() }.getOrNull() ?: return
        val actuallyMining = stats.localMining || stats.reportedHashrateHps > 0
        if (actuallyMining) return
        val threads = (if (lastMiningThreads > 0) lastMiningThreads else s.mining.availableCpuThreads).coerceAtLeast(1)
        appendLog(RuntimeLog("mining", "Mining is on but the node isn't mining; re-applying mining control with $threads thread(s)."))
        enableLocalNodeMining(threads, quiet = true)
    }

    fun select(section: AppSection) {
        update { it.copy(selectedSection = section) }
    }

    fun setWalletSetupMode(mode: WalletSetupMode) {
        update {
            it.copy(
                walletSetupMode = mode,
                onboardingRecoveryPhrase = null,
                onboardingRecoveryConfirmed = false,
                onboardingSeedCheckFirst = "",
                onboardingSeedCheckSecond = ""
            )
        }
    }

    fun setSeedWordCount(count: SeedWordCount) {
        update { it.copy(seedWordCount = count) }
    }

    fun setOnboardingWalletName(name: String) {
        update { it.copy(onboardingWalletName = name) }
    }

    fun setOnboardingSeedPhrase(seedPhrase: String) {
        update {
            it.copy(
                onboardingSeedPhrase = seedPhrase,
                onboardingRecoveryPhrase = null,
                onboardingSeedCheckFirst = "",
                onboardingSeedCheckSecond = ""
            )
        }
    }

    fun setOnboardingPassword(password: String) {
        update { it.copy(onboardingPassword = password) }
    }

    fun setOnboardingPasswordConfirm(password: String) {
        update { it.copy(onboardingPasswordConfirm = password) }
    }

    fun setOnboardingNodeEnabled(enabled: Boolean) {
        update { it.copy(onboardingNodeEnabled = enabled) }
    }

    fun setOnboardingMiningEnabled(enabled: Boolean) {
        update { it.copy(onboardingMiningEnabled = enabled) }
    }

    fun setOnboardingAutoLockMinutes(minutes: Int) {
        update { it.copy(onboardingAutoLockMinutes = minutes.coerceIn(1, 120)) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        val value = minutes.coerceIn(1, 120)
        val snapshot = _uiState.value
        update {
            it.copy(
                onboardingAutoLockMinutes = value,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        if (snapshot.onboardingComplete) {
            setupStore.save(
                PersistedSetup(
                    onboardingComplete = true,
                    walletName = snapshot.activeWalletName.ifBlank { snapshot.walletName },
                    walletAddress = snapshot.activeWalletAddress,
                    managedWallets = snapshot.managedWallets,
                    managedWalletAddresses = managedAddressesFor(snapshot),
                    nodeEnabled = snapshot.onboardingNodeEnabled,
                    miningEnabled = snapshot.onboardingMiningEnabled,
                    balancesHidden = snapshot.balancesHidden,
                    autoLockMinutes = value,
                    darkTheme = snapshot.darkTheme,
                    torEnabled = snapshot.torEnabled,
                    torOnly = snapshot.torOnly,
                    torSocksHost = snapshot.torSocksHost,
                    torSocksPort = snapshot.torSocksPort,
                    managedTorEnabled = snapshot.managedTorEnabled,
                    onionSeedEnabled = snapshot.onionSeedEnabled,
                    managedTorSocksPort = snapshot.managedTorSocksPort
                )
            )
        }
    }

    fun setOnboardingRecoveryConfirmed(confirmed: Boolean) {
        update { it.copy(onboardingRecoveryConfirmed = confirmed) }
    }

    fun setOnboardingSelfCustodyConfirmed(confirmed: Boolean) {
        update { it.copy(onboardingSelfCustodyConfirmed = confirmed) }
    }

    fun setOnboardingSeedCheckFirst(value: String) {
        update { it.copy(onboardingSeedCheckFirst = value.trim().lowercase()) }
    }

    fun setOnboardingSeedCheckSecond(value: String) {
        update { it.copy(onboardingSeedCheckSecond = value.trim().lowercase()) }
    }

    fun setOnboardingPrivacyConfirmed(confirmed: Boolean) {
        update { it.copy(onboardingPrivacyConfirmed = confirmed) }
    }

    fun setOnboardingTutorialConfirmed(confirmed: Boolean) {
        update { it.copy(onboardingTutorialConfirmed = confirmed) }
    }

    fun onboardingBack() {
        update { current ->
            current.copy(onboardingStep = current.onboardingStep.previous())
        }
    }

    fun onboardingNext() {
        when (_uiState.value.onboardingStep) {
            OnboardingStep.Wallet -> {
                prepareOnboardingRecoveryPhrase()
                return
            }
            OnboardingStep.Security -> {
                createOrRestoreOnboardingWallet()
                return
            }
            OnboardingStep.Dashboard -> {
                completeOnboarding()
                return
            }
            else -> Unit
        }
        update { current ->
            current.copy(onboardingStep = current.onboardingStep.next())
        }
    }

    fun completeOnboarding() {
        val snapshot = _uiState.value
        val walletName = snapshot.onboardingWalletName.ifBlank { "main" }
        snapshot.onboardingPassword
            .takeIf { it.isNotBlank() }
            ?.toCharArray()
            ?.let { password ->
                try {
                    storeSessionPassword(password)
                } finally {
                    password.fill('\u0000')
                }
            }
        setupStore.save(
            PersistedSetup(
                onboardingComplete = true,
                walletName = walletName,
                walletAddress = snapshot.onboardingWalletAddress,
                managedWallets = listOf(walletName),
                managedWalletAddresses = listOf(snapshot.onboardingWalletAddress).filter { it.isZokaAddress() },
                nodeEnabled = snapshot.onboardingNodeEnabled,
                miningEnabled = snapshot.onboardingMiningEnabled,
                balancesHidden = snapshot.balancesHidden,
                autoLockMinutes = snapshot.onboardingAutoLockMinutes,
                darkTheme = snapshot.darkTheme,
                torEnabled = snapshot.torEnabled,
                torOnly = snapshot.torOnly,
                torSocksHost = snapshot.torSocksHost,
                torSocksPort = snapshot.torSocksPort,
                managedTorEnabled = snapshot.managedTorEnabled,
                onionSeedEnabled = snapshot.onionSeedEnabled,
                managedTorSocksPort = snapshot.managedTorSocksPort
            )
        )
        update {
            it.copy(
                onboardingComplete = true,
                selectedSection = AppSection.Dashboard,
                walletName = walletName,
                managedWalletName = walletName,
                managedWallets = listOf(walletName),
                managedWalletAddresses = listOf(it.onboardingWalletAddress).filter { address -> address.isZokaAddress() },
                activeWalletName = walletName,
                activeWalletAddress = it.onboardingWalletAddress,
                appLocked = false,
                loginBusy = false,
                loginError = null,
                lockMessage = null,
                lastActivityMillis = System.currentTimeMillis(),
                walletLocked = false,
                onboardingPassword = "",
                onboardingPasswordConfirm = "",
                onboardingSeedPhrase = "",
                onboardingRecoveryPhrase = null,
                onboardingSeedCheckFirst = "",
                onboardingSeedCheckSecond = "",
                onboardingError = null
            )
        }
        // Onboarding just unlocked the wallet (created or restored), but
        // nothing has fetched its real balance/history yet — without this,
        // the dashboard shows stale/empty data until the next periodic poll
        // (or until the user manually locks and unlocks). Kick off a fetch
        // immediately so a freshly restored wallet shows its real balance
        // right away instead of requiring a relog.
        snapshot.onboardingPassword.takeIf { it.isNotBlank() }?.let { rawPassword ->
            refreshUnlockedWalletData(
                snapshot.onboardingWalletAddress,
                walletName,
                rawPassword.toCharArray(),
                rawPassword.toCharArray()
            )
        }
    }

    fun toggleTheme() {
        val snapshot = _uiState.value
        val darkTheme = !snapshot.darkTheme
        update { it.copy(darkTheme = darkTheme) }
        if (snapshot.onboardingComplete) {
            setupStore.save(
                PersistedSetup(
                    onboardingComplete = true,
                    walletName = snapshot.activeWalletName.ifBlank { snapshot.walletName },
                    walletAddress = snapshot.activeWalletAddress,
                    managedWallets = snapshot.managedWallets,
                    managedWalletAddresses = managedAddressesFor(snapshot),
                    nodeEnabled = snapshot.onboardingNodeEnabled,
                    miningEnabled = snapshot.onboardingMiningEnabled,
                    balancesHidden = snapshot.balancesHidden,
                    autoLockMinutes = snapshot.onboardingAutoLockMinutes,
                    darkTheme = darkTheme,
                    torEnabled = snapshot.torEnabled,
                    torOnly = snapshot.torOnly,
                    torSocksHost = snapshot.torSocksHost,
                    torSocksPort = snapshot.torSocksPort,
                    managedTorEnabled = snapshot.managedTorEnabled,
                    onionSeedEnabled = snapshot.onionSeedEnabled,
                    managedTorSocksPort = snapshot.managedTorSocksPort
                )
            )
        }
    }

    fun toggleBalances() {
        val snapshot = _uiState.value
        val hidden = !snapshot.balancesHidden
        // This is an explicit user interaction with the unlocked wallet, so
        // it must count as activity — otherwise hiding the balance and then
        // reading it for a while (with no other input) can silently trip
        // enforceAutoLock(), and the next click on the eye icon lands on an
        // already-locked screen, making the toggle look broken.
        update { it.copy(balancesHidden = hidden, lastActivityMillis = System.currentTimeMillis()) }
        if (snapshot.onboardingComplete) {
            setupStore.save(
                PersistedSetup(
                    onboardingComplete = true,
                    walletName = snapshot.activeWalletName.ifBlank { snapshot.walletName },
                    walletAddress = snapshot.activeWalletAddress,
                    managedWallets = snapshot.managedWallets,
                    managedWalletAddresses = managedAddressesFor(snapshot),
                    nodeEnabled = snapshot.onboardingNodeEnabled,
                    miningEnabled = snapshot.onboardingMiningEnabled,
                    balancesHidden = hidden,
                    autoLockMinutes = snapshot.onboardingAutoLockMinutes,
                    darkTheme = snapshot.darkTheme,
                    torEnabled = snapshot.torEnabled,
                    torOnly = snapshot.torOnly,
                    torSocksHost = snapshot.torSocksHost,
                    torSocksPort = snapshot.torSocksPort,
                    managedTorEnabled = snapshot.managedTorEnabled,
                    onionSeedEnabled = snapshot.onionSeedEnabled,
                    managedTorSocksPort = snapshot.managedTorSocksPort
                )
            )
        }
    }

    fun setTorEnabled(enabled: Boolean) {
        update {
            it.copy(
                torEnabled = enabled,
                torOnly = if (enabled) it.torOnly else false,
                onionSeedEnabled = if (enabled) it.onionSeedEnabled else false,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        if (!enabled) {
            torSupervisor.stop()
        }
        persistCurrentSetup()
        appendLog(RuntimeLog("privacy", if (enabled) "Tor routing preference enabled." else "Tor routing preference disabled."))
    }

    /// Master switch for the simplified Privacy UI: a single "connect over Tor
    /// (.onion)" toggle. ON enables managed Tor in HYBRID mode -- the node is
    /// reachable over its onion (no open port) AND can still dial clearnet/TCP
    /// seeds, so it isn't left isolated when the only onion seed on the network
    /// is itself (the case while the network is small). Deliberately NOT
    /// tor-only: tor-only left the node unable to reach any peer and, combined
    /// with the managed-Tor bootstrap gate, blocked the node from starting at
    /// all. The mixed onion+TCP dial noise that tor-only was meant to hide is
    /// instead filtered out of the log panel (see NodeController.isNetworkChurn).
    /// OFF turns Tor off entirely and falls back to plain clearnet/TCP. The old
    /// per-flag toggles and manual SOCKS fields are gone; managed Tor always
    /// uses 127.0.0.1:<managedTorSocksPort>.
    fun setTorOnionMode(enabled: Boolean) {
        update {
            it.copy(
                torEnabled = enabled,
                managedTorEnabled = enabled,
                torOnly = false,
                torSocksHost = "127.0.0.1",
                torSocksPort = if (enabled) it.managedTorSocksPort else it.torSocksPort,
                onionSeedEnabled = if (enabled) it.onionSeedEnabled else false,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        if (!enabled) {
            torSupervisor.stop()
        }
        persistCurrentSetup()
        appendLog(
            RuntimeLog(
                "privacy",
                if (enabled) "Tor onion mode enabled (managed Tor, hybrid onion + clearnet)."
                else "Tor onion mode disabled (direct clearnet)."
            )
        )
    }

    fun setTorOnly(enabled: Boolean) {
        update {
            it.copy(
                torEnabled = if (enabled) true else it.torEnabled,
                torOnly = enabled,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        persistCurrentSetup()
        appendLog(RuntimeLog("privacy", if (enabled) "Tor-only network policy enabled." else "Tor-only network policy disabled."))
    }

    fun setManagedTorEnabled(enabled: Boolean) {
        update {
            it.copy(
                managedTorEnabled = enabled,
                torEnabled = if (enabled) true else it.torEnabled,
                torSocksHost = if (enabled) "127.0.0.1" else it.torSocksHost,
                torSocksPort = if (enabled) it.managedTorSocksPort else it.torSocksPort,
                onionSeedEnabled = if (enabled) it.onionSeedEnabled else false,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        if (!enabled) {
            torSupervisor.stop()
        }
        persistCurrentSetup()
        appendLog(RuntimeLog("privacy", if (enabled) "Managed Tor enabled." else "Managed Tor disabled; external SOCKS settings will be used if Tor stays enabled."))
    }

    fun setOnionSeedEnabled(enabled: Boolean) {
        update {
            it.copy(
                torEnabled = if (enabled) true else it.torEnabled,
                managedTorEnabled = if (enabled) true else it.managedTorEnabled,
                onionSeedEnabled = enabled,
                torSocksHost = if (enabled) "127.0.0.1" else it.torSocksHost,
                torSocksPort = if (enabled) it.managedTorSocksPort else it.torSocksPort,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        persistCurrentSetup()
        appendLog(RuntimeLog("privacy", if (enabled) "Onion seed opt-in enabled." else "Onion seed opt-in disabled."))
    }

    fun setTorSocksHost(host: String) {
        update {
            it.copy(
                torSocksHost = host.trim().ifBlank { "127.0.0.1" },
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        persistCurrentSetup()
    }

    fun setTorSocksPort(portText: String) {
        val parsed = portText.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 65535) ?: return
        update {
            it.copy(
                torSocksPort = parsed,
                managedTorSocksPort = if (it.managedTorEnabled) parsed else it.managedTorSocksPort,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        persistCurrentSetup()
    }

    fun recordActivity() {
        val snapshot = _uiState.value
        if (!snapshot.onboardingComplete || snapshot.appLocked) return
        val now = System.currentTimeMillis()
        if (now - snapshot.lastActivityMillis < 1_000) return
        update { it.copy(lastActivityMillis = now) }
    }

    fun login(password: CharArray) {
        val snapshot = _uiState.value
        if (snapshot.loginBusy) {
            password.fill('\u0000')
            return
        }
        val walletSelector = snapshot.activeWalletAddress.ifBlank { snapshot.activeWalletName.ifBlank { snapshot.walletName } }
        val walletName = snapshot.activeWalletName.ifBlank { snapshot.walletName.ifBlank { walletSelector } }
        if (walletSelector.isBlank()) {
            password.fill('\u0000')
            update { it.copy(loginError = "No wallet is configured. Reset setup to continue.") }
            return
        }
        if (password.isEmpty()) {
            password.fill('\u0000')
            update { it.copy(loginError = "Wallet password is required") }
            return
        }
        val sessionPasswordCandidate = password.copyOf()
        val unlockPassword = password.copyOf()
        val balancePassword = password.copyOf()
        val historyPassword = password.copyOf()
        password.fill('\u0000')
        update {
            it.copy(
                loginBusy = true,
                loginError = null
            )
        }
        scope.launch {
            runCatching {
                walletService.listWallets(unlockPassword)
            }.onSuccess { result ->
                val current = _uiState.value
                val visibleWallets = visibleManagedWallets(result.privateWallets, current)
                val selected = visibleWallets.firstOrNull { it.address == current.activeWalletAddress }
                    ?: visibleWallets.firstOrNull { it.address == walletSelector }
                    ?: visibleWallets.firstOrNull { it.name == walletName }
                    ?: visibleWallets.firstOrNull()
                if (selected == null) {
                    sessionPasswordCandidate.fill('\u0000')
                    unlockPassword.fill('\u0000')
                    balancePassword.fill('\u0000')
                    historyPassword.fill('\u0000')
                    update {
                        it.copy(
                            appLocked = true,
                            loginBusy = false,
                            walletLocked = true,
                            walletBalance = null,
                            walletHistory = WalletHistory(wallet = walletName),
                            loginError = "No wallet was found for this setup."
                        )
                    }
                    return@onSuccess
                }
                if (selected.encrypted && !selected.unlocked) {
                    sessionPasswordCandidate.fill('\u0000')
                    unlockPassword.fill('\u0000')
                    balancePassword.fill('\u0000')
                    historyPassword.fill('\u0000')
                    update {
                        it.copy(
                            appLocked = true,
                            loginBusy = false,
                            walletLocked = true,
                            walletBalance = null,
                            walletHistory = WalletHistory(wallet = selected.name),
                            loginError = "Wallet '${selected.name}' exists but could not be unlocked with that password."
                        )
                    }
                    appendLog(RuntimeLog("wallet", "Wallet unlock failed for '${selected.name}': password rejected."))
                    return@onSuccess
                }
                storeSessionPassword(sessionPasswordCandidate)
                sessionPasswordCandidate.fill('\u0000')
                unlockPassword.fill('\u0000')
                update {
                    it.copy(
                        appLocked = false,
                        loginBusy = false,
                        loginError = null,
                        lockMessage = null,
                        lastActivityMillis = System.currentTimeMillis(),
                        walletLocked = false,
                        wallets = visibleWallets,
                        walletName = selected.name,
                        activeWalletName = selected.name,
                        activeWalletAddress = selected.address,
                        walletBalance = selected.toWalletBalance(),
                        walletHistory = WalletHistory(wallet = selected.name, count = selected.txCount),
                        walletError = null
                    )
                }
                appendLog(RuntimeLog("wallet", "Unlocked wallet '${selected.name}'"))
                refreshUnlockedWalletData(selected.address, selected.name, balancePassword, historyPassword)
                maybeAutoStartNode()
            }.onFailure { error ->
                sessionPasswordCandidate.fill('\u0000')
                unlockPassword.fill('\u0000')
                balancePassword.fill('\u0000')
                historyPassword.fill('\u0000')
                update {
                    it.copy(
                        appLocked = true,
                        loginBusy = false,
                        walletLocked = true,
                        walletBalance = null,
                        walletHistory = WalletHistory(wallet = walletName),
                        loginError = walletUnlockFailureMessage(walletName, error)
                    )
                }
                appendLog(RuntimeLog("wallet", "Wallet unlock failed for '$walletName': ${safeLogMessage(error)}"))
            }
        }
    }

    private fun maybeAutoStartNode() {
        val snapshot = _uiState.value
        if (!snapshot.onboardingNodeEnabled) {
            appendLog(RuntimeLog("node", "Auto-start skipped: node disabled in setup."))
            return
        }
        if (snapshot.nodeStatus != ServiceStatus.Offline) {
            appendLog(RuntimeLog("node", "Auto-start skipped: node is already ${snapshot.nodeStatus.name.lowercase()}."))
            return
        }
        if (snapshot.mining.running) {
            appendLog(RuntimeLog("node", "Auto-start skipped: external miner already owns the node."))
            return
        }
        // Onion is this network's default transport. Bring managed Tor up
        // automatically so the Privacy panel and the Node come online together
        // on launch, with no switches to flip. setTorOnionMode is idempotent
        // and hybrid (onion + clearnet): an already-on Tor stays on, and if
        // managed Tor is slow to bootstrap the node still starts over clearnet
        // (startNode never blocks on Tor). This is what makes "connect on
        // launch" fully automatic end to end.
        if (!(snapshot.torEnabled && snapshot.managedTorEnabled)) {
            appendLog(RuntimeLog("privacy", "Auto-enabling Tor onion mode before node start."))
            setTorOnionMode(true)
        }
        appendLog(RuntimeLog("node", "Auto-starting full node after wallet unlock."))
        startNode(autoMine = snapshot.onboardingMiningEnabled)
    }

    fun lockApp(message: String = "Wallet locked.") {
        lockAppInternal(message)
    }

    fun selectWallet(name: String) {
        val current = _uiState.value
        val wallet = current.wallets.firstOrNull { it.address == name }
            ?: current.wallets.firstOrNull { it.name == name }
        val nextName = wallet?.name ?: name
        val nextAddress = wallet?.address ?: name.takeIf { it.isZokaAddress() } ?: current.activeWalletAddress
        val managedWallets = managedNamesFor(current, listOf(nextName))
        val managedWalletAddresses = managedAddressesFor(current, listOf(nextAddress))
        if (wallet != null && current.onboardingComplete) {
            clearSessionPassword()
            setupStore.save(
                PersistedSetup(
                    onboardingComplete = true,
                    walletName = wallet.name,
                    walletAddress = wallet.address,
                    managedWallets = managedWallets,
                    managedWalletAddresses = managedWalletAddresses,
                    nodeEnabled = current.onboardingNodeEnabled,
                    miningEnabled = current.onboardingMiningEnabled,
                    balancesHidden = current.balancesHidden,
                    autoLockMinutes = current.onboardingAutoLockMinutes,
                    darkTheme = current.darkTheme,
                    torEnabled = current.torEnabled,
                    torOnly = current.torOnly,
                    torSocksHost = current.torSocksHost,
                    torSocksPort = current.torSocksPort,
                    managedTorEnabled = current.managedTorEnabled,
                    onionSeedEnabled = current.onionSeedEnabled,
                    managedTorSocksPort = current.managedTorSocksPort
                )
            )
        }
        update {
            it.copy(
                activeWalletName = nextName,
                walletName = nextName,
                managedWallets = managedWallets,
                managedWalletAddresses = managedWalletAddresses,
                activeWalletAddress = nextAddress,
                walletLocked = true,
                walletBalance = null,
                walletHistory = WalletHistory(wallet = nextName),
                walletError = null,
                secretRevealBusy = false,
                secretRevealError = null,
                revealedSecretWallet = "",
                revealedRecoveryPhrase = null,
                revealedScanPrivateKey = null,
                revealedSpendPrivateKey = null,
                pinChangeBusy = false,
                pinChangeError = null,
                pinChangeResult = null
            )
        }
    }

    fun refreshWallets(password: CharArray? = null) {
        if (_uiState.value.walletBusy) return
        update { it.copy(walletBusy = true, walletError = null) }
        scope.launch {
            runCatching {
                walletService.listWallets(password)
            }.onSuccess { result ->
                val current = _uiState.value
                val visibleWallets = visibleManagedWallets(result.privateWallets, current)
                val selected = visibleWallets.firstOrNull { it.address == current.activeWalletAddress }
                    ?: visibleWallets.firstOrNull { it.name == current.activeWalletName }
                    ?: visibleWallets.firstOrNull()
                val managedWallets = managedNamesFor(
                    current,
                    visibleWallets.map { it.name } + listOfNotNull(selected?.name)
                )
                val managedWalletAddresses = managedAddressesFor(
                    current,
                    visibleWallets.map { it.address } + listOfNotNull(selected?.address)
                )
                update {
                    it.copy(
                        wallets = visibleWallets,
                        activeWalletName = selected?.name ?: it.activeWalletName,
                        walletName = selected?.name ?: it.walletName,
                        activeWalletAddress = selected?.address ?: it.activeWalletAddress,
                        managedWallets = managedWallets,
                        managedWalletAddresses = managedWalletAddresses,
                        walletBusy = false,
                        walletError = null
                    )
                }
                if (current.onboardingComplete && managedWalletAddresses != current.managedWalletAddresses) {
                    persistCurrentSetup()
                }
            }.onFailure { error ->
                update {
                    it.copy(
                        walletBusy = false,
                        walletError = error.message ?: "Wallet refresh failed"
                    )
                }
            }
        }
    }

    fun createAdditionalWallet(name: String, password: CharArray, seedWords: Int = SeedWordCount.TwentyFour.words) {
        if (_uiState.value.walletCreationBusy) {
            password.fill('\u0000')
            return
        }
        val walletName = name.trim()
        if (walletName.isBlank()) {
            password.fill('\u0000')
            update { it.copy(walletCreationError = "Wallet name is required") }
            return
        }
        if (password.size < 8) {
            password.fill('\u0000')
            update { it.copy(walletCreationError = "Wallet password must be at least 8 characters") }
            return
        }
        val sessionPasswordCandidate = password.copyOf()
        val createPassword = password.copyOf()
        password.fill('\u0000')
        update {
            it.copy(
                walletCreationBusy = true,
                walletCreationError = null,
                walletCreationResult = null
            )
        }
        scope.launch {
            runCatching {
                walletService.createWallet(
                    name = walletName,
                    password = createPassword,
                    seedWords = seedWords
                )
            }.onSuccess { result ->
                storeSessionPassword(sessionPasswordCandidate)
                sessionPasswordCandidate.fill('\u0000')
                createPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Created wallet '${result.name}'"))
                val current = _uiState.value
                val managedWallets = (current.managedWallets + result.name)
                    .filter { it.isNotBlank() }
                    .distinct()
                val managedWalletAddresses = managedAddressesFor(current, listOf(result.address))
                setupStore.save(
                    PersistedSetup(
                        onboardingComplete = current.onboardingComplete,
                        walletName = result.name,
                        walletAddress = result.address,
                        managedWallets = managedWallets,
                        managedWalletAddresses = managedWalletAddresses,
                        nodeEnabled = current.onboardingNodeEnabled,
                        miningEnabled = current.onboardingMiningEnabled,
                        balancesHidden = current.balancesHidden,
                        autoLockMinutes = current.onboardingAutoLockMinutes,
                        darkTheme = current.darkTheme,
                        torEnabled = current.torEnabled,
                        torOnly = current.torOnly,
                        torSocksHost = current.torSocksHost,
                        torSocksPort = current.torSocksPort,
                        managedTorEnabled = current.managedTorEnabled,
                        onionSeedEnabled = current.onionSeedEnabled,
                        managedTorSocksPort = current.managedTorSocksPort
                    )
                )
                update {
                    it.copy(
                        walletCreationBusy = false,
                        walletCreationError = null,
                        walletCreationResult = result,
                        activeWalletName = result.name,
                        walletName = result.name,
                        managedWalletName = result.name,
                        managedWallets = managedWallets,
                        managedWalletAddresses = managedWalletAddresses,
                        activeWalletAddress = result.address,
                        walletLocked = false,
                        walletBalance = null,
                        walletHistory = WalletHistory(wallet = result.name)
                    )
                }
                refreshWallets()
            }.onFailure { error ->
                sessionPasswordCandidate.fill('\u0000')
                createPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Wallet creation failed: ${safeLogMessage(error)}"))
                update {
                    it.copy(
                        walletCreationBusy = false,
                        walletCreationError = error.message ?: "Wallet creation failed"
                    )
                }
            }
        }
    }

    fun exportWalletBackup(wallet: String, outPath: String, password: CharArray) {
        if (_uiState.value.walletBackupBusy) {
            password.fill('\u0000')
            return
        }
        val snapshot = _uiState.value
        val walletSelector = resolveWalletAddress(wallet, snapshot)
        val walletName = resolveWalletLabel(wallet, snapshot)
        if (walletSelector.isBlank()) {
            password.fill('\u0000')
            update { it.copy(walletBackupError = "Select a wallet before exporting a backup") }
            return
        }
        if (outPath.isBlank()) {
            password.fill('\u0000')
            update { it.copy(walletBackupError = "Backup path is required") }
            return
        }
        val backupPassword = password.copyOf()
        password.fill('\u0000')
        update {
            it.copy(
                walletBackupBusy = true,
                walletBackupError = null,
                walletBackupResult = null
            )
        }
        scope.launch {
            runCatching {
                walletService.exportEncryptedBackup(
                    wallet = walletSelector,
                    outPath = outPath,
                    password = backupPassword
                )
            }.onSuccess { result ->
                backupPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Encrypted backup exported for '$walletName'"))
                update {
                    it.copy(
                        walletBackupBusy = false,
                        walletBackupError = null,
                        walletBackupResult = result.copy(name = walletName, address = walletSelector),
                        walletImportError = null
                    )
                }
            }.onFailure { error ->
                backupPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Backup export failed: ${safeLogMessage(error)}"))
                update {
                    it.copy(
                        walletBackupBusy = false,
                        walletBackupError = error.message ?: "Backup export failed"
                    )
                }
            }
        }
    }

    fun restoreWalletBackup(inPath: String, password: CharArray, importName: String? = null) {
        if (_uiState.value.walletImportBusy) {
            password.fill('\u0000')
            return
        }
        if (inPath.isBlank()) {
            password.fill('\u0000')
            update { it.copy(walletImportError = "Backup path is required") }
            return
        }
        val sessionPasswordCandidate = password.copyOf()
        val backupPassword = password.copyOf()
        password.fill('\u0000')
        update {
            it.copy(
                walletImportBusy = true,
                walletImportError = null,
                walletImportResult = null
            )
        }
        scope.launch {
            runCatching {
                walletService.restoreEncryptedBackup(
                    inPath = inPath,
                    password = backupPassword,
                    importName = importName
                )
            }.onSuccess { result ->
                storeSessionPassword(sessionPasswordCandidate)
                sessionPasswordCandidate.fill('\u0000')
                backupPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Encrypted backup restored as '${result.name}'"))
                val current = _uiState.value
                val managedWallets = (current.managedWallets + result.name)
                    .filter { it.isNotBlank() }
                    .distinct()
                val managedWalletAddresses = managedAddressesFor(current, listOf(result.address))
                setupStore.save(
                    PersistedSetup(
                        onboardingComplete = current.onboardingComplete,
                        walletName = result.name,
                        walletAddress = result.address,
                        managedWallets = managedWallets,
                        managedWalletAddresses = managedWalletAddresses,
                        nodeEnabled = current.onboardingNodeEnabled,
                        miningEnabled = current.onboardingMiningEnabled,
                        balancesHidden = current.balancesHidden,
                        autoLockMinutes = current.onboardingAutoLockMinutes,
                        darkTheme = current.darkTheme,
                        torEnabled = current.torEnabled,
                        torOnly = current.torOnly,
                        torSocksHost = current.torSocksHost,
                        torSocksPort = current.torSocksPort,
                        managedTorEnabled = current.managedTorEnabled,
                        onionSeedEnabled = current.onionSeedEnabled,
                        managedTorSocksPort = current.managedTorSocksPort
                    )
                )
                update {
                    it.copy(
                        walletImportBusy = false,
                        walletImportError = null,
                        walletImportResult = result,
                        walletBackupError = null,
                        activeWalletName = result.name,
                        walletName = result.name,
                        managedWalletName = result.name,
                        managedWallets = managedWallets,
                        managedWalletAddresses = managedWalletAddresses,
                        activeWalletAddress = result.address,
                        walletLocked = false,
                        walletBalance = null,
                        walletHistory = WalletHistory(wallet = result.name)
                    )
                }
                refreshWallets()
            }.onFailure { error ->
                sessionPasswordCandidate.fill('\u0000')
                backupPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Backup restore failed: ${safeLogMessage(error)}"))
                update {
                    it.copy(
                        walletImportBusy = false,
                        walletImportError = error.message ?: "Backup restore failed"
                    )
                }
            }
        }
    }

    fun unlockWallet(wallet: String, password: CharArray) {
        if (_uiState.value.walletBusy) {
            password.fill('\u0000')
            return
        }
        val walletName = wallet.ifBlank { _uiState.value.activeWalletName }
        val walletSelector = wallet.ifBlank { _uiState.value.activeWalletAddress.ifBlank { _uiState.value.activeWalletName } }
        val sessionPasswordCandidate = password.copyOf()
        val unlockPassword = password.copyOf()
        val balancePassword = password.copyOf()
        val historyPassword = password.copyOf()
        password.fill('\u0000')
        update { it.copy(walletBusy = true, walletError = null) }
        scope.launch {
            runCatching {
                walletService.listWallets(unlockPassword)
            }.onSuccess { result ->
                val current = _uiState.value
                val visibleWallets = visibleManagedWallets(result.privateWallets, current)
                val selected = visibleWallets.firstOrNull { it.address == walletSelector }
                    ?: visibleWallets.firstOrNull { it.name == walletSelector }
                    ?: visibleWallets.firstOrNull { it.name == walletName }
                    ?: visibleWallets.firstOrNull { it.address == current.activeWalletAddress }
                if (selected == null) {
                    sessionPasswordCandidate.fill('\u0000')
                    unlockPassword.fill('\u0000')
                    balancePassword.fill('\u0000')
                    historyPassword.fill('\u0000')
                    update {
                        it.copy(
                            walletBusy = false,
                            walletLocked = true,
                            walletError = "No wallet was found for '$walletName'."
                        )
                    }
                    return@onSuccess
                }
                if (selected.encrypted && !selected.unlocked) {
                    sessionPasswordCandidate.fill('\u0000')
                    unlockPassword.fill('\u0000')
                    balancePassword.fill('\u0000')
                    historyPassword.fill('\u0000')
                    update {
                        it.copy(
                            walletBusy = false,
                            walletLocked = true,
                            walletBalance = null,
                            walletHistory = WalletHistory(wallet = selected.name),
                            walletError = "Wallet exists but could not be unlocked with that password."
                        )
                    }
                    return@onSuccess
                }
                update {
                    it.copy(
                        walletBusy = false,
                        walletLocked = false,
                        wallets = visibleWallets,
                        walletName = selected.name,
                        activeWalletName = selected.name,
                        activeWalletAddress = selected.address,
                        walletBalance = selected.toWalletBalance(),
                        walletHistory = WalletHistory(wallet = selected.name, count = selected.txCount),
                        walletError = null
                    )
                }
                storeSessionPassword(sessionPasswordCandidate)
                sessionPasswordCandidate.fill('\u0000')
                unlockPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Unlocked wallet '${selected.name}'"))
                refreshUnlockedWalletData(selected.address, selected.name, balancePassword, historyPassword)
                // Re-unlocking the session (after auto-lock, or via the Node
                // panel's "Wallet session" switch) also brings the network back
                // automatically. Guarded/idempotent: skips if the node is
                // already online or a miner owns the state.
                maybeAutoStartNode()
            }.onFailure { error ->
                sessionPasswordCandidate.fill('\u0000')
                unlockPassword.fill('\u0000')
                balancePassword.fill('\u0000')
                historyPassword.fill('\u0000')
                update {
                    it.copy(
                        walletBusy = false,
                        walletLocked = true,
                        walletError = error.message ?: "Wallet unlock failed"
                    )
                }
            }
        }
    }

    fun lockWallet() {
        clearSessionPassword()
        update {
            it.copy(
                walletLocked = true,
                walletBalance = null,
                walletHistory = WalletHistory(wallet = it.activeWalletName)
            )
        }
    }

    fun estimateSendFee(amount: String, priority: String = "normal") {
        if (_uiState.value.sendBusy) return
        update {
            it.copy(
                sendBusy = true,
                sendError = null,
                sendFeeEstimate = null,
                sendResult = null
            )
        }
        scope.launch {
            runCatching {
                walletService.estimateFee(amount, priority)
            }.onSuccess { estimate ->
                appendLog(RuntimeLog("send", "Fee estimate ready (${estimate.priority}): ${estimate.fee}"))
                update {
                    it.copy(
                        sendBusy = false,
                        sendFeeEstimate = estimate,
                        sendError = null
                    )
                }
            }.onFailure { error ->
                appendLog(RuntimeLog("send", "Fee estimate failed: ${safeLogMessage(error)}"))
                update {
                    it.copy(
                        sendBusy = false,
                        sendError = error.message ?: "Fee estimate failed"
                    )
                }
            }
        }
    }

    fun sendPrivateFunds(fromWallet: String, to: String, amount: String, password: CharArray, fee: String? = null) {
        if (_uiState.value.sendBusy) {
            password.fill('\u0000')
            return
        }
        val snapshot = _uiState.value
        val from = fromWallet.ifBlank { snapshot.activeWalletAddress.ifBlank { snapshot.activeWalletName.ifBlank { snapshot.walletName } } }
        val resolvedFrom = resolveWalletAddress(from, snapshot)
        if (resolvedFrom.isBlank()) {
            password.fill('\u0000')
            update { it.copy(sendError = "Select a wallet before sending.") }
            return
        }
        val selectedFee = fee ?: snapshot.sendFeeEstimate?.fee ?: "0"
        val sendPassword = password.copyOf()
        password.fill('\u0000')
        update {
            it.copy(
                sendBusy = true,
                sendError = null,
                sendResult = null
            )
        }
        scope.launch {
            runCatching {
                walletService.sendPrivate(
                    from = resolvedFrom,
                    to = to,
                    amount = amount,
                    fee = selectedFee,
                    password = sendPassword
                )
            }.onSuccess { result ->
                appendLog(RuntimeLog("send", "Private transfer queued: ${result.splitCount} tx(s), ${result.hash.ifBlank { "hash unavailable" }}"))
                update {
                    it.copy(
                        sendBusy = false,
                        sendResult = result,
                        sendError = null,
                        // Keep wallet UNLOCKED — session password stays in memory.
                        // Balance will refresh automatically on next pollRpc cycle.
                        walletBalance = null,
                        walletHistory = WalletHistory(wallet = it.activeWalletName)
                    )
                }
                // Refresh with session password so wallet stays visible immediately
                val pwd = copySessionPassword()
                if (pwd != null) refreshWallets(pwd) else refreshWallets()
            }.onFailure { error ->
                appendLog(RuntimeLog("send", "Private transfer failed: ${safeLogMessage(error)}"))
                update {
                    it.copy(
                        sendBusy = false,
                        sendError = error.message ?: "Private send failed"
                    )
                }
            }
        }
    }

    fun clearSendResult() {
        update {
            it.copy(sendFeeEstimate = null, sendResult = null, sendError = null)
        }
    }

    fun clearWalletCreationResult() {
        update {
            it.copy(walletCreationResult = null, walletCreationError = null)
        }
    }

    fun clearWalletBackupMessages() {
        update {
            it.copy(
                walletBackupResult = null,
                walletBackupError = null,
                walletImportResult = null,
                walletImportError = null
            )
        }
    }

    fun revealRecoveryPhrase(password: CharArray) {
        revealWalletSecrets(password = password, revealPrivateKeys = false)
    }

    fun revealPrivateKeys(password: CharArray) {
        revealWalletSecrets(password = password, revealPrivateKeys = true)
    }

    fun clearRevealedSecrets() {
        update {
            it.copy(
                secretRevealBusy = false,
                secretRevealError = null,
                revealedSecretWallet = "",
                revealedRecoveryPhrase = null,
                revealedScanPrivateKey = null,
                revealedSpendPrivateKey = null,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
    }

    fun clearPinChangeMessage() {
        update { it.copy(pinChangeError = null, pinChangeResult = null) }
    }

    fun changeWalletPin(
        oldPassword: CharArray,
        newPassword: CharArray,
        confirmPassword: CharArray
    ) {
        val snapshot = _uiState.value
        if (snapshot.pinChangeBusy) {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
            confirmPassword.fill('\u0000')
            return
        }
        val walletSelector = resolveWalletAddress(snapshot.activeWalletAddress, snapshot)
        when {
            walletSelector.isBlank() -> {
                oldPassword.fill('\u0000')
                newPassword.fill('\u0000')
                confirmPassword.fill('\u0000')
                update { it.copy(pinChangeError = "Select a wallet before changing the PIN.") }
                return
            }
            oldPassword.isEmpty() -> {
                oldPassword.fill('\u0000')
                newPassword.fill('\u0000')
                confirmPassword.fill('\u0000')
                update { it.copy(pinChangeError = "Current wallet password is required.") }
                return
            }
            newPassword.size < 8 -> {
                oldPassword.fill('\u0000')
                newPassword.fill('\u0000')
                confirmPassword.fill('\u0000')
                update { it.copy(pinChangeError = "New PIN/password must be at least 8 characters.") }
                return
            }
            !newPassword.contentEquals(confirmPassword) -> {
                oldPassword.fill('\u0000')
                newPassword.fill('\u0000')
                confirmPassword.fill('\u0000')
                update { it.copy(pinChangeError = "New PIN/password confirmation does not match.") }
                return
            }
        }

        val currentPassword = oldPassword.copyOf()
        val nextPassword = newPassword.copyOf()
        oldPassword.fill('\u0000')
        newPassword.fill('\u0000')
        confirmPassword.fill('\u0000')
        update {
            it.copy(
                pinChangeBusy = true,
                pinChangeError = null,
                pinChangeResult = null,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        scope.launch {
            runCatching {
                walletService.changePassphrase(walletSelector, currentPassword, nextPassword)
            }.onSuccess { result ->
                currentPassword.fill('\u0000')
                nextPassword.fill('\u0000')
                clearSessionPassword()
                update {
                    it.copy(
                        pinChangeBusy = false,
                        pinChangeError = null,
                        pinChangeResult = result,
                        walletLocked = true,
                        walletBalance = null,
                        walletHistory = WalletHistory(wallet = it.activeWalletName),
                        lastActivityMillis = System.currentTimeMillis()
                    )
                }
                appendLog(RuntimeLog("wallet", "Wallet PIN/password changed."))
            }.onFailure { error ->
                currentPassword.fill('\u0000')
                nextPassword.fill('\u0000')
                update {
                    it.copy(
                        pinChangeBusy = false,
                        pinChangeError = PrivacyGuards.redactForLog(error.message ?: "Wallet PIN/password change failed."),
                        pinChangeResult = null
                    )
                }
            }
        }
    }

    fun resetSetup() {
        clearSessionPassword()
        torSupervisor.stop()
        setupStore.clear()
        update {
            ZSilentUiState(
                zokaReady = cli.ready,
                darkTheme = it.darkTheme,
                logs = listOf(RuntimeLog("app", "Setup reset. Wallet files were not deleted."))
            )
        }
    }

    fun resetLocalWallet(): Boolean {
        val darkTheme = _uiState.value.darkTheme
        return runCatching {
            integratedMiningRequested = false
            nodeController.stop()
            torSupervisor.stop()
            miningController.close()
            clearSessionPassword()
            setupStore.clear()
            deleteLocalWalletFiles()
        }.onSuccess { deletedEntries ->
            update {
                ZSilentUiState(
                    zokaReady = cli.ready,
                    darkTheme = darkTheme,
                    logs = listOf(RuntimeLog("app", "Local wallet reset complete. Deleted $deletedEntries wallet file(s)/folder(s)."))
                )
            }
        }.onFailure { error ->
            val message = PrivacyGuards.redactForLog(error.message ?: "Local wallet reset failed.")
            appendLog(RuntimeLog("app", "Local wallet reset failed: $message"))
            update {
                it.copy(
                    loginError = "Could not reset local wallet: $message",
                    walletError = "Could not reset local wallet: $message"
                )
            }
        }.isSuccess
    }

    private fun deleteLocalWalletFiles(): Int {
        val network = ZokaNetworkConfig.Network.ifBlank { "mainnet" }
        val walletRoot = File(File(cli.dataRoot, ".zoka"), network).canonicalFile
        val targets = listOf(
            File(walletRoot, "private_wallets"),
            File(walletRoot, "wallets")
        )
        var deletedEntries = 0
        targets.forEach { target ->
            if (!target.exists()) return@forEach
            deletedEntries += deleteDirectoryTreeWithin(target, walletRoot)
        }
        return deletedEntries
    }

    private fun deleteDirectoryTreeWithin(target: File, allowedRoot: File): Int {
        val root = target.canonicalFile
        val allowed = allowedRoot.canonicalFile
        val allowedPrefix = allowed.path + File.separator
        require(root.path.startsWith(allowedPrefix)) {
            "Refusing to delete outside Zoka wallet data root: ${root.path}"
        }
        var deletedEntries = 0
        root.walkBottomUp().forEach { entry ->
            if (entry.exists()) {
                if (!entry.delete() && entry.exists()) {
                    throw IllegalStateException("Failed to delete ${entry.path}")
                }
                deletedEntries += 1
            }
        }
        return deletedEntries
    }

    private fun revealWalletSecrets(password: CharArray, revealPrivateKeys: Boolean) {
        val snapshot = _uiState.value
        if (snapshot.secretRevealBusy) {
            password.fill('\u0000')
            return
        }
        val walletSelector = resolveWalletAddress(snapshot.activeWalletAddress, snapshot)
        if (walletSelector.isBlank()) {
            password.fill('\u0000')
            update { it.copy(secretRevealError = "Select a wallet before revealing recovery data.") }
            return
        }
        if (password.isEmpty()) {
            password.fill('\u0000')
            update { it.copy(secretRevealError = "Wallet password is required to reveal recovery data.") }
            return
        }
        val revealPassword = password.copyOf()
        password.fill('\u0000')
        update {
            it.copy(
                secretRevealBusy = true,
                secretRevealError = null,
                revealedSecretWallet = "",
                revealedRecoveryPhrase = null,
                revealedScanPrivateKey = null,
                revealedSpendPrivateKey = null,
                lastActivityMillis = System.currentTimeMillis()
            )
        }
        scope.launch {
            runCatching {
                walletService.exportSecrets(walletSelector, revealPassword)
            }.onSuccess { secrets ->
                revealPassword.fill('\u0000')
                val secretWallet = secrets.userAddress.ifBlank { secrets.address.ifBlank { walletSelector } }
                update {
                    it.copy(
                        secretRevealBusy = false,
                        secretRevealError = if (!revealPrivateKeys && secrets.mnemonic.isNullOrBlank()) {
                            "This wallet has no recovery phrase stored in encrypted wallet data."
                        } else {
                            null
                        },
                        revealedSecretWallet = secretWallet,
                        revealedRecoveryPhrase = if (revealPrivateKeys) null else secrets.mnemonic,
                        revealedScanPrivateKey = if (revealPrivateKeys) secrets.scanPrivateKeyHex else null,
                        revealedSpendPrivateKey = if (revealPrivateKeys) secrets.spendPrivateKeyHex else null,
                        lastActivityMillis = System.currentTimeMillis()
                    )
                }
                appendLog(RuntimeLog("wallet", if (revealPrivateKeys) "Private keys revealed after password confirmation." else "Recovery phrase revealed after password confirmation."))
            }.onFailure { error ->
                revealPassword.fill('\u0000')
                update {
                    it.copy(
                        secretRevealBusy = false,
                        secretRevealError = PrivacyGuards.redactForLog(error.message ?: "Could not reveal wallet recovery data."),
                        revealedSecretWallet = "",
                        revealedRecoveryPhrase = null,
                        revealedScanPrivateKey = null,
                        revealedSpendPrivateKey = null
                    )
                }
            }
        }
    }

    fun startNode(autoMine: Boolean = false, threads: Int? = null) {
        val snapshot = _uiState.value
        val password = copySessionPassword()
        if (password == null || password.isEmpty()) {
            password?.fill('\u0000')
            appendLog(RuntimeLog("node", "Unlock the wallet session before starting the node. ZSilent will reuse the in-memory session password."))
            return
        }
        if (!autoMine && snapshot.mining.running && snapshot.nodeStatus != ServiceStatus.Online) {
            password.fill('\u0000')
            appendLog(RuntimeLog("node", "Mining is already active and owns the chain state. Stop that miner first, or start node + mining as one integrated session."))
            return
        }
        if (autoMine && !snapshot.miningSyncReady) {
            appendLog(RuntimeLog("mining", "Mining locked until sync completes: node height ${snapshot.chainHeight}, network height ${snapshot.networkHeight}, remaining ${snapshot.syncRemainingBlocks}. Starting node-only sync."))
        }
        integratedMiningRequested = autoMine && snapshot.miningSyncReady
        val passwordText = try {
            String(password)
        } finally {
            password.fill('\u0000')
        }
        scope.launch {
            var torProxy = if (snapshot.torEnabled) "${snapshot.torSocksHost}:${snapshot.torSocksPort}" else ""
            var externalAddrs = emptyList<String>()

            if (snapshot.torEnabled && snapshot.managedTorEnabled) {
                val torResult = runCatching {
                    torSupervisor.start(
                        TorLaunchConfig(
                            socksHost = "127.0.0.1",
                            socksPort = snapshot.managedTorSocksPort.coerceIn(1, 65535)
                        )
                    )
                }

                torResult.onSuccess { tor ->
                    torProxy = "${tor.socksHost}:${tor.socksPort}"
                    externalAddrs = listOf(tor.onionMultiaddr)
                    update {
                        it.copy(
                            torEnabled = true,
                            torSocksHost = tor.socksHost,
                            torSocksPort = tor.socksPort,
                            managedTorSocksPort = tor.socksPort,
                            torOnionHost = tor.onionHost,
                            torOnionMultiaddr = tor.onionMultiaddr,
                            torSeedAddress = tor.onionMultiaddr,
                            torError = null
                        )
                    }
                }.onFailure { error ->
                    // Never block node startup on Tor. Hybrid mode means the
                    // node can run over clearnet even if managed Tor didn't come
                    // up (Tor bootstrap can be slow or the network overloaded);
                    // it just won't advertise its onion this run. Previously a
                    // Tor failure with tor-only/onion-seed set aborted startup
                    // entirely, leaving the node switch stuck in Error -- the
                    // node must always at least start.
                    val message = safeLogMessage(error)
                    update { it.copy(torStatus = ServiceStatus.Error, torError = message) }
                    appendLog(RuntimeLog("tor", "Managed Tor failed: $message"))
                    torProxy = ""
                    externalAddrs = emptyList()
                    appendLog(RuntimeLog("node", "Starting the node over direct clearnet; onion will be unavailable until Tor recovers."))
                }
            }

            nodeController.start(
                password = passwordText,
                address = resolveActiveWalletAddress(snapshot),
                network = ZokaNetworkConfig.Network,
                autoMine = false,
                threads = threads,
                torProxy = torProxy,
                torOnly = snapshot.torOnly,
                externalAddrs = externalAddrs
            )
            if (externalAddrs.isNotEmpty()) {
                watchNodeReadySeedAddress()
            }
            if (autoMine && snapshot.miningSyncReady) {
                var enabled = false
                repeat(30) {
                    if (!enabled) {
                        delay(1_000)
                        enabled = enableLocalNodeMining(threads ?: snapshot.mining.availableCpuThreads.coerceAtLeast(1), quiet = true)
                    }
                }
                if (!enabled) {
                    integratedMiningRequested = false
                    val message = "Node started, but mining control was not reachable yet. Node remains online; enable mining manually after RPC is ready."
                    update { it.copy(miningStatus = ServiceStatus.Error, miningDiagnostic = message, localMiningEnabled = false) }
                    appendLog(RuntimeLog("mining", message))
                }
            }
        }
    }

    fun stopNode() {
        integratedMiningRequested = false
        update { it.copy(localMiningEnabled = false, miningDiagnostic = null) }
        nodeController.stop()
        if (_uiState.value.managedTorEnabled) {
            torSupervisor.stop()
        }
    }

    private fun watchNodeReadySeedAddress() {
        scope.launch {
            repeat(180) {
                val seedAddress = readOnionSeedAddress(nodeController.readyFile)
                if (seedAddress.isNotBlank()) {
                    update { it.copy(torSeedAddress = seedAddress) }
                    appendLog(RuntimeLog("tor", "Onion seed address ready."))
                    return@launch
                }
                delay(500)
            }
        }
    }

    private fun readOnionSeedAddress(file: File): String {
        if (!file.isFile) return ""
        return runCatching {
            val root = json.parseToJsonElement(file.readText()).jsonObject
            val seedAddresses = root["seed_addresses"]
                ?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty()
            seedAddresses.firstOrNull { it.startsWith("/onion3/") }
                ?: root["seed_address"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("/onion3/") }
                ?: ""
        }.getOrDefault("")
    }

    fun syncNow() {
        val snapshot = _uiState.value
        if (!canRunRpcSync(snapshot)) {
            appendLog(RuntimeLog("sync", rpcSyncBlockedReason(snapshot)))
            return
        }
        appendLog(RuntimeLog("sync", "Manual sync requested from ZSilent."))
        scope.launch {
            runRpcSyncOnce(manual = true, limit = snapshot.syncRemainingBlocks.coerceAtLeast(1).coerceAtMost(512))
        }
    }

    fun startMining(threads: Int) {
        val snapshot = _uiState.value
        val password = copySessionPassword()
        if (password == null || password.isEmpty()) {
            password?.fill('\u0000')
            rejectMiningRequest("Unlock the wallet session before enabling mining. ZSilent will reuse the in-memory session password for rewards.")
            return
        }
        if (!snapshot.miningSyncReady) {
            password.fill('\u0000')
            rejectMiningRequest("Mining blocked until the node is close to the network head: node height ${snapshot.chainHeight}, network height ${snapshot.networkHeight}, remaining ${snapshot.syncRemainingBlocks} (tolerance $MINING_SYNC_TOLERANCE_BLOCKS block(s)). Start the node first and let it sync.")
            return
        }
        if (snapshot.nodeStatus == ServiceStatus.Starting || snapshot.nodeStatus == ServiceStatus.Stopping) {
            password.fill('\u0000')
            rejectMiningRequest("Node is still changing state. Wait for it to finish before enabling mining.")
            return
        }
        if (snapshot.mining.running) {
            password.fill('\u0000')
            appendLog(RuntimeLog("mining", "Mining is already active. ZSilent will keep monitoring the live miner."))
            update { it.copy(miningBlockReason = null) }
            return
        }
        password.fill('\u0000')
        integratedMiningRequested = true
        update {
            it.copy(
                miningStatus = ServiceStatus.Starting,
                miningBlockReason = null,
                miningDiagnostic = null
            )
        }
        if (snapshot.nodeStatus == ServiceStatus.Online) {
            appendLog(RuntimeLog("mining", "Enabling mining inside the running full node. Node stays online for peers."))
            scope.launch {
                enableLocalNodeMining(threads)
            }
            return
        }

        appendLog(RuntimeLog("mining", "Starting node-only mode first, then enabling mining through the local node control channel."))
        startNode(autoMine = false)
        scope.launch {
            var enabled = false
            repeat(30) {
                if (!enabled) {
                    delay(1_000)
                    enabled = enableLocalNodeMining(threads, quiet = true)
                }
            }
            if (!enabled) {
                integratedMiningRequested = false
                val message = "Could not reach local node mining control after startup. Node remains independent; try mining again once Node is online."
                update { it.copy(miningStatus = ServiceStatus.Error, miningDiagnostic = message, localMiningEnabled = false) }
                appendLog(RuntimeLog("mining", message))
            }
        }
    }

    fun stopMining() {
        val snapshot = _uiState.value
        integratedMiningRequested = false
        update { it.copy(miningBlockReason = null, miningDiagnostic = null, localMiningEnabled = false) }
        if (snapshot.miningControlledByApp) {
            miningController.stop()
            return
        }
        if (snapshot.nodeStatus == ServiceStatus.Online || snapshot.miningStatus == ServiceStatus.Starting) {
            appendLog(RuntimeLog("mining", "Disabling mining through the local node control channel. Node remains online."))
            update { it.copy(miningStatus = ServiceStatus.Stopping) }
            scope.launch {
                runCatching {
                    localRpc.stopMining()
                }.onSuccess {
                    update {
                        it.copy(
                            miningStatus = ServiceStatus.Offline,
                            localMiningEnabled = false,
                            mining = it.mining.copy(running = false, hashrateHps = 0)
                        )
                    }
                }.onFailure { error ->
                    update { it.copy(miningStatus = ServiceStatus.Error, miningDiagnostic = safeLogMessage(error)) }
                    appendLog(RuntimeLog("mining", "Could not disable mining via local node RPC: ${safeLogMessage(error)}"))
                }
            }
            return
        }
        miningController.stop()
    }

    private suspend fun enableLocalNodeMining(threads: Int, quiet: Boolean = false): Boolean {
        lastMiningThreads = threads.coerceAtLeast(1)
        return runCatching {
            localRpc.startMining(threads)
        }.onSuccess {
            update { it.copy(miningStatus = ServiceStatus.Starting, miningDiagnostic = null, localMiningEnabled = true) }
            if (!quiet) {
                appendLog(RuntimeLog("mining", "Local node accepted mining start request with ${threads.coerceAtLeast(1)} thread(s)."))
            }
        }.onFailure { error ->
            if (!quiet) {
                integratedMiningRequested = false
                update { it.copy(miningStatus = ServiceStatus.Error, miningDiagnostic = safeLogMessage(error), localMiningEnabled = false) }
                appendLog(RuntimeLog("mining", "Could not enable mining via local node RPC: ${safeLogMessage(error)}"))
            }
        }.isSuccess
    }

    fun close() {
        clearSessionPassword()
        miningController.close()
        nodeController.close()
        torSupervisor.stop()
        ZokaProcessKiller.terminateManagedZokaProcesses(cli)
        rpc.close()
        localRpc.close()
    }

    private fun resolveActiveWalletAddress(snapshot: ZSilentUiState): String {
        val direct = snapshot.activeWalletAddress.trim()
        if (direct.startsWith("zka1")) return direct
        return snapshot.wallets.firstOrNull { it.name == snapshot.activeWalletName }?.address?.takeIf { it.startsWith("zka1") }
            ?: snapshot.wallets.firstOrNull { it.name == snapshot.walletName }?.address?.takeIf { it.startsWith("zka1") }
            ?: snapshot.wallets.firstOrNull { it.address.startsWith("zka1") }?.address
            ?: direct
    }

    private suspend fun pollRpc() {
        val snapshot = _uiState.value
        val localStats = runCatching { localRpc.stats() }.getOrNull()
        val localRpcHeight = localStats?.height ?: runCatching { localRpc.height() }.getOrNull()
        val mayReadDbDirectly = localRpcHeight == null &&
            snapshot.nodeStatus == ServiceStatus.Offline &&
            snapshot.miningStatus == ServiceStatus.Offline
        // Throttle direct DB reads to once per 15s — each call spawns a zoka subprocess
        val now = System.currentTimeMillis()
        val localDbHeight = if (mayReadDbDirectly && (now - lastLocalHeightReadMillis > 15_000L)) {
            lastLocalHeightReadMillis = now
            readLocalChainHeight().getOrElse {
                maxOf(snapshot.chainHeight, snapshot.mining.chainHeight)
            }
        } else {
            maxOf(snapshot.chainHeight, snapshot.mining.chainHeight)
        }
        val localHeight = maxOf(localDbHeight, localRpcHeight ?: 0L, snapshot.mining.chainHeight)
        val localRpcOnline = localRpcHeight != null
        val localRpcMining = localStats?.let { it.localMining || it.reportedHashrateHps > 0 } == true
        val miningDiagnostic = snapshot.miningDiagnostic

        runCatching {
            val stats = rpc.stats()
            val networkHeight = stats.height.coerceAtLeast(0)
            val remaining = (networkHeight - localHeight).coerceAtLeast(0)
            // Synced once the local node has caught up to the network head.
            // A freshly reset network sits at genesis (height 0); reaching the
            // network requires NO blocks, so 0 >= 0 must count as synced —
            // otherwise mining stays locked forever on a brand-new chain.
            // The RPC-offline case is handled in the onFailure branch below.
            val synced = localHeight >= networkHeight
            // Mining unlocks a little earlier than the exact-sync banner:
            // trailing the head by a couple of blocks is normal gossip lag,
            // not a reason to keep the miner cold.
            val miningReady = remaining <= MINING_SYNC_TOLERANCE_BLOCKS
            update {
                val reconciledMining = if (localRpcMining) {
                    it.mining.copy(
                        running = true,
                        chainHeight = maxOf(it.mining.chainHeight, localStats?.height ?: 0L, localHeight),
                        difficultyBits = (localStats?.difficulty ?: it.mining.difficultyBits.toLong()).toInt(),
                        hashrateHps = maxOf(it.mining.hashrateHps, localStats?.reportedHashrateHps ?: 0L)
                    )
                } else if (localRpcOnline && !it.miningControlledByApp && it.mining.running) {
                    // The node is reachable and reports it is NOT mining:
                    // clear the integrated-mining snapshot. Without this, one
                    // poll that catches the node still draining a stopped
                    // batch re-marks running=true and nothing ever unsets it,
                    // leaving the mining switch stuck ON after a stop.
                    it.mining.copy(running = false, hashrateHps = 0)
                } else {
                    it.mining
                }
                it.copy(
                    rpcOnline = true,
                    localRpcOnline = localRpcOnline,
                    nodeStatus = when {
                        it.nodeStatus == ServiceStatus.Stopping -> it.nodeStatus
                        localRpcOnline -> ServiceStatus.Online
                        else -> it.nodeStatus
                    },
                    miningStatus = when {
                        localRpcMining && !miningDiagnostic.isNullOrBlank() -> ServiceStatus.Error
                        localRpcMining -> ServiceStatus.Online
                        // Same one-way stickiness fix: only downgrade a
                        // previously-confirmed Online once the node itself
                        // says mining is off. Starting/Stopping/Error are
                        // left alone so startup warmup and error surfacing
                        // keep working.
                        localRpcOnline && !it.miningControlledByApp &&
                            it.miningStatus == ServiceStatus.Online -> ServiceStatus.Offline
                        else -> it.miningStatus
                    },
                    mining = reconciledMining,
                    localMiningEnabled = localRpcMining,
                    localConnectedPeers = localStats?.connectedPeers ?: it.localConnectedPeers,
                    chainHeight = localHeight,
                    networkHeight = networkHeight,
                    syncRemainingBlocks = remaining,
                    syncProgressPercent = syncProgressPercent(localHeight, networkHeight),
                    networkSynced = synced,
                    miningSyncReady = miningReady,
                    syncStatusMessage = if (synced) {
                        "OK synced with network bootstrap."
                    } else if (it.syncBusy) {
                        "Importing blocks from network bootstrap."
                    } else {
                        "Syncing from network bootstrap."
                    },
                    stats = stats
                )
            }
            maybeStartAutoRpcSync(localHeight, networkHeight)
            // Auto-push blocks when local is ahead of remote (works through NAT,
            // covers both new blocks from mining and blocks loaded from local DB).
            if (localHeight > networkHeight && localRpcOnline && !blockPushInProgress) {
                val from = maxOf(networkHeight + 1, lastPushedRemoteHeight + 1)
                if (from <= localHeight) {
                    scope.launch { pushBlockRangeToRemotes(from, localHeight) }
                }
            }
        }.onFailure {
            // Bootstrap RPC unreachable — fall back to the local node's own
            // sync view, which is fed by its P2P mesh. A node that reports a
            // healthy mesh and near-zero lag is safe to mine from even while
            // the centralized bootstrap endpoint is saturated; otherwise
            // (isolated node, real lag, node offline) mining stays locked.
            val localSync = if (localRpcOnline) {
                runCatching { localRpc.syncStatus() }.getOrNull()
            } else {
                null
            }
            val meshPeers = localSync?.connectedPeers ?: 0L
            // Serverless (no bootstrap RPC): trust the node's own P2P sync view.
            // - ready_to_mine is the node's authoritative gate (caught up to the
            //   P2P head with at least its min_mine_peers). The old
            //   MIN_MESH_PEERS_FOR_MINING=2 UI gate blocked a bootstrapping
            //   2-node network (each node sees only 1 peer) from ever mining.
            // - target_height is the P2P-derived network head, so the UI shows a
            //   real "network block" instead of 0 (which came from the dead RPC).
            val meshMiningReady = localSync?.readyToMine == true
            val meshNetworkHeight = localSync?.targetHeight?.coerceAtLeast(localHeight) ?: 0L
            val meshSynced = localSync != null && localSync.lag <= MINING_SYNC_TOLERANCE_BLOCKS
            update {
                it.copy(
                    rpcOnline = false,
                    localRpcOnline = localRpcOnline,
                    nodeStatus = when {
                        it.nodeStatus == ServiceStatus.Stopping -> it.nodeStatus
                        localRpcOnline -> ServiceStatus.Online
                        else -> it.nodeStatus
                    },
                    localMiningEnabled = localRpcMining,
                    localConnectedPeers = localStats?.connectedPeers ?: (localSync?.connectedPeers ?: it.localConnectedPeers),
                    chainHeight = localHeight,
                    networkHeight = if (meshNetworkHeight > 0) meshNetworkHeight else it.networkHeight,
                    networkSynced = meshSynced,
                    miningSyncReady = meshMiningReady,
                    syncStatusMessage = when {
                        meshMiningReady -> "Synced over P2P with $meshPeers peer(s) (serverless); mining available."
                        meshSynced -> "Synced with $meshPeers peer(s); mining unlocks at ${localSync?.minPeers ?: 1} peer(s)."
                        localSync != null -> "Syncing over P2P mesh: $meshPeers peer(s), ${localSync.lag} block(s) behind."
                        else -> "Waiting for the local node's P2P sync view."
                    }
                )
            }
        }
        // Refresh balance whenever the local node is online — runs even when
        // the remote RPC is offline so mining rewards appear without restarting.
        val balRefNow = System.currentTimeMillis()
        val balRefSnap = _uiState.value
        if (!balRefSnap.walletLocked && !balRefSnap.walletBusy && localRpcOnline &&
            balRefNow - lastBalanceRefreshMillis > 30_000L) {
            lastBalanceRefreshMillis = balRefNow
            val balRefPwd = copySessionPassword()
            if (balRefPwd != null && balRefPwd.isNotEmpty()) {
                val addr = balRefSnap.activeWalletAddress.ifBlank { balRefSnap.activeWalletName }
                update { it.copy(lastBalanceRefreshedAt = balRefNow) }
                refreshUnlockedWalletData(addr, balRefSnap.activeWalletName, balRefPwd.copyOf(), balRefPwd.copyOf())
                balRefPwd.fill(0.toChar())
            }
        }
    }

    private fun maybeStartAutoRpcSync(localHeight: Long, networkHeight: Long) {
        if (networkHeight <= localHeight) return
        val now = System.currentTimeMillis()
        if (now - lastAutoRpcSyncAttemptMillis < 45_000L) return
        if (now < autoRpcSyncBackoffUntilMillis) return
        val snapshot = _uiState.value
        if (!canRunRpcSync(snapshot)) return
        lastAutoRpcSyncAttemptMillis = now
        appendLog(RuntimeLog("sync", "Node chain is ${networkHeight - localHeight} block(s) behind the network bootstrap. Starting background RPC sync."))
        scope.launch {
            runRpcSyncOnce(manual = false, limit = (networkHeight - localHeight).coerceAtLeast(1).coerceAtMost(512))
        }
    }

    private fun canRunRpcSync(snapshot: ZSilentUiState): Boolean {
        // `rpc-sync` opens the chain DB directly with its own RocksDB handle.
        // RocksDB only allows one process to hold that DB open at a time, and
        // the embedded fullnode (whenever it's running at all, not just while
        // Starting/Stopping) already owns it for as long as the process is
        // alive — including the warmup window before its own API/P2P sync has
        // caught up. Running rpc-sync concurrently makes both sides spin on
        // lock contention instead of progressing. Only allow it when there is
        // no managed node process holding the DB, i.e. fully Offline.
        return cli.zoka != null &&
            !rpcSyncInProgress &&
            !snapshot.mining.running &&
            snapshot.miningStatus != ServiceStatus.Online &&
            snapshot.nodeStatus == ServiceStatus.Offline
    }

    private fun rpcSyncBlockedReason(snapshot: ZSilentUiState): String {
        if (cli.zoka == null) return "Cannot sync: zoka binary was not found."
        if (rpcSyncInProgress || snapshot.syncBusy) return "Sync is already running."
        if (snapshot.nodeStatus != ServiceStatus.Offline) {
            return "The full node already syncs itself while it's running; stop it before running a direct RPC sync."
        }
        if (snapshot.miningStatus == ServiceStatus.Online || snapshot.mining.running) return "Stop mining before running direct RPC sync."
        return "Sync is not available right now."
    }

    private suspend fun runRpcSyncOnce(manual: Boolean, limit: Long) = withContext(Dispatchers.IO) {
        if (rpcSyncInProgress) return@withContext
        val zoka = cli.zoka ?: run {
            appendLog(RuntimeLog("sync", "Cannot sync: zoka binary was not found."))
            return@withContext
        }
        rpcSyncInProgress = true
        update {
            it.copy(
                syncBusy = true,
                syncStatusMessage = if (manual) "Manual sync is importing blocks from the network bootstrap." else "Importing blocks from network bootstrap."
            )
        }
        try {
            val output = cli.withZokaCommandLock {
                val child = cli.processBuilder(
                    zoka,
                    buildList {
                        add("rpc-sync")
                        add("--network")
                        add(ZokaNetworkConfig.Network)
                        if (ZokaNetworkConfig.ManifestPath.isNotBlank()) {
                            add("--network-config")
                            add(ZokaNetworkConfig.ManifestPath)
                        }
                        add("--limit")
                        add(limit.coerceAtLeast(1).toString())
                        add("--json")
                    }
                )
                    .redirectErrorStream(true)
                    .start()
                val finished = child.waitFor(180, TimeUnit.SECONDS)
                if (!finished) {
                    child.destroyForcibly()
                    error("RPC sync timed out.")
                }
                val commandOutput = child.inputStream.bufferedReader().readText()
                val exit = child.exitValue()
                if (exit != 0) {
                    error(PrivacyGuards.redactForLog(commandOutput.ifBlank { "zoka rpc-sync exited with code $exit" }))
                }
                commandOutput
            }
            val response = json.decodeFromString<RpcSyncResponse>(output.trim())
            update {
                it.copy(
                    chainHeight = maxOf(it.chainHeight, response.endHeight),
                    syncRemainingBlocks = (it.networkHeight - response.endHeight).coerceAtLeast(0),
                    syncProgressPercent = syncProgressPercent(response.endHeight, it.networkHeight),
                    networkSynced = response.endHeight >= it.networkHeight,
                    miningSyncReady = response.endHeight + MINING_SYNC_TOLERANCE_BLOCKS >= it.networkHeight,
                    syncStatusMessage = if (response.imported > 0) {
                        "Imported ${response.imported} block(s) from network bootstrap."
                    } else {
                        "Node chain already matches the network bootstrap."
                    }
                )
            }
            val lastEvent = response.events.lastOrNull()?.let(PrivacyGuards::redactForLog)
            appendLog(RuntimeLog("sync", "RPC sync complete: ${response.startHeight} -> ${response.endHeight}, imported ${response.imported} block(s)."))
            if (!lastEvent.isNullOrBlank()) {
                appendLog(RuntimeLog("sync", lastEvent))
            }
        } catch (error: Throwable) {
            val message = PrivacyGuards.redactForLog(error.message ?: "RPC sync failed.")
            val lockBusy = message.contains("lock", ignoreCase = true) ||
                message.contains("LOCK", ignoreCase = false) ||
                message.contains("IO error", ignoreCase = true) ||
                message.contains("storage busy", ignoreCase = true)
            if (lockBusy) {
                autoRpcSyncBackoffUntilMillis = System.currentTimeMillis() + 5 * 60_000L
                val statusHint = "RPC sync skipped: chain DB is held by the running node. Auto-retry paused for 5 minutes. Stop the node and press 'Sync now' to import a large batch quickly."
                update { it.copy(syncStatusMessage = statusHint) }
                appendLog(RuntimeLog("sync", statusHint))
            } else {
                update { it.copy(syncStatusMessage = "RPC sync failed: $message") }
                appendLog(RuntimeLog("sync", "RPC sync failed: $message"))
            }
        } finally {
            rpcSyncInProgress = false
            update { it.copy(syncBusy = false) }
        }
    }

    private suspend fun readLocalChainHeight(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val zoka = cli.zoka ?: error("zoka binary not found")
            cli.withZokaCommandLock {
                val child = cli.processBuilder(
                    zoka,
                    listOf(
                        "height",
                        "--network", ZokaNetworkConfig.Network,
                        "--json"
                    )
                )
                    .redirectErrorStream(true)
                    .start()
                val finished = child.waitFor(4, TimeUnit.SECONDS)
                if (!finished) {
                    child.destroyForcibly()
                    error("node height read timed out")
                }
                val output = child.inputStream.bufferedReader().readText()
                val exit = child.exitValue()
                if (exit != 0) {
                    error(PrivacyGuards.redactForLog(output.ifBlank { "zoka height exited with code $exit" }))
                }
                json.decodeFromString<LocalChainHeightResponse>(output.trim()).height
            }
        }
    }

    private fun syncProgressPercent(localHeight: Long, networkHeight: Long): Double {
        if (networkHeight <= 0L) return 0.0
        return ((localHeight.toDouble() / networkHeight.toDouble()) * 100.0).coerceIn(0.0, 100.0)
    }

    private fun enforceAutoLock() {
        val snapshot = _uiState.value
        if (!snapshot.onboardingComplete || snapshot.appLocked) return
        if (runtimeKeepsSessionOpen(snapshot)) {
            update { it.copy(lastActivityMillis = System.currentTimeMillis()) }
            return
        }
        val timeoutMillis = snapshot.onboardingAutoLockMinutes.coerceAtLeast(1) * 60_000L
        val idleMillis = System.currentTimeMillis() - snapshot.lastActivityMillis
        if (idleMillis >= timeoutMillis) {
            lockAppInternal("Locked after ${snapshot.onboardingAutoLockMinutes} minute(s) of inactivity.")
        }
    }

    private fun runtimeKeepsSessionOpen(snapshot: ZSilentUiState): Boolean {
        return snapshot.nodeStatus == ServiceStatus.Starting ||
            snapshot.nodeStatus == ServiceStatus.Online ||
            snapshot.nodeStatus == ServiceStatus.Stopping ||
            snapshot.mining.running ||
            snapshot.miningStatus == ServiceStatus.Starting ||
            snapshot.miningStatus == ServiceStatus.Online ||
            snapshot.miningStatus == ServiceStatus.Stopping
    }

    private fun lockAppInternal(message: String) {
        clearSessionPassword()
        // Kill all processes when closing wallet session
        miningController.close()
        nodeController.close()
        torSupervisor.stop()
        ZokaProcessKiller.terminateManagedZokaProcesses(cli)
        update {
            it.copy(
                appLocked = true,
                loginBusy = false,
                loginError = null,
                lockMessage = message,
                walletLocked = true,
                walletBalance = null,
                walletHistory = WalletHistory(wallet = it.activeWalletName),
                miningStatus = ServiceStatus.Offline,
                localMiningEnabled = false,
                miningDiagnostic = null,
                miningBlockReason = null,
                mining = it.mining.copy(running = false, hashrateHps = 0),
                sendFeeEstimate = null,
                sendResult = null,
                sendError = null,
                walletCreationResult = null,
                walletBackupResult = null,
                walletImportResult = null,
                secretRevealBusy = false,
                secretRevealError = null,
                revealedSecretWallet = "",
                revealedRecoveryPhrase = null,
                revealedScanPrivateKey = null,
                revealedSpendPrivateKey = null,
                pinChangeBusy = false,
                pinChangeError = null,
                pinChangeResult = null
            )
        }
    }

    private fun managedAddressesFor(current: ZSilentUiState, extraAddresses: List<String> = emptyList()): List<String> {
        return (current.managedWalletAddresses + current.activeWalletAddress + current.onboardingWalletAddress + extraAddresses)
            .map { it.trim() }
            .filter { it.isZokaAddress() }
            .distinct()
    }

    private fun managedNamesFor(current: ZSilentUiState, extraNames: List<String> = emptyList()): List<String> {
        return (current.managedWallets + current.managedWalletName + current.walletName + current.onboardingWalletName + extraNames)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.isZokaAddress() }
            .distinct()
    }

    private fun resolveWalletAddress(selector: String, current: ZSilentUiState): String {
        val trimmed = selector.trim()
            .ifBlank { current.activeWalletAddress.ifBlank { current.activeWalletName.ifBlank { current.walletName } } }
        if (trimmed.isZokaAddress()) return trimmed
        return current.wallets.firstOrNull { it.address == trimmed }?.address
            ?: current.wallets.firstOrNull { it.name == trimmed }?.address
            ?: current.activeWalletAddress.takeIf { it.isZokaAddress() }
            ?: current.onboardingWalletAddress.takeIf { it.isZokaAddress() }
            ?: ""
    }

    private fun resolveWalletLabel(selector: String, current: ZSilentUiState): String {
        val trimmed = selector.trim()
        return current.wallets.firstOrNull { it.address == trimmed || it.name == trimmed }?.name
            ?: current.activeWalletName.ifBlank { current.walletName }.ifBlank { trimmed }
    }

    private fun visibleManagedWallets(wallets: List<LocalWallet>, current: ZSilentUiState): List<LocalWallet> {
        val managedNames = current.managedWallets
            .ifEmpty {
                listOf(current.managedWalletName, current.walletName, current.onboardingWalletName)
                    .filter { name -> name.isNotBlank() }
            }
            .toSet()
        val managedAddresses = managedAddressesFor(current)
            .toSet()
        return when {
            current.onboardingComplete && managedAddresses.isNotEmpty() -> {
                wallets.filter { it.address in managedAddresses }
            }
            current.onboardingComplete && managedNames.isNotEmpty() -> {
                wallets.filter { it.name in managedNames }
            }
            else -> wallets
        }
    }

    private fun LocalWallet.toWalletBalance(): WalletBalance {
        return WalletBalance(
            wallet = name,
            address = address,
            balanceAtoms = balanceAtoms,
            balance = balance,
            lockedBalanceAtoms = lockedBalanceAtoms,
            lockedBalance = lockedBalance,
            totalBalanceAtoms = totalBalanceAtoms,
            totalBalance = totalBalance,
            viewOnly = false
        )
    }

    private fun refreshUnlockedWalletData(
        walletAddress: String,
        walletLabel: String,
        balancePassword: CharArray,
        historyPassword: CharArray
    ) {
        scope.launch {
            runCatching {
                val walletSelector = walletAddress.ifBlank { walletLabel }
                val balance = walletService.balance(walletSelector, balancePassword)
                val history = walletService.history(walletSelector, historyPassword)
                balance to history
            }.onSuccess { (balance, history) ->
                update {
                    // Guard against a stale in-flight request: this fetch was
                    // issued for `walletAddress`, but the user may have since
                    // unlocked, reset, or created a different wallet while the
                    // CLI call was running. If the active wallet changed in
                    // the meantime, drop this result instead of overwriting
                    // the current wallet's balance with a previous wallet's
                    // data (e.g. "Clear all data" -> create new wallet while
                    // an old periodic refresh was still in flight).
                    if (walletAddress.isNotBlank() &&
                        it.activeWalletAddress.isNotBlank() &&
                        it.activeWalletAddress != walletAddress
                    ) {
                        return@update it
                    }
                    val displayName = balance.wallet.ifBlank { walletLabel }
                    val address = balance.address.ifBlank { walletAddress.ifBlank { it.activeWalletAddress } }
                    it.copy(
                        walletName = displayName,
                        activeWalletName = displayName,
                        activeWalletAddress = address,
                        managedWalletAddresses = managedAddressesFor(it, listOf(address)),
                        walletBalance = balance,
                        walletHistory = history,
                        walletError = null
                    )
                }
            }.onFailure { error ->
                balancePassword.fill('\u0000')
                historyPassword.fill('\u0000')
                appendLog(RuntimeLog("wallet", "Wallet opened; balance/history refresh skipped: ${safeLogMessage(error)}"))
                update {
                    it.copy(
                        walletError = "Wallet unlocked. Balance and history will refresh when chain data is available."
                    )
                }
            }
        }
    }

    private fun startMiningDiagnosticLogTail() {
        scope.launch(Dispatchers.IO) {
            val network = ZokaNetworkConfig.Network.ifBlank { "mainnet" }
            val logFile = File(File(File(cli.dataRoot, ".zoka"), network), "mining_diagnostic.log")
            var offset = logFile.takeIf { it.exists() && it.isFile }?.length() ?: 0L
            var announced = false
            while (true) {
                try {
                    if (!logFile.exists()) {
                        if (!announced) {
                            appendLog(RuntimeLog("mining", "Waiting for mining diagnostic log at ${logFile.absolutePath}"))
                            announced = true
                        }
                        delay(1_000)
                        continue
                    }
                    announced = false
                    val length = logFile.length()
                    if (length < offset) offset = 0L
                    if (length > offset) {
                        RandomAccessFile(logFile, "r").use { reader ->
                            reader.seek(offset)
                            while (true) {
                                val raw = reader.readLine() ?: break
                                val clean = PrivacyGuards.redactForLog(raw.trim())
                                if (clean.isNotBlank()) {
                                    appendLog(RuntimeLog("mining", clean))
                                    observeMiningDiagnosticLine(clean)
                                }
                            }
                            offset = reader.filePointer
                        }
                    }
                } catch (error: Throwable) {
                    appendLog(RuntimeLog("mining", "Mining diagnostic log read failed: ${safeLogMessage(error)}"))
                    delay(2_000)
                }
                delay(1_000)
            }
        }
    }

    private fun observeMiningDiagnosticLine(line: String) {
        val lower = line.lowercase()
        if (lower.contains("batch starting")) {
            update { it.copy(miningDiagnostic = null) }
            return
        }
        if (lower.contains("chain db has no atomic economics marker")) {
            val message = "Mining cannot start on this local chain DB because it lacks atomic economics metadata. Node remains online; reset/sync a fresh mainnet DB or run the explicit DB migration before mining."
            integratedMiningRequested = false
            val shouldAnnounce = _uiState.value.miningDiagnostic != message
            update {
                it.copy(
                    miningStatus = ServiceStatus.Error,
                    miningBlockReason = message,
                    miningDiagnostic = message,
                    mining = it.mining.copy(hashrateHps = 0)
                )
            }
            if (shouldAnnounce) {
                appendLog(RuntimeLog("mining", message))
            }
            return
        }
        if (lower.contains("batch error") || lower.contains("task join error")) {
            val message = line.substringAfter(":", line).trim().ifBlank { line }
            update { it.copy(miningDiagnostic = message) }
        }
    }

    private fun appendLog(log: RuntimeLog) {
        update { current ->
            current.copy(logs = (listOf(log) + current.logs).take(400))
        }
    }

    private fun rejectMiningRequest(reason: String) {
        integratedMiningRequested = false
        appendLog(RuntimeLog("mining", reason))
        update {
            it.copy(
                miningStatus = if (it.mining.running) it.miningStatus else ServiceStatus.Offline,
                miningBlockReason = reason
            )
        }
    }

    private fun safeLogMessage(error: Throwable): String {
        return PrivacyGuards.redactForLog(error.message ?: error::class.simpleName ?: "unknown error")
    }

    private fun walletUnlockFailureMessage(walletName: String, error: Throwable): String {
        val detail = safeLogMessage(error).ifBlank { "zoka did not return a reason" }
        return "Wallet unlock failed for '$walletName': $detail"
    }

    @Synchronized
    private fun storeSessionPassword(password: CharArray) {
        clearSessionPassword()
        sessionPassword = password.copyOf()
        update { it.copy(sessionUnlocked = true) }
    }

    @Synchronized
    private fun copySessionPassword(): CharArray? {
        return sessionPassword?.copyOf()
    }

    @Synchronized
    private fun clearSessionPassword() {
        sessionPassword?.fill('\u0000')
        sessionPassword = null
        update { it.copy(sessionUnlocked = false) }
    }

    private fun update(block: (ZSilentUiState) -> ZSilentUiState) {
        _uiState.value = block(_uiState.value)
    }

    private fun persistCurrentSetup(snapshot: ZSilentUiState = _uiState.value) {
        if (!snapshot.onboardingComplete) return
        setupStore.save(
            PersistedSetup(
                onboardingComplete = true,
                walletName = snapshot.activeWalletName.ifBlank { snapshot.walletName },
                walletAddress = snapshot.activeWalletAddress,
                managedWallets = snapshot.managedWallets,
                managedWalletAddresses = managedAddressesFor(snapshot),
                nodeEnabled = snapshot.onboardingNodeEnabled,
                miningEnabled = snapshot.onboardingMiningEnabled,
                balancesHidden = snapshot.balancesHidden,
                autoLockMinutes = snapshot.onboardingAutoLockMinutes,
                darkTheme = snapshot.darkTheme,
                torEnabled = snapshot.torEnabled,
                torOnly = snapshot.torOnly,
                torSocksHost = snapshot.torSocksHost.ifBlank { "127.0.0.1" },
                torSocksPort = snapshot.torSocksPort.coerceIn(1, 65535),
                managedTorEnabled = snapshot.managedTorEnabled,
                onionSeedEnabled = snapshot.onionSeedEnabled,
                managedTorSocksPort = snapshot.managedTorSocksPort.coerceIn(1, 65535)
            )
        )
    }

    private fun prepareOnboardingRecoveryPhrase() {
        val snapshot = _uiState.value
        if (snapshot.onboardingBusy) return
        if (snapshot.walletSetupMode == WalletSetupMode.ImportSeed) {
            val normalizedSeed = snapshot.onboardingSeedPhrase
                .trim()
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .joinToString(" ")
            update {
                it.copy(
                    onboardingStep = OnboardingStep.Recovery,
                    onboardingSeedPhrase = normalizedSeed,
                    onboardingRecoveryPhrase = normalizedSeed,
                    onboardingSeedCheckFirst = "",
                    onboardingSeedCheckSecond = "",
                    onboardingError = null
                )
            }
            return
        }

        update {
            it.copy(
                onboardingStep = OnboardingStep.Recovery,
                onboardingBusy = true,
                onboardingError = null,
                onboardingRecoveryPhrase = null,
                onboardingSeedCheckFirst = "",
                onboardingSeedCheckSecond = ""
            )
        }
        scope.launch {
            runCatching {
                walletService.generateRecoveryPhrase(snapshot.seedWordCount.words)
            }.onSuccess { result ->
                update {
                    it.copy(
                        onboardingBusy = false,
                        onboardingRecoveryPhrase = result.phrase,
                        onboardingSeedPhrase = result.phrase,
                        onboardingRecoveryConfirmed = false,
                        onboardingError = null
                    )
                }
            }.onFailure { error ->
                update {
                    it.copy(
                        onboardingBusy = false,
                        onboardingError = error.message ?: "Recovery phrase generation failed"
                    )
                }
            }
        }
    }

    private fun createOrRestoreOnboardingWallet() {
        val snapshot = _uiState.value
        if (snapshot.onboardingBusy) return
        val recoveryPhrase = (snapshot.onboardingRecoveryPhrase ?: snapshot.onboardingSeedPhrase).trim()
        val isRestore = snapshot.walletSetupMode == WalletSetupMode.ImportSeed
        update {
            it.copy(
                onboardingStep = OnboardingStep.Account,
                onboardingBusy = true,
                onboardingError = null
            )
        }
        scope.launch {
            val baseName = snapshot.onboardingWalletName.ifBlank { "main" }
            val nameCandidates = buildList {
                add(baseName)
                for (i in 2..9) add("$baseName-$i")
            }
            var lastError: Throwable? = null
            var resolved: WalletCreationResult? = null
            for (candidate in nameCandidates) {
                val attempt = runCatching {
                    if (isRestore) {
                        walletService.importFromSeed(
                            name = candidate,
                            seed = recoveryPhrase.toCharArray(),
                            password = snapshot.onboardingPassword.toCharArray()
                        )
                    } else {
                        // Create-new flow normally happens earlier via
                        // prepareOnboardingRecoveryPhrase, but if it somehow
                        // re-enters here we still try.
                        walletService.createWallet(
                            name = candidate,
                            password = snapshot.onboardingPassword.toCharArray(),
                            seedWords = snapshot.seedWordCount.words
                        )
                    }
                }
                if (attempt.isSuccess) {
                    resolved = attempt.getOrNull()
                    break
                }
                val err = attempt.exceptionOrNull()
                lastError = err
                val isNameConflict = err?.message?.contains("already exists", ignoreCase = true) == true
                if (!isNameConflict) break
                // Otherwise: keep iterating with the next suffixed candidate.
            }
            if (resolved != null) {
                update {
                    it.copy(
                        onboardingBusy = false,
                        onboardingStep = OnboardingStep.Account,
                        onboardingWalletName = resolved.name,
                        onboardingWalletAddress = resolved.address,
                        walletName = resolved.name,
                        managedWalletName = resolved.name,
                        managedWallets = listOf(resolved.name),
                        managedWalletAddresses = listOf(resolved.address).filter { address -> address.isZokaAddress() },
                        activeWalletName = resolved.name,
                        activeWalletAddress = resolved.address
                    )
                }
                refreshWallets()
            } else {
                val raw = lastError?.message ?: "Wallet setup failed."
                val pretty = humanizeOnboardingError(raw, isRestore)
                update {
                    it.copy(
                        onboardingBusy = false,
                        onboardingError = pretty
                    )
                }
            }
        }
    }

    /// Translate raw CLI errors into a one-line message that helps the user
    /// understand what to do next during onboarding.
    private fun humanizeOnboardingError(raw: String, isRestore: Boolean): String {
        val trimmed = raw.trim()
        return when {
            trimmed.contains("already exists", ignoreCase = true) -> {
                if (isRestore) {
                    "A wallet with this name already exists and ZSilent could not find a free variant. Open the Wallet panel, delete or rename the previous wallet, then try the restore again."
                } else {
                    "A wallet with this name already exists. Pick a different account name."
                }
            }
            trimmed.contains("invalid recovery phrase", ignoreCase = true) ||
                trimmed.contains("mnemonic", ignoreCase = true) ->
                "The recovery phrase is not valid. Check the spelling and order of every word."
            trimmed.contains("password", ignoreCase = true) ->
                "The password could not be accepted. Pick a different one and try again."
            else -> trimmed
        }
    }

    // Push a range of blocks from local to all remote nodes.
    // Runs automatically whenever local is ahead of the remote RPC —
    // covers both freshly mined blocks and blocks loaded from local DB on startup.
    private suspend fun pushBlockRangeToRemotes(fromHeight: Long, toHeight: Long) {
        if (blockPushInProgress) return
        blockPushInProgress = true
        try {
            val targets = listOf(rpc.baseUrl).filter { it.isNotBlank() }
            var pushed = 0
            for (h in fromHeight..toHeight) {
                val rawJson = try {
                    localRpc.getRawBlock(h)
                } catch (e: Exception) {
                    break // local node not ready yet
                }
                for (url in targets) {
                    try {
                        val remote = com.zsilent.core.rpc.ZokaRpcClient(url)
                        remote.submitBlock(rawJson)
                        remote.close()
                    } catch (_: Exception) {}
                }
                lastPushedRemoteHeight = h
                pushed++
            }
            if (pushed > 0) {
                appendLog(RuntimeLog("sync", "Auto-pushed $pushed block(s) ($fromHeight–${fromHeight + pushed - 1}) to remote nodes"))
            }
        } finally {
            blockPushInProgress = false
        }
    }

    // Called from the mining observer when a new block is found.
    private suspend fun pushMinedBlockToRemotes(height: Long) {
        pushBlockRangeToRemotes(height, height)
    }
}

private fun String.isZokaAddress(): Boolean = trim().startsWith("zka1")

private fun OnboardingStep.next(): OnboardingStep = when (this) {
    OnboardingStep.Welcome -> OnboardingStep.SelfCustody
    OnboardingStep.SelfCustody -> OnboardingStep.Wallet
    OnboardingStep.Wallet -> OnboardingStep.Recovery
    OnboardingStep.Recovery -> OnboardingStep.VerifySeed
    OnboardingStep.VerifySeed -> OnboardingStep.Security
    OnboardingStep.Security -> OnboardingStep.Account
    OnboardingStep.Account -> OnboardingStep.Privacy
    OnboardingStep.Privacy -> OnboardingStep.Tutorial
    OnboardingStep.Tutorial -> OnboardingStep.Dashboard
    OnboardingStep.Dashboard -> OnboardingStep.Dashboard
}

private fun OnboardingStep.previous(): OnboardingStep = when (this) {
    OnboardingStep.Welcome -> OnboardingStep.Welcome
    OnboardingStep.SelfCustody -> OnboardingStep.Welcome
    OnboardingStep.Wallet -> OnboardingStep.SelfCustody
    OnboardingStep.Recovery -> OnboardingStep.Wallet
    OnboardingStep.VerifySeed -> OnboardingStep.Recovery
    OnboardingStep.Security -> OnboardingStep.VerifySeed
    OnboardingStep.Account -> OnboardingStep.Security
    OnboardingStep.Privacy -> OnboardingStep.Account
    OnboardingStep.Tutorial -> OnboardingStep.Privacy
    OnboardingStep.Dashboard -> OnboardingStep.Tutorial
}
