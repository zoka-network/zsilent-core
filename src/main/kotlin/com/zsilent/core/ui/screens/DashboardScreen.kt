package com.zsilent.core.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyIconButton
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.ThinDivider
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.onboarding.NoirKeyValue
import com.zsilent.core.ui.onboarding.NoirMetricCard
import com.zsilent.core.ui.onboarding.NoirScreenShell
import com.zsilent.core.ui.theme.ZTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

private const val DASHBOARD_SAMPLE_INTERVAL_MILLIS = 5_000L
private const val DASHBOARD_SAMPLE_LIMIT = 60
private const val DASHBOARD_LIVE_WINDOW_MILLIS = 15_000L

internal data class DashboardTelemetrySample(
    val timestampMillis: Long,
    val localHeight: Long,
    val targetHeight: Long,
    val localHeightKnown: Boolean,
    val targetHeightKnown: Boolean,
    val lag: Long,
    val peers: Long,
    val gossipResponses: Long,
    val syncFresh: Boolean,
    val peersFresh: Boolean
)

internal fun appendDashboardTelemetrySample(
    history: List<DashboardTelemetrySample>,
    sample: DashboardTelemetrySample,
    limit: Int = DASHBOARD_SAMPLE_LIMIT
): List<DashboardTelemetrySample> {
    if (limit <= 0) return emptyList()
    return (history + sample).takeLast(limit)
}

internal fun dashboardCounterRatePerMinute(
    samples: List<DashboardTelemetrySample>,
    counter: (DashboardTelemetrySample) -> Long
): List<Double> = samples.zipWithNext().map { (previous, current) ->
    val elapsedMillis = current.timestampMillis - previous.timestampMillis
    val delta = counter(current) - counter(previous)
    if (delta < 0L || elapsedMillis <= 0L) 0.0 else delta.toDouble() * 60_000.0 / elapsedMillis.toDouble()
}

internal fun dashboardMetricColumns(widthDp: Float): Int = when {
    widthDp >= 900f -> 4
    widthDp >= 520f -> 2
    else -> 1
}

internal enum class DashboardHealth {
    Healthy,
    Degraded,
    NeedsAttention,
    Starting,
    Idle,
    Monitoring
}

internal data class DashboardAlert(
    val title: String,
    val detail: String,
    val tone: ZTone
)

internal data class DashboardRuntimeSummary(
    val health: DashboardHealth,
    val label: String,
    val detail: String,
    val tone: ZTone,
    val alerts: List<DashboardAlert>
)

internal data class DashboardStatusIndicator(
    val label: String,
    val tone: ZTone
)

internal data class DashboardBalancePresentation(
    val account: String,
    val address: String,
    val snapshotAvailable: Boolean,
    val spendableAtoms: Long,
    val lockedAtoms: Long,
    val totalAtoms: Long,
    val spendableText: String?,
    val lockedText: String?,
    val totalText: String?,
    val transactions: Int
)

internal data class DashboardActionPolicy(
    val nodeEnabled: Boolean,
    val nodeLabel: String,
    val nodeReason: String?,
    val miningEnabled: Boolean,
    val miningLabel: String,
    val miningReason: String?,
    val torEnabled: Boolean,
    val torLabel: String,
    val torReason: String?
)

internal fun dashboardActionPolicy(
    ui: ZSilentUiState,
    rpcEndpointLocal: Boolean = true
): DashboardActionPolicy {
    val nodeBusy = ui.nodeStatus == ServiceStatus.Starting || ui.nodeStatus == ServiceStatus.Stopping
    val nodeRunning = ui.nodeStatus == ServiceStatus.Online
    val miningBusy = ui.miningStatus == ServiceStatus.Starting || ui.miningStatus == ServiceStatus.Stopping
    val soloRunning = ui.mining.running
    val externalMinerOwnsChain = soloRunning && !nodeRunning
    val minerOwnershipUnclear = !nodeRunning && !soloRunning && ui.miningStatus in setOf(
        ServiceStatus.Online,
        ServiceStatus.Starting,
        ServiceStatus.Stopping
    )
    val minerMayOwnChain = externalMinerOwnsChain || minerOwnershipUnclear
    val miningStateUnclear = ui.miningStatus == ServiceStatus.Online && !soloRunning
    val canStartSolo = rpcEndpointLocal && ui.zokaReady && nodeRunning && ui.localRpcOnline && ui.miningSyncReady
    val nodeReason = when {
        nodeRunning || nodeBusy -> null
        externalMinerOwnsChain -> "stop the external miner before starting the managed node"
        minerOwnershipUnclear -> "wait for the miner state to settle before starting the managed node"
        !ui.zokaReady -> "Zoka binaries unavailable"
        !ui.sessionUnlocked -> "unlock the wallet session to start the node"
        else -> null
    }
    val miningReason = when {
        !rpcEndpointLocal -> "configured RPC is remote · mining controls disabled"
        soloRunning -> null
        miningBusy -> "miner is changing state"
        miningStateUnclear -> "miner status is reconciling"
        !ui.zokaReady -> "Zoka binaries unavailable"
        !nodeRunning -> "node offline · miner unavailable"
        !ui.localRpcOnline -> "${if (rpcEndpointLocal) "local" else "configured"} RPC offline · miner unavailable"
        !ui.miningSyncReady -> ui.miningBlockReason?.takeIf { it.isNotBlank() } ?: "miner waiting for sync"
        else -> null
    }
    val torBusy = ui.torStatus == ServiceStatus.Starting || ui.torStatus == ServiceStatus.Stopping
    val nodeRouteLocked = ui.nodeStatus in setOf(ServiceStatus.Online, ServiceStatus.Starting, ServiceStatus.Stopping)
    val torCanChange = !torBusy && !nodeRouteLocked
    val torActive = ui.torEnabled && ui.torStatus == ServiceStatus.Online
    val torReason = when {
        torBusy -> "Tor is changing state"
        nodeRouteLocked -> "stop the managed node before changing its route"
        else -> null
    }
    return DashboardActionPolicy(
        nodeEnabled = !nodeBusy && (nodeRunning || (ui.zokaReady && ui.sessionUnlocked && !minerMayOwnChain)),
        nodeLabel = when (ui.nodeStatus) {
            ServiceStatus.Starting -> "Starting node"
            ServiceStatus.Stopping -> "Stopping node"
            ServiceStatus.Online -> "Stop node"
            ServiceStatus.Offline, ServiceStatus.Error -> "Start node"
        },
        nodeReason = nodeReason,
        miningEnabled = rpcEndpointLocal && !miningBusy && !miningStateUnclear && (soloRunning || canStartSolo),
        miningLabel = when {
            ui.miningStatus == ServiceStatus.Starting -> "Starting miner"
            ui.miningStatus == ServiceStatus.Stopping -> "Stopping miner"
            miningStateUnclear -> "Updating miner"
            soloRunning -> "Stop miner"
            else -> "Start miner"
        },
        miningReason = miningReason,
        torEnabled = torCanChange,
        torLabel = when (ui.torStatus) {
            ServiceStatus.Starting -> "Starting Tor"
            ServiceStatus.Stopping -> "Stopping Tor"
            else -> when {
                nodeRouteLocked && torActive -> "Tor route active"
                nodeRouteLocked && ui.torEnabled -> "Tor requested · direct fallback"
                nodeRouteLocked -> "Direct route active"
                ui.torEnabled -> "Use direct on start"
                else -> "Use Tor on start"
            }
        },
        torReason = torReason
    )
}

