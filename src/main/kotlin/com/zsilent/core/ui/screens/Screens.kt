package com.zsilent.core.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as GraphicsPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.AppSection
import com.zsilent.core.models.LocalWallet
import com.zsilent.core.models.SeedWordCount
import com.zsilent.core.models.SendResult
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.outlined.Palette
import com.zsilent.core.models.LogLevel
import com.zsilent.core.models.RuntimeLog
import com.zsilent.core.ui.components.Truncate
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.outlined.QrCode
import com.zsilent.core.ui.theme.ZTheme
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PowerSettingsNew
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.components.CopyIconButton
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.ErrorText
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.MonoText
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.SegmentedSelector
import com.zsilent.core.ui.components.ZLabel
import com.zsilent.core.ui.components.Stat
import com.zsilent.core.ui.components.PanelHeader
import com.zsilent.core.ui.components.PasswordField
import com.zsilent.core.ui.components.QrCode
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.StatusPill
import com.zsilent.core.ui.components.ThinDivider
import com.zsilent.core.ui.components.zFieldColors
import com.zsilent.core.ui.components.zButtonColors
import com.zsilent.core.ui.components.zPastelButtonColors
import com.zsilent.core.ui.components.zOutlinedButtonColors
import kotlinx.coroutines.delay
import java.net.URI
import java.nio.file.Path

