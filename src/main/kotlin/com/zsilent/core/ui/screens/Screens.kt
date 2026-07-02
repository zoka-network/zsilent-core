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
import com.zsilent.core.ui.components.CopyIconButton
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.ErrorText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.MonoText
import com.zsilent.core.ui.components.Panel
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            com.zsilent.core.ui.onboarding.NoirSection(
                title = "Network snapshot",
                meta = if (ui.rpcOnline) "linked" else "offline",
                modifier = Modifier.weight(1f)
            ) {
                com.zsilent.core.ui.onboarding.NoirKeyValue("Bootstrap RPC", if (ui.rpcOnline) "online" else "offline")
                com.zsilent.core.ui.onboarding.NoirKeyValue("Endpoint", ZokaNetworkConfig.displayUrl(ui.networkRpcUrl), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Node height", ui.chainHeight.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Network height", ui.networkHeight.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Difficulty", ui.stats.difficulty.toString(), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Total emission", "%.4f ZKA".format(ui.stats.emissionZka), mono = true)
                com.zsilent.core.ui.onboarding.NoirKeyValue("Miners on net", ui.stats.minersOnline.toString())
                ui.miningDiagnostic?.takeIf { it.isNotBlank() }?.let { diagnostic ->
                    Text(diagnostic, color = com.zsilent.core.ui.onboarding.NoirError, fontSize = 11.sp)
                }
            }
        }

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
                if (ui.rpcOnline) "LIVE NETWORK" else "LOCAL SNAPSHOT",
                color = com.zsilent.core.ui.onboarding.NoirTextSubtle,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Height",
                value = heightValue.toString(),
                samples = heightHistory,
                accent = com.zsilent.core.ui.onboarding.PastelSalmon,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Hashrate",
                value = formatHashrate(effectiveHashrate),
                samples = hashrateHistory,
                accent = com.zsilent.core.ui.onboarding.PastelViolet,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Balance",
                value = activeWalletTotal(ui, activeWallet),
                samples = balanceHistory,
                accent = com.zsilent.core.ui.onboarding.PastelOrange,
                valueColor = com.zsilent.core.ui.onboarding.PastelOrange,
                hidden = ui.walletLocked || ui.balancesHidden,
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Next unlock",
                value = if (nextUnlockBlocks == null) "—" else "${nextUnlockBlocks} blk",
                samples = unlockHistory,
                accent = com.zsilent.core.ui.onboarding.PastelPink,
                detail = if (nextUnlockHeight == null) "no maturing rewards" else "at height ${nextUnlockHeight}",
                modifier = Modifier.weight(1f)
            )
            com.zsilent.core.ui.onboarding.NoirMetricCard(
                label = "Last block",
                value = lastBlockText,
                samples = lastBlockHistory,
                accent = com.zsilent.core.ui.onboarding.PastelOrange,
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
    val etaLabel = syncEtaLabel(syncSamples, ui.syncRemainingBlocks, ui.networkSynced, ui.rpcOnline)
    val speedLabel = syncSpeedLabel(syncSamples, ui.networkSynced)
    Panel(
        title = "Network sync",
        meta = if (ui.networkSynced) "ok synced" else if (ui.syncBusy) "importing" else if (ui.rpcOnline) "syncing" else "offline",
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

private enum class WalletPage {
    Home,
    Create
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
        title = if (walletPage == WalletPage.Create) "Add wallet" else "Your accounts",
        subtitle = if (walletPage == WalletPage.Create) {
            "Create an additional private account inside ZSilent Core."
        } else {
            "Manage private wallets without exposing keys."
        }
    ) {
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
                        // ── Top action bar (rescan + add wallet) ──────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NoirIconButton(
                                icon = com.zsilent.core.ui.onboarding.BootstrapIcons.ArrowClockwise,
                                contentDescription = "Rescan wallets",
                                enabled = !ui.walletBusy,
                                onClick = { appState.refreshWallets() }
                            )
                        }

                        // ── Hero balance card ─────────────────────────────────
                        WalletHeroCard(
                            ui = ui,
                            activeWallet = activeWallet,
                            onAddWallet = {
                                appState.clearWalletCreationResult()
                                walletPage = WalletPage.Create
                            },
                            onToggleHide = appState::toggleBalances,
                            onRefresh = { appState.refreshWallets() }
                        )

                        // ── Balance breakdown chips ──────────────────────────
                        WalletBreakdownRow(ui = ui, activeWallet = activeWallet)

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
                        com.zsilent.core.ui.onboarding.NoirSection(
                            title = "Recent private activity",
                            meta = if (ui.walletLocked) "locked" else "last ${ui.walletHistory.transactions.size.coerceAtMost(8)}"
                        ) {
                            if (ui.walletLocked) {
                                Text(
                                    "Unlock the wallet to load history.",
                                    color = com.zsilent.core.ui.onboarding.NoirTextMuted,
                                    fontSize = 12.sp
                                )
                            } else if (ui.walletHistory.transactions.isEmpty()) {
                                Text(
                                    "No private transactions yet.",
                                    color = com.zsilent.core.ui.onboarding.NoirTextMuted,
                                    fontSize = 12.sp
                                )
                            } else {
                                ui.walletHistory.transactions.take(8).forEach { tx ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                shortHash(tx.txHash),
                                                color = com.zsilent.core.ui.onboarding.NoirText,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp
                                            )
                                            CopyIconButton(tx.txHash, enabled = tx.txHash.isNotBlank())
                                        }
                                        Text(
                                            tx.status,
                                            color = com.zsilent.core.ui.onboarding.NoirTextMuted,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                    }

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
    onRefresh: () -> Unit = {}
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    val address = activeWallet?.address ?: ui.activeWalletAddress.ifBlank { "" }
    val totalAtoms = ui.walletBalance?.totalBalanceAtoms ?: activeWallet?.totalBalanceAtoms ?: 0L
    val hidden = ui.balancesHidden || ui.walletLocked
    val (whole, decimals) = splitZkaForDisplay(totalAtoms)

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp
    ) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .background(com.zsilent.core.ui.onboarding.NoirCardGradient, shape)
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
                // Refresh — black button, actually triggers refresh
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
            .background(androidx.compose.ui.graphics.Color(0xFF050506), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription,
            tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
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
fun SendScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
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

    ScreenScaffold("Send", "Private zero-knowledge transfer.") {
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
                        Row(Modifier.fillMaxWidth().background(Color(0xFF3C3E44), RoundedCornerShape(10.dp)).padding(12.dp),
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
                .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape)
                .border(BorderStroke(2.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
                    .graphicsLayer(scaleX = checkScale.value, scaleY = checkScale.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Transfer sent!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
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
        "confirmed", "finalized" -> Color(0xFF2E7D32)
        "pending"                -> Color(0xFFF57C00)
        "failed"                 -> Color(0xFFD32F2F)
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
fun ReceiveScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    ScreenScaffold("Receive", "Show a receive address and QR code.") {
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
    val dotOn = Color(0xFF22C55E)
    val dotWarming = Color(0xFFEAB308)
    val dotOff = Color(0xFF722F37)
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

    ScreenScaffold("Node", "Run and monitor the Zoka full node.") {
        Panel(title = "Node controls", meta = ui.nodeStatus.name) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SessionReadyCard(
                    title = "Wallet session",
                    ready = ui.sessionUnlocked,
                    detail = if (ui.sessionUnlocked) "Unlocked; node can use the in-memory session." else "Unlock the wallet first.",
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Node", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = nodeDotColor)
                        }
                        Switch(
                            checked = nodeOn,
                            enabled = nodeSwitchEnabled,
                            colors = com.zsilent.core.ui.components.zSwitchColors(),
                            onCheckedChange = { wantOn ->
                                if (wantOn && canStartNode) {
                                    appState.startNode(autoMine = false)
                                } else if (!wantOn && canStopNode) {
                                    appState.stopNode()
                                }
                            }
                        )
                    }
                    Text(
                        nodeStateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = nodeDotColor
                    )
                }
            }
            val nodeHint = when {
                nodeWarming && !nodeFullyOn -> "Node is starting. Wait for RPC and sync warmup (can take up to a minute)."
                nodeFullyOn -> "Node is online. Switch off to stop the full node process."
                miningOwnsLocalState -> "Mining owns the chain state. Stop mining first to free the node."
                !ui.sessionUnlocked -> "Unlock the wallet session first. ZSilent will not ask for the password again on this panel."
                else -> "Switching ON starts the full node in node-only mode (no mining)."
            }
            Text(
                nodeHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (miningOwnsLocalState) {
                ErrorText("Mining is already using the chain state. Stop that miner first, or run node + mining from ZSilent as one integrated session.")
            }
        }

        if (nodeProgressActive) {
            RuntimeProgressPanel(
                title = "Node startup",
                phase = if (ui.nodeStatus == ServiceStatus.Starting) "Launching node process" else "Waiting for RPC",
                detail = if (ui.networkSynced) "Node chain is matched with the network. Mining can be enabled." else "The process is alive; ZSilent is waiting for node RPC, peer warmup and chain sync.",
                startedAtMillis = nodeProgressStartedAt,
                rows = listOf(
                    "Node RPC" to if (ui.localRpcOnline) "online" else "waiting",
                    "Bootstrap RPC" to if (ui.rpcOnline) "online" else "waiting",
                    "Node height" to ui.chainHeight.toString(),
                    "Network height" to ui.networkHeight.toString(),
                    "Remaining" to ui.syncRemainingBlocks.toString(),
                    "Pending queue" to ui.pendingPrivateTransfers.count.toString(),
                    "Mining" to if (ui.mining.running) "active" else "not active"
                )
            )
        }

        NetworkSyncPanel(ui, onSyncNow = appState::syncNow)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Status", ui.nodeStatus.name, if (ui.localRpcOnline) "node RPC online" else "node RPC waiting", Modifier.weight(1f))
            StatusCard("Height", ui.chainHeight.toString(), "node chain tip", Modifier.weight(1f))
            StatusCard("Queue", ui.pendingPrivateTransfers.count.toString(), "private txs waiting", Modifier.weight(1f))
            StatusCard("Difficulty", ui.stats.difficulty.toString(), "network", Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(title = "Zoka node", meta = if (ui.localRpcOnline) "online" else "offline", modifier = Modifier.weight(1f)) {
                KeyValueRow("RPC", if (ui.localRpcOnline) "online" else "offline")
                KeyValueRow("RPC address", ZokaNetworkConfig.displayUrl(ui.localRpcUrl))
                CopyableKeyValueRow("Bootstrap", ui.bootstrapPeer)
                KeyValueRow("Zoka binaries", if (ui.zokaReady) "ready" else "missing")
            }

            Panel(title = "Network state", meta = ZokaNetworkConfig.Network, modifier = Modifier.weight(1f)) {
                KeyValueRow("Bootstrap RPC", if (ui.rpcOnline) ZokaNetworkConfig.displayUrl(ui.networkRpcUrl) else "offline")
                KeyValueRow("Sync gate", if (ui.networkSynced) "mining enabled" else "mining locked")
                KeyValueRow("Connected peers", ui.stats.connectedPeers.toString())
                KeyValueRow("Online miners", ui.stats.minersOnline.toString())
                KeyValueRow("Emission", "%.4f ZKA".format(ui.stats.emissionZka))
                KeyValueRow("Miner state blocks", ui.mining.blocksMined.toString())
                KeyValueRow("Miner state rewards", "${ui.mining.totalRewards} ZKA")
                KeyValueRow("Supply emitted", "${ui.mining.totalSupplyEmitted} ZKA")
                KeyValueRow("Pending queue DB", ui.pendingPrivateTransfers.db.ifBlank { ".zoka/${ZokaNetworkConfig.Network}/blockchain.db" })
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
        ui.networkSynced &&
        ui.miningStatus != ServiceStatus.Starting &&
        !ui.mining.running &&
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

    ScreenScaffold("Mining", "Configure mining without exposing wallet secrets.") {
        Panel(title = "Mining controls", meta = ui.miningStatus.name) {
            val miningFullyOn = ui.localMiningEnabled ||
                ui.mining.running ||
                (ui.miningStatus == ServiceStatus.Online && ui.nodeStatus == ServiceStatus.Online)
            val miningMeasuring = miningFullyOn && effectiveHashrate <= 0L && !miningFaulted
            val miningWarming = !miningFullyOn && (
                miningRequestActive ||
                    ui.miningStatus == ServiceStatus.Starting ||
                    ui.nodeStatus == ServiceStatus.Starting ||
                    ui.nodeStatus == ServiceStatus.Stopping
                )
            val miningOn = miningFullyOn || miningWarming
            val nodeOnline = ui.nodeStatus == ServiceStatus.Online
            val miningPrereqs = nodeOnline && ui.networkSynced
            val miningSwitchEnabled = if (miningOn) canStopMining else (canStart && miningPrereqs)
            val dotColorOn = Color(0xFF22C55E)
            val dotColorWarming = Color(0xFFEAB308)
            val dotColorOff = Color(0xFF722F37)
            val miningDotColor = when {
                miningFaulted -> dotColorOff
                miningFullyOn -> dotColorOn
                miningWarming -> dotColorWarming
                else -> dotColorOff
            }
            val miningStateLabel = when {
                miningFaulted -> "ERROR"
                miningFullyOn && miningMeasuring -> "ON · MEASURING"
                miningFullyOn -> "ON"
                miningWarming -> "WARMING"
                else -> "OFF"
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SessionReadyCard(
                    title = "Reward session",
                    ready = ui.sessionUnlocked,
                    detail = if (ui.sessionUnlocked) "Unlocked; rewards use the selected wallet session." else "Unlock the wallet first.",
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = threads,
                    onValueChange = { value -> threads = value.filter { it.isDigit() }.take(2) },
                    label = { Text("Threads") },
                    singleLine = true,
                    enabled = !miningOn,
                    isError = threads.isNotBlank() && parsedThreads == null,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.widthIn(min = 110.dp, max = 140.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mining", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = miningDotColor)
                        }
                        Switch(
                            checked = miningOn,
                            enabled = miningSwitchEnabled,
                            colors = com.zsilent.core.ui.components.zSwitchColors(),
                            onCheckedChange = { wantOn ->
                                if (wantOn && canStart && miningPrereqs) {
                                    miningRequestActive = true
                                    miningProgressStartedAt = System.currentTimeMillis()
                                    appState.startMining(parsedThreads ?: 1)
                                } else if (!wantOn && canStopMining) {
                                    appState.stopMining()
                                }
                            }
                        )
                    }
                    Text(
                        miningStateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = miningDotColor
                    )
                }
            }
            val hintText = when {
                !ui.miningDiagnostic.isNullOrBlank() -> ui.miningDiagnostic.orEmpty()
                miningWarming && !miningFullyOn -> "Warming up. Mining will start alongside the node - this can take 30-60 seconds before hashrate is reported."
                miningFullyOn && miningMeasuring -> "Mining is running. Measuring hashrate; the first sample lands after the first PoW solve. Switch off to return to node-only mode."
                miningFullyOn -> "Mining is running with the node (integrated). Switch off to return to node-only mode."
                !nodeOnline -> "Start the Node first in the Node section. Mining unlocks when the node is online."
                !ui.networkSynced -> "Wait for node sync (${ui.syncRemainingBlocks} block(s) remaining). Mining unlocks when synced."
                !rewardAddress.startsWith("zka1") -> "Pick a wallet with a zka1 address before mining."
                !ui.sessionUnlocked -> "Unlock the wallet session first. ZSilent will reuse it for mining rewards."
                parsedThreads == null -> "Set a valid number of mining threads."
                nodeChangingState -> "Wait for the node to finish starting or stopping."
                else -> "Switching ON will start mining alongside the running node."
            }
            Text(
                hintText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (externalMining) {
                ErrorText("Mining is active outside ZSilent. The app can monitor it, but stop it from the console that started it.")
            }
            ui.miningBlockReason?.takeIf { it.isNotBlank() }?.let { blockReason ->
                ErrorText(blockReason)
            }
            ui.miningDiagnostic?.takeIf { it.isNotBlank() && it != ui.miningBlockReason }?.let { diagnostic ->
                ErrorText(diagnostic)
            }
            val lastMiningLog = ui.logs.firstOrNull { it.source == "mining" }?.message
            if (!lastMiningLog.isNullOrBlank()) {
                Text(
                    "Last miner event: $lastMiningLog",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        NetworkSyncPanel(ui, onSyncNow = appState::syncNow)

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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard(
                "Status",
                ui.miningStatus.name,
                if (!ui.networkSynced) "sync locked" else if (miningFaulted) "check logs" else if (ui.miningControlledByApp) "ZSilent miner" else if (ui.localMiningEnabled || ui.mining.running) "node or external miner" else "idle",
                Modifier.weight(1f)
            )
            StatusCard("Hashrate", formatHashrate(effectiveHashrate), ui.mining.algorithm.ifBlank { "waiting" }, Modifier.weight(1f))
            StatusCard("Miners", ui.stats.minersOnline.toString(), "online", Modifier.weight(1f))
            StatusCard("Height", ui.chainHeight.toString(), "network ${ui.networkHeight}", Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(title = "Reward target", meta = "address only", modifier = Modifier.weight(1f)) {
                CopyableKeyValueRow("Wallet", activeWallet?.name ?: ui.activeWalletName.ifBlank { "not selected" })
                CopyableKeyValueRow("Address", activeWallet?.address ?: rewardAddress.ifBlank { "not selected" })
                KeyValueRow("Available CPU", ui.mining.availableCpuThreads.coerceAtLeast(1).toString())
                KeyValueRow("Mode", ui.mining.mode.ifBlank { "pow-${ZokaNetworkConfig.Network}" })
                KeyValueRow("Network miners", ui.stats.minersOnline.toString())
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

@Composable
fun PrivacyScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var hostText by remember(ui.torSocksHost) { mutableStateOf(ui.torSocksHost) }
    var portText by remember(ui.torSocksPort) { mutableStateOf(ui.torSocksPort.toString()) }
    ScreenScaffold("Privacy", "Privacy controls for secrets, logs and display safety.") {
        Panel(title = "Tor privacy", meta = if (ui.torOnly) "tor-only" else if (ui.torEnabled) "enabled" else "off") {
            ToggleRow("Enable Tor proxy for node networking", ui.torEnabled, appState::setTorEnabled)
            ToggleRow("Require Tor-only seed connections", ui.torOnly, appState::setTorOnly)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hostText,
                    onValueChange = { hostText = it; appState.setTorSocksHost(it) },
                    label = { Text("SOCKS host") }, singleLine = true,
                    colors = zFieldColors(), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5); appState.setTorSocksPort(portText) },
                    label = { Text("SOCKS port") }, singleLine = true,
                    colors = zFieldColors(), modifier = Modifier.weight(1f)
                )
            }
            KeyValueRow("Launch flag", if (ui.torEnabled) "--tor-proxy ${ui.torSocksHost}:${ui.torSocksPort}" else "not used")
            KeyValueRow("Clearnet seeds", if (ui.torOnly) "blocked by policy" else "allowed by node config")
        }
        Panel(title = "Display privacy", meta = if (ui.balancesHidden) "hidden" else "visible") {
            ToggleRow("Hide balances on screen", ui.balancesHidden) { appState.toggleBalances() }
            ToggleRow("Wallet session", !ui.appLocked) { appState.lockApp("Locked manually.") }
        }
        Panel(title = "Security posture", meta = "default") {
            KeyValueRow("Telemetry", "disabled")
            KeyValueRow("Private keys in logs", "blocked")
            KeyValueRow("Encrypted exports", "required")
            KeyValueRow("Auto-lock", "${ui.onboardingAutoLockMinutes} min")
            KeyValueRow("Network privacy", if (ui.torOnly) "Tor-only" else if (ui.torEnabled) "Tor preferred" else "direct")
        }
    }
}

@Composable
fun LogsScreen(ui: ZSilentUiState) {
    ScreenScaffold("Logs", "Runtime logs with sensitive lines redacted.") {
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
                            Text(
                                logCategoryLabel(log.source),
                                modifier = Modifier.widthIn(min = 72.dp, max = 96.dp),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = logSourceColor(log.source)
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

@Composable
private fun logSourceColor(source: String): Color {
    return when (source.lowercase()) {
        "mining" -> Color(0xFFEAB308)
        "node" -> Color(0xFF22C55E)
        "wallet" -> Color(0xFF79A7FF)
        "sync" -> MaterialTheme.colorScheme.tertiary
        "privacy" -> Color(0xFFA78BFA)
        "send" -> Color(0xFFFB7185)
        "system", "app" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun logCategoryLabel(source: String): String {
    return "[${source.ifBlank { "runtime" }.uppercase()}]"
}

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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMenuCard(
                title = "Security",
                detail = "Change PIN, recovery phrase and private keys",
                icon = Icons.Outlined.Lock,
                meta = "secure",
                modifier = Modifier.weight(1f),
                onClick = { onOpen(SettingsPage.Security) }
            )
            SettingsMenuCard(
                title = "Network",
                detail = if (ui.rpcOnline) "RPC online" else "RPC offline",
                icon = Icons.Outlined.Sync,
                meta = "rpc",
                modifier = Modifier.weight(1f),
                onClick = { onOpen(SettingsPage.Network) }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMenuCard(
                title = "About",
                detail = "ZSilent Core, Zoka and runtime details",
                icon = Icons.Outlined.Info,
                meta = "details",
                modifier = Modifier.weight(1f),
                onClick = { onOpen(SettingsPage.About) }
            )
            SettingsMenuCard(
                title = "Danger zone",
                detail = "Clear app setup and return to onboarding",
                icon = Icons.Outlined.WarningAmber,
                meta = "reset",
                modifier = Modifier.weight(1f),
                onClick = { onOpen(SettingsPage.Danger) }
            )
        }
    }
}

@Composable
private fun SettingsMenuCard(
    title: String,
    detail: String,
    icon: ImageVector,
    meta: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Panel(modifier = modifier, title = title, meta = meta) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = onClick,
                colors = zButtonColors(),
                shape = MaterialTheme.shapes.small,
                contentPadding = SettingsButtonPadding
            ) {
                Text("Open")
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
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
    Panel(title = "RPC endpoint", meta = if (ui.rpcOnline) "online" else "offline") {
        CopyableKeyValueRow("Endpoint", ZokaNetworkConfig.displayUrl(ui.networkRpcUrl))
        KeyValueRow("Status", if (ui.rpcOnline) "connected" else "unreachable")
        KeyValueRow("Network height", ui.networkHeight.toString())
        KeyValueRow("Node height", ui.chainHeight.toString())
        KeyValueRow("Sync gate", if (ui.networkSynced) "synced" else "syncing")
    }
    Panel(title = "Local node", meta = if (ui.localRpcOnline) "online" else "offline") {
        KeyValueRow("Local RPC", if (ui.localRpcOnline) "${ZokaNetworkConfig.displayUrl(ui.localRpcUrl)} online" else "offline")
        CopyableKeyValueRow("Bootstrap", ui.bootstrapPeer)
        KeyValueRow("Connected peers", ui.stats.connectedPeers.toString())
        KeyValueRow("Network miners", ui.stats.minersOnline.toString())
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
        KeyValueRow("Bootstrap RPC", if (ui.rpcOnline) "${ZokaNetworkConfig.displayUrl(ui.networkRpcUrl)} online" else "offline")
        KeyValueRow("Node RPC", if (ui.localRpcOnline) "${ZokaNetworkConfig.displayUrl(ui.localRpcUrl)} online" else "offline")
        KeyValueRow("Node height", ui.chainHeight.toString())
        KeyValueRow("Network height", ui.networkHeight.toString())
        KeyValueRow("Sync gate", if (ui.networkSynced) "mining enabled" else "mining locked")
        KeyValueRow("Tor policy", if (ui.torOnly) "Tor-only" else if (ui.torEnabled) "Tor preferred" else "direct")
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
    if (!rpcOnline) return "waiting for bootstrap"
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