internal fun dashboardBalancePresentation(ui: ZSilentUiState): DashboardBalancePresentation {
    val selectedName = ui.activeWalletName.trim()
    val selectedAddress = ui.activeWalletAddress.trim()
    val explicitSelection = selectedName.isNotBlank() || selectedAddress.isNotBlank()
    val wallet = when {
        selectedAddress.isNotBlank() -> ui.wallets.firstOrNull { it.address == selectedAddress }
        selectedName.isNotBlank() -> ui.wallets.firstOrNull { it.name == selectedName }
        ui.wallets.size == 1 -> ui.wallets.single()
        else -> null
    }
    val balance = ui.walletBalance?.takeIf { snapshot ->
        when {
            selectedAddress.isNotBlank() -> snapshot.address == selectedAddress
            selectedName.isNotBlank() -> snapshot.wallet == selectedName
            wallet != null -> snapshot.address == wallet.address || snapshot.wallet == wallet.name
            else -> false
        }
    }
    val snapshotAvailable = balance != null || wallet != null
    val spendableAtoms = balance?.balanceAtoms ?: wallet?.balanceAtoms ?: 0L
    val lockedAtoms = balance?.lockedBalanceAtoms ?: wallet?.lockedBalanceAtoms ?: 0L
    val reportedTotalAtoms = balance?.totalBalanceAtoms ?: wallet?.totalBalanceAtoms ?: 0L
    return DashboardBalancePresentation(
        account = wallet?.name ?: balance?.wallet ?: selectedName.ifBlank { if (explicitSelection) "selected wallet" else "not selected" },
        address = wallet?.address ?: balance?.address ?: selectedAddress,
        snapshotAvailable = snapshotAvailable,
        spendableAtoms = spendableAtoms,
        lockedAtoms = lockedAtoms,
        totalAtoms = max(reportedTotalAtoms, spendableAtoms + lockedAtoms),
        spendableText = balance?.balance ?: wallet?.balance,
        lockedText = balance?.lockedBalance ?: wallet?.lockedBalance,
        totalText = balance?.totalBalance ?: wallet?.totalBalance,
        transactions = if (dashboardHistoryMatchesSelection(ui)) {
            max(wallet?.txCount ?: 0, ui.walletHistory.count)
        } else {
            wallet?.txCount ?: 0
        }
    )
}

internal fun dashboardRuntimeSummary(
    ui: ZSilentUiState,
    nowMillis: Long = System.currentTimeMillis(),
    rpcEndpointLocal: Boolean = true
): DashboardRuntimeSummary {
    val syncFresh = dashboardTelemetryFresh(
        ui.localRpcOnline,
        ui.nodeSyncStatusObservedAtMillis,
        ui.nodeSyncStatusConsecutiveFailures,
        nowMillis
    )
    val peersFresh = dashboardTelemetryFresh(
        ui.localRpcOnline,
        ui.peerTelemetryObservedAtMillis,
        ui.peerTelemetryConsecutiveFailures,
        nowMillis
    )
    val peerCount = dashboardPeerCount(ui)
    val syncStatus = ui.nodeSyncStatus.status.trim().lowercase()
    val syncStateError = syncFresh && syncStatus in setOf("error", "failed")
    val liveSyncError = ui.nodeSyncStatus.lastSyncError?.trim()?.takeIf {
        syncFresh && it.isNotBlank()
    }
    val alerts = buildList {
        if (!ui.zokaReady) {
            add(DashboardAlert("Zoka binaries are unavailable", "Node, wallet and mining commands cannot run until the packaged binaries are present.", ZTone.Err))
        }
        if (ui.nodeStatus == ServiceStatus.Error) {
            add(DashboardAlert("Node needs attention", "The managed node entered an error state. Open Node and Logs for the current diagnostic.", ZTone.Err))
        }
        if (ui.miningStatus == ServiceStatus.Error || !ui.miningDiagnostic.isNullOrBlank()) {
            add(DashboardAlert("Mining needs attention", ui.miningDiagnostic?.takeIf { it.isNotBlank() } ?: "The miner entered an error state.", ZTone.Err))
        }
        if (ui.torEnabled && (ui.torStatus == ServiceStatus.Error || !ui.torError.isNullOrBlank())) {
            add(DashboardAlert("Tor needs attention", ui.torError?.takeIf { it.isNotBlank() } ?: "The managed Tor process entered an error state.", ZTone.Err))
        } else if (ui.nodeStatus == ServiceStatus.Online && ui.torEnabled &&
            (ui.managedTorEnabled || ui.torOnly) && ui.torStatus == ServiceStatus.Offline
        ) {
            add(
                DashboardAlert(
                    if (ui.torOnly) "Tor-only route is unavailable" else "Managed Tor is offline",
                    if (ui.torOnly) {
                        "The selected Tor-only policy cannot dial direct peers until Tor returns."
                    } else {
                        "The node can use direct fallback, but the requested managed Tor route is not currently active."
                    },
                    ZTone.Warn
                )
            )
        }
        if (syncStateError || liveSyncError != null) {
            add(DashboardAlert("Chain synchronization reported an error", liveSyncError ?: "The live /sync/status sample is in an error state.", ZTone.Err))
        }
        if (ui.nodeStatus == ServiceStatus.Online && !ui.localRpcOnline) {
            add(
                DashboardAlert(
                    "${if (rpcEndpointLocal) "Local" else "Configured"} RPC is unavailable",
                    "The managed node process is online, but the ${if (rpcEndpointLocal) "local" else "configured"} API endpoint is not responding yet.",
                    ZTone.Warn
                )
            )
        }
        if (ui.nodeStatus == ServiceStatus.Online && ui.localRpcOnline && peersFresh && peerCount <= 0L) {
            add(
                DashboardAlert(
                    if (rpcEndpointLocal) "Local node has no peers" else "Configured endpoint has no peers",
                    "The ${if (rpcEndpointLocal) "local" else "configured"} RPC responds, but its current P2P mesh is empty. New network updates wait for a peer connection.",
                    ZTone.Warn
                )
            )
        }
        if (syncFresh && ui.nodeSyncStatus.minPeers > 0L &&
            ui.nodeSyncStatus.connectedPeers < ui.nodeSyncStatus.minPeers && !(peersFresh && peerCount <= 0L)
        ) {
            add(
                DashboardAlert(
                    "Peer threshold is not met",
                    "The sync service reports ${ui.nodeSyncStatus.connectedPeers}/${ui.nodeSyncStatus.minPeers} peer(s) required for its ready state.",
                    ZTone.Warn
                )
            )
        }
        if (ui.localRpcOnline && ui.peerTelemetryConsecutiveFailures >= 3) {
            add(DashboardAlert("Peer telemetry is stale", "/peers failed ${ui.peerTelemetryConsecutiveFailures} consecutive poll(s). Route counts below are last known.", ZTone.Warn))
        }
        if (ui.localRpcOnline && ui.nodeSyncStatusConsecutiveFailures >= 3) {
            add(DashboardAlert("Sync telemetry is stale", "/sync/status failed ${ui.nodeSyncStatusConsecutiveFailures} consecutive poll(s). Chain details below are last known.", ZTone.Warn))
        }
        if (!ui.walletLocked && !ui.balancesHidden && !dashboardBalancePresentation(ui).snapshotAvailable && !ui.walletError.isNullOrBlank()) {
            add(DashboardAlert("Wallet data is unavailable", ui.walletError, ZTone.Warn))
        }
    }
    val changing = ui.nodeStatus == ServiceStatus.Starting || ui.nodeStatus == ServiceStatus.Stopping ||
        ui.torStatus == ServiceStatus.Starting || ui.torStatus == ServiceStatus.Stopping ||
        ui.miningStatus == ServiceStatus.Starting || ui.miningStatus == ServiceStatus.Stopping
    val peerRequirementMet = ui.nodeSyncStatus.minPeers <= 0L ||
        ui.nodeSyncStatus.connectedPeers >= ui.nodeSyncStatus.minPeers
    val syncStatusReady = syncStatus in setOf("caught_up", "synced") || ui.nodeSyncStatus.readyToMine
    val syncReady = syncFresh && !syncStateError && peerRequirementMet && syncStatusReady &&
        (ui.nodeSyncStatus.readyToMine || ui.nodeSyncStatus.lag <= 2L)
    val health = when {
        alerts.any { it.tone == ZTone.Err } -> DashboardHealth.NeedsAttention
        alerts.any { it.tone == ZTone.Warn } -> DashboardHealth.Degraded
        changing -> DashboardHealth.Starting
        ui.nodeStatus == ServiceStatus.Offline -> DashboardHealth.Idle
        ui.nodeStatus == ServiceStatus.Online && ui.localRpcOnline && syncReady && peersFresh && peerCount > 0L -> DashboardHealth.Healthy
        ui.nodeStatus == ServiceStatus.Online -> DashboardHealth.Monitoring
        else -> DashboardHealth.Idle
    }
    val label = when (health) {
        DashboardHealth.Healthy -> "HEALTHY"
        DashboardHealth.Degraded -> "DEGRADED"
        DashboardHealth.NeedsAttention -> "NEEDS ATTENTION"
        DashboardHealth.Starting -> "CHANGING"
        DashboardHealth.Idle -> "IDLE"
        DashboardHealth.Monitoring -> "MONITORING"
    }
    val tone = when (health) {
        DashboardHealth.Healthy -> ZTone.On
        DashboardHealth.Degraded, DashboardHealth.Starting -> ZTone.Warn
        DashboardHealth.NeedsAttention -> ZTone.Err
        DashboardHealth.Idle, DashboardHealth.Monitoring -> ZTone.Off
    }
    val retainingValidSample = ui.nodeSyncStatusConsecutiveFailures in 1..2 || ui.peerTelemetryConsecutiveFailures in 1..2
    val endpointLabel = if (rpcEndpointLocal) "Local RPC" else "The configured RPC"
    val detail = when (health) {
        DashboardHealth.Healthy -> if (retainingValidSample) {
            "$endpointLabel remains healthy using the latest valid sample while a telemetry retry is in progress; charts pause until the next successful poll."
        } else {
            "$endpointLabel, chain sync and its current peer mesh are reporting healthy live samples."
        }
        DashboardHealth.Degraded -> "Core services are available, but one or more current signals need review."
        DashboardHealth.NeedsAttention -> "A current service error is active. The alert below identifies the affected component."
        DashboardHealth.Starting -> "Services are changing state. Live telemetry will settle when startup or shutdown completes."
        DashboardHealth.Idle -> "The managed node is stopped. Tor and the unlocked wallet may remain active independently."
        DashboardHealth.Monitoring -> "The node is online; ZSilent is waiting for enough fresh sync and peer telemetry to confirm full health."
    }
    return DashboardRuntimeSummary(health, label, detail, tone, alerts)
}