@Composable
fun DashboardScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    val activeWallet = activeLocalWallet(ui)
    val effectiveHashrate = maxOf(ui.mining.hashrateHps, ui.stats.reportedHashrateHps)
    val miningDetail = when {
        !ui.miningDiagnostic.isNullOrBlank() -> "needs attention"
        ui.localMiningEnabled && effectiveHashrate <= 0L -> "warming up"
        ui.miningControlledByApp -> "ZSilent miner"
        ui.mining.running -> "active"
        else -> "idle"
    }
    val nodeTone = when {
        ui.nodeStatus == com.zsilent.core.models.ServiceStatus.Online -> com.zsilent.core.ui.onboarding.NoirStatusTone.Ok
        ui.nodeStatus == com.zsilent.core.models.ServiceStatus.Starting -> com.zsilent.core.ui.onboarding.NoirStatusTone.Warn
        ui.nodeStatus == com.zsilent.core.models.ServiceStatus.Error -> com.zsilent.core.ui.onboarding.NoirStatusTone.Bad
        else -> com.zsilent.core.ui.onboarding.NoirStatusTone.Neutral
    }
    val syncTone = if (ui.networkSynced) com.zsilent.core.ui.onboarding.NoirStatusTone.Ok else com.zsilent.core.ui.onboarding.NoirStatusTone.Warn
    val miningTone = when {
        !ui.miningDiagnostic.isNullOrBlank() -> com.zsilent.core.ui.onboarding.NoirStatusTone.Bad
        ui.mining.running && effectiveHashrate > 0L -> com.zsilent.core.ui.onboarding.NoirStatusTone.Ok
        ui.mining.running || ui.localMiningEnabled -> com.zsilent.core.ui.onboarding.NoirStatusTone.Warn
        else -> com.zsilent.core.ui.onboarding.NoirStatusTone.Neutral
    }
    val peerCount = ui.stats.connectedPeers
    val peerTone = when {
        peerCount >= 2 -> com.zsilent.core.ui.onboarding.NoirStatusTone.Ok
        peerCount >= 1 -> com.zsilent.core.ui.onboarding.NoirStatusTone.Warn
        else -> com.zsilent.core.ui.onboarding.NoirStatusTone.Bad
    }

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Dashboard",
        title = "Private command center",
        subtitle = "Wallet, node and mining at a glance. Nothing leaves this device."
    ) {
        // ── Top status row ─────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.zsilent.core.ui.onboarding.NoirStatusPill(
                label = "Node",
                value = ui.nodeStatus.name,
                detail = if (ui.localRpcOnline) "RPC reachable" else "RPC offline",
                tone = nodeTone,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirStatusPill(
                label = "Sync",
                value = if (ui.networkSynced) "OK" else "${ui.syncRemainingBlocks} behind",
                detail = if (ui.networkSynced) "tip matched with network" else "importing blocks",
                tone = syncTone,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirStatusPill(
                label = "Peers",
                value = peerCount.toString(),
                detail = if (peerCount == 0L) "isolated" else "connected via p2p",
                tone = peerTone,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirStatusPill(
                label = "Nodes",
                value = (ui.localConnectedPeers + 1).toString(),
                detail = if (ui.localConnectedPeers >= 1L) "on the network" else "just this node",
                tone = if (ui.localConnectedPeers >= 1L) com.zsilent.core.ui.onboarding.NoirStatusTone.Ok else com.zsilent.core.ui.onboarding.NoirStatusTone.Warn,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirStatusPill(
                label = "Mining",
                value = if (ui.mining.running) formatHashrate(effectiveHashrate) else "off",
                detail = miningDetail,
                tone = miningTone,
                modifier = Modifier.weight(1f)
            )
        }

        DashboardMetricStripNoir(ui, activeWallet, effectiveHashrate)

        NetworkSyncPanel(ui, onSyncNow = appState::syncNow)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.zsilent.core.ui.onboarding.NoirSection(
                title = "Active wallet",
                meta = if (ui.walletLocked) "locked" else "unlocked",
                modifier = Modifier.weight(1f)
            ) {
                com.zsilent.core.ui.onboarding.NoirKeyValue("Account", activeWallet?.name ?: ui.activeWalletName.ifBlank { "not selected" })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Address", color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val addr = activeWallet?.address ?: ui.activeWalletAddress.ifBlank { "not selected" }
                        Text(
                            shortHash(addr),
                            color = com.zsilent.core.ui.onboarding.NoirText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        CopyIconButton(addr, enabled = addr.isNotBlank() && addr != "not selected")
                    }
                }
                com.zsilent.core.ui.onboarding.NoirKeyValue("Available", activeWalletAvailable(ui, activeWallet), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Locked", activeWalletLocked(ui, activeWallet), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Total", activeWalletTotal(ui, activeWallet), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Transactions", (activeWallet?.txCount ?: ui.walletHistory.count).toString())
            }

            val netLive = ui.localRpcOnline && (ui.stats.connectedPeers > 0 || ui.networkSynced)
            com.zsilent.core.ui.onboarding.NoirSection(
                title = "Network",
                meta = if (netLive) "connected (p2p)" else if (ui.localRpcOnline) "node online" else "offline",
                modifier = Modifier.weight(1f)
            ) {
                com.zsilent.core.ui.onboarding.NoirKeyValue("Reachability", if (netLive) "live via P2P mesh" else "isolated")
                com.zsilent.core.ui.onboarding.NoirKeyValue("Connected peers", ui.localConnectedPeers.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Node height", ui.chainHeight.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Network height", ui.networkHeight.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Difficulty", ui.stats.difficulty.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Total emission", "%.4f ZKA".format(ui.stats.emissionZka), mono = true)
                ui.miningDiagnostic?.takeIf { it.isNotBlank() }?.let { diagnostic ->
                    Text(diagnostic, color = com.zsilent.core.ui.onboarding.NoirError, fontSize = 11.sp)
                }
            }
        }

        DashboardActions(ui, appState)

        com.zsilent.core.ui.onboarding.NoirSection(
            title = "Recent private activity",
            meta = "last 5"
        ) {
            if (ui.walletLocked) {
                Text("Unlock the wallet to load history.", color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 12.sp)
            } else if (ui.walletHistory.transactions.isEmpty()) {
                Text("No private transactions yet.", color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 12.sp)
            } else {
                ui.walletHistory.transactions.take(5).forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(shortHash(tx.txHash), color = com.zsilent.core.ui.onboarding.NoirText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            CopyIconButton(tx.txHash, enabled = tx.txHash.isNotBlank())
                        }
                        Text(tx.status, color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

/**
 * Fila de acciones del Dashboard, portada del prototipo.
 *
 * ZSilent no tenía ninguna: para arrancar el nodo, el minero o Tor había que
 * navegar a Node, Mining o Privacy. El prototipo las pone acá porque son las
 * tres cosas que uno quiere hacer apenas abre la app.
 *
 * El minero queda deshabilitado si el nodo no está `Online` — minar sin nodo no
 * hace nada, y un botón que acepta el clic y no produce efecto se lee como un
 * bug. El motivo se muestra al lado en vez de dejar el botón mudo.
 */
@Composable
private fun DashboardActions(ui: ZSilentUiState, appState: ZSilentAppState) {
    val nodeOnline = ui.nodeStatus == com.zsilent.core.models.ServiceStatus.Online
    val nodeBusy = ui.nodeStatus == com.zsilent.core.models.ServiceStatus.Starting ||
        ui.nodeStatus == com.zsilent.core.models.ServiceStatus.Stopping
    val mining = ui.mining.running

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZButton(
            onClick = { if (nodeOnline) appState.stopNode() else appState.startNode() },
            variant = ZButtonVariant.Accent,
            enabled = !nodeBusy,
        ) {
            Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
            ZButtonText(if (nodeOnline) "Stop node" else "Start node")
        }
        ZButton(
            // Mismo default que usa ZSilentAppState cuando no hay una elección
            // previa de hilos.
            onClick = {
                if (mining) appState.stopMining()
                else appState.startMining(ui.mining.availableCpuThreads.coerceAtLeast(1))
            },
            variant = ZButtonVariant.Outline,
            enabled = nodeOnline,
        ) {
            Icon(Icons.Outlined.Bolt, null, modifier = Modifier.size(14.dp))
            ZButtonText(if (mining) "Stop miner" else "Start miner")
        }
        ZButton(
            onClick = { appState.setTorEnabled(!ui.torEnabled) },
            variant = ZButtonVariant.Outline,
        ) {
            Icon(Icons.Outlined.Security, null, modifier = Modifier.size(14.dp))
            ZButtonText(if (ui.torEnabled) "Disable Tor" else "Enable Tor")
        }
        if (!nodeOnline && !nodeBusy) {
            Pill("node offline — miner disabled", ZTone.Warn)
        }
    }
}

@Composable
private fun DashboardMetricStripNoir(
    ui: ZSilentUiState,
    activeWallet: LocalWallet?,
    effectiveHashrate: Long
) {
    val heightValue = ui.chainHeight.takeIf { it > 0 } ?: ui.stats.height
    val emissionValue = ui.stats.emissionZka
    val balanceSample = if (ui.walletLocked || ui.balancesHidden) {
        0.0
    } else {
        (ui.walletBalance?.totalBalanceAtoms ?: activeWallet?.totalBalanceAtoms ?: 0L).toDouble()
    }
    val nextUnlockBlocks = ui.mining.nextRewardUnlockInBlocks
    val nextUnlockHeight = ui.mining.nextRewardUnlockHeight
    val lastBlockUnix = ui.stats.lastBlockTime

    // Sparkline histories
    var heightHistory by remember { mutableStateOf(seedMetricHistory(heightValue.toDouble())) }
    var hashrateHistory by remember { mutableStateOf(seedMetricHistory(effectiveHashrate.toDouble())) }
    var balanceHistory by remember(activeWallet?.address) { mutableStateOf(seedMetricHistory(balanceSample)) }
    var emissionHistory by remember { mutableStateOf(seedMetricHistory(emissionValue)) }
    var unlockHistory by remember { mutableStateOf(seedMetricHistory((nextUnlockBlocks ?: 0L).toDouble())) }
    var lastBlockHistory by remember { mutableStateOf(seedMetricHistory(lastBlockUnix.toDouble())) }

    LaunchedEffect(heightValue) {
        heightHistory = appendMetricSample(heightHistory, heightValue.toDouble())
    }
    LaunchedEffect(effectiveHashrate) {
        hashrateHistory = appendMetricSample(hashrateHistory, effectiveHashrate.toDouble())
    }
    LaunchedEffect(balanceSample, ui.walletLocked, ui.balancesHidden) {
        balanceHistory = appendMetricSample(balanceHistory, balanceSample)
    }
    LaunchedEffect(emissionValue) {
        emissionHistory = appendMetricSample(emissionHistory, emissionValue)
    }
    LaunchedEffect(nextUnlockBlocks) {
        unlockHistory = appendMetricSample(unlockHistory, (nextUnlockBlocks ?: 0L).toDouble())
    }
    LaunchedEffect(lastBlockUnix) {
        lastBlockHistory = appendMetricSample(lastBlockHistory, lastBlockUnix.toDouble())
    }

    // Live ticker for "last block N seconds ago"
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastBlockUnix) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val lastBlockText = com.zsilent.core.ui.onboarding.formatTimeAgo(lastBlockUnix, nowMillis)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "METRICS",
                color = com.zsilent.core.ui.onboarding.NoirTextSubtle,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp
            )
            Text(
                if (ui.localRpcOnline && (ui.stats.connectedPeers > 0 || ui.networkSynced)) "LIVE NETWORK" else "LOCAL SNAPSHOT",
                color = com.zsilent.core.ui.onboarding.NoirTextSubtle,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp
            )
        }
        // Altura pareja: sin esto las tarjetas con `detail` crecian mas que las
        // otras y la fila quedaba despareja.
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Height",
                value = heightValue.toString(),
                samples = heightHistory,
                accent = ZTheme.metrics.at(0),
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Hashrate",
                value = formatHashrate(effectiveHashrate),
                samples = hashrateHistory,
                accent = ZTheme.metrics.at(1),
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Balance",
                value = activeWalletTotal(ui, activeWallet),
                samples = balanceHistory,
                accent = ZTheme.metrics.at(2),
                valueColor = ZTheme.metrics.at(2),
                hidden = ui.walletLocked || ui.balancesHidden,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Next unlock",
                value = if (nextUnlockBlocks == null) "—" else "${nextUnlockBlocks} blk",
                samples = unlockHistory,
                accent = ZTheme.metrics.at(3),
                detail = if (nextUnlockHeight == null) "no maturing rewards" else "at height ${nextUnlockHeight}",
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Last block",
                value = lastBlockText,
                samples = lastBlockHistory,
                accent = ZTheme.metrics.at(4),
                detail = if (lastBlockUnix > 0L) "network heartbeat" else "no data",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class SyncSample(
    val height: Long,
    val progress: Double,
    val timestampMillis: Long
)

@Composable
private fun SessionReadyCard(
    title: String,
    ready: Boolean,
    detail: String,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(if (ready) "session unlocked" else "session locked", active = ready)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NetworkSyncPanel(ui: ZSilentUiState, modifier: Modifier = Modifier, onSyncNow: (() -> Unit)? = null) {
    val progress = (ui.syncProgressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val syncActive = ui.syncBusy || (!ui.networkSynced && (ui.rpcOnline || ui.localRpcOnline || ui.nodeStatus == ServiceStatus.Online))
    var pulse by remember { mutableStateOf(0) }
    var progressHistory by remember { mutableStateOf(seedMetricHistory(ui.syncProgressPercent, points = 36)) }
    var syncSamples by remember {
        mutableStateOf(
            listOf(
                SyncSample(
                    height = ui.chainHeight,
                    progress = ui.syncProgressPercent,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        )
    }
    LaunchedEffect(syncActive) {
        while (syncActive) {
            delay(140)
            pulse = (pulse + 1) % 10_000
        }
    }
    LaunchedEffect(ui.chainHeight, ui.networkHeight, ui.syncProgressPercent, ui.syncBusy) {
        val now = System.currentTimeMillis()
        progressHistory = appendMetricSample(progressHistory, ui.syncProgressPercent, points = 36)
        val last = syncSamples.lastOrNull()
        if (last == null || last.height != ui.chainHeight || now - last.timestampMillis > 8_000L) {
            syncSamples = (syncSamples + SyncSample(ui.chainHeight, ui.syncProgressPercent, now)).takeLast(18)
        }
    }
    val etaLabel = syncEtaLabel(syncSamples, ui.syncRemainingBlocks, ui.networkSynced, ui.localRpcOnline)
    val speedLabel = syncSpeedLabel(syncSamples, ui.networkSynced)
    Panel(
        title = "Network sync",
        meta = if (ui.networkSynced) "ok synced" else if (ui.syncBusy) "importing" else if (ui.localRpcOnline) "syncing" else "offline",
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = if (ui.networkSynced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        )
        Text(
            ui.syncStatusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SyncMotionStrip(
            progress = progress,
            active = syncActive,
            pulse = pulse,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyValueRow("ETA", etaLabel)
                KeyValueRow("Speed", speedLabel)
                KeyValueRow("Wallet block", ui.chainHeight.toString())
                KeyValueRow("Network block", ui.networkHeight.toString())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Progress line",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SparklineChart(
                    samples = progressHistory,
                    color = if (ui.networkSynced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
                KeyValueRow("Remaining", ui.syncRemainingBlocks.toString())
                KeyValueRow("Progress", "%.1f%%".format(ui.syncProgressPercent))
            }
        }
        Text(
            syncEncouragement(ui, syncSamples),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onSyncNow != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    colors = zOutlinedButtonColors(),
                    onClick = onSyncNow,
                    enabled = ui.rpcOnline &&
                        !ui.networkSynced &&
                        !ui.syncBusy &&
                        ui.nodeStatus == ServiceStatus.Offline &&
                        ui.miningStatus == ServiceStatus.Offline &&
                        !ui.mining.running,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = SettingsButtonPadding
                ) {
                    Text(if (ui.syncBusy) "Syncing" else "Sync now")
                }
            }
        }
    }
}

@Composable
private fun SyncMotionStrip(
    progress: Float,
    active: Boolean,
    pulse: Int,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val accent = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val success = MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        val centerY = size.height * 0.54f
        val startX = 14f
        val endX = size.width - 14f
        val width = (endX - startX).coerceAtLeast(1f)
        val progressX = startX + width * progress.coerceIn(0f, 1f)
        drawLine(
            color = trackColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = if (progress >= 1f) success else accent.copy(alpha = 0.82f),
            start = Offset(startX, centerY),
            end = Offset(progressX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        val phase = (pulse % 48) / 48f
        if (active) {
            repeat(4) { index ->
                val dotPhase = (phase + index * 0.18f) % 1f
                val dotX = startX + width * dotPhase
                drawCircle(
                    color = accent.copy(alpha = 0.20f + index * 0.12f),
                    radius = 4.5f + index,
                    center = Offset(dotX, centerY)
                )
            }
        }
        drawCircle(
            color = if (progress >= 1f) success else accent,
            radius = 7.5f,
            center = Offset(progressX, centerY)
        )
        drawCircle(
            color = trackColor,
            radius = 3.5f,
            center = Offset(startX, centerY)
        )
        drawCircle(
            color = if (progress >= 1f) success else trackColor,
            radius = 3.5f,
            center = Offset(endX, centerY)
        )
    }
}

@Composable
private fun SparklineChart(
    samples: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height * 0.82f),
                end = Offset(size.width, size.height * 0.82f),
                strokeWidth = 1f
            )

            if (samples.size < 2) {
                drawLine(
                    color = mutedColor,
                    start = Offset(0f, size.height * 0.5f),
                    end = Offset(size.width, size.height * 0.5f),
                    strokeWidth = 1.2f
                )
                return@Canvas
            }

            val min = samples.minOrNull() ?: 0.0
            val max = samples.maxOrNull() ?: min
            val range = (max - min).takeIf { it > 0.000001 } ?: 1.0
            val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
            val points = samples.mapIndexed { index, raw ->
                val normalized = ((raw - min) / range).toFloat().coerceIn(0f, 1f)
                Offset(
                    x = index * stepX,
                    y = size.height - (normalized * size.height * 0.78f) - size.height * 0.1f
                )
            }
            val linePath = GraphicsPath().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            val areaPath = GraphicsPath().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(areaPath, color = color.copy(alpha = 0.16f))
            drawPath(
                linePath,
                color = color.copy(alpha = 0.88f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Contenedor de sección que se puede embeber.
 *
 * `ScreenScaffold` aporta fondo, scroll y relleno de pantalla completa. Cuando
 * Send y Receive se renderizan como pestañas dentro de Wallet, ese cromo ya lo
 * pone el shell de Wallet: anidarlo daría doble scroll y doble relleno.
 */
@Composable
private fun EmbeddableShell(
    embedded: Boolean,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    if (embedded) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
        ) { content() }
    } else {
        ScreenScaffold(title, subtitle) { content() }
    }
}

private enum class WalletPage {
    Home,
    Create,
    Send,
    Receive
}

/**
 * Fila de pestañas del Wallet: subrayado de 2dp bajo la activa, etiqueta en mono
 * mayúscula con tracking, como en el prototipo.
 */
@Composable
private fun WalletTabs(selected: WalletPage, onSelect: (WalletPage) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tabs = listOf(
        WalletPage.Home to "overview",
        WalletPage.Send to "send",
        WalletPage.Receive to "receive",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEach { (page, label) ->
                val active = selected == page
                // width(IntrinsicSize.Max) es imprescindible: sin eso el
                // fillMaxWidth del subrayado expande la columna a todo el ancho
                // disponible, la primera pestana se come la fila y las otras dos
                // quedan fuera de pantalla. Fue exactamente el bug que dejo Send
                // y Receive inalcanzables.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onSelect(page) }
                ) {
                    Text(
                        label.uppercase(),
                        style = ZTheme.text.eyebrow,
                        color = if (active) scheme.onSurface else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (active) scheme.primary else Color.Transparent,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

@Composable
fun WalletScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var walletPage by remember { mutableStateOf(WalletPage.Home) }
    var newWalletName by remember { mutableStateOf("") }
    var newWalletPassword by remember { mutableStateOf("") }
    var newWalletPasswordConfirm by remember { mutableStateOf("") }
    var newWalletSeedWords by remember { mutableStateOf(SeedWordCount.TwentyFour) }
    var backupPath by remember(ui.activeWalletName) {
        mutableStateOf(defaultWalletBackupPath(ui.activeWalletName.ifBlank { ui.walletName }))
    }
    var backupPassword by remember { mutableStateOf("") }
    var importPath by remember { mutableStateOf("") }
    var importName by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    val activeWallet = activeLocalWallet(ui)
    val canCreateWallet = newWalletName.isNotBlank() &&
        newWalletPassword.length >= 8 &&
        newWalletPassword == newWalletPasswordConfirm &&
        !ui.walletCreationBusy
    val canExportBackup = ui.activeWalletAddress.isNotBlank() &&
        backupPath.isNotBlank() &&
        backupPassword.isNotBlank() &&
        !ui.walletBackupBusy
    val canImportBackup = importPath.isNotBlank() &&
        importPassword.isNotBlank() &&
        !ui.walletImportBusy

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Wallet",
        // El prototipo titula con el NOMBRE de la wallet, no con un rotulo
        // generico: es el dato que identifica en que cuenta estas parado.
        title = if (walletPage == WalletPage.Create) "Add wallet"
                else activeWallet?.name ?: ui.activeWalletName.ifBlank { "Wallet" },
        subtitle = if (walletPage == WalletPage.Create) {
            "Create an additional private account inside ZSilent Core."
        } else {
            "Balances, sending and receiving. Every operation is private by default."
        }
    ) {
        // Pestañas del prototipo. Send y Receive dejaron de ser secciones del
        // menú (D3) y viven acá. `Create` no es una pestaña: es una subpágina a
        // la que se llega desde Home, así que no aparece en la fila.
        // La tarjeta de balance y el desglose viven ARRIBA de las pestanas y
        // fuera del Crossfade, como en el prototipo: siguen visibles en Send y
        // en Receive. Antes desaparecian al cambiar de pestana, y perdias de
        // vista cuanto tenias justo cuando ibas a enviar.
        if (walletPage != WalletPage.Create) {
            WalletHeroCard(
                ui = ui,
                activeWallet = activeWallet,
                onAddWallet = {
                    appState.clearWalletCreationResult()
                    walletPage = WalletPage.Create
                },
                onToggleHide = appState::toggleBalances,
                onRefresh = { appState.refreshWallets() },
                onSend = { walletPage = WalletPage.Send },
                onReceive = { walletPage = WalletPage.Receive },
            )
            WalletBreakdownRow(ui = ui, activeWallet = activeWallet)

            WalletTabs(
                selected = walletPage,
                onSelect = { walletPage = it },
            )
        }

        Crossfade(
            targetState = walletPage,
            animationSpec = tween(180),
            label = "wallet-page"
        ) { page ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (page) {
                    WalletPage.Home -> {
                        // ── Wallets list (only when there's more than one) ──
                        if (ui.wallets.size > 1) {
                            WalletsListSection(ui = ui, onSelect = appState::selectWallet) {
                                appState.clearWalletCreationResult()
                                walletPage = WalletPage.Create
                            }
                        }

                        // ── Errors surface ──────────────────────────────────
                        ui.walletError?.let { WalletErrorRow(it) }
                        ui.walletCreationError?.let { WalletErrorRow(it) }

                        // ── Recent activity ────────────────────────────────
                        WalletHistoryTable(ui)
                    }

                    WalletPage.Send -> SendScreen(ui, appState, embedded = true)
                    WalletPage.Receive -> ReceiveScreen(ui, appState, embedded = true)
                    WalletPage.Create -> {
                        OutlinedButton(
                            colors = zOutlinedButtonColors(),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = SettingsButtonPadding,
                            onClick = {
                                walletPage = WalletPage.Home
                                appState.clearWalletCreationResult()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Back to wallet")
                        }

                        Panel(title = "Create wallet", meta = "encrypted") {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newWalletName,
                                    onValueChange = { newWalletName = it.trim() },
                                    label = { Text("Wallet name") },
                                    singleLine = true,
                                    colors = zFieldColors(),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SeedWordCount.entries.forEach { count ->
                                        FilterChip(
                                            selected = newWalletSeedWords == count,
                                            onClick = { newWalletSeedWords = count },
                                            label = { Text("${count.words} words") }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                PasswordField(newWalletPassword, { newWalletPassword = it }, "Wallet password", Modifier.weight(1f))
                                PasswordField(newWalletPasswordConfirm, { newWalletPasswordConfirm = it }, "Confirm password", Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        appState.createAdditionalWallet(
                                            name = newWalletName,
                                            password = newWalletPassword.toCharArray(),
                                            seedWords = newWalletSeedWords.words
                                        )
                                        newWalletPassword = ""
                                        newWalletPasswordConfirm = ""
                                    },
                                    enabled = canCreateWallet,
                                    colors = zButtonColors(),
                                    shape = MaterialTheme.shapes.small,
                                    contentPadding = SettingsButtonPadding
                                ) { Text(if (ui.walletCreationBusy) "Creating..." else "Create") }
                            }
                            ui.walletCreationError?.let { ErrorText(it) }
                            ui.walletCreationResult?.let { created ->
                                ThinDivider()
                                KeyValueRow("Created", created.name)
                                CopyableKeyValueRow("Address", created.address)
                                created.recoveryPhrase?.let { phrase ->
                                    SelectionContainer {
                                        Text(phrase, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    created.recoveryPhrase?.let { phrase -> CopyIconButton(phrase) }
                                    OutlinedButton(
                                        colors = zOutlinedButtonColors(),
                                        onClick = appState::clearWalletCreationResult,
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = SettingsButtonPadding
                                    ) { Text("I saved it") }
                                    Button(
                                        onClick = {
                                            appState.clearWalletCreationResult()
                                            walletPage = WalletPage.Home
                                        },
                                        colors = zButtonColors(),
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = SettingsButtonPadding
                                    ) { Text("Done") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WALLET HELPERS (noir)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoirIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val border = if (enabled) com.zsilent.core.ui.onboarding.NoirOutlineStrong
        else com.zsilent.core.ui.onboarding.NoirOutline
    val tint = if (enabled) com.zsilent.core.ui.onboarding.NoirSilver
        else com.zsilent.core.ui.onboarding.NoirTextSubtle
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                com.zsilent.core.ui.onboarding.NoirSurfaceElev,
                shape
            )
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, border),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun WalletHeroCard(
    ui: ZSilentUiState,
    activeWallet: LocalWallet?,
    onAddWallet: () -> Unit,
    onToggleHide: () -> Unit,
    onRefresh: () -> Unit = {},
    onSend: () -> Unit = {},
    onReceive: () -> Unit = {}
) {
    val shape = MaterialTheme.shapes.medium
    val address = activeWallet?.address ?: ui.activeWalletAddress.ifBlank { "" }
    val totalAtoms = ui.walletBalance?.totalBalanceAtoms ?: activeWallet?.totalBalanceAtoms ?: 0L
    val hidden = ui.balancesHidden || ui.walletLocked
    val (whole, decimals) = splitZkaForDisplay(totalAtoms)

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .background(com.zsilent.core.ui.onboarding.NoirCardGradient, shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top row: address + copy | refresh + add + eye (all black)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (address.isBlank()) "no wallet selected" else truncateAddressForHero(address),
                    color = com.zsilent.core.ui.onboarding.NoirText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (address.isNotBlank()) CopyIconButton(address)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // El prototipo pone SEND y RECEIVE aca, pegados al balance:
                // es donde uno los busca despues de mirar cuanto tiene.
                ZButton(onClick = onSend, variant = ZButtonVariant.Accent, size = ZButtonSize.Small) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(12.dp))
                    ZButtonText("Send")
                }
                ZButton(onClick = onReceive, variant = ZButtonVariant.Outline, size = ZButtonSize.Small) {
                    Icon(Icons.Outlined.QrCode, null, modifier = Modifier.size(12.dp))
                    ZButtonText("Receive")
                }
                BlackIconButton(icon = Icons.Outlined.Sync, contentDescription = "Refresh balance", onClick = onRefresh)
                BlackIconButton(
                    icon = com.zsilent.core.ui.onboarding.BootstrapIcons.PatchPlus,
                    contentDescription = "Add wallet", onClick = onAddWallet
                )
                BlackIconButton(
                    icon = if (hidden) com.zsilent.core.ui.onboarding.BootstrapIcons.EyeSlash
                        else com.zsilent.core.ui.onboarding.BootstrapIcons.Eye,
                    contentDescription = if (hidden) "Show balances" else "Hide balances",
                    onClick = onToggleHide
                )
            }
        }

        // Hero balance
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "TOTAL BALANCE",
                color = com.zsilent.core.ui.onboarding.NoirTextSubtle,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp
            )
            if (hidden) {
                Text(
                    "•••• ••••",
                    color = com.zsilent.core.ui.onboarding.NoirText,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        whole,
                        color = com.zsilent.core.ui.onboarding.PastelOrange,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        ".$decimals",
                        color = com.zsilent.core.ui.onboarding.PastelOrange,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "ZKA",
                        color = com.zsilent.core.ui.onboarding.PastelOrange,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (hidden) "≈ •••• USD" else "≈ \$— USD",
                    color = com.zsilent.core.ui.onboarding.NoirTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!hidden) {
                    Text(
                        "fiat price feed coming soon",
                        color = com.zsilent.core.ui.onboarding.NoirTextSubtle,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
    } // close Surface
}

/** Black rounded button with white icon — used for wallet action buttons. */
@Composable
private fun BlackIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(34.dp)
            // Fondo y tinte del tema. Antes el tinte era Color.White fijo sobre
            // surfaceContainerLowest, que en tema claro es #FFFFFF: los tres
            // botones de la tarjeta de balance quedaban blanco sobre blanco,
            // invisibles.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
    }
}


/**
 * Historial como tabla, siguiendo el prototipo.
 *
 * **Las columnas no son las del prototipo, y es a proposito.** Alli la tabla
 * muestra KIND, COUNTERPARTY y AMOUNT, pero esas tres no existen en
 * `WalletHistoryEntry`: solo hay hash, estado, hora, salidas y las banderas
 * `amountHidden` / `feeHidden`. El prototipo puede mostrarlas porque son datos
 * inventados.
 *
 * Reproducirlas aca significaria fabricar valores o exponer justo lo que el
 * protocolo oculta — la contraparte es precisamente lo que protege la
 * inenlazabilidad. Asi que la columna de monto dice "private" en vez de un
 * numero: es la verdad, y ademas es la promesa del producto.
 */
@Composable
private fun WalletHistoryTable(ui: ZSilentUiState) {
    val scheme = MaterialTheme.colorScheme
    val txs = ui.walletHistory.transactions
    Panel(contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(ZTheme.spacing.panel)) {
            PanelHeader(
                title = "Transaction history",
                subtitle = "Wallet-local view — private by construction",
                right = {
                    Pill(
                        if (ui.walletLocked) "locked" else "${txs.size} entries",
                        if (ui.walletLocked) ZTone.Warn else ZTone.Default
                    )
                }
            )
        }
        when {
            ui.walletLocked -> Box(Modifier.padding(ZTheme.spacing.panel)) {
                EmptyState("Wallet locked", sub = "Unlock the wallet to load history.")
            }
            txs.isEmpty() -> Box(Modifier.padding(ZTheme.spacing.panel)) {
                EmptyState("No transactions yet", sub = "Receive ZKA or start mining to see activity.")
            }
            else -> Column {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = ZTheme.spacing.panel, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("HASH" to 3f, "OUTPUTS" to 1f, "AMOUNT" to 1.4f, "STATUS" to 1.4f, "TIME" to 1.4f)
                        .forEach { (label, w) ->
                            Text(
                                label,
                                modifier = Modifier.weight(w),
                                style = ZTheme.text.fieldLabel,
                                color = scheme.onSurfaceVariant
                            )
                        }
                }
                HorizontalDivider(color = scheme.outlineVariant)
                txs.take(12).forEach { tx ->
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = ZTheme.spacing.panel, vertical = ZTheme.spacing.row),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Truncate(
                            tx.txHash, head = 12, tail = 6,
                            modifier = Modifier.weight(3f),
                            color = scheme.onSurface
                        )
                        Text(
                            tx.outputs.toString(),
                            modifier = Modifier.weight(1f),
                            style = ZTheme.text.mono,
                            color = scheme.onSurfaceVariant
                        )
                        Text(
                            if (tx.amountHidden) "private" else "visible",
                            modifier = Modifier.weight(1.4f),
                            style = ZTheme.text.mono,
                            color = scheme.onSurfaceVariant
                        )
                        Box(Modifier.weight(1.4f)) {
                            Pill(
                                tx.status.ifBlank { "unknown" },
                                when (tx.status.lowercase()) {
                                    "confirmed", "finalized" -> ZTone.On
                                    "pending" -> ZTone.Warn
                                    "failed" -> ZTone.Err
                                    else -> ZTone.Default
                                }
                            )
                        }
                        Text(
                            if (tx.timestamp > 0L) formatLogClock(tx.timestamp * 1000L) else "—",
                            modifier = Modifier.weight(1.4f),
                            style = ZTheme.text.mono,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun WalletBreakdownRow(ui: ZSilentUiState, activeWallet: LocalWallet?) {
    val balance       = ui.walletBalance
    val spendableAtoms = balance?.balanceAtoms     ?: activeWallet?.balanceAtoms     ?: 0L
    val maturingAtoms  = balance?.lockedBalanceAtoms ?: activeWallet?.lockedBalanceAtoms ?: 0L
    val totalAtoms     = balance?.totalBalanceAtoms  ?: activeWallet?.totalBalanceAtoms  ?: 0L
    val pendingCount   = ui.pendingPrivateTransfers.count
    val txCount        = activeWallet?.txCount ?: ui.walletHistory.count
    val hidden         = ui.walletLocked || ui.balancesHidden

    // Sparkline histories — reset when wallet changes, update on value change
    var spendableHistory by remember(activeWallet?.address) { mutableStateOf(seedMetricHistory(spendableAtoms.toDouble())) }
    var maturingHistory  by remember(activeWallet?.address) { mutableStateOf(seedMetricHistory(maturingAtoms.toDouble())) }
    var totalHistory     by remember(activeWallet?.address) { mutableStateOf(seedMetricHistory(totalAtoms.toDouble())) }
    LaunchedEffect(spendableAtoms) { spendableHistory = appendMetricSample(spendableHistory, spendableAtoms.toDouble()) }
    LaunchedEffect(maturingAtoms)  { maturingHistory  = appendMetricSample(maturingHistory,  maturingAtoms.toDouble()) }
    LaunchedEffect(totalAtoms)     { totalHistory     = appendMetricSample(totalHistory,     totalAtoms.toDouble()) }

    // Live "updated X seconds ago" ticker
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ui.lastBalanceRefreshedAt) { while (true) { nowMs = System.currentTimeMillis(); delay(5_000) } }
    val updatedLabel = if (ui.lastBalanceRefreshedAt > 0L) {
        val secs = ((nowMs - ui.lastBalanceRefreshedAt) / 1000).coerceAtLeast(0)
        when { secs < 5 -> "just now"; secs < 60 -> "${secs}s ago"; else -> "${secs / 60}m ago" }
    } else if (!ui.walletLocked) "loading..." else ""

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("BALANCE BREAKDOWN", color = com.zsilent.core.ui.onboarding.NoirTextSubtle, fontSize = 10.sp, letterSpacing = 1.8.sp)
            if (updatedLabel.isNotBlank()) {
                Text(
                    if (ui.walletLocked) "LOCKED" else "AUTO · $updatedLabel",
                    color = if (ui.walletLocked) com.zsilent.core.ui.onboarding.NoirTextSubtle
                            else com.zsilent.core.ui.onboarding.NoirSuccess,
                    fontSize = 10.sp, letterSpacing = 1.4.sp
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label    = "Spendable",
                value    = if (hidden) "••••" else formatZkaCompact(spendableAtoms),
                samples  = spendableHistory,
                accent   = com.zsilent.core.ui.onboarding.PastelGreen,
                detail   = "available now",
                hidden   = hidden,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label    = "Maturing",
                value    = if (hidden) "••••" else formatZkaCompact(maturingAtoms),
                samples  = maturingHistory,
                accent   = com.zsilent.core.ui.onboarding.PastelOrange,
                detail   = if (maturingAtoms > 0L) "coinbase locked" else "none locking",
                hidden   = hidden,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label    = "Pending",
                value    = if (pendingCount > 0) "$pendingCount tx" else "—",
                samples  = seedMetricHistory(pendingCount.toDouble()),
                accent   = com.zsilent.core.ui.onboarding.PastelPink,
                detail   = if (pendingCount > 0) "queued · not mined" else "no pending sends",
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label    = "Total",
                value    = if (hidden) "••••" else formatZkaCompact(totalAtoms),
                samples  = totalHistory,
                accent   = com.zsilent.core.ui.onboarding.PastelViolet,
                detail   = "$txCount transactions",
                hidden   = hidden,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BreakdownChip(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .background(com.zsilent.core.ui.onboarding.NoirCardGradient, shape)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, com.zsilent.core.ui.onboarding.NoirOutline),
                shape
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label.uppercase(), color = com.zsilent.core.ui.onboarding.NoirTextSubtle, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text(value, color = com.zsilent.core.ui.onboarding.NoirText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        Text(detail, color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun WalletsListSection(
    ui: ZSilentUiState,
    onSelect: (String) -> Unit,
    onAddWallet: () -> Unit
) {
    com.zsilent.core.ui.onboarding.NoirSection(
        title = "Accounts",
        meta = "${ui.wallets.size} found"
    ) {
        ui.wallets.forEach { wallet ->
            val active = wallet.address == ui.activeWalletAddress
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (active) com.zsilent.core.ui.onboarding.NoirSurfaceElev
                        else androidx.compose.ui.graphics.Color.Transparent,
                        shape
                    )
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) com.zsilent.core.ui.onboarding.NoirAccent
                            else com.zsilent.core.ui.onboarding.NoirOutline
                        ),
                        shape
                    )
                    .clickable { onSelect(wallet.address) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        wallet.name,
                        color = com.zsilent.core.ui.onboarding.NoirText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        truncateAddressForHero(wallet.address),
                        color = com.zsilent.core.ui.onboarding.NoirTextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (active) {
                        Text(
                            "ACTIVE",
                            color = com.zsilent.core.ui.onboarding.NoirSuccess,
                            fontSize = 10.sp,
                            letterSpacing = 1.4.sp
                        )
                    }
                    CopyIconButton(wallet.address)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            NoirIconButton(
                icon = com.zsilent.core.ui.onboarding.BootstrapIcons.PatchPlus,
                contentDescription = "Add wallet",
                onClick = onAddWallet
            )
        }
    }
}

@Composable
private fun WalletErrorRow(message: String) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.zsilent.core.ui.onboarding.NoirSurfaceElev, shape)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, com.zsilent.core.ui.onboarding.NoirError.copy(alpha = 0.5f)),
                shape
            )
            .padding(12.dp)
    ) {
        Text(message, color = com.zsilent.core.ui.onboarding.NoirError, fontSize = 12.sp)
    }
}

/** Returns `("12,096", "000000")` for atoms = 12_096_000_000. */
private fun splitZkaForDisplay(atoms: Long): Pair<String, String> {
    if (atoms <= 0L) return "0" to "000000"
    val whole = atoms / 1_000_000L
    val frac = (atoms % 1_000_000L).toString().padStart(6, '0')
    val wholeFormatted = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return wholeFormatted to frac
}

/** Compact ZKA display: trims trailing zero decimals, keeps comma grouping. */
private fun formatZkaCompact(atoms: Long): String {
    if (atoms <= 0L) return "0 ZKA"
    val (whole, frac) = splitZkaForDisplay(atoms)
    val trimmed = frac.trimEnd('0').ifBlank { "" }
    return if (trimmed.isEmpty()) "$whole ZKA" else "$whole.$trimmed ZKA"
}

/** "zka1Dv...mWhu" — keeps first 8 + last 6 chars. */
private fun truncateAddressForHero(addr: String): String {
    if (addr.length <= 18) return addr
    return "${addr.take(8)}...${addr.takeLast(6)}"
}

@Composable
fun SendScreen(ui: ZSilentUiState, appState: ZSilentAppState, embedded: Boolean = false) {
    var fromWallet  by remember { mutableStateOf("") }
    var address     by remember { mutableStateOf("") }
    var amount      by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var reviewOpen  by remember { mutableStateOf(false) }
    val recipientLooksValid = address.startsWith("zka1")
    val amountLooksValid    = amount.toDoubleOrNull()?.let { it > 0.0 } == true
    val selectedWallet      = ui.wallets.firstOrNull { it.address == fromWallet }
    val fromLooksValid      = ui.wallets.any { it.address == fromWallet }

    LaunchedEffect(ui.wallets, ui.activeWalletAddress) {
        if (fromWallet.isBlank() || ui.wallets.none { it.address == fromWallet }) {
            fromWallet = ui.wallets.firstOrNull { it.address == ui.activeWalletAddress && it.balanceAtoms > 0 }?.address
                ?: ui.wallets.firstOrNull { it.balanceAtoms > 0 }?.address
                ?: ui.wallets.firstOrNull { it.address == ui.activeWalletAddress }?.address
                ?: ui.wallets.firstOrNull()?.address
                ?: ui.activeWalletAddress
        }
    }

    EmbeddableShell(embedded, "Send", "Private zero-knowledge transfer.") {
        when {
            // ── SUCCESS ────────────────────────────────────────────────────
            ui.sendResult != null -> {
                SendSuccessPanel(
                    result = ui.sendResult!!,
                    ui = ui,
                    onNewTransfer = {
                        address = ""; amount = ""; password = ""; reviewOpen = false
                        appState.clearSendResult()
                    }
                )

                // Recent activity after successful send
                if (ui.walletHistory.transactions.isNotEmpty()) {
                    com.zsilent.core.ui.onboarding.NoirSection(
                        title = "Recent activity",
                        meta = "last ${ui.walletHistory.transactions.size.coerceAtMost(5)}"
                    ) {
                        ui.walletHistory.transactions.take(5).forEach { tx ->
                            SendActivityRow(tx)
                        }
                    }
                }
            }

            // ── SENDING (busy) ─────────────────────────────────────────────
            ui.sendBusy -> {
                SendingPanel()
            }

            // ── REVIEW ────────────────────────────────────────────────────
            reviewOpen -> {
                val fee = ui.sendFeeEstimate
                com.zsilent.core.ui.onboarding.NoirSection(title = "Review transfer", meta = "private · shielded") {
                    com.zsilent.core.ui.onboarding.NoirKeyValue("From", selectedWallet?.name ?: "selected wallet")
                    com.zsilent.core.ui.onboarding.NoirKeyValue("Amount", fee?.amount ?: "$amount ZKA", mono = true)
                    com.zsilent.core.ui.onboarding.NoirKeyValue("Fee", fee?.fee ?: "estimated on sign", mono = true)
                    com.zsilent.core.ui.onboarding.NoirKeyValue("Recipient", "${address.take(16)}…${address.takeLast(8)}", mono = true)
                    com.zsilent.core.ui.onboarding.NoirKeyValue("Privacy", "zero-knowledge · keys never leave device")
                }
                ui.sendError?.let { ErrorText(it) }
                PasswordField(password, { password = it }, "Wallet password to sign", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        colors = zOutlinedButtonColors(), shape = MaterialTheme.shapes.small,
                        contentPadding = SettingsButtonPadding,
                        onClick = { reviewOpen = false; password = "" }, enabled = !ui.sendBusy
                    ) { Text("Back") }
                    Button(
                        onClick = {
                            appState.sendPrivateFunds(fromWallet, address, amount, password.toCharArray(), fee?.fee)
                            password = ""
                        },
                        enabled = password.isNotBlank() && fee != null && !ui.sendBusy,
                        // The one essential action of the Send flow gets a pastel
                        // highlight — every other button on this screen stays neutral.
                        colors = zPastelButtonColors(com.zsilent.core.ui.onboarding.PastelGreen),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = SettingsButtonPadding
                    ) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign and send")
                    }
                }
            }

            // ── FORM ──────────────────────────────────────────────────────
            else -> {
                // Source wallet selector
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ui.wallets.forEach { wallet ->
                        val active = wallet.address == fromWallet
                        val shape  = RoundedCornerShape(12.dp)
                        val accent = com.zsilent.core.ui.onboarding.PastelGreen
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (active) com.zsilent.core.ui.onboarding.NoirCardGradientElev
                                    else com.zsilent.core.ui.onboarding.NoirCardGradient,
                                    shape
                                )
                                .border(
                                    BorderStroke(1.dp,
                                        if (active) accent else com.zsilent.core.ui.onboarding.NoirOutline),
                                    shape
                                )
                                .clickable { fromWallet = wallet.address; appState.selectWallet(wallet.address) }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(wallet.name,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                color = if (active) accent else com.zsilent.core.ui.onboarding.NoirText)
                            Text(wallet.balance + " ZKA",
                                fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                color = if (active) accent else com.zsilent.core.ui.onboarding.NoirTextMuted)
                            if (active) Text("SELECTED", color = accent, fontSize = 10.sp, letterSpacing = 1.4.sp)
                        }
                    }
                }

                // Recipient + Amount — white card with paste/max/icons
                val clipboardMgr = androidx.compose.ui.platform.LocalClipboardManager.current
                com.zsilent.core.ui.onboarding.NoirSection(title = "Transfer details", meta = "zk-private") {
                    // Recipient address with paste + check icon
                    OutlinedTextField(
                        value = address, onValueChange = { address = it.trim() },
                        label = { Text("Recipient address") },
                        placeholder = { Text("zka1…", color = com.zsilent.core.ui.onboarding.NoirTextSubtle) },
                        isError = address.isNotBlank() && !recipientLooksValid,
                        colors = zFieldColors(), shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.AccountBalanceWallet, null, tint = com.zsilent.core.ui.onboarding.NoirTextSubtle, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (recipientLooksValid) {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = com.zsilent.core.ui.onboarding.NoirSuccess, modifier = Modifier.size(16.dp))
                                } else {
                                    // Paste button with icon
                                    OutlinedButton(
                                        onClick = { address = clipboardMgr.getText()?.text?.trim() ?: address },
                                        colors = zOutlinedButtonColors(), shape = MaterialTheme.shapes.small,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Paste", fontSize = 11.sp)
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                    )
                    // Amount with MAX button + currency icon
                    val maxBalance = selectedWallet?.balance ?: "0"
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it.trim() },
                        label = { Text("Amount ZKA") },
                        isError = amount.isNotBlank() && !amountLooksValid,
                        colors = zFieldColors(), shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.CurrencyExchange, null, tint = com.zsilent.core.ui.onboarding.NoirTextSubtle, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { amount = maxBalance },
                                    colors = zOutlinedButtonColors(), shape = MaterialTheme.shapes.small,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Outlined.KeyboardDoubleArrowUp, null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("MAX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                    )
                    ui.sendFeeEstimate?.let { fee ->
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Estimated fee", color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 12.sp)
                            Text(fee.fee, color = com.zsilent.core.ui.onboarding.NoirText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    ui.sendError?.let { ErrorText(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            colors = zOutlinedButtonColors(), shape = MaterialTheme.shapes.small,
                            contentPadding = SettingsButtonPadding,
                            onClick = { appState.estimateSendFee(amount) },
                            enabled = amountLooksValid && !ui.sendBusy
                        ) { Text("Estimate fee") }
                        Button(
                            onClick = { reviewOpen = true; appState.estimateSendFee(amount) },
                            enabled = recipientLooksValid && amountLooksValid && fromLooksValid && !ui.sendBusy,
                            colors = zButtonColors(), shape = MaterialTheme.shapes.small,
                            contentPadding = SettingsButtonPadding
                        ) {
                            Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Review →")
                        }
                    }
                }

                // Recent activity
                if (!ui.walletLocked && ui.walletHistory.transactions.isNotEmpty()) {
                    com.zsilent.core.ui.onboarding.NoirSection(
                        title = "Recent activity",
                        meta = "last ${ui.walletHistory.transactions.size.coerceAtMost(5)}"
                    ) {
                        ui.walletHistory.transactions.take(5).forEach { tx -> SendActivityRow(tx) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SendSuccessPanel(
    result: com.zsilent.core.models.SendResult,
    ui: ZSilentUiState,
    onNewTransfer: () -> Unit
) {
    val checkScale = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(120)
        checkScale.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Animated check circle
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(ZTheme.semantics.success.copy(alpha = 0.12f), CircleShape)
                .border(BorderStroke(2.dp, ZTheme.semantics.success.copy(alpha = 0.4f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ZTheme.semantics.success, CircleShape)
                    .graphicsLayer(scaleX = checkScale.value, scaleY = checkScale.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(40.dp))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Transfer sent!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ZTheme.semantics.success)
            Text("Private · Zero-knowledge · Broadcasting to network",
                color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 12.sp)
        }

        // TX hashes
        com.zsilent.core.ui.onboarding.NoirSection(title = "Transaction", meta = "${result.splitCount} tx") {
            result.transactionHashes.forEachIndexed { i, hash ->
                val status = singleTransferStatus(ui, hash)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TX ${i + 1}  $status", color = com.zsilent.core.ui.onboarding.NoirTextSubtle, fontSize = 10.sp, letterSpacing = 1.2.sp)
                        Text(hash, color = com.zsilent.core.ui.onboarding.NoirText, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CopyIconButton(hash)
                        // Explorer link
                        OutlinedButton(
                            colors = zOutlinedButtonColors(), shape = MaterialTheme.shapes.small,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            onClick = {
                                runCatching {
                                    java.awt.Desktop.getDesktop().browse(java.net.URI("https://www.zokascan.cc/tx/$hash"))
                                }
                            }
                        ) { Text("Explorer ↗", fontSize = 11.sp) }
                    }
                }
            }
            ThinDivider()
            com.zsilent.core.ui.onboarding.NoirKeyValue("To", "${result.to.take(14)}…${result.to.takeLast(8)}", mono = true)
        }

        Button(
            onClick = onNewTransfer,
            colors = zButtonColors(), shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) { Text("New transfer") }
    }
}

@Composable
private fun SendingPanel() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Text("Signing transaction…", style = MaterialTheme.typography.titleMedium, color = com.zsilent.core.ui.onboarding.NoirText)
        Text("Building zero-knowledge proof on device. This may take 20-30 seconds.",
            color = com.zsilent.core.ui.onboarding.NoirTextMuted, fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SendActivityRow(tx: com.zsilent.core.models.WalletHistoryEntry) {
    val statusColor = when (tx.status.lowercase()) {
        "confirmed", "finalized" -> ZTheme.semantics.success
        "pending"                -> ZTheme.semantics.warning
        "failed"                 -> ZTheme.semantics.danger
        else                     -> com.zsilent.core.ui.onboarding.NoirTextMuted
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(statusColor, CircleShape))
            Column {
                Text(shortHash(tx.txHash), color = com.zsilent.core.ui.onboarding.NoirText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(tx.status, color = statusColor, fontSize = 10.sp, letterSpacing = 0.8.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CopyIconButton(tx.txHash, enabled = tx.txHash.isNotBlank())
            OutlinedButton(
                colors = zOutlinedButtonColors(), shape = MaterialTheme.shapes.small,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                onClick = { runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI("https://www.zokascan.cc/tx/${tx.txHash}")) } }
            ) { Text("↗", fontSize = 11.sp) }
        }
    }
}

@Composable
fun ReceiveScreen(ui: ZSilentUiState, appState: ZSilentAppState, embedded: Boolean = false) {
    EmbeddableShell(embedded, "Receive", "Show a receive address and QR code.") {
        Panel(title = "Wallet selector", meta = "${ui.wallets.size} accounts") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.wallets.forEach { wallet ->
                    OutlinedButton(colors = zOutlinedButtonColors(), onClick = { appState.selectWallet(wallet.address) }) {
                        Text("${wallet.name}${if (wallet.address == ui.activeWalletAddress) " / active" else ""}")
                    }
                }
                OutlinedButton(colors = zOutlinedButtonColors(), onClick = { appState.refreshWallets() }, enabled = !ui.walletBusy) { Text("Rescan") }
            }
        }
        Panel(title = "Receive address", meta = "qr") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                QrCode(ui.activeWalletAddress)
                Column(
                    modifier = Modifier.widthIn(max = 680.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(ui.activeWalletName.ifBlank { "No wallet selected" }, style = MaterialTheme.typography.titleLarge)
                    SelectionContainer {
                        MonoText(
                            ui.activeWalletAddress.ifBlank { "Create or select a wallet to receive ZKA." },
                            maxLines = 4
                        )
                    }
                    CopyIconButton(ui.activeWalletAddress)
                    ThinDivider()
                    KeyValueRow("Privacy", "receive address only")
                    KeyValueRow("Keys", "never exported")
                }
            }
        }
    }
}

@Composable
fun NodeScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var nodeProgressStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    val nodeProgressActive = ui.nodeStatus == ServiceStatus.Starting ||
        (ui.nodeStatus == ServiceStatus.Online && (!ui.localRpcOnline || !ui.networkSynced))
    val miningOwnsLocalState = ui.mining.running &&
        ui.nodeStatus != ServiceStatus.Online
    val nodeChangingState = ui.nodeStatus == ServiceStatus.Starting || ui.nodeStatus == ServiceStatus.Stopping
    val nodeFullyOn = ui.nodeStatus == ServiceStatus.Online
    val nodeWarming = ui.nodeStatus == ServiceStatus.Starting ||
        ui.nodeStatus == ServiceStatus.Stopping
    val nodeOn = nodeFullyOn || nodeWarming
    val canStartNode = ui.sessionUnlocked &&
        ui.nodeStatus != ServiceStatus.Online &&
        !nodeChangingState &&
        !miningOwnsLocalState
    val canStopNode = ui.nodeStatus == ServiceStatus.Online || ui.nodeStatus == ServiceStatus.Starting
    val nodeSwitchEnabled = if (nodeOn) canStopNode else canStartNode
    val dotOn = ZTheme.semantics.success
    val dotWarming = ZTheme.semantics.warning
    val dotOff = MaterialTheme.colorScheme.onSurfaceVariant
    val nodeDotColor = when {
        nodeFullyOn -> dotOn
        nodeWarming -> dotWarming
        else -> dotOff
    }
    val nodeStateLabel = when {
        nodeFullyOn -> "ON"
        nodeWarming -> "WARMING"
        else -> "OFF"
    }
    LaunchedEffect(nodeProgressActive) {
        if (nodeProgressActive) {
            nodeProgressStartedAt = System.currentTimeMillis()
        }
    }

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Node",
        title = "Zoka core runtime",
        subtitle = "Operate your local node — the source of truth for balances, mining and privacy."
    ) {
        // Estado explicito del nodo. El prototipo solo distingue ONLINE/OFFLINE;
        // aca hay un cuarto caso que la app ya conocia y no mostraba: el nodo
        // esta Online pero su RPC todavia no responde o la cadena no alcanzo la
        // punta. Eso no es "encendido" ni es "apagado" — es warming, y es
        // justamente cuando uno se pregunta por que no puede minar.
        val nodeReady = nodeFullyOn && ui.localRpcOnline && ui.networkSynced
        val nodeError = ui.nodeStatus == ServiceStatus.Error
        val stateLabel = when {
            nodeError -> "ERROR"
            nodeReady -> "READY"
            nodeFullyOn || nodeWarming -> "WARMING"
            else -> "OFFLINE"
        }
        val stateTone = when {
            nodeError -> ZTone.Err
            nodeReady -> ZTone.On
            nodeFullyOn || nodeWarming -> ZTone.Warn
            else -> ZTone.Off
        }

        Panel(title = "Node controls", meta = ui.nodeStatus.name) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stateLabel,
                        style = ZTheme.text.monoDisplay,
                        color = when {
                            nodeError -> ZTheme.semantics.danger
                            nodeReady -> ZTheme.semantics.success
                            nodeFullyOn || nodeWarming -> ZTheme.semantics.warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("rpc ${if (ui.localRpcOnline) "online" else "waiting"}",
                             if (ui.localRpcOnline) ZTone.On else ZTone.Warn)
                        Pill("chain ${if (ui.networkSynced) "synced" else "syncing"}",
                             if (ui.networkSynced) ZTone.On else ZTone.Warn)
                        Pill("binaries ${if (ui.zokaReady) "ready" else "missing"}",
                             if (ui.zokaReady) ZTone.On else ZTone.Err)
                        Pill("session ${if (ui.sessionUnlocked) "unlocked" else "locked"}",
                             if (ui.sessionUnlocked) ZTone.On else ZTone.Warn)
                    }
                }

                // Botones en lugar del interruptor, pero con la MISMA logica de
                // habilitacion: canStartNode / canStopNode. Un boton que acepta
                // el clic y no hace nada es peor que un interruptor gris.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (nodeOn) {
                        ZButton(
                            onClick = { if (canStopNode) appState.stopNode() },
                            variant = ZButtonVariant.Danger,
                            enabled = canStopNode,
                        ) {
                            Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                            ZButtonText("Stop node")
                        }
                    } else {
                        ZButton(
                            onClick = { if (canStartNode) appState.startNode(autoMine = false) },
                            variant = ZButtonVariant.Accent,
                            enabled = canStartNode,
                        ) {
                            Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                            ZButtonText("Start node")
                        }
                    }
                    // El prototipo pone aca "Reload config", que no existe en
                    // ZSilent. Se usa la accion real equivalente: forzar sync.
                    ZButton(
                        onClick = appState::syncNow,
                        variant = ZButtonVariant.Outline,
                        enabled = ui.localRpcOnline && !ui.syncBusy,
                    ) {
                        Icon(Icons.Outlined.Sync, null, modifier = Modifier.size(14.dp))
                        ZButtonText("Sync now")
                    }
                }
            }

            val nodeHint = when {
                nodeWarming && !nodeFullyOn -> "Node is starting. Wait for RPC and sync warmup (can take up to a minute)."
                nodeReady -> "Node is online and aligned with the network."
                nodeFullyOn -> "Node process is up; waiting for RPC and chain sync before it is fully usable."
                miningOwnsLocalState -> "Mining owns the chain state. Stop mining first to free the node."
                !ui.sessionUnlocked -> "Unlock the wallet session first. ZSilent will not ask for the password again on this panel."
                else -> "Starting the node runs it in node-only mode (no mining)."
            }
            HintText(nodeHint)
            if (miningOwnsLocalState) {
                ErrorText("Mining is already using the chain state. Stop that miner first, or run node + mining from ZSilent as one integrated session.")
            }
        }

        if (nodeProgressActive) {
            RuntimeProgressPanel(
                title = "Node startup",
                phase = if (ui.nodeStatus == ServiceStatus.Starting) "Launching node process" else "Waiting for RPC",
                detail = if (ui.miningSyncReady) "Node chain is close enough to the network head. Mining can be enabled." else "The process is alive; ZSilent is waiting for node RPC, peer warmup and chain sync.",
                startedAtMillis = nodeProgressStartedAt,
                rows = listOf(
                    "Node RPC" to if (ui.localRpcOnline) "online" else "waiting",
                    "P2P peers" to ui.localConnectedPeers.toString(),
                    "Node height" to ui.chainHeight.toString(),
                    "Network height" to ui.networkHeight.toString(),
                    "Remaining" to ui.syncRemainingBlocks.toString(),
                    "Pending queue" to ui.pendingPrivateTransfers.count.toString(),
                    "Mining" to if (ui.mining.running) "active" else "not active"
                )
            )
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Status", ui.nodeStatus.name, if (ui.localRpcOnline) "node RPC online" else "node RPC waiting", Modifier.weight(1f))
            StatusCard("Height", ui.chainHeight.toString(), "node chain tip", Modifier.weight(1f))
            StatusCard("Queue", ui.pendingPrivateTransfers.count.toString(), "private txs waiting", Modifier.weight(1f))
            StatusCard("Difficulty", ui.stats.difficulty.toString(), "network", Modifier.weight(1f))
            // El prototipo muestra ETA junto a las alturas. Se deriva de los
            // bloques que faltan; si ya esta sincronizado no hay nada que
            // estimar y se muestra un guion en vez de un cero enganoso.
            StatusCard(
                "ETA",
                if (ui.networkSynced || ui.syncRemainingBlocks <= 0L) "—"
                else "${maxOf(1L, ui.syncRemainingBlocks / 120L)} min",
                if (ui.networkSynced) "synced" else "to chain tip",
                Modifier.weight(1f)
            )
        }

        // Dos columnas, como el prototipo: a la izquierda lo que necesita
        // ancho -- sincronizacion y tabla de peers --, y a la derecha una
        // columna angosta con todo lo que describe COMO se alcanza este nodo:
        // su RPC, su estado de red, sus endpoints y su bootstrap.
        //
        // No se elimino ningun panel; solo se reacomodaron.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
        ) {
            Column(
                Modifier.weight(1.6f),
                verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                NetworkSyncPanel(ui, onSyncNow = appState::syncNow)
                NodePeerTable(ui)
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                Panel(title = "Zoka node", meta = if (ui.localRpcOnline) "online" else "offline", modifier = Modifier.weight(1f)) {
                    KeyValueRow("RPC", if (ui.localRpcOnline) "online" else "offline")
                    KeyValueRow("RPC address", ZokaNetworkConfig.displayUrl(ui.localRpcUrl))
                    CopyableKeyValueRow("Bootstrap", ui.bootstrapPeer)
                    KeyValueRow("Zoka binaries", if (ui.zokaReady) "ready" else "missing")
                }

                Panel(title = "Network state", meta = ZokaNetworkConfig.Network, modifier = Modifier.weight(1f)) {
                    KeyValueRow("P2P mesh", if (ui.localConnectedPeers > 0) "connected" else "isolated")
                    KeyValueRow("Sync gate", if (ui.miningSyncReady) "mining enabled" else "mining locked")
                    KeyValueRow("Connected peers", ui.localConnectedPeers.toString())
                    KeyValueRow("Your mining", if (ui.mining.running) "on" else "off")
                    KeyValueRow("Difficulty", ui.stats.difficulty.toString())
                    KeyValueRow("Emission", "%.4f ZKA".format(ui.stats.emissionZka))
                    KeyValueRow("Miner state blocks", ui.mining.blocksMined.toString())
                    KeyValueRow("Miner state rewards", "${ui.mining.totalRewards} ZKA")
                    KeyValueRow("Supply emitted", "${ui.mining.totalSupplyEmitted} ZKA")
                    KeyValueRow("Pending queue DB", ui.pendingPrivateTransfers.db.ifBlank { ".zoka/${ZokaNetworkConfig.Network}/blockchain.db" })
                }
            Panel(title = "Seed · bootstrap", meta = "help fresh nodes join") {
                Text(
                    "Share these so a new node can bootstrap directly from you — no open port, no central server. iroh pulls a full snapshot in ~90s; the onion is your reachable p2p address.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (ui.irohEndpointId.isNotBlank()) {
                    CopyableKeyValueRow("iroh EndpointId", ui.irohEndpointId)
                } else {
                    KeyValueRow("iroh EndpointId", if (ui.localRpcOnline) "resolving…" else "start the node to view")
                }
                if (ui.torOnionMultiaddr.isNotBlank()) {
                    CopyableKeyValueRow("Onion address", ui.torOnionMultiaddr)
                } else {
                    KeyValueRow("Onion address", if (ui.onionSeedEnabled) "waiting for Tor…" else "enable the onion seed in Settings")
                }
            }
            }
        }

    }
}


/**
 * Tabla de peers, siguiendo el prototipo.
 *
 * **Faltan dos columnas del prototipo a proposito.** Alli la tabla muestra
 * LATENCY y HEIGHT por peer, pero `PeerInfo` solo tiene `peerId`, `transport` y
 * `address`. El prototipo puede mostrarlas porque son datos inventados;
 * conseguirlas de verdad significa exponerlas desde el RPC del nodo, que es
 * trabajo del lado Rust y no de la interfaz.
 *
 * Se conserva el desglose Tor/directo/total, que el prototipo no tiene y que
 * dice de un vistazo si estas saliendo por onion o en claro.
 */
@Composable
private fun NodePeerTable(ui: ZSilentUiState) {
    val scheme = MaterialTheme.colorScheme
    Panel(contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(ZTheme.spacing.panel), verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
            PanelHeader(
                title = "Peer table",
                subtitle = "Your own node's view — serverless, no central directory",
                right = { Pill("${ui.localConnectedPeers} connected", if (ui.localConnectedPeers > 0) ZTone.On else ZTone.Off) }
            )
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
                StatusCard("Tor", ui.peerTorCount.toString(), "onion", Modifier.weight(1f))
                StatusCard("Direct", ui.peerDirectCount.toString(), "clearnet", Modifier.weight(1f))
                StatusCard("Total", ui.localConnectedPeers.toString(), "p2p mesh", Modifier.weight(1f))
            }
        }
        if (ui.peers.isEmpty()) {
            Box(Modifier.padding(ZTheme.spacing.panel)) {
                EmptyState(
                    if (ui.localRpcOnline) "No connected peers yet" else "Node offline",
                    sub = if (ui.localRpcOnline) "Dialing the seed and the mesh over Tor/iroh."
                          else "Start the node to see connected peers."
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ZTheme.spacing.panel, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("ENDPOINT" to 3f, "TRANSPORT" to 1.2f, "ADDRESS" to 2f).forEach { (label, w) ->
                    Text(label, modifier = Modifier.weight(w), style = ZTheme.text.fieldLabel, color = scheme.onSurfaceVariant)
                }
                Box(Modifier.width(40.dp))
            }
            HorizontalDivider(color = scheme.outlineVariant)
            ui.peers.take(24).forEach { peer ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = ZTheme.spacing.panel, vertical = ZTheme.spacing.row),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Truncate(peer.peerId, head = 14, tail = 6, modifier = Modifier.weight(3f), color = scheme.onSurface)
                    Box(Modifier.weight(1.2f)) {
                        Pill(
                            peer.transport,
                            when (peer.transport.lowercase()) {
                                "tor" -> ZTone.Info
                                "direct" -> ZTone.Default
                                else -> ZTone.Off
                            }
                        )
                    }
                    Text(
                        peer.address ?: "—",
                        modifier = Modifier.weight(2f),
                        style = ZTheme.text.mono,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(Modifier.width(40.dp)) { CopyIconButton(peer.peerId) }
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun MiningScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var miningRequestActive by remember { mutableStateOf(false) }
    var miningProgressStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var threads by remember(ui.mining.availableCpuThreads) {
        mutableStateOf(ui.mining.availableCpuThreads.coerceAtLeast(1).toString())
    }
    val activeWallet = activeLocalWallet(ui)
    val parsedThreads = threads.toIntOrNull()?.coerceAtLeast(1)
    val rewardAddress = activeWallet?.address ?: ui.activeWalletAddress
    val localMiningWallets = ui.wallets.filter { it.address.startsWith("zka1") }
    val effectiveHashrate = maxOf(ui.mining.hashrateHps, ui.stats.reportedHashrateHps)
    val miningFaulted = !ui.miningDiagnostic.isNullOrBlank() || ui.miningStatus == ServiceStatus.Error
    val nodeChangingState = ui.nodeStatus == ServiceStatus.Starting || ui.nodeStatus == ServiceStatus.Stopping
    val integratedMining = ui.nodeStatus == ServiceStatus.Online &&
        (ui.localMiningEnabled || ui.mining.running || ui.miningStatus == ServiceStatus.Starting) &&
        !ui.miningControlledByApp
    val externalMining = ui.mining.running && !ui.miningControlledByApp && ui.nodeStatus != ServiceStatus.Online
    val miningProgressActive = miningRequestActive ||
        ui.miningStatus == ServiceStatus.Starting ||
        (ui.mining.running && effectiveHashrate <= 0L && !miningFaulted)
    val canStart = ui.zokaReady &&
        rewardAddress.startsWith("zka1") &&
        ui.sessionUnlocked &&
        parsedThreads != null &&
        ui.miningSyncReady &&
        ui.miningStatus != ServiceStatus.Starting &&
        !ui.mining.running &&
        // Excluyentes: el nodo no corre las dos a la vez, asi que la interfaz no
        // ofrece arrancar esta mientras la otra manda. Se bloquea en vez de
        // apagar el equipo por su cuenta: parar el worker deja de aportar
        // trabajo a gente que cuenta con vos, y esa no es una decision que
        // deba tomar un boton de otra seccion.
        !ui.teamMiningActive &&
        !nodeChangingState
    val canStopMining = ui.miningControlledByApp || integratedMining || ui.localMiningEnabled
    LaunchedEffect(ui.mining.running, ui.miningStatus, ui.nodeStatus, ui.miningBlockReason, ui.miningDiagnostic) {
        if ((ui.mining.running && effectiveHashrate > 0L) ||
            ui.miningStatus == ServiceStatus.Error ||
            ui.nodeStatus == ServiceStatus.Error ||
            ui.miningBlockReason != null ||
            ui.miningDiagnostic != null
        ) {
            miningRequestActive = false
        }
    }

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Mining",
        title = "Solo mining",
        subtitle = "Contribute hashrate directly to the network. All rewards land in your local wallet."
    ) {
        // El panel "Mining controls" se fundio en el de hashrate en vivo.

        // Mismo cartel que Node y Mining Pool: una palabra grande que dice de
        // una si esto esta trabajando o no. PAUSED es su propio estado y no un
        // OFFLINE: la seccion esta apagada porque el usuario eligio el equipo,
        // no porque algo haya fallado, y mezclar las dos cosas manda a buscar
        // un problema que no existe.
        SoloMiningStateBanner(
            ui = ui,
            hashrate = effectiveHashrate,
            faulted = miningFaulted,
            canStart = canStart,
            canStop = canStopMining,
            threads = parsedThreads,
            onStart = { parsedThreads?.let { t -> miningRequestActive = true; appState.startMining(t) } },
            onStop = { appState.stopMining() },
        )

        if (miningProgressActive && !miningFaulted) {
            RuntimeProgressPanel(
                title = "Mining warmup",
                phase = when {
                    ui.nodeStatus == ServiceStatus.Starting -> "Starting integrated node"
                    ui.miningStatus == ServiceStatus.Starting -> "Launching miner"
                    ui.mining.running && effectiveHashrate <= 0L -> "Benchmarking hashrate"
                    else -> "Waiting for first mining snapshot"
                },
                detail = "This can take a little while while the node opens the chain DB, warms sync and initializes PoW.",
                startedAtMillis = miningProgressStartedAt,
                rows = listOf(
                    "Node" to ui.nodeStatus.name,
                    "Sync" to if (ui.networkSynced) "OK synced" else "${ui.syncRemainingBlocks} block(s) remaining",
                    "Hashrate" to formatHashrate(effectiveHashrate),
                    "Height" to ui.mining.chainHeight.toString(),
                    "Threads" to (parsedThreads ?: ui.mining.availableCpuThreads.coerceAtLeast(1)).toString()
                )
            )
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard(
                "Status",
                // Con el equipo activo esta seccion esta apagada por decision del
                // usuario, no por un fallo. Decir OFFLINE a secas invitaria a
                // buscar un problema donde no lo hay.
                if (ui.teamMiningActive) "Paused" else ui.miningStatus.name,
                when {
                    ui.teamMiningActive -> "team mining is on"
                    !ui.miningSyncReady -> "sync locked"
                    miningFaulted -> "check logs"
                    ui.miningControlledByApp -> "ZSilent miner"
                    ui.localMiningEnabled || ui.mining.running -> "node or external miner"
                    else -> "idle"
                },
                Modifier.weight(1f)
            )
            StatusCard("Hashrate", formatHashrate(effectiveHashrate), ui.mining.algorithm.ifBlank { "waiting" }, Modifier.weight(1f))
            StatusCard("Peers", ui.localConnectedPeers.toString(), "connected via p2p", Modifier.weight(1f))
            StatusCard("Height", ui.chainHeight.toString(), "network ${ui.networkHeight}", Modifier.weight(1f))
        }

        // Dos columnas como el prototipo: hashrate en vivo a la izquierda con
        // sus estadisticas y sus controles, recompensas a la derecha.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
            MiningLivePanel(
                ui = ui,
                hashrate = effectiveHashrate,
                running = ui.mining.running || ui.localMiningEnabled,
                threads = parsedThreads ?: 1,
                canStart = canStart,
                canStop = canStopMining,
                onThreads = { threads = it.toString() },
                onStart = { parsedThreads?.let { t -> miningRequestActive = true; appState.startMining(t) } },
                onStop = { appState.stopMining() },
                hint = when {
                    // Primero que todo lo demas: si el equipo manda, cualquier
                    // otro motivo es secundario y confunde.
                    ui.teamMiningActive ->
                        "Team mining is active in Mining Pool. Solo and team mining cannot run at " +
                            "once on this node — stop the team worker there to mine solo."
                    externalMining -> "Mining is running outside ZSilent."
                    ui.mining.running && effectiveHashrate <= 0L -> "Mining is running. Measuring hashrate; the first sample lands after the first PoW solve."
                    ui.mining.running -> "Mining is running alongside the node."
                    !ui.sessionUnlocked -> "Unlock the wallet session first."
                    ui.nodeStatus != ServiceStatus.Online -> "Start the node before mining."
                    !ui.miningSyncReady -> "Chain is still syncing. Mining unlocks once the node reaches the network head."
                    !rewardAddress.startsWith("zka1") -> "Select a wallet with a full zka1 address before mining."
                    else -> "Starting the miner runs it alongside the node."
                },
                blockReason = ui.miningBlockReason,
                diagnostic = ui.miningDiagnostic,
                externalMining = externalMining,
                modifier = Modifier.weight(2f),
            )
            MiningRewardsPanel(ui = ui, rewardAddress = rewardAddress, modifier = Modifier.weight(1f))
        }

        MiningPreflightPanel(
            sessionUnlocked = ui.sessionUnlocked,
            nodeOnline = ui.nodeStatus == ServiceStatus.Online,
            syncReady = ui.miningSyncReady,
            rewardAddressOk = rewardAddress.startsWith("zka1"),
            threadsOk = (parsedThreads ?: 0) >= 1,
            binariesOk = ui.zokaReady,
        )

        // Network sync va aca abajo y a todo el ancho: es contexto de red,
        // no un control de mineria, asi que no compite con el grafico ni
        // con el pre-flight por la atencion de arriba.
        NetworkSyncPanel(ui, onSyncNow = appState::syncNow)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(title = "Reward target", meta = "address only", modifier = Modifier.weight(1f)) {
                CopyableKeyValueRow("Wallet", activeWallet?.name ?: ui.activeWalletName.ifBlank { "not selected" })
                CopyableKeyValueRow("Address", activeWallet?.address ?: rewardAddress.ifBlank { "not selected" })
                KeyValueRow("Available CPU", ui.mining.availableCpuThreads.coerceAtLeast(1).toString())
                KeyValueRow("Mode", ui.mining.mode.ifBlank { "pow-${ZokaNetworkConfig.Network}" })
                KeyValueRow("Connected peers", ui.localConnectedPeers.toString())
                KeyValueRow("Reported network hashrate", formatHashrate(maxOf(ui.stats.reportedHashrateHps, effectiveHashrate)))
                if (!rewardAddress.startsWith("zka1")) {
                    ErrorText("Select a wallet with a full zka1 address before mining.")
                }
                if (ui.mining.legacyPowSimDetected) {
                    ErrorText("Legacy mining state detected. Reset the old pow-sim state before mining.")
                }
            }

            Panel(title = "Active wallet funds", meta = "personal", modifier = Modifier.weight(1f)) {
                KeyValueRow("Available", activeWalletAvailable(ui, activeWallet))
                KeyValueRow("Locked", activeWalletLocked(ui, activeWallet))
                KeyValueRow("Total", activeWalletTotal(ui, activeWallet))
                KeyValueRow("Transactions", activeWallet?.txCount?.toString() ?: ui.walletHistory.count.toString())
                KeyValueRow("Maturity", "${ui.mining.coinbaseMaturityBlocks} blocks")
            }
        }

        Panel(title = "Mining accounts", meta = "${localMiningWallets.size} eligible") {
            if (localMiningWallets.isEmpty()) {
                EmptyState("No wallet addresses found.")
            } else {
                localMiningWallets.take(6).forEach { wallet ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(wallet.name, modifier = Modifier.weight(0.8f))
                        MonoText("available ${privateBalanceLabel(ui, null, wallet.balance)}", modifier = Modifier.weight(1f))
                        MonoText("locked ${privateBalanceLabel(ui, null, wallet.lockedBalance)}", modifier = Modifier.weight(1f))
                        CopyIconButton(wallet.address)
                    }
                }
            }
        }

        Panel(title = "Node pending transfers", meta = "${ui.pendingPrivateTransfers.count} queued") {
            KeyValueRow("Queue", ui.pendingPrivateTransfers.count.toString())
            KeyValueRow("Node DB", ui.pendingPrivateTransfers.db.ifBlank { ".zoka/${ZokaNetworkConfig.Network}/blockchain.db" })
            if (ui.pendingPrivateTransfers.pendingPrivateTxHashes.isEmpty()) {
                EmptyState("No transfers waiting for mining.")
            } else {
                ui.pendingPrivateTransfers.pendingPrivateTxHashes.take(8).forEachIndexed { index, hash ->
                    CopyableKeyValueRow("Tx ${index + 1}", hash)
                }
            }
        }
    }
}


/**
 * Hashrate en vivo con sus estadisticas y sus controles, como el prototipo.
 *
 * Las barras van en el azul de la paleta de metricas: es el unico color de la
 * pantalla, y con el minero parado bajan al 25% de opacidad en vez de
 * desaparecer, para que se lea "apagado" y no "colgado".
 *
 * Los controles viven aca y no en un panel aparte: arrancar el minero y elegir
 * hilos son decisiones que se toman mirando el grafico.
 */
/**
 * Cartel de estado del minado solo, hermano del de Node y del de Mining Pool.
 *
 * `MINING` sale del hashrate real, no de que el usuario haya pulsado el boton:
 * el nodo puede aceptar la orden y tardar en dar la primera muestra, y decir
 * que mina antes de que mine es justamente lo que confundia.
 */
@Composable
private fun SoloMiningStateBanner(
    ui: ZSilentUiState,
    hashrate: Long,
    faulted: Boolean,
    canStart: Boolean,
    canStop: Boolean,
    threads: Int?,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val running = ui.mining.running || ui.localMiningEnabled
    val label = when {
        faulted || ui.miningStatus == ServiceStatus.Error -> "ERROR"
        ui.teamMiningActive -> "PAUSED"
        running && hashrate > 0L -> "MINING"
        running || ui.miningStatus == ServiceStatus.Starting -> "WARMING"
        else -> "OFFLINE"
    }
    val tint = when (label) {
        "MINING" -> ZTheme.semantics.success
        "WARMING" -> ZTheme.semantics.warning
        "ERROR" -> ZTheme.semantics.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Panel(title = "Solo mining", meta = label.lowercase()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = ZTheme.text.monoDisplay, color = tint)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        "chain ${if (ui.miningSyncReady) "synced" else "syncing"}",
                        if (ui.miningSyncReady) ZTone.On else ZTone.Warn
                    )
                    Pill(
                        "session ${if (ui.sessionUnlocked) "unlocked" else "locked"}",
                        if (ui.sessionUnlocked) ZTone.On else ZTone.Warn
                    )
                    Pill(
                        if (ui.teamMiningActive) "team mining on" else "team mining off",
                        if (ui.teamMiningActive) ZTone.Warn else ZTone.Off
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (running) {
                    ZButton(onClick = onStop, variant = ZButtonVariant.Danger, enabled = canStop) {
                        Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                        ZButtonText("Stop miner")
                    }
                } else {
                    ZButton(onClick = onStart, variant = ZButtonVariant.Accent, enabled = canStart) {
                        Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                        ZButtonText("Start miner")
                    }
                }
            }
        }
        HintText(
            when (label) {
                "ERROR" -> "The miner reported a problem. Check Logs for the reason."
                "PAUSED" ->
                    "Team mining is running in Mining Pool. Solo and team mining cannot run at " +
                        "once on this node — stop the team worker there to mine solo."
                "MINING" -> "Mining with ${threads ?: 1} thread(s). Rewards land in your local wallet."
                "WARMING" -> "The miner is up; the first hashrate sample lands after the first PoW solve."
                else -> "Idle. Starting the miner runs it alongside the node."
            }
        )
    }
}

@Composable
private fun MiningLivePanel(
    ui: ZSilentUiState,
    hashrate: Long,
    running: Boolean,
    threads: Int,
    canStart: Boolean,
    canStop: Boolean,
    onThreads: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    hint: String,
    blockReason: String?,
    diagnostic: String?,
    externalMining: Boolean,
    modifier: Modifier = Modifier,
) {
    val current = rememberUpdatedState(hashrate)
    val live = rememberUpdatedState(running)
    val samples = remember { mutableStateListOf<Long>().apply { repeat(30) { add(0L) } } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            samples.removeAt(0)
            samples.add(if (live.value) current.value else 0L)
        }
    }
    val max = maxOf(1L, samples.maxOrNull() ?: 1L)
    val bar = ZTheme.metrics.live
    val maxThreads = ui.mining.availableCpuThreads.coerceAtLeast(1)

    Panel(modifier = modifier) {
        PanelHeader(
            title = "Live hashrate",
            subtitle = "Last ~20 seconds · peak " + formatHashrate(max),
            right = { Pill(if (running) "running" else "idle", if (running) ZTone.On else ZTone.Off) }
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            samples.forEach { value ->
                val fraction = (value.toFloat() / max.toFloat()).coerceIn(0.04f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .alpha(if (running) 1f else 0.25f)
                        .background(bar, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
            Stat("Threads", "" + threads + " / " + maxThreads, modifier = Modifier.weight(1f))
            Stat("Height", ui.chainHeight.toString(), modifier = Modifier.weight(1f))
            Stat("Difficulty", ui.stats.difficulty.toString(), modifier = Modifier.weight(1f))
            Stat("Peers", ui.localConnectedPeers.toString(), modifier = Modifier.weight(1f))
        }
        ThinDivider()
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (running) {
                ZButton(onClick = onStop, variant = ZButtonVariant.Danger, enabled = canStop) {
                    Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                    ZButtonText("Stop miner")
                }
            } else {
                ZButton(onClick = onStart, variant = ZButtonVariant.Accent, enabled = canStart) {
                    Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                    ZButtonText("Start miner")
                }
            }
            ZLabel("Threads")
            // Las opciones se DERIVAN del hardware, no son una lista fija.
            //
            // El prototipo muestra 1/2/4 y una lista fija filtrada capaba en 8:
            // en una maquina de 16 nucleos nunca habrias podido usar mas de la
            // mitad. availableCpuThreads viene de available_parallelism() del
            // nodo, o sea el conteo real, y de ahi salen los escalones: minimo,
            // dos, mitad, tres cuartos y todo. Siempre incluye el maximo.
            val options = listOf(1, 2, maxThreads / 2, (maxThreads * 3) / 4, maxThreads)
                .map { it.coerceIn(1, maxThreads) }
                .distinct()
                .sorted()
            SegmentedSelector(
                options = options.map { it.toString() },
                selected = threads.toString(),
                onSelect = { onThreads(it.toIntOrNull() ?: 1) },
                enabled = !running,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }

        // La sesion de recompensas y los diagnosticos vivian en un panel aparte
        // mas arriba. Estos textos explican por que el boton esta deshabilitado,
        // asi que su lugar es al lado del boton y no en otra tarjeta.
        SessionReadyCard(
            title = "Reward session",
            ready = ui.sessionUnlocked,
            detail = if (ui.sessionUnlocked) "Unlocked; rewards use the selected wallet session."
                     else "Unlock the wallet first.",
            modifier = Modifier.fillMaxWidth()
        )
        HintText(hint)
        if (externalMining) {
            ErrorText("Mining is active outside ZSilent. The app can monitor it, but stop it from the console that started it.")
        }
        blockReason?.takeIf { it.isNotBlank() }?.let { ErrorText(it) }
        diagnostic?.takeIf { it.isNotBlank() && it != blockReason }?.let { ErrorText(it) }
    }
}

/** Recompensas y madurez del coinbase, como el panel derecho del prototipo. */
@Composable
private fun MiningRewardsPanel(ui: ZSilentUiState, rewardAddress: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Panel(modifier = modifier) {
        PanelHeader(title = "Rewards", subtitle = "Coinbase maturity")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ZLabel("Reward address")
            Truncate(
                rewardAddress.ifBlank { "not selected" },
                head = 12, tail = 6,
                color = scheme.onSurface
            )
        }
        // El modelo SI trae el desglose que muestra el prototipo:
        // lockedRewards es lo que esta madurando y totalRewards el acumulado.
        // Antes esto mostraba totalRewards etiquetado como "maturing", que es
        // el total y no lo bloqueado.
        val unlockedAtoms = (ui.mining.totalRewardsAtoms - ui.mining.lockedRewardsAtoms).coerceAtLeast(0L)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
            Stat(
                "Locked", ui.mining.lockedRewards,
                hint = if (ui.mining.lockedRewardCount > 0) "${ui.mining.lockedRewardCount} maturing" else "maturing",
                tone = ZTone.Warn,
                modifier = Modifier.weight(1f)
            )
            Stat(
                "Unlocked", formatZkaCompact(unlockedAtoms),
                hint = "spendable",
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
            Stat("Blocks mined", ui.mining.blocksMined.toString(), modifier = Modifier.weight(1f))
            Stat("Total", ui.mining.totalRewards, hint = "cumulative", modifier = Modifier.weight(1f))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerHigh.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                .border(BorderStroke(1.dp, scheme.outlineVariant), MaterialTheme.shapes.small)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ZLabel("Next unlock")
            val h = ui.mining.nextRewardUnlockHeight
            Text(
                if (h != null) "block $h" else "—",
                style = ZTheme.text.mono,
                color = scheme.onSurface
            )
            HintText(
                if (ui.mining.nextRewardUnlockInBlocks != null) {
                    "${ui.mining.nextRewardUnlockInBlocks} blocks away · coinbase matures after ${ui.mining.coinbaseMaturityBlocks} confirmations."
                } else {
                    "No maturing rewards. Coinbase matures after ${ui.mining.coinbaseMaturityBlocks} confirmations."
                }
            )
            HintText("Mined rewards stay locked until they reach this depth.")
        }
    }
}

@Composable
private fun MiningPreflightPanel(
    sessionUnlocked: Boolean,
    nodeOnline: Boolean,
    syncReady: Boolean,
    rewardAddressOk: Boolean,
    threadsOk: Boolean,
    binariesOk: Boolean,
) {
    val checks = listOf(
        "Wallet session unlocked" to sessionUnlocked,
        "Node online" to nodeOnline,
        "Chain synced to network head" to syncReady,
        "Reward address configured" to rewardAddressOk,
        "Valid thread count" to threadsOk,
        "Node binaries present" to binariesOk,
    )
    val blocked = checks.count { !it.second }
    Panel(
        title = "Pre-flight",
        meta = if (blocked == 0) "ready" else "$blocked blocking"
    ) {
        checks.forEach { (label, ok) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = ZTheme.spacing.row / 2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Pill(if (ok) "ok" else "blocked", if (ok) ZTone.On else ZTone.Err)
            }
        }
    }
}