internal fun dashboardSyncIndicator(ui: ZSilentUiState, nowMillis: Long): DashboardStatusIndicator {
    val retainedFresh = dashboardTelemetryFresh(
        ui.localRpcOnline,
        ui.nodeSyncStatusObservedAtMillis,
        ui.nodeSyncStatusConsecutiveFailures,
        nowMillis
    )
    val sampleFresh = dashboardTelemetrySampleFresh(
        ui.localRpcOnline,
        ui.nodeSyncStatusObservedAtMillis,
        ui.nodeSyncStatusConsecutiveFailures,
        nowMillis
    )
    val status = ui.nodeSyncStatus.status.trim().lowercase()
    val liveError = sampleFresh && (
        status in setOf("error", "failed") || !ui.nodeSyncStatus.lastSyncError.isNullOrBlank()
    )
    val ready = sampleFresh && !liveError && (
        ui.nodeSyncStatus.readyToMine ||
            (status in setOf("caught_up", "synced") && ui.nodeSyncStatus.lag <= 2L)
    )
    return when {
        liveError -> DashboardStatusIndicator("sync error", ZTone.Err)
        ready && ui.nodeSyncStatus.lag <= 0L -> DashboardStatusIndicator("chain at tip", ZTone.On)
        ready -> DashboardStatusIndicator("chain ready · ${ui.nodeSyncStatus.lag} behind", ZTone.On)
        sampleFresh -> DashboardStatusIndicator(
            "sync ${status.ifBlank { "observed" }}${if (ui.nodeSyncStatus.lag > 0L) " · ${ui.nodeSyncStatus.lag} behind" else ""}",
            ZTone.Warn
        )
        retainedFresh -> DashboardStatusIndicator(
            "sync last valid · retry ${ui.nodeSyncStatusConsecutiveFailures}/2",
            ZTone.Warn
        )
        else -> DashboardStatusIndicator("sync unavailable", ZTone.Off)
    }
}