@Composable
fun PrivacyScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Privacy",
        title = "Privacy stack",
        subtitle = "Tor routing, discreet mode, and posture disclosure."
    ) {
        Panel(
            title = "Tor network",
            meta = if (ui.torEnabled) ui.torStatus.name.lowercase() else "off"
        ) {
            ToggleRow(
                "Connect over Tor (.onion)",
                ui.torEnabled,
                appState::setTorOnionMode
            )
            HintText(
                if (ui.torEnabled)
                    "Your node bootstraps and syncs only over Tor onion routes. No open ports needed, your IP stays hidden, and the mining console stays clean (no mixed clearnet dial attempts)."
                else
                    "Off: the node connects over plain clearnet TCP. Turn this on to route the network through Tor (recommended, and required if you're behind CGNAT)."
            )
            KeyValueRow("Tor process", ui.torStatus.name.lowercase())
            ui.torError?.takeIf { it.isNotBlank() }?.let { ErrorText(it) }
            if (ui.torEnabled) {
                if (ui.torOnionHost.isNotBlank()) {
                    CopyableKeyValueRow("Your onion host", ui.torOnionHost)
                } else {
                    KeyValueRow("Your onion host", if (ui.torStatus == ServiceStatus.Starting) "starting Tor…" else "created on first start")
                }
            }
        }
        Panel(
            title = "Onion seed",
            meta = if (ui.onionSeedEnabled) "publishing" else "private"
        ) {
            ToggleRow(
                "Publish my node as an onion seed",
                ui.onionSeedEnabled,
                appState::setOnionSeedEnabled
            )
            HintText(
                "When on, other people can bootstrap the network through your node using your onion address below — without you opening any port. Share it or send it to the network operator to be added to the seed list. Turning this on also enables Tor above."
            )
            if (ui.torSeedAddress.isNotBlank()) {
                CopyableKeyValueRow("Your seed address", ui.torSeedAddress)
            } else if (ui.torOnionMultiaddr.isNotBlank()) {
                CopyableKeyValueRow("Your onion address", ui.torOnionMultiaddr)
            } else if (ui.onionSeedEnabled) {
                KeyValueRow("Your seed address", if (ui.torStatus == ServiceStatus.Starting) "waiting for Tor…" else "waiting for node")
            } else {
                KeyValueRow("Your seed address", "enable to generate")
            }
        }
        Panel(title = "Display privacy", meta = if (ui.balancesHidden) "hidden" else "visible") {
            ToggleRow("Hide balances on screen", ui.balancesHidden) { appState.toggleBalances() }
            ToggleRow("Wallet session", !ui.appLocked) { appState.lockApp("Locked manually.") }
        }
        Panel(
            title = "Experimental",
            meta = if (ui.explorerEnabled) "1 enabled" else "off"
        ) {
            ToggleRow("Chain explorer", ui.explorerEnabled) { appState.setExplorerEnabled(it) }
            HintText(
                "Adds an Explorer section to the sidebar for browsing blocks and pending " +
                    "transactions. It reads only from your own node — no public explorer is " +
                    "contacted — so it can only show what your node has already synced."
            )
        }
        // El prototipo muestra la postura con pills y no como pares clave/valor:
        // de un vistazo se ve si algo esta fuera de lugar, sin leer cada linea.
        Panel(title = "Security posture", meta = "what this build promises") {
            PostureRow("Telemetry", true, "disabled")
            PostureRow("Private keys in logs", true, "blocked")
            PostureRow("Local-first execution", true, "yes")
            PostureRow("Encrypted exports", true, "required")
            PostureRow("Remote node", true, "disabled by default")
            PostureRow(
                "Tor transport",
                ui.torEnabled,
                if (ui.torEnabled) "onion" else "clearnet"
            )
            PostureRow(
                "Auto-lock",
                ui.onboardingAutoLockMinutes > 0,
                if (ui.onboardingAutoLockMinutes > 0) "${ui.onboardingAutoLockMinutes} min" else "disabled"
            )
        }
    }
}


/**
 * Fila de postura de seguridad: etiqueta a la izquierda, pill a la derecha.
 *
 * `ok = false` no significa error, significa "esta promesa no se esta
 * cumpliendo ahora mismo" — por ejemplo Tor apagado. Por eso usa `warn` y no
 * `err`.
 */
@Composable
private fun PostureRow(label: String, ok: Boolean, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = ZTheme.spacing.row / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Pill(value, if (ok) ZTone.On else ZTone.Warn)
    }
}

@Composable
fun LogsScreen(ui: ZSilentUiState) {
    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Logs",
        title = "Runtime log stream",
        subtitle = "Sanitized, local, redacted. Private data never appears."
    ) {
        Panel(title = "Runtime log", meta = "${ui.logs.size} lines") {
            if (ui.logs.isEmpty()) {
                EmptyState("No runtime logs yet.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp, max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ui.logs) { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // El prototipo abre cada linea con la hora. El dato
                            // ya estaba en RuntimeLog.timestampMillis y no se
                            // mostraba.
                            Text(
                                formatLogClock(log.timestampMillis),
                                modifier = Modifier.widthIn(min = 64.dp),
                                style = ZTheme.text.mono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                logCategoryLabel(log.source),
                                modifier = Modifier.widthIn(min = 72.dp, max = 96.dp),
                                style = ZTheme.text.mono,
                                color = logSourceColor(log.source)
                            )
                            val level = effectiveLogLevel(log)
                            Text(
                                level.name.uppercase(),
                                modifier = Modifier.widthIn(min = 62.dp),
                                style = ZTheme.text.pill,
                                color = logLevelColor(level)
                            )
                            Text(
                                log.message,
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Color del prefijo `[FUENTE]` en los logs.
 *
 * Antes cada fuente tenía su propio matiz (amarillo, verde, azul, violeta,
 * rosa). En una paleta acromática eso no tiene dónde ir, y además el prototipo
 * no colorea por fuente sino por NIVEL: `[FUENTE]` va siempre en muted y el
 * color lo lleva INFO/SUCCESS/WARNING/ERROR.
 *
 * Acá se neutraliza. La coloración por nivel llega en la etapa 13, que es la
 * que porta la pantalla de Logs completa.
 */
/** `HH:MM:SS` local, como la primera columna del visor del prototipo. */
private fun formatLogClock(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))

@Composable
private fun logSourceColor(source: String): Color =
    MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Severidad efectiva de una línea.
 *
 * Si el emisor declaró un nivel, gana ese. Si no —que es el caso de casi todos
 * los sitios hoy— se infiere del texto con una heurística **deliberadamente
 * conservadora**: solo marca error o warning ante señales fuertes, y ante la
 * duda devuelve `Info`. Un log pintado de rojo por equivocación es peor que uno
 * sin color, porque manda a buscar un problema que no existe.
 *
 * Es una medida de transición: a medida que los call sites declaren su nivel,
 * la heurística deja de intervenir sola.
 */
private fun effectiveLogLevel(log: RuntimeLog): LogLevel {
    if (log.level != LogLevel.Info) return log.level
    val m = log.message.lowercase()
    return when {
        listOf("failed", "failure", "error", "could not", "cannot ", "unreachable",
               "denied", "rejected", "invalid", "missing").any { it in m } -> LogLevel.Error
        listOf("warning", "retry", "retrying", "timeout", "timed out", "waiting",
               "unsupported", "outdated").any { it in m } -> LogLevel.Warning
        listOf("started", "online", "connected", "completed", "success", "ready",
               "accepted", "synced").any { it in m } -> LogLevel.Success
        else -> LogLevel.Info
    }
}

@Composable
private fun logLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.Success -> ZTheme.semantics.success
    LogLevel.Warning -> ZTheme.semantics.warning
    LogLevel.Error -> ZTheme.semantics.danger
    LogLevel.Info -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun logCategoryLabel(source: String): String {
    return "[${source.ifBlank { "runtime" }.uppercase()}]"
}

@Composable
private fun ComingSoonScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String
) {
    ScreenScaffold(title, subtitle) {
        Panel(title = title, meta = "Coming soon") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.widthIn(max = 420.dp)
                )
            }
        }
    }
}