@Composable
fun DashboardScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    val reportedWork = if (ui.mining.running) ui.mining.hashrateHps.coerceAtLeast(0L) else 0L
    val rpcEndpointLocal = remember(appState) { appState.localRpc.isLocalEndpoint() }
    var clockMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var telemetry by remember { mutableStateOf(emptyList<DashboardTelemetrySample>()) }
    val latestUi by rememberUpdatedState(ui)

    LaunchedEffect(Unit) {
        while (true) {
            clockMillis = System.currentTimeMillis()
            delay(5_000L)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val sampledAt = System.currentTimeMillis()
            telemetry = appendDashboardTelemetrySample(
                telemetry,
                dashboardTelemetrySample(latestUi, sampledAt)
            )
            delay(DASHBOARD_SAMPLE_INTERVAL_MILLIS)
        }
    }

    val runtime = dashboardRuntimeSummary(ui, clockMillis, rpcEndpointLocal)
    NoirScreenShell(
        eyebrow = "Dashboard",
        title = "Operations overview",
        subtitle = "Current health, real session telemetry and private wallet activity in one place."
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = 1280.dp).align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                DashboardRuntimePanel(ui, runtime, appState, clockMillis, rpcEndpointLocal)

                if (runtime.alerts.isNotEmpty()) {
                    Panel(title = "Active alerts", meta = "${runtime.alerts.size} current signal(s)") {
                        runtime.alerts.forEach { DashboardAlertCard(it) }
                        HintText("Only current service state affects this section; retained log history does not make the dashboard red.")
                    }
                }

                DashboardMetricStrip(ui, telemetry, clockMillis, rpcEndpointLocal)
                DashboardPrimaryGrid(ui, telemetry, clockMillis, rpcEndpointLocal)
                DashboardSecondaryGrid(ui, reportedWork)
                DashboardRecentActivity(ui, clockMillis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardRuntimePanel(
    ui: ZSilentUiState,
    runtime: DashboardRuntimeSummary,
    appState: ZSilentAppState,
    nowMillis: Long,
    rpcEndpointLocal: Boolean
) {
    val peersFresh = dashboardTelemetryFresh(ui.localRpcOnline, ui.peerTelemetryObservedAtMillis, ui.peerTelemetryConsecutiveFailures, nowMillis)
    val peersSampleFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.peerTelemetryObservedAtMillis, ui.peerTelemetryConsecutiveFailures, nowMillis)
    val syncIndicator = dashboardSyncIndicator(ui, nowMillis)
    val peerCount = dashboardPeerCount(ui)
    val stateColor = when (runtime.tone) {
        ZTone.On -> ZTheme.semantics.success
        ZTone.Warn -> ZTheme.semantics.warning
        ZTone.Err -> ZTheme.semantics.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Panel(title = "Runtime overview", meta = "current state") {
        Text(runtime.label, style = ZTheme.text.monoDisplay, color = stateColor)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill("node ${ui.nodeStatus.name.lowercase()}", dashboardServiceTone(ui.nodeStatus))
            Pill(
                "${if (rpcEndpointLocal) "local" else "configured"} RPC ${if (ui.localRpcOnline) "online" else "offline"}",
                if (ui.localRpcOnline) ZTone.On else ZTone.Off
            )
            Pill(syncIndicator.label, syncIndicator.tone)
            Pill(
                when {
                    peersSampleFresh -> "$peerCount ${if (rpcEndpointLocal) "local" else "RPC"} peer(s)"
                    peersFresh -> "peers last valid · retry ${ui.peerTelemetryConsecutiveFailures}/2"
                    else -> "peers unavailable"
                },
                when {
                    peersSampleFresh && peerCount > 0L -> ZTone.On
                    peersFresh -> ZTone.Warn
                    else -> ZTone.Off
                }
            )
            Pill(
                if (ui.torEnabled) "tor ${ui.torStatus.name.lowercase()}" else "tor disabled",
                if (ui.torEnabled) dashboardServiceTone(ui.torStatus) else ZTone.Off
            )
            Pill(
                if (ui.mining.running) "solo mining running" else "mining off",
                if (ui.mining.running) ZTone.On else ZTone.Off
            )
        }
        HintText(runtime.detail)
        ThinDivider()
        DashboardActions(ui, appState, rpcEndpointLocal)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardActions(ui: ZSilentUiState, appState: ZSilentAppState, rpcEndpointLocal: Boolean) {
    val policy = dashboardActionPolicy(ui, rpcEndpointLocal)
    val nodeOnline = ui.nodeStatus == ServiceStatus.Online
    val mining = ui.mining.running
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZButton(
            onClick = { if (nodeOnline) appState.stopNode() else appState.startNode() },
            variant = ZButtonVariant.Accent,
            enabled = policy.nodeEnabled
        ) {
            Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
            ZButtonText(policy.nodeLabel)
        }
        ZButton(
            onClick = {
                if (mining) appState.stopMining()
                else appState.startMining(ui.mining.availableCpuThreads.coerceAtLeast(1))
            },
            variant = ZButtonVariant.Outline,
            enabled = policy.miningEnabled
        ) {
            Icon(Icons.Outlined.Bolt, null, modifier = Modifier.size(14.dp))
            ZButtonText(policy.miningLabel)
        }
        ZButton(
            onClick = { appState.setTorOnionMode(!ui.torEnabled) },
            variant = ZButtonVariant.Outline,
            enabled = policy.torEnabled
        ) {
            Icon(Icons.Outlined.Security, null, modifier = Modifier.size(14.dp))
            ZButtonText(policy.torLabel)
        }
        if (policy.nodeReason != null) {
            Pill(policy.nodeReason, ZTone.Warn)
        }
        if (policy.miningReason != null) {
            Pill(policy.miningReason, ZTone.Warn)
        }
        if (policy.torReason != null) {
            Pill(policy.torReason, ZTone.Info)
        }
    }
}

private data class DashboardMetricSpec(
    val label: String,
    val value: String,
    val detail: String,
    val samples: List<Double>,
    val accent: Color,
    val valueColor: Color? = null
)

@Composable
private fun DashboardMetricStrip(
    ui: ZSilentUiState,
    telemetry: List<DashboardTelemetrySample>,
    nowMillis: Long,
    rpcEndpointLocal: Boolean
) {
    val syncFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.nodeSyncStatusObservedAtMillis, ui.nodeSyncStatusConsecutiveFailures, nowMillis)
    val peersFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.peerTelemetryObservedAtMillis, ui.peerTelemetryConsecutiveFailures, nowMillis)
    val peerCount = dashboardPeerCount(ui)
    // Keep only the current contiguous live run. If telemetry goes stale and
    // later recovers, a new line begins instead of visually bridging the gap.
    // La serie retenida no se tira por un hueco.
    //
    // `takeLastWhile` cortaba en la primera muestra no fresca contando desde
    // el final, así que una sola lectura tardía dejaba la lista vacía y el
    // gráfico en blanco -- aunque hubiera cuarenta muestras buenas detrás. El
    // contador de muestras bajaba (44, 43, ...) y la línea desaparecía.
    //
    // Ahora se descartan solo las muestras no frescas y se conserva el resto,
    // que es lo que un gráfico de los últimos minutos debe mostrar: los datos
    // que sí se pudieron leer.
    val syncSamples = if (syncFresh) telemetry.filter { it.syncFresh } else emptyList()
    val peerSamples = if (peersFresh) telemetry.takeLastWhile { it.peersFresh } else emptyList()
    val gossipRates = dashboardCounterRatePerMinute(syncSamples) { it.gossipResponses }
    val currentGossipRate = if (syncFresh) gossipRates.lastOrNull() else null
    val height = dashboardLocalHeight(ui, nowMillis)
    val heightKnown = dashboardLocalHeightKnown(ui, nowMillis)
    val targetKnown = dashboardTargetHeightKnown(ui, nowMillis)
    val lag = dashboardLag(ui, nowMillis)
    val specs = listOf(
        DashboardMetricSpec(
            if (rpcEndpointLocal) "Chain height" else "RPC chain height",
            if (heightKnown) height.toString() else "—",
            if (syncFresh) "live /sync/status" else if (heightKnown) "last known" else "not reported",
            syncSamples.filter { it.localHeightKnown }.map { it.localHeight.toDouble() },
            ZTheme.metrics.at(0)
        ),
        DashboardMetricSpec(
            "Sync lag",
            if (targetKnown) "$lag blk" else "—",
            if (syncFresh && targetKnown && lag <= 2L) "near observed tip" else if (syncFresh && targetKnown) "catching up" else "telemetry unavailable",
            syncSamples.filter { it.targetHeightKnown }.map { it.lag.toDouble() },
            if (syncFresh && targetKnown && lag <= 2L) ZTheme.semantics.success else ZTheme.metrics.at(1)
        ),
        DashboardMetricSpec(
            if (rpcEndpointLocal) "Local peers" else "RPC peers",
            if (peersFresh || ui.peerTelemetryObservedAtMillis > 0L) peerCount.toString() else "—",
            if (peersFresh) "live adjacent mesh" else if (ui.peerTelemetryObservedAtMillis > 0L) "last known" else "not reported",
            peerSamples.map { it.peers.toDouble() },
            ZTheme.metrics.at(2)
        ),
        DashboardMetricSpec(
            "Block responses",
            currentGossipRate?.let { "${dashboardRate(it)}/m" } ?: "—",
            if (syncFresh) "rate from session counter" else "waiting for live counter",
            gossipRates,
            ZTheme.metrics.at(3)
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 520.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OBSERVED TELEMETRY", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("DASHBOARD WINDOW · 5S SAMPLES", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("OBSERVED TELEMETRY", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("DASHBOARD WINDOW · 5S SAMPLES", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = dashboardMetricColumns(maxWidth.value)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                specs.chunked(columns).forEach { rowSpecs ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowSpecs.forEach { spec ->
                            NoirMetricCard(
                                label = spec.label,
                                value = spec.value,
                                detail = spec.detail,
                                samples = spec.samples,
                                accent = spec.accent,
                                valueColor = spec.valueColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardPrimaryGrid(
    ui: ZSilentUiState,
    telemetry: List<DashboardTelemetrySample>,
    nowMillis: Long,
    rpcEndpointLocal: Boolean
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val ancho = maxWidth
        if (ancho >= 860.dp) {
            // Lo que va a medir cada panel, derivado de los mismos pesos con
            // que se dibujan. Se calcula aca porque este es el unico punto que
            // conoce el ancho: los hijos ya no pueden medirse solos sin
            // romper la fila de igual alto.
            val usable = ancho - ZTheme.spacing.gap
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                DashboardChainPanel(
                    ui, telemetry, nowMillis, rpcEndpointLocal,
                    usable * (1.45f / 2.45f),
                    Modifier.weight(1.45f).fillMaxHeight()
                )
                DashboardPeerPanel(
                    ui, nowMillis, rpcEndpointLocal,
                    usable * (1f / 2.45f),
                    Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
                DashboardChainPanel(ui, telemetry, nowMillis, rpcEndpointLocal, ancho)
                DashboardPeerPanel(ui, nowMillis, rpcEndpointLocal, ancho)
            }
        }
    }
}

@Composable
private fun DashboardChainPanel(
    ui: ZSilentUiState,
    telemetry: List<DashboardTelemetrySample>,
    nowMillis: Long,
    rpcEndpointLocal: Boolean,
    availableWidth: Dp,
    modifier: Modifier = Modifier
) {
    val syncFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.nodeSyncStatusObservedAtMillis, ui.nodeSyncStatusConsecutiveFailures, nowMillis)
    val samples = if (syncFresh) telemetry.takeLastWhile { it.syncFresh } else emptyList()
    val localSeries = samples.takeLastWhile { it.localHeightKnown }.map { it.localHeight.toDouble() }
    val targetSeries = samples.takeLastWhile { it.targetHeightKnown }.map { it.targetHeight.toDouble() }
    val localHeight = dashboardLocalHeight(ui, nowMillis)
    val targetHeight = dashboardTargetHeight(ui, nowMillis)
    val localKnown = dashboardLocalHeightKnown(ui, nowMillis)
    val targetKnown = dashboardTargetHeightKnown(ui, nowMillis)
    val lag = dashboardLag(ui, nowMillis)
    Panel(
        title = "Chain movement",
        meta = if (syncFresh) "live · ${samples.size} sample(s)" else "last known",
        modifier = modifier
    ) {
        DashboardInlineMetricGrid(
            listOf(
                (if (rpcEndpointLocal) "Local" else "Configured RPC") to if (localKnown) localHeight.toString() else "—",
                "Observed target" to if (targetKnown) targetHeight.toString() else "—",
                "Lag" to if (targetKnown) "$lag blocks" else "—"
            ),
            availableWidth
        )
        Box(Modifier.fillMaxWidth().height(176.dp), contentAlignment = Alignment.Center) {
            DashboardLineChart(
                series = listOf(
                    DashboardChartSeries(localSeries, ZTheme.metrics.at(0), fill = true),
                    DashboardChartSeries(targetSeries, ZTheme.metrics.at(2))
                ),
                modifier = Modifier.fillMaxWidth().height(176.dp)
            )
            if (max(localSeries.size, targetSeries.size) < 2) {
                Text(
                    if (syncFresh) "Collecting live chain samples…" else "Live sync telemetry is unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (localSeries.isNotEmpty()) {
                DashboardLegend(if (rpcEndpointLocal) "local height" else "configured RPC height", ZTheme.metrics.at(0))
            }
            if (targetSeries.isNotEmpty()) {
                DashboardLegend("observed target", ZTheme.metrics.at(2))
            }
        }
        HintText("Five-second samples are retained only while this Dashboard is open. A stale sync response does not extend the line.")
    }
}

@Composable
private fun DashboardPeerPanel(
    ui: ZSilentUiState,
    nowMillis: Long,
    rpcEndpointLocal: Boolean,
    availableWidth: Dp,
    modifier: Modifier = Modifier
) {
    val splitKnown = ui.peerTelemetryObservedAtMillis > 0L
    val splitFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.peerTelemetryObservedAtMillis, ui.peerTelemetryConsecutiveFailures, nowMillis)
    val total = dashboardPeerCount(ui)
    val tor = ui.peerTorCount.coerceIn(0L, total)
    val direct = ui.peerDirectCount.coerceIn(0L, (total - tor).coerceAtLeast(0L))
    val other = (total - tor - direct).coerceAtLeast(0L)
    Panel(
        title = "Peer routes",
        meta = when {
            splitFresh -> "live /peers"
            splitKnown -> "last known"
            else -> "unavailable"
        },
        modifier = modifier
    ) {
        run {
            if (availableWidth >= 430.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashboardPeerRing(total, tor, direct, other, splitKnown, Modifier.size(142.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardRouteRow("Tor", if (splitKnown) tor.toString() else "—", ZTheme.metrics.at(0))
                        DashboardRouteRow("Direct", if (splitKnown) direct.toString() else "—", ZTheme.metrics.at(2))
                        DashboardRouteRow("Other", if (splitKnown) other.toString() else "—", ZTheme.metrics.at(4))
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DashboardPeerRing(total, tor, direct, other, splitKnown, Modifier.size(132.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardRouteRow("Tor", if (splitKnown) tor.toString() else "—", ZTheme.metrics.at(0))
                        DashboardRouteRow("Direct", if (splitKnown) direct.toString() else "—", ZTheme.metrics.at(2))
                        DashboardRouteRow("Other", if (splitKnown) other.toString() else "—", ZTheme.metrics.at(4))
                    }
                }
            }
        }
        NoirKeyValue("Adjacent peers", if (splitKnown) total.toString() else "not reported", mono = true)
        NoirKeyValue("Minimum to mine", ui.nodeSyncStatus.minPeers.takeIf { it > 0L }?.toString() ?: "not reported", mono = true)
        NoirKeyValue(
            "Observed",
            dashboardObservedLabel(ui.peerTelemetryObservedAtMillis, nowMillis)
        )
        HintText(
            if (rpcEndpointLocal) {
                "This is the local node's adjacent mesh, not a global node count. Onion identity alone does not prove external reachability."
            } else {
                "This is the configured RPC endpoint's adjacent mesh, not this PC's peer count or a global node count."
            }
        )
    }
}

@Composable
private fun DashboardSecondaryGrid(
    ui: ZSilentUiState,
    reportedWork: Long
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val ancho = maxWidth
        if (ancho >= 860.dp) {
            val usable = ancho - ZTheme.spacing.gap
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                DashboardMiningPanel(
                    ui, reportedWork,
                    usable * (1.08f / 2.08f),
                    Modifier.weight(1.08f).fillMaxHeight()
                )
                DashboardBalancePanel(ui, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
                DashboardMiningPanel(ui, reportedWork, ancho)
                DashboardBalancePanel(ui)
            }
        }
    }
}

@Composable
private fun DashboardMiningPanel(
    ui: ZSilentUiState,
    reportedWork: Long,
    availableWidth: Dp,
    modifier: Modifier = Modifier
) {
    Panel(
        title = "Mining status",
        meta = if (ui.mining.running) "solo miner running" else "idle",
        modifier = modifier
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("REPORTED SOLO WORK", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ui.mining.running) dashboardHashrate(reportedWork) else "—", style = ZTheme.text.monoDisplay, color = ZTheme.metrics.at(3))
            }
            Pill(
                if (ui.mining.running) "solo active" else "idle",
                if (ui.mining.running) ZTone.On else ZTone.Off
            )
        }
        ThinDivider()
        DashboardInlineMetricGrid(
            listOf(
                "Solo blocks" to ui.mining.blocksMined.toString(),
                "Solo difficulty" to (ui.mining.difficultyBits.takeIf { it > 0 }?.toString() ?: "—"),
                "CPU threads" to ui.mining.availableCpuThreads.toString()
            ),
            availableWidth
        )
        DashboardInlineMetricGrid(
            listOf(
                "Average solve" to dashboardSeconds(ui.mining.avgSolveTimeSecs),
                "Last solve" to dashboardSeconds(ui.mining.lastSolveTimeSecs)
            ),
            availableWidth
        )
        HintText(
            "The numeric values are the latest retained solo-miner snapshot; no freshness timestamp is available, and reported work is not a hardware-side hashrate measurement."
        )
    }
}