// P2PExchangeScreen and MiningPoolScreen live in their own files. Both take
// (ui, appState) and read real state, so no Coming Soon placeholders remain.

// ExplorerScreen lives in ExplorerScreen.kt — it reads real chain state and is
// wired into the router. No Coming Soon placeholder for this section.

@Composable
fun SettingsScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var page by remember { mutableStateOf(SettingsPage.Home) }
    var confirmReset by remember { mutableStateOf(false) }
    val title = if (page == SettingsPage.Home) "Settings" else page.title
    val subtitle = if (page == SettingsPage.Home) {
        "Personal controls for appearance, security, wallets, history and privacy."
    } else {
        page.subtitle
    }

    ScreenScaffold(title, subtitle) {
        if (page != SettingsPage.Home) {
            OutlinedButton(
                colors = zOutlinedButtonColors(),
                shape = MaterialTheme.shapes.small,
                contentPadding = SettingsButtonPadding,
                onClick = {
                    page = SettingsPage.Home
                    confirmReset = false
                }
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Back to settings")
            }
        }

        Crossfade(
            targetState = page,
            animationSpec = tween(180),
            label = "settings-page"
        ) { currentPage ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (currentPage) {
                    SettingsPage.Home -> SettingsHome(ui) { page = it }
                    SettingsPage.Security -> SettingsSecurityPage(ui, appState)
                    SettingsPage.Network -> SettingsNetworkPage(ui)
                    SettingsPage.Experimental -> SettingsExperimentalPage(ui, appState)
                    SettingsPage.Wallets -> SettingsWalletsPage(ui, appState)
            SettingsPage.Appearance -> SettingsAppearancePage(ui, appState)
            SettingsPage.About -> SettingsAboutPage(ui)
                    SettingsPage.Danger -> SettingsDangerPage(
                        confirmReset = confirmReset,
                        onAskConfirm = { confirmReset = true },
                        onCancel = { confirmReset = false },
                        onReset = {
                            if (appState.resetLocalWallet()) {
                                confirmReset = false
                            }
                        }
                    )
                }
            }
        }
    }
}

private enum class SettingsPage(
    val title: String,
    val subtitle: String
) {
    Home("Settings", "Personal controls for ZSilent Core."),
    Security("Security", "Change PIN, reveal recovery phrase and private keys."),
    Network("Network", "RPC endpoint and connection status."),
    Appearance("Appearance", "Theme, density and motion."),
    Wallets("Wallets", "Accounts on this device."),
    Experimental("Experimental", "Opt in to features that can move funds."),
    About("About", "ZSilent Core details and runtime status."),
    Danger("Danger zone", "Reset app setup deliberately.")
}

private val SettingsButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)

@Composable
private fun RuntimeProgressPanel(
    title: String,
    phase: String,
    detail: String,
    startedAtMillis: Long,
    rows: List<Pair<String, String>>
) {
    var nowMillis by remember(startedAtMillis) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis, title) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Panel(title = title, meta = "live") {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        )
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        KeyValueRow("Phase", phase)
        KeyValueRow("Elapsed", formatElapsedMillis(nowMillis - startedAtMillis))
        rows.forEach { (label, value) ->
            KeyValueRow(label, value)
        }
    }
}

@Composable
private fun SettingsHome(ui: ZSilentUiState, onOpen: (SettingsPage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsStateBanner(ui)
        val torLive = ui.torEnabled && ui.torStatus == ServiceStatus.Online
        val hasWallet = ui.activeWalletAddress.isNotBlank()
        val experimentalOn = ui.p2pExchangeEnabled || ui.teamMiningEnabled

        // Rejilla pareja de tres columnas. Antes dos tarjetas no llevaban
        // weight y se dimensionaban por su texto, asi que la fila quedaba
        // desalineada contra la de arriba.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMenuCard(
                title = "Security",
                detail = "Change PIN, recovery phrase and private keys",
                icon = Icons.Outlined.Lock,
                meta = if (ui.sessionUnlocked) "unlocked" else "locked",
                health = if (ui.sessionUnlocked) SettingHealth.Ok else SettingHealth.Warn,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Security) }
            )
            SettingsMenuCard(
                title = "Network",
                detail = when {
                    !ui.localRpcOnline -> "Node offline: no chain, no peers, no offers."
                    ui.localConnectedPeers > 0 -> "Connected to ${ui.localConnectedPeers} peer(s) over p2p."
                    else -> "Node online but alone: nothing reaches you and nothing leaves."
                },
                icon = Icons.Outlined.Sync,
                meta = when {
                    !ui.localRpcOnline -> "offline"
                    ui.localConnectedPeers > 0 -> "connected"
                    else -> "no peers"
                },
                health = when {
                    !ui.localRpcOnline -> SettingHealth.Bad
                    ui.localConnectedPeers > 0 -> SettingHealth.Ok
                    else -> SettingHealth.Warn
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Network) }
            )
            // Faltaba: Tor es parte del protocolo y se decide aca, no solo en
            // Privacy. Sin el, la verificacion de pagos del exchange no corre.
            SettingsMenuCard(
                title = "Privacy & Tor",
                detail = if (torLive) "Traffic routed over Tor onion services."
                else "Tor is off. Payment verification cannot run without it.",
                icon = Icons.Outlined.Shield,
                meta = if (torLive) "tor on" else if (ui.torEnabled) "starting" else "tor off",
                health = when {
                    torLive -> SettingHealth.Ok
                    ui.torEnabled -> SettingHealth.Warn
                    else -> SettingHealth.Bad
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Network) }
            )
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMenuCard(
                title = "Wallets",
                detail = if (hasWallet) "Active: ${ui.activeWalletName.ifBlank { "unnamed" }}"
                else "No wallet selected on this device.",
                icon = Icons.Outlined.AccountBalanceWallet,
                meta = if (hasWallet) "ready" else "none",
                health = if (hasWallet) SettingHealth.Ok else SettingHealth.Bad,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Wallets) }
            )
            SettingsMenuCard(
                title = "Appearance",
                detail = "Theme, density and motion.",
                icon = Icons.Outlined.Palette,
                meta = "display",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Appearance) }
            )
            SettingsMenuCard(
                title = "About",
                detail = "ZSilent Core, Zoka and runtime details",
                icon = Icons.Outlined.Info,
                meta = "details",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.About) }
            )
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMenuCard(
                title = "Experimental",
                detail = listOfNotNull(
                    "P2P Exchange".takeIf { ui.p2pExchangeEnabled },
                    "Team Mining".takeIf { ui.teamMiningEnabled }
                ).takeIf { it.isNotEmpty() }
                    ?.let { "On: ${it.joinToString(" + ")}. These can move funds." }
                    ?: "Off. Nothing experimental can touch your balance.",
                icon = Icons.Outlined.CurrencyExchange,
                // Ambar cuando estan encendidas, no verde: funcionan, pero
                // mueven dinero y conviene que se note que estan activas.
                meta = if (experimentalOn) "active" else "off",
                health = if (experimentalOn) SettingHealth.Warn else SettingHealth.Neutral,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Experimental) }
            )
            SettingsMenuCard(
                title = "Danger zone",
                detail = "Clear app setup and return to onboarding",
                icon = Icons.Outlined.WarningAmber,
                meta = "reset",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpen(SettingsPage.Danger) }
            )
        }
    }
}

/**
 * Salud de una entrada del menú de ajustes.
 *
 * Verde lo que está en orden, ámbar lo que funciona pero pide atención, rojo lo
 * que está roto o apagado y hace falta. `Neutral` es para lo que no tiene estado
 * —apariencia, por ejemplo— y ahí no se pinta nada: un punto gris al lado de
 * cada fila enseña a ignorar los puntos.
 */
private enum class SettingHealth { Ok, Warn, Bad, Neutral }

@Composable
private fun SettingHealth.color(): Color = when (this) {
    SettingHealth.Ok -> ZTheme.semantics.success
    SettingHealth.Warn -> ZTheme.semantics.warning
    SettingHealth.Bad -> ZTheme.semantics.danger
    SettingHealth.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun SettingHealth.icon(): ImageVector = when (this) {
    SettingHealth.Ok -> Icons.Outlined.CheckCircle
    SettingHealth.Warn -> Icons.Outlined.ErrorOutline
    SettingHealth.Bad -> Icons.Outlined.Cancel
    SettingHealth.Neutral -> Icons.Outlined.RadioButtonUnchecked
}

@Composable
private fun SettingsMenuCard(
    title: String,
    detail: String,
    icon: ImageVector,
    meta: String,
    modifier: Modifier = Modifier,
    health: SettingHealth = SettingHealth.Neutral,
    onClick: () -> Unit
) {
    Panel(modifier = modifier, title = title, meta = meta) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                // El icono de la tarjeta toma el color de su estado: asi el
                // vistazo general se hace sin leer una palabra.
                tint = if (health == SettingHealth.Neutral) MaterialTheme.colorScheme.primary
                else health.color(),
                modifier = Modifier.size(34.dp)
            )
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (health != SettingHealth.Neutral) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono ademas de color: quien no distingue rojo de verde
                    // necesita la forma para leer el estado.
                    Icon(
                        health.icon(),
                        contentDescription = null,
                        tint = health.color(),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(meta, style = ZTheme.text.eyebrow, color = health.color())
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            ZButton(onClick = onClick, variant = ZButtonVariant.Outline, size = ZButtonSize.Small) {
                ZButtonText("Open")
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Cartel de estado de la app, hermano de los de Node, Mining, Pool y Exchange.
 *
 * Resume de un vistazo lo que hace falta para operar. `READY` exige todo a la
 * vez, porque cada pieza que falta bloquea algo distinto: sin sesión no se firma,
 * sin nodo no hay cadena, sin Tor no se verifica un pago.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsStateBanner(ui: ZSilentUiState) {
    val sessionOk = ui.sessionUnlocked
    val nodeOk = ui.nodeStatus == ServiceStatus.Online
    val torOk = ui.torEnabled && ui.torStatus == ServiceStatus.Online
    val binariesOk = ui.zokaReady
    val syncOk = ui.networkSynced

    val ready = sessionOk && nodeOk && binariesOk && syncOk
    val broken = !binariesOk || ui.nodeStatus == ServiceStatus.Error
    val label = when {
        broken -> "ATTENTION"
        ready && torOk -> "READY"
        ready -> "READY"
        else -> "INCOMPLETE"
    }
    val tint = when {
        broken -> ZTheme.semantics.danger
        ready -> ZTheme.semantics.success
        else -> ZTheme.semantics.warning
    }

    Panel(title = "This device", meta = label.lowercase()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = ZTheme.text.monoDisplay, color = tint)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Pill("session ${if (sessionOk) "unlocked" else "locked"}", if (sessionOk) ZTone.On else ZTone.Warn)
                    Pill("node ${if (nodeOk) "online" else "offline"}", if (nodeOk) ZTone.On else ZTone.Err)
                    Pill("tor ${if (torOk) "on" else "off"}", if (torOk) ZTone.On else ZTone.Warn)
                    Pill("chain ${if (syncOk) "synced" else "syncing"}", if (syncOk) ZTone.On else ZTone.Warn)
                    Pill("binaries ${if (binariesOk) "ready" else "missing"}", if (binariesOk) ZTone.On else ZTone.Err)
                    Pill(
                        "network ${com.zsilent.core.config.ZokaNetworkConfig.Network.ifBlank { "mainnet" }}",
                        if (com.zsilent.core.config.ZokaNetworkConfig.Network.equals("testnet", true)) ZTone.Warn else ZTone.Default
                    )
                }
            }
        }
        HintText(
            when {
                !binariesOk -> "The node binaries are missing from this install. Reinstall ZSilent; nothing can run without them."
                ui.nodeStatus == ServiceStatus.Error -> "The node reported an error. Check Logs for the reason before trusting any balance shown here."
                !sessionOk -> "The wallet session is locked. Unlock it to sign anything."
                !nodeOk -> "The node is offline. Start it from the Node section."
                !syncOk -> "The chain is still catching up. Balances and offers may be behind until it finishes."
                !torOk -> "Tor is off. The app works, but payment verification in P2P Exchange needs it and will stay blocked."
                else -> "Everything needed to operate is up."
            }
        )
    }
}

@Composable
private fun SettingsSecurityPage(
    ui: ZSilentUiState,
    appState: ZSilentAppState
) {
    var notice by remember { mutableStateOf<String?>(null) }
    var pinEditorOpen by remember { mutableStateOf(false) }
    var currentPin by remember(ui.activeWalletAddress, ui.activeWalletName) { mutableStateOf("") }
    var newPin by remember(ui.activeWalletAddress, ui.activeWalletName) { mutableStateOf("") }
    var confirmPin by remember(ui.activeWalletAddress, ui.activeWalletName) { mutableStateOf("") }
    var recoveryPassword by remember(ui.activeWalletAddress, ui.activeWalletName) { mutableStateOf("") }
    val hasVisibleSecrets = ui.revealedRecoveryPhrase != null ||
        ui.revealedScanPrivateKey != null ||
        ui.revealedSpendPrivateKey != null

    Panel(title = "Access", meta = "${ui.onboardingAutoLockMinutes} min") {
        SettingsActionButton(
            title = "Change PIN",
            detail = "Prepared for wallet passphrase rotation.",
            icon = Icons.Outlined.Lock,
            actionLabel = if (pinEditorOpen) "Close" else "Open",
            onClick = {
                pinEditorOpen = !pinEditorOpen
                currentPin = ""
                newPin = ""
                confirmPin = ""
                appState.clearPinChangeMessage()
            }
        )
        if (pinEditorOpen) {
            PasswordField(
                value = currentPin,
                onValueChange = { currentPin = it },
                label = "Current PIN/password",
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = "New PIN/password",
                    modifier = Modifier.weight(1f)
                )
                PasswordField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = "Confirm new PIN/password",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        appState.changeWalletPin(
                            currentPin.toCharArray(),
                            newPin.toCharArray(),
                            confirmPin.toCharArray()
                        )
                        currentPin = ""
                        newPin = ""
                        confirmPin = ""
                        notice = null
                    },
                    enabled = currentPin.isNotBlank() && newPin.isNotBlank() && confirmPin.isNotBlank() && !ui.pinChangeBusy,
                    colors = zButtonColors(),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = SettingsButtonPadding
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(if (ui.pinChangeBusy) "Changing..." else "Change PIN")
                }
                OutlinedButton(
                    colors = zOutlinedButtonColors(),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = SettingsButtonPadding,
                    onClick = {
                        pinEditorOpen = false
                        currentPin = ""
                        newPin = ""
                        confirmPin = ""
                        appState.clearPinChangeMessage()
                    }
                ) { Text("Cancel") }
            }
        }
        ui.pinChangeError?.let { ErrorText(it) }
        ui.pinChangeResult?.let { result ->
            Text(
                "PIN/password changed for ${result.name}. Use the new password on next unlock.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        KeyValueRow("Auto-lock", "${ui.onboardingAutoLockMinutes} min")
        Slider(
            value = ui.onboardingAutoLockMinutes.toFloat(),
            onValueChange = { appState.setAutoLockMinutes(it.toInt()) },
            valueRange = 1f..120f,
            steps = 118
        )
    }

    Panel(title = "Recovery", meta = "sensitive") {
        Text(
            "Enter the wallet password to reveal recovery data. Nothing shown here is stored in Settings or written to logs.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PasswordField(
            value = recoveryPassword,
            onValueChange = { recoveryPassword = it },
            label = "Wallet password",
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    appState.revealRecoveryPhrase(recoveryPassword.toCharArray())
                    recoveryPassword = ""
                    notice = null
                },
                enabled = recoveryPassword.isNotBlank() && !ui.secretRevealBusy,
                colors = zButtonColors(),
                shape = MaterialTheme.shapes.small,
                contentPadding = SettingsButtonPadding,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(if (ui.secretRevealBusy) "Checking..." else "Show phrase")
            }
            Button(
                onClick = {
                    appState.revealPrivateKeys(recoveryPassword.toCharArray())
                    recoveryPassword = ""
                    notice = null
                },
                enabled = recoveryPassword.isNotBlank() && !ui.secretRevealBusy,
                colors = zButtonColors(),
                shape = MaterialTheme.shapes.small,
                contentPadding = SettingsButtonPadding,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Show keys")
            }
        }
        if (hasVisibleSecrets) {
            OutlinedButton(
                colors = zOutlinedButtonColors(),
                shape = MaterialTheme.shapes.small,
                contentPadding = SettingsButtonPadding,
                onClick = {
                    appState.clearRevealedSecrets()
                    recoveryPassword = ""
                }
            ) {
                Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Hide secrets")
            }
        }
        ui.secretRevealError?.let { ErrorText(it) }
        if (hasVisibleSecrets) {
            Text(
                "Sensitive recovery data is visible. Store it offline and hide it before leaving the device unattended.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            if (ui.revealedSecretWallet.isNotBlank()) {
                CopyableKeyValueRow("Wallet", ui.revealedSecretWallet)
            }
        }
        ui.revealedRecoveryPhrase?.let { phrase ->
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Type", "recovery phrase")
                    MonoText(phrase, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                    CopyableKeyValueRow("Recovery phrase", phrase)
                }
            }
        }
        if (ui.revealedScanPrivateKey != null || ui.revealedSpendPrivateKey != null) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Type", "private keys")
                    ui.revealedScanPrivateKey?.let { CopyableKeyValueRow("Scan private key", it) }
                    ui.revealedSpendPrivateKey?.let { CopyableKeyValueRow("Spend private key", it) }
                }
            }
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SettingsNetworkPage(ui: ZSilentUiState) {
    val netLive = ui.localRpcOnline && (ui.localConnectedPeers > 0 || ui.networkSynced)
    Panel(title = "Network", meta = if (netLive) "live (p2p)" else if (ui.localRpcOnline) "node online" else "offline") {
        KeyValueRow("Transport", "P2P mesh — Tor onion + iroh (serverless)")
        KeyValueRow("Reachability", if (netLive) "connected via peers" else "isolated")
        KeyValueRow("Connected peers", ui.localConnectedPeers.toString())
        KeyValueRow("Network height", ui.networkHeight.toString())
        KeyValueRow("Node height", ui.chainHeight.toString())
        KeyValueRow("Sync gate", if (ui.networkSynced) "synced" else "syncing")
    }
    Panel(title = "Local node", meta = if (ui.localRpcOnline) "online" else "offline") {
        KeyValueRow("Local RPC", if (ui.localRpcOnline) "${ZokaNetworkConfig.displayUrl(ui.localRpcUrl)} online" else "offline")
        CopyableKeyValueRow("Bootstrap", ui.bootstrapPeer)
        KeyValueRow("Connected peers", ui.localConnectedPeers.toString())
        KeyValueRow("Your mining", if (ui.mining.running) "on" else "off")
    }
}