@Composable
private fun DashboardBalancePanel(
    ui: ZSilentUiState,
    modifier: Modifier = Modifier
) {
    val balance = dashboardBalancePresentation(ui)
    val privateVisible = !ui.walletLocked && !ui.balancesHidden
    Panel(
        title = "Private balance",
        meta = when {
            ui.walletLocked -> "locked"
            ui.balancesHidden -> "hidden"
            balance.snapshotAvailable -> "wallet snapshot"
            else -> "unavailable"
        },
        modifier = modifier
    ) {
        NoirKeyValue("Account", balance.account)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Address", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(
                Modifier.widthIn(max = 270.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dashboardShortHash(ui.shownAddress(balance.address).ifBlank { "not selected" }),
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CopyIconButton(ui.shownAddress(balance.address), enabled = balance.address.isNotBlank())
            }
        }
        if (!privateVisible) {
            EmptyState(
                if (ui.walletLocked) "Balance is protected while the wallet is locked." else "Balance display is hidden.",
                sub = "The chart is removed as well, so it cannot reveal a previous balance pattern."
            )
        } else if (!balance.snapshotAvailable) {
            EmptyState(
                "Balance has not been reported for the selected wallet.",
                sub = ui.walletError?.takeIf { it.isNotBlank() } ?: "ZSilent will show it after a successful wallet refresh."
            )
        } else {
            DashboardBalanceBar(balance.spendableAtoms, balance.lockedAtoms, balance.totalAtoms, Modifier.fillMaxWidth().height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardLegend("spendable", ZTheme.semantics.success)
                DashboardLegend("locked", ZTheme.semantics.warning)
            }
            NoirKeyValue("Available", dashboardBalanceText(balance.spendableText), mono = true)
            NoirKeyValue("Locked", dashboardBalanceText(balance.lockedText), mono = true)
            NoirKeyValue("Total", dashboardBalanceText(balance.totalText), mono = true)
            NoirKeyValue("Transactions", balance.transactions.toString(), mono = true)
            HintText("Balance freshness is not reported by the current wallet API; values are labelled as a retained snapshot.")
        }
    }
}