@Composable
private fun SettingsExperimentalPage(ui: ZSilentUiState, appState: ZSilentAppState) {
    var tronUrl by remember(ui.tronApiUrl) { mutableStateOf(ui.tronApiUrl) }
    var tronApiKey by remember(ui.tronApiKey) { mutableStateOf(ui.tronApiKey) }
    var fallbackUrls by remember(ui.tronFallbackUrls) { mutableStateOf(ui.tronFallbackUrls) }
    var providerError by remember { mutableStateOf<String?>(null) }
    var providerNotice by remember { mutableStateOf<String?>(null) }

    Panel(title = "P2P Exchange", meta = "experimental opt-in") {
        ToggleRow("Enable P2P Exchange", ui.p2pExchangeEnabled) {
            appState.setP2PExchangeEnabled(it)
        }
        Text(
            "This section can move real ZKA and USDT. It stays out of the sidebar until " +
                "you opt in, and the first entry still requires explicit risk acceptance.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        KeyValueRow(
            "Risk acknowledgement",
            if (ui.p2pExchangeRisksAccepted) "accepted" else "required on first entry"
        )
        HintText("There is no escrow, arbiter, refund or per-offer amount cap in v1.")
    }

    Panel(title = "Team Mining", meta = "experimental opt-in") {
        ToggleRow("Enable Team Mining", ui.teamMiningEnabled) {
            appState.setTeamMiningEnabled(it)
        }
        Text(
            "This section coordinates mining with people you know. It stays out of the " +
                "sidebar until you opt in. There is no team wallet and no coordinator fee.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        HintText(
            "Whole block rewards rotate by contributed work. Short-term payouts are uneven, " +
                "and a member can hijack a block; the team can detect and expel them but cannot " +
                "make that otherwise-valid block impossible."
        )
    }

    // La red de Zoka va ANTES que la de Tron a proposito: es la decision de la
    // que cuelga todo lo demas -- cadena, carteras, saldo, ofertas -- mientras
    // que la de Tron solo decide contra que USDT se cobra.
    run {
        // Lo elegido y lo que esta corriendo son dos cosas distintas mientras no
        // se reinicie. Se muestran las dos: si solo se mostrara la eleccion, el
        // usuario creeria estar en testnet y operaria con dinero real.
        var chosen by remember {
            mutableStateOf(com.zsilent.core.core.storage.NetworkPreference.read())
        }
        val running = com.zsilent.core.config.ZokaNetworkConfig.Network
            .ifBlank { "mainnet" }
            .lowercase()
        val pendingRestart = chosen != running

        Panel(
            title = "Zoka network",
            meta = if (pendingRestart) "restart required" else running
        ) {
            SegmentedSelector(
                options = listOf("Mainnet", "Testnet"),
                selected = if (chosen == "testnet") "Testnet" else "Mainnet",
                onSelect = {
                    val value = if (it == "Testnet") "testnet" else "mainnet"
                    com.zsilent.core.core.storage.NetworkPreference.write(value)
                    chosen = value
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(
                    text = "RUNNING: ${running.uppercase()}",
                    tone = if (running == "testnet") ZTone.Warn else ZTone.On
                )
                if (pendingRestart) {
                    Pill(text = "SELECTED: ${chosen.uppercase()}", tone = ZTone.Warn)
                }
            }
            if (pendingRestart) {
                HintText(
                    "Close ZSilent and open it again to switch. The node, the wallets and " +
                        "the chain are separate per network, so nothing can move between " +
                        "them and nothing you already have is touched."
                )
                if (chosen == "testnet") {
                    // Lo que hay que decir ANTES, no despues: al reiniciar
                    // aparece la pantalla de desbloqueo sin ninguna wallet, y
                    // sin este aviso eso se lee como "perdi todo".
                    HintText(
                        "You have no wallet on testnet yet, so after restarting the unlock " +
                            "screen will show none. That is expected — your mainnet wallet " +
                            "is untouched, and there is a “Back to mainnet” button there."
                    )
                }
            } else if (running == "testnet") {
                HintText(
                    "Testnet coins are worthless on purpose — this is where to rehearse a " +
                        "sale end to end without risking real ZKA or real USDT."
                )
            } else {
                HintText("Real coins. Balances, offers and mining rewards here are real money.")
            }
        }
    }

    Panel(
        title = "Tron network",
        meta = if (ui.tronNetwork == "nile") "Nile testnet" else "Mainnet"
    ) {
        com.zsilent.core.ui.components.SegmentedSelector(
            options = listOf("Mainnet", "Nile testnet"),
            selected = if (ui.tronNetwork == "nile") "Nile testnet" else "Mainnet",
            onSelect = { appState.setTronNetwork(if (it == "Nile testnet") "nile" else "mainnet") },
            modifier = Modifier.fillMaxWidth()
        )
        if (ui.tronNetwork == "nile") {
            HintText(
                "Nile is play money — for testing the exchange flow without Tor or real " +
                    "USDT at risk. Payments never cross networks: a Nile transfer cannot " +
                    "settle a mainnet offer, or the reverse."
            )
        } else {
            HintText("Real USDT. Payment checks require Tor — this is not optional on mainnet.")
        }
        HintText("Applies the next time the node starts.")
    }

    Panel(title = "Tron payment provider", meta = "applies after node restart") {
        OutlinedTextField(
            value = tronUrl,
            onValueChange = { tronUrl = it.trim() },
            label = { Text("Primary Tron API URL") },
            singleLine = true,
            colors = zFieldColors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        PasswordField(
            value = tronApiKey,
            onValueChange = { tronApiKey = it },
            label = "Optional API key",
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fallbackUrls,
            onValueChange = { fallbackUrls = it },
            label = { Text("Fallback URLs (comma or newline separated)") },
            minLines = 2,
            colors = zFieldColors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        providerError?.let { ErrorText(it) }
        providerNotice?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                val urls = listOf(tronUrl) + fallbackUrls
                    .split(',', '\n')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val invalid = urls.firstOrNull { !isHttpProviderUrl(it) }
                if (invalid != null) {
                    providerError = "Use a complete http:// or https:// URL: $invalid"
                    providerNotice = null
                } else {
                    appState.setTronApiSettings(tronUrl, tronApiKey, fallbackUrls)
                    providerError = null
                    providerNotice = "Saved. Restart the local node to apply these provider settings."
                }
            },
            colors = zButtonColors(),
            shape = MaterialTheme.shapes.small,
            contentPadding = SettingsButtonPadding
        ) {
            Text("Save provider settings")
        }
        HintText(
            "The node always sends these queries through its SOCKS proxy. It refuses to " +
                "verify payments when Tor is unavailable; there is no direct fallback."
        )
    }
}

private fun isHttpProviderUrl(value: String): Boolean = runCatching {
    val uri = URI(value.trim())
    (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
}.getOrDefault(false)


/**
 * Ajustes de apariencia (decisión D4).
 *
 * El tema ya soportaba densidad y animaciones desde la etapa 0, pero nada las
 * exponía: los tres valores viajaban por `CompositionLocal` con su default y no
 * había forma de cambiarlos. Acá se cierra ese circuito.
 */

/**
 * Categoría Wallets del prototipo.
 *
 * Lista las cuentas y permite marcar la activa. **No incluye borrar**, que el
 * prototipo sí ofrece: `ZSilentAppState` no expone ninguna operación de borrado
 * de wallet, así que no sería reorganizar sino agregar una acción destructiva
 * sobre material de claves. Eso necesita su propio diseño — confirmación,
 * verificación de que hay backup — y no corresponde a una etapa de porteo
 * visual.
 *
 * Exportar e importar tampoco se duplican acá: sus formularios, con ruta y
 * contraseña, ya viven en la pantalla Wallet.
 */
@Composable
private fun SettingsWalletsPage(ui: ZSilentUiState, appState: ZSilentAppState) {
    Panel(title = "Accounts", meta = "${ui.wallets.size} on this device") {
        if (ui.wallets.isEmpty()) {
            EmptyState("No wallets yet.")
        } else {
            ui.wallets.forEach { wallet ->
                val active = wallet.address == ui.activeWalletAddress
                Row(
                    Modifier.fillMaxWidth().padding(vertical = ZTheme.spacing.row / 2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            wallet.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Truncate(
                            wallet.address,
                            head = 14, tail = 6,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (active) {
                        Pill("default", ZTone.On)
                    } else {
                        ZButton(
                            onClick = { appState.selectWallet(wallet.name) },
                            variant = ZButtonVariant.Outline,
                            size = ZButtonSize.Small,
                        ) { ZButtonText("Set default") }
                    }
                }
            }
        }
        HintText("Exportar e importar backups se hace desde la pantalla Wallet, donde están los formularios de ruta y contraseña.")
    }
}

@Composable
private fun SettingsAppearancePage(ui: ZSilentUiState, appState: ZSilentAppState) {
    Panel(title = "Theme", meta = if (ui.darkTheme) "dark" else "light") {
        ToggleRow("Dark theme", ui.darkTheme) { appState.setDarkTheme(it) }
        HintText("También se puede cambiar desde el interruptor de la barra superior.")
    }
    Panel(title = "Density", meta = if (ui.compactDensity) "compact" else "cozy") {
        ToggleRow("Compact density", ui.compactDensity) { appState.setCompactDensity(it) }
        HintText("Compacta reduce rellenos y altura de controles. Útil en pantallas chicas.")
    }
    Panel(title = "Motion", meta = if (ui.animationsEnabled) "on" else "off") {
        ToggleRow("Enable animations", ui.animationsEnabled) { appState.setAnimationsEnabled(it) }
        HintText(
            "Apaga las transiciones y los indicadores que laten. Recomendado en " +
                "máquinas lentas o si el movimiento molesta."
        )
    }
}

@Composable
private fun SettingsAboutPage(ui: ZSilentUiState) {
    Panel(title = "ZSilent Core", meta = "desktop") {
        KeyValueRow("Version", ZokaNetworkConfig.AppVersion)
        KeyValueRow("Package", "ZSilent Core")
        KeyValueRow("Runtime", "Kotlin JVM 17+")
        KeyValueRow("UI", "Compose Desktop / Material 3")
        KeyValueRow("Installer", "Windows MSI")
    }
    Panel(title = "Details", meta = if (ui.zokaReady) "ready" else "missing") {
        KeyValueRow("Zoka binaries", if (ui.zokaReady) "detected" else "not detected")
        KeyValueRow("Network", if (ui.localRpcOnline && ui.localConnectedPeers > 0) "p2p mesh (${ui.localConnectedPeers} peer(s))" else "isolated")
        KeyValueRow("Node RPC", if (ui.localRpcOnline) "${ZokaNetworkConfig.displayUrl(ui.localRpcUrl)} online" else "offline")
        KeyValueRow("Node height", ui.chainHeight.toString())
        KeyValueRow("Network height", ui.networkHeight.toString())
        KeyValueRow("Sync gate", if (ui.miningSyncReady) "mining enabled" else "mining locked")
        KeyValueRow("Tor policy", if (ui.torEnabled) "Tor onion (only)" else "direct clearnet")
    }
}

@Composable
private fun SettingsDangerPage(
    confirmReset: Boolean,
    onAskConfirm: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    Panel(title = "Clear all data", meta = "danger") {
        Text(
            "This returns ZSilent Core to onboarding and removes local encrypted wallet files from this device. Funds stay on-chain; recovery requires the seed phrase.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!confirmReset) {
            Button(
                colors = zButtonColors(),
                shape = MaterialTheme.shapes.small,
                contentPadding = SettingsButtonPadding,
                onClick = onAskConfirm
            ) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Clear all data")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    colors = zOutlinedButtonColors(),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = SettingsButtonPadding,
                    onClick = onCancel
                ) { Text("Cancel") }
                Button(
                    onClick = onReset,
                    colors = zButtonColors(),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = SettingsButtonPadding
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Confirm clear")
                }
            }
        }
    }
}

@Composable
private fun SettingsActionButton(
    title: String,
    detail: String,
    icon: ImageVector,
    actionLabel: String = "Open",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = zButtonColors(),
            shape = MaterialTheme.shapes.small,
            contentPadding = SettingsButtonPadding
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = com.zsilent.core.ui.components.zSwitchColors())
    }
}

private fun activeLocalWallet(ui: ZSilentUiState): LocalWallet? {
    return ui.wallets.firstOrNull { it.address == ui.activeWalletAddress }
        ?: ui.wallets.firstOrNull { it.name == ui.activeWalletName }
}

private fun activeWalletAvailable(ui: ZSilentUiState, wallet: LocalWallet?): String {
    return privateBalanceLabel(ui, ui.walletBalance?.balance, wallet?.balance)
}

private fun activeWalletLocked(ui: ZSilentUiState, wallet: LocalWallet?): String {
    return privateBalanceLabel(ui, ui.walletBalance?.lockedBalance, wallet?.lockedBalance)
}

private fun activeWalletTotal(ui: ZSilentUiState, wallet: LocalWallet?): String {
    return privateBalanceLabel(ui, ui.walletBalance?.totalBalance, wallet?.totalBalance)
}

private fun privateBalanceLabel(ui: ZSilentUiState, value: String?, fallback: String? = null): String {
    val resolved = value?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }
    return when {
        ui.balancesHidden -> "hidden"
        resolved == null && ui.walletLocked -> "requires unlock"
        resolved == null -> "0 ZKA"
        else -> "$resolved ZKA"
    }
}

private fun shortHash(value: String): String {
    return if (value.length <= 18) value else "${value.take(10)}...${value.takeLast(8)}"
}

private fun syncEtaLabel(
    samples: List<SyncSample>,
    remainingBlocks: Long,
    synced: Boolean,
    rpcOnline: Boolean
): String {
    if (synced) return "complete"
    if (!rpcOnline) return "waiting for node"
    if (remainingBlocks <= 0L) return "almost ready"
    val rate = syncBlocksPerSecond(samples)
    if (rate <= 0.0) return "calculating"
    val seconds = (remainingBlocks / rate).toLong().coerceAtLeast(1L)
    return formatDurationSeconds(seconds)
}

private fun syncSpeedLabel(samples: List<SyncSample>, synced: Boolean): String {
    if (synced) return "synced"
    val rate = syncBlocksPerSecond(samples)
    return if (rate <= 0.0) {
        "waiting for blocks"
    } else {
        "%.2f blocks/s".format(rate)
    }
}

private fun syncBlocksPerSecond(samples: List<SyncSample>): Double {
    if (samples.size < 2) return 0.0
    val last = samples.last()
    val first = samples.firstOrNull { it.height < last.height } ?: return 0.0
    val deltaBlocks = (last.height - first.height).coerceAtLeast(0L)
    val deltaSeconds = (last.timestampMillis - first.timestampMillis).coerceAtLeast(1L) / 1_000.0
    if (deltaBlocks <= 0L || deltaSeconds <= 0.0) return 0.0
    return deltaBlocks / deltaSeconds
}

private fun syncEncouragement(ui: ZSilentUiState, samples: List<SyncSample>): String {
    return when {
        ui.networkSynced -> "Ready. Node chain is aligned with the network."
        ui.syncBusy && syncBlocksPerSecond(samples) > 0.0 -> "Importing verified blocks. You can leave this screen open and watch the line climb."
        ui.syncBusy -> "Preparing block import. The first blocks can take a moment while the chain database opens."
        ui.localRpcOnline -> "Node is alive. ZSilent is comparing heights and waiting for the next sync step."
        ui.rpcOnline -> "Bootstrap is reachable. Start sync when ready, or keep the node open for live catch-up."
        else -> "Waiting for bootstrap RPC. Wallet secrets stay on this device while network status reconnects."
    }
}

private fun formatElapsedMillis(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatDurationSeconds(seconds: Long): String {
    val clean = seconds.coerceAtLeast(0L)
    val hours = clean / 3_600L
    val minutes = (clean % 3_600L) / 60L
    val secs = clean % 60L
    return when {
        hours > 0L -> "%dh %02dm".format(hours, minutes)
        minutes > 0L -> "%dm %02ds".format(minutes, secs)
        else -> "%ds".format(secs)
    }
}

private fun seedMetricHistory(value: Double, points: Int = 24): List<Double> {
    return List(points) { value }
}

private fun appendMetricSample(history: List<Double>, value: Double, points: Int = 24): List<Double> {
    val cleanValue = if (value.isFinite()) value else 0.0
    return (history + cleanValue).takeLast(points)
}

private fun defaultWalletBackupPath(walletName: String): String {
    val safeName = walletName
        .ifBlank { "wallet" }
        .map { char -> if (char.isLetterOrDigit() || char == '-' || char == '_') char else '_' }
        .joinToString("")
    val desktop = Path.of(System.getProperty("user.home"), "Desktop")
    return desktop.resolve("$safeName.zoka-wallet-backup.json").toString()
}

private fun transferStatusLabel(ui: ZSilentUiState, result: SendResult): String {
    val statuses = result.transactionHashes.map { singleTransferStatus(ui, it) }.toSet()
    return when {
        result.transactionHashes.isEmpty() -> "not available"
        statuses.size == 1 -> statuses.first()
        statuses.contains("queued") -> "partially queued"
        statuses.contains("confirmed") -> "partially confirmed"
        else -> "recorded"
    }
}

private fun singleTransferStatus(ui: ZSilentUiState, hash: String): String {
    if (hash.isBlank()) return "unknown"
    if (ui.pendingPrivateTransfers.pendingPrivateTxHashes.any { it.equals(hash, ignoreCase = true) }) {
        return "queued"
    }
    val historyStatus = ui.walletHistory.transactions
        .firstOrNull { it.txHash.equals(hash, ignoreCase = true) }
        ?.status
        ?.takeIf { it.isNotBlank() }
    return historyStatus ?: "recorded"
}

private fun formatHashrate(hashesPerSecond: Long): String {
    val value = hashesPerSecond.toDouble()
    return when {
        hashesPerSecond <= 0L -> "0 H/s"
        value >= 1_000_000_000_000.0 -> "%.2f TH/s".format(value / 1_000_000_000_000.0)
        value >= 1_000_000_000.0 -> "%.2f GH/s".format(value / 1_000_000_000.0)
        value >= 1_000_000.0 -> "%.2f MH/s".format(value / 1_000_000.0)
        value >= 1_000.0 -> "%.2f KH/s".format(value / 1_000.0)
        else -> "$hashesPerSecond H/s"
    }
}