@Composable
private fun DashboardRecentActivity(ui: ZSilentUiState, nowMillis: Long) {
    val historyMatchesSelection = dashboardHistoryMatchesSelection(ui)
    Panel(
        title = "Recent private activity",
        meta = when {
            ui.walletLocked -> "protected"
            !historyMatchesSelection -> "refreshing"
            else -> "last ${minOf(5, ui.walletHistory.transactions.size)}"
        }
    ) {
        when {
            ui.walletLocked -> EmptyState("Unlock the wallet to load private activity.", sub = "Transaction amounts and fees remain hidden by design.")
            !historyMatchesSelection -> EmptyState("Activity is refreshing for the selected wallet.", sub = "A retained history from another wallet is never shown here.")
            ui.walletHistory.transactions.isEmpty() && !ui.walletError.isNullOrBlank() -> EmptyState("Wallet activity is unavailable.", sub = ui.walletError)
            ui.walletHistory.transactions.isEmpty() -> EmptyState("No private transactions are present in the latest wallet snapshot.", sub = "New wallet activity will appear after a successful refresh.")
            else -> ui.walletHistory.transactions.take(5).forEachIndexed { index, tx ->
                if (index > 0) ThinDivider()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                dashboardShortHash(tx.txHash),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CopyIconButton(tx.txHash, enabled = tx.txHash.isNotBlank())
                        }
                        Text(
                            buildString {
                                append(if (tx.timestamp > 0L) com.zsilent.core.ui.onboarding.formatTimeAgo(tx.timestamp, nowMillis) else "time not reported")
                                if (tx.amountHidden || tx.kind.isBlank()) {
                                    append(" · ${tx.outputs} output(s) · amount private")
                                } else {
                                    append(" · ${tx.kind} · ${historyAmountLabel(tx)}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Pill(tx.status.ifBlank { "reported" }, dashboardTransactionTone(tx.status))
                }
            }
        }
    }
}

private data class DashboardChartSeries(
    val values: List<Double>,
    val color: Color,
    val fill: Boolean = false
)

@Composable
private fun DashboardLineChart(series: List<DashboardChartSeries>, modifier: Modifier = Modifier) {
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val cleanSeries = series.map { item ->
        item.copy(values = item.values.map { if (it.isFinite()) it else 0.0 })
    }.filter { it.values.isNotEmpty() }
    Canvas(modifier) {
        val inset = 5.dp.toPx()
        val gridStroke = 1.dp.toPx()
        val lineStroke = 2.4.dp.toPx()
        val markerRadius = 4.dp.toPx()
        val left = inset
        val right = (size.width - inset).coerceAtLeast(left)
        val top = inset
        val bottom = (size.height - inset).coerceAtLeast(top)
        val plotWidth = (right - left).coerceAtLeast(1f)
        val plotHeight = (bottom - top).coerceAtLeast(1f)
        repeat(4) { index ->
            val y = top + plotHeight * index / 3f
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = gridStroke)
        }
        repeat(6) { index ->
            val x = left + plotWidth * index / 5f
            drawLine(grid.copy(alpha = 0.28f), Offset(x, top), Offset(x, bottom), strokeWidth = gridStroke)
        }
        if (cleanSeries.isEmpty()) return@Canvas
        val allValues = cleanSeries.flatMap { it.values }
        val rawMin = allValues.minOrNull() ?: 0.0
        val rawMax = allValues.maxOrNull() ?: rawMin
        val padding = if (abs(rawMax - rawMin) < 0.000001) max(abs(rawMax) * 0.02, 1.0) else (rawMax - rawMin) * 0.10
        val minValue = rawMin - padding
        val maxValue = rawMax + padding
        val range = (maxValue - minValue).coerceAtLeast(0.000001)
        cleanSeries.forEach { item ->
            val points = item.values.mapIndexed { index, value ->
                val x = if (item.values.size <= 1) left + plotWidth * 0.5f else left + plotWidth * index / (item.values.size - 1).toFloat()
                val normalized = ((value - minValue) / range).toFloat().coerceIn(0f, 1f)
                Offset(x, bottom - normalized * plotHeight)
            }
            if (points.size >= 2 && item.fill) {
                val area = Path().apply {
                    moveTo(points.first().x, bottom)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, bottom)
                    close()
                }
                drawPath(area, item.color.copy(alpha = 0.13f))
            }
            if (points.size >= 2) {
                val path = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    path,
                    item.color.copy(alpha = 0.92f),
                    style = Stroke(width = lineStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            points.lastOrNull()?.let { drawCircle(item.color, radius = markerRadius, center = it) }
        }
    }
}

@Composable
private fun DashboardPeerRing(
    total: Long,
    tor: Long,
    direct: Long,
    other: Long,
    splitKnown: Boolean,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val segments = if (splitKnown) {
        listOf(tor to ZTheme.metrics.at(0), direct to ZTheme.metrics.at(2), other to ZTheme.metrics.at(4))
    } else {
        listOf(total to MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2f + 2f
            val arcSize = Size(
                (size.width - inset * 2f).coerceAtLeast(1f),
                (size.height - inset * 2f).coerceAtLeast(1f)
            )
            val arcTopLeft = Offset(inset, inset)
            drawArc(track.copy(alpha = 0.55f), -90f, 360f, false, topLeft = arcTopLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (total > 0L) {
                var start = -90f
                segments.filter { it.first > 0L }.forEach { (count, color) ->
                    val rawSweep = 360f * count.toFloat() / total.toFloat()
                    val gap = minOf(3.5f, rawSweep * 0.18f)
                    drawArc(color, start + gap / 2f, (rawSweep - gap).coerceAtLeast(0.5f), false, topLeft = arcTopLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    start += rawSweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (splitKnown) total.toString() else "—", style = ZTheme.text.monoDisplay, color = MaterialTheme.colorScheme.onSurface)
            Text("PEERS", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardBalanceBar(
    spendable: Long,
    locked: Long,
    total: Long,
    modifier: Modifier = Modifier
) {
    val spendableColor = ZTheme.semantics.success
    val lockedColor = ZTheme.semantics.warning
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(track.copy(alpha = 0.6f), cornerRadius = radius)
        if (total > 0L) {
            val spendableWidth = size.width * spendable.coerceAtLeast(0L).toFloat() / total.toFloat()
            val lockedWidth = size.width * locked.coerceAtLeast(0L).toFloat() / total.toFloat()
            if (spendableWidth > 0f) {
                drawRoundRect(spendableColor, size = size.copy(width = spendableWidth), cornerRadius = radius)
            }
            if (lockedWidth > 0f) {
                drawRoundRect(
                    lockedColor,
                    topLeft = Offset(spendableWidth.coerceAtMost(size.width), 0f),
                    size = size.copy(width = lockedWidth.coerceAtMost(size.width - spendableWidth)),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
private fun DashboardAlertCard(alert: DashboardAlert) {
    val tint = when (alert.tone) {
        ZTone.On -> ZTheme.semantics.success
        ZTone.Warn -> ZTheme.semantics.warning
        ZTone.Err -> ZTheme.semantics.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (alert.tone) {
        ZTone.On -> Icons.Outlined.CheckCircle
        ZTone.Warn -> Icons.Outlined.WarningAmber
        ZTone.Err -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.Info
    }
    val shape = MaterialTheme.shapes.small
    Row(
        Modifier.fillMaxWidth().background(tint.copy(alpha = 0.08f), shape)
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.30f)), shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(alert.title, style = MaterialTheme.typography.labelLarge, color = tint)
            Text(alert.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardInlineMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = ZTheme.text.monoLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
/**
 * Métricas en fila si entran, apiladas si no.
 *
 * El ancho llega por parámetro y no se mide acá con `BoxWithConstraints`.
 * `BoxWithConstraints` es un `SubcomposeLayout`, y Compose no sabe darle
 * medidas intrínsecas: en cuanto este componente cae dentro de una fila con
 * `height(IntrinsicSize.Min)` —que es como se consigue que dos paneles queden
 * de igual alto— la aplicación se cae con "Asking for intrinsic measurements
 * of SubcomposeLayout layouts is not supported". Solo pasaba al maximizar,
 * porque recién ahí el Dashboard cambia a la disposición de dos columnas.
 */
private fun DashboardInlineMetricGrid(metrics: List<Pair<String, String>>, availableWidth: Dp) {
    run {
        if (availableWidth >= 500.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                metrics.forEach { (label, value) ->
                    DashboardInlineMetric(label, value, Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { (label, value) -> DashboardInlineMetric(label, value) }
            }
        }
    }
}

@Composable
private fun DashboardLegend(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardRouteRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        DashboardLegend(label, color)
        Text(value, style = ZTheme.text.monoLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun dashboardTelemetrySample(ui: ZSilentUiState, nowMillis: Long): DashboardTelemetrySample {
    val syncFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.nodeSyncStatusObservedAtMillis, ui.nodeSyncStatusConsecutiveFailures, nowMillis)
    val peersFresh = dashboardTelemetrySampleFresh(ui.localRpcOnline, ui.peerTelemetryObservedAtMillis, ui.peerTelemetryConsecutiveFailures, nowMillis)
    return DashboardTelemetrySample(
        timestampMillis = nowMillis,
        localHeight = dashboardLocalHeight(ui, nowMillis),
        targetHeight = dashboardTargetHeight(ui, nowMillis),
        localHeightKnown = syncFresh && dashboardLocalHeightKnown(ui, nowMillis),
        targetHeightKnown = syncFresh && dashboardTargetHeightKnown(ui, nowMillis),
        lag = dashboardLag(ui, nowMillis),
        peers = dashboardPeerCount(ui),
        gossipResponses = ui.nodeSyncStatus.gossipRxBlockResponse.coerceAtLeast(0L),
        syncFresh = syncFresh,
        peersFresh = peersFresh
    )
}

/**
 * Si la telemetría se puede tratar como viva.
 *
 * La edad se acota a cero a propósito. `nowMillis` es el reloj del tablero,
 * que avanza cada 5 s; `observedAtMillis` es el instante real en que llegó el
 * dato del nodo, cada ~3 s. Como 3 no divide a 5, lo normal es que el dato sea
 * **más nuevo** que el último tic del reloj y la resta dé negativo.
 *
 * Antes la condición era `in 0L..DASHBOARD_LIVE_WINDOW_MILLIS`, y un negativo
 * queda fuera de ese rango: el tablero declaraba "no disponible" justo cuando
 * el dato era más fresco que nunca. Simulando las dos cadencias, eso pasaba el
 * **60 % del tiempo** — las métricas titilaban sin parar y no se estabilizaban
 * nunca, con el nodo respondiendo en milisegundos.
 *
 * Un dato del futuro no existe acá: ambas marcas salen del mismo
 * `System.currentTimeMillis()` de la misma máquina, así que un negativo solo
 * significa "llegó entre dos tics" y su edad real es cero.
 */
private fun dashboardTelemetryFresh(
    localRpcOnline: Boolean,
    observedAtMillis: Long,
    consecutiveFailures: Int,
    nowMillis: Long
): Boolean = localRpcOnline && observedAtMillis > 0L && consecutiveFailures < 3 &&
    (nowMillis - observedAtMillis).coerceAtLeast(0L) <= DASHBOARD_LIVE_WINDOW_MILLIS

/**
 * Si la serie del gráfico se puede seguir dibujando.
 *
 * Exigía `consecutiveFailures == 0`, o sea que **una sola** lectura tardía
 * bastaba para declarar la telemetría muerta: el gráfico se vaciaba, las
 * tarjetas pasaban a "last known" y todo volvía un segundo después. Contra un
 * nodo que responde por Tor eso pasa seguido, y el tablero quedaba titilando
 * sin parar.
 *
 * La función de la que depende ya tolera hasta tres fallos seguidos para
 * mostrar los valores; esta descartaba esa holgura justo para lo que más se
 * nota. Ahora comparten criterio: un tropiezo aislado no borra nada, y hacen
 * falta varios seguidos para admitir que ya no hay telemetría viva.
 */
internal fun dashboardTelemetrySampleFresh(
    localRpcOnline: Boolean,
    observedAtMillis: Long,
    consecutiveFailures: Int,
    nowMillis: Long
): Boolean = dashboardTelemetryFresh(localRpcOnline, observedAtMillis, consecutiveFailures, nowMillis)

private fun dashboardPeerCount(ui: ZSilentUiState): Long = ui.peers.size.toLong()

internal fun dashboardLocalHeight(ui: ZSilentUiState, nowMillis: Long): Long = when {
    dashboardTelemetryFresh(ui.localRpcOnline, ui.nodeSyncStatusObservedAtMillis, ui.nodeSyncStatusConsecutiveFailures, nowMillis) ->
        ui.nodeSyncStatus.localHeight
    dashboardLocalHeightKnown(ui, nowMillis) -> ui.chainHeight
    else -> 0L
}.coerceAtLeast(0L)

internal fun dashboardTargetHeight(ui: ZSilentUiState, nowMillis: Long): Long = when {
    dashboardTelemetryFresh(ui.localRpcOnline, ui.nodeSyncStatusObservedAtMillis, ui.nodeSyncStatusConsecutiveFailures, nowMillis) ->
        ui.nodeSyncStatus.targetHeight
    dashboardTargetHeightKnown(ui, nowMillis) -> ui.networkHeight
    else -> 0L
}.coerceAtLeast(0L)

internal fun dashboardLocalHeightKnown(ui: ZSilentUiState, nowMillis: Long): Boolean =
    dashboardTelemetryFresh(
        ui.localRpcOnline,
        ui.nodeSyncStatusObservedAtMillis,
        ui.nodeSyncStatusConsecutiveFailures,
        nowMillis
    ) || ui.localRpcOnline || ui.chainHeight > 0L

internal fun dashboardTargetHeightKnown(ui: ZSilentUiState, nowMillis: Long): Boolean =
    dashboardTelemetryFresh(
        ui.localRpcOnline,
        ui.nodeSyncStatusObservedAtMillis,
        ui.nodeSyncStatusConsecutiveFailures,
        nowMillis
    ) || ui.rpcOnline || ui.networkHeight > 0L

private fun dashboardLag(ui: ZSilentUiState, nowMillis: Long): Long {
    val syncFresh = dashboardTelemetryFresh(ui.localRpcOnline, ui.nodeSyncStatusObservedAtMillis, ui.nodeSyncStatusConsecutiveFailures, nowMillis)
    val local = dashboardLocalHeight(ui, nowMillis)
    val target = dashboardTargetHeight(ui, nowMillis)
    return when {
        syncFresh -> ui.nodeSyncStatus.lag.coerceAtLeast(0L)
        dashboardLocalHeightKnown(ui, nowMillis) && dashboardTargetHeightKnown(ui, nowMillis) ->
            (target - local).coerceAtLeast(0L)
        else -> 0L
    }
}

private fun dashboardServiceTone(status: ServiceStatus): ZTone = when (status) {
    ServiceStatus.Online -> ZTone.On
    ServiceStatus.Starting, ServiceStatus.Stopping -> ZTone.Warn
    ServiceStatus.Error -> ZTone.Err
    ServiceStatus.Offline -> ZTone.Off
}

private fun dashboardObservedLabel(observedAtMillis: Long, nowMillis: Long): String {
    if (observedAtMillis <= 0L) return "not reported"
    val seconds = ((nowMillis - observedAtMillis).coerceAtLeast(0L) / 1_000L)
    return when {
        seconds < 60L -> "${seconds}s ago"
        seconds < 3_600L -> "${seconds / 60L}m ago"
        else -> "${seconds / 3_600L}h ago"
    }
}

private fun dashboardHashrate(value: Long): String = when {
    value >= 1_000_000_000L -> "%.2f GH/s".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.2f MH/s".format(value / 1_000_000.0)
    value >= 1_000L -> "%.2f kH/s".format(value / 1_000.0)
    else -> "$value H/s"
}

private fun dashboardRate(value: Double): String = when {
    value >= 100.0 -> "%.0f".format(value)
    value >= 10.0 -> "%.1f".format(value)
    else -> "%.2f".format(value)
}

private fun dashboardSeconds(value: Double): String = if (value > 0.0) "%.1fs".format(value) else "—"

private fun dashboardBalanceText(value: String?): String = value?.takeIf { it.isNotBlank() }?.let { "$it ZKA" } ?: "not reported"

internal fun dashboardHistoryMatchesSelection(ui: ZSilentUiState): Boolean {
    val historyWallet = ui.walletHistory.wallet.trim()
    if (ui.walletHistory.transactions.isEmpty() && ui.walletHistory.count <= 0) return true

    val selectedAddress = ui.activeWalletAddress.trim()
    val selectedName = ui.activeWalletName.trim()
    val selectedWallet = when {
        selectedAddress.isNotBlank() -> ui.wallets.firstOrNull { it.address == selectedAddress }
        selectedName.isNotBlank() -> ui.wallets.firstOrNull { it.name == selectedName }
        else -> ui.wallets.singleOrNull()
    }
    val resolvedName = selectedWallet?.name.orEmpty().ifBlank {
        selectedName.takeIf { selectedAddress.isBlank() }.orEmpty()
    }
    return historyWallet.isNotBlank() && resolvedName.isNotBlank() && historyWallet == resolvedName
}

private fun dashboardShortHash(value: String): String = when {
    value.length <= 20 -> value
    else -> "${value.take(10)}…${value.takeLast(8)}"
}

internal fun dashboardTransactionTone(status: String): ZTone {
    val normalized = status.trim().lowercase().replace('_', ' ').replace('-', ' ')
    return when {
        normalized in setOf("failed", "failure", "rejected", "cancelled", "canceled", "expired") -> ZTone.Err
        normalized in setOf("pending", "queued", "processing", "unconfirmed", "not confirmed", "not completed") -> ZTone.Warn
        normalized in setOf("confirmed", "completed", "complete", "success", "succeeded", "released") -> ZTone.On
        else -> ZTone.Info
    }
}
