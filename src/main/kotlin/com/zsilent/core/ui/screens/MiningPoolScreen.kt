package com.zsilent.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zsilent.core.models.MiningTeam
import com.zsilent.core.models.TeamMiningStatus
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.ErrorText
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.LegendDot
import com.zsilent.core.ui.components.MeterBar
import androidx.compose.foundation.layout.padding
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.components.MetricAccent
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.SegmentedSelector
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.zButtonColors
import com.zsilent.core.ui.components.zFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Non-custodial team mining backed entirely by the user's local node.
 *
 * The coordinator never owns a team wallet. Every candidate commits one
 * member's normal address as coinbase recipient, and partial solutions only
 * affect whose turn is next.
 */
@Composable
fun MiningPoolScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var feed by remember { mutableStateOf<TeamMiningFeed>(TeamMiningFeed.Loading) }
    var inviteCode by remember { mutableStateOf("") }
    var risksUnderstood by remember { mutableStateOf(false) }
    var powerLevel by remember { mutableStateOf(PowerLevel.Normal) }
    var actionBusy by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        feed = withContext(Dispatchers.IO) {
            try {
                TeamMiningFeed.Loaded(appState.localRpc.teamMiningStatus())
            } catch (_: ZokaRpcClient.EndpointUnsupported) {
                TeamMiningFeed.Unsupported
            } catch (t: Throwable) {
                TeamMiningFeed.Failed(t.message ?: "Could not reach the local node")
            }
        }
    }

    fun runAction(
        label: String,
        action: suspend () -> TeamMiningStatus
    ) {
        if (actionBusy != null) return
        actionBusy = label
        actionError = null
        actionNotice = null
        scope.launch {
            try {
                val status = withContext(Dispatchers.IO) { action() }
                feed = TeamMiningFeed.Loaded(status)
                actionNotice = label
            } catch (_: ZokaRpcClient.EndpointUnsupported) {
                feed = TeamMiningFeed.Unsupported
            } catch (t: Throwable) {
                actionError = t.message ?: "Team Mining request failed"
            } finally {
                actionBusy = null
            }
        }
    }

    LaunchedEffect(ui.localRpcOnline) {
        if (!ui.localRpcOnline) {
            feed = TeamMiningFeed.NodeOffline
            return@LaunchedEffect
        }
        while (isActive) {
            reload()
            delay(5_000)
        }
    }

    val status = (feed as? TeamMiningFeed.Loaded)?.status
    val team = status?.team
    val localMember = team?.members?.get(status.localAddress)
    val availableCores = ui.mining.availableCpuThreads.coerceAtLeast(1)
    val contributedCores = powerLevel.coresFrom(availableCores)

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Mining pool",
        title = "Team mining · experimental",
        subtitle = "Combine hashrate without handing anyone your rewards."
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard(
                "Your hashrate",
                formatHashrate(localMember?.estimatedHashrateHps ?: 0),
                if (status?.hashing == true) "submitting shares" else "team worker stopped",
                Modifier.weight(1f)
            )
            StatusCard(
                "Team hashrate",
                formatHashrate(status?.combinedHashrateHps ?: 0),
                "${team?.members?.values?.count { it.active && !it.expelled } ?: 0} active member(s)",
                Modifier.weight(1f)
            )
            StatusCard(
                "Shares",
                "${status?.localAcceptedShares ?: 0} / ${status?.teamAcceptedShares ?: 0}",
                "yours / team",
                Modifier.weight(1f)
            )
            StatusCard(
                "Est. turn",
                status?.estimatedTurn?.let { "#$it" } ?: "—",
                team?.activeTemplate?.let { "block ${it.height}" } ?: "waiting for template",
                Modifier.weight(1f)
            )
        }

        when (val current = feed) {
            TeamMiningFeed.Loading -> LoadingPanel()
            TeamMiningFeed.NodeOffline -> NodeOfflinePanel()
            TeamMiningFeed.Unsupported -> UnsupportedNodePanel()
            is TeamMiningFeed.Failed -> Panel(title = "Team Mining unavailable", meta = "error") {
                ErrorText(current.message)
                HintText("The app only talks to the node on this device. Start or restart it and try again.")
            }
            is TeamMiningFeed.Loaded -> Unit
        }

        actionError?.let { ErrorText(it) }
        actionNotice?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        DataLegendPanel()

        RiskPanel(
            checked = risksUnderstood,
            onCheckedChange = { risksUnderstood = it }
        )

        if (status != null && team == null) {
            Panel(
                title = "Create or join a team",
                meta = status.joinStatus.replace('_', ' ')
            ) {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = {
                        inviteCode = it
                            .trim()
                            .uppercase()
                            .filter { char -> char.isLetterOrDigit() || char == '-' }
                    },
                    label = { Text("Team invite code") },
                    singleLine = true,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            runAction("Team created.") {
                                appState.localRpc.createMiningTeam()
                            }
                        },
                        enabled = risksUnderstood && actionBusy == null,
                        colors = zButtonColors()
                    ) {
                        Text("Create team")
                    }
                    OutlinedButton(
                        onClick = {
                            runAction("Join request sent; waiting for the coordinator.") {
                                appState.localRpc.joinMiningTeam(inviteCode)
                            }
                        },
                        enabled = risksUnderstood &&
                            inviteCode.isNotBlank() &&
                            actionBusy == null
                    ) {
                        Text("Join team")
                    }
                    ActionProgress(actionBusy)
                }
                if (status.joinStatus == "join_requested") {
                    HintText(
                        "The signed join request is in gossip. Keep both nodes online; this " +
                            "screen will update when the coordinator accepts it."
                    )
                } else {
                    HintText(
                        "Invite codes are discovered through the live P2P mesh. The coordinator " +
                            "must be online; there is no central team directory."
                    )
                }
            }
        }

        if (status != null && team != null) {
            TeamIdentityPanel(status, team)
            MiningControlPanel(
                status = status,
                team = team,
                powerLevel = powerLevel,
                availableCores = availableCores,
                contributedCores = contributedCores,
                actionBusy = actionBusy,
                onPowerLevel = { powerLevel = it },
                onStart = {
                    runAction("Team Mining started with $contributedCores core(s).") {
                        appState.localRpc.startTeamMining(contributedCores)
                    }
                },
                onStop = {
                    runAction("Team Mining stopped; the node remains online.") {
                        appState.localRpc.stopTeamMining()
                    }
                }
            )
            MembersPanel(status, team)
            TeamExitPanel(
                status = status,
                actionBusy = actionBusy,
                onLeave = {
                    runAction("Leave request sent.") {
                        appState.localRpc.leaveMiningTeam()
                    }
                }
            )
        }
    }
}

/**
 * Leyenda de procedencia de los números, portada del prototipo.
 *
 * Esta pantalla mezcla métricas reales del dispositivo con estimaciones y con
 * valores que todavía no produce el protocolo. Sin decir cuál es cuál, una
 * estimación con el mismo aspecto que una lectura real invita a tomar
 * decisiones sobre un número que nadie midió.
 */
@Composable
private fun DataLegendPanel() {
    val rows = listOf(
        Triple("local", ZTone.On, "Métrica real de tu dispositivo"),
        Triple("estimation", ZTone.Info, "Derivada de lo local más los peers"),
        Triple("simulation", ZTone.Warn, "Valor de demostración en este build"),
        Triple("future", ZTone.Off, "Espera protocolo real"),
    )
    Panel(title = "De dónde salen estos números", meta = "leyenda") {
        rows.forEach { (label, tone, detail) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(label, tone)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RiskPanel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Panel(title = "Understand the payout model", meta = "required before joining") {
        TeamRiskLine(
            "Rewards are statistical",
            "Whole block rewards rotate by contributed work. A short run can be very uneven; " +
                "the result only converges toward your share over many blocks."
        )
        TeamRiskLine(
            "Hijacking is detectable, not preventable",
            "A member who finds a valid block can rewrite its coinbase to pay themselves. The " +
                "team detects that receiver and expels the signed member when attributable, " +
                "but the wider network must still accept an otherwise-valid block."
        )
        TeamRiskLine(
            "Mine with people you know",
            "There is no team wallet, fee, escrow or recovery desk. The coordinator tracks work " +
                "and templates; it never holds your reward keys."
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(
                "I understand the uneven payouts and hijack limitation.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TeamRiskLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TeamIdentityPanel(status: TeamMiningStatus, team: MiningTeam) {
    val role = when (status.localAddress) {
        team.coordinator -> "coordinator"
        team.backupCoordinator -> "backup coordinator"
        else -> "member"
    }
    Panel(title = "Your team", meta = role) {
        CopyableKeyValueRow("Invite code", team.inviteCode)
        CopyableKeyValueRow("Team id", team.teamId)
        KeyValueRow("Epoch / revision", "${team.epoch} / ${team.revision}")
        KeyValueRow("Confirmed team blocks", team.confirmedBlocks.toString())
        team.activeTemplate?.let {
            KeyValueRow("Current block", it.height.toString())
            CopyableKeyValueRow("Assigned coinbase", it.recipientAddress)
            KeyValueRow("Network difficulty", "${it.networkDifficultyBits} bits")
        } ?: HintText("The coordinator will publish a template while Team Mining is running.")
        team.lastHijackedRecipient?.let {
            ErrorText("Last hijacked coinbase receiver: $it")
        }
    }
}

@Composable
private fun MiningControlPanel(
    status: TeamMiningStatus,
    team: MiningTeam,
    powerLevel: PowerLevel,
    availableCores: Int,
    contributedCores: Int,
    actionBusy: String?,
    onPowerLevel: (PowerLevel) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Panel(
        title = "Power to contribute",
        meta = when {
            status.hashing -> "hashing"
            status.enabled -> "waiting for template"
            else -> "stopped"
        }
    ) {
        SegmentedSelector(
            options = PowerLevel.entries.map { it.label },
            selected = powerLevel.label,
            onSelect = { picked ->
                onPowerLevel(PowerLevel.entries.first { it.label == picked })
            },
            enabled = !status.enabled && actionBusy == null,
            modifier = Modifier.fillMaxWidth()
        )
        MeterBar(
            fraction = contributedCores.toFloat() / availableCores.toFloat(),
            accent = MetricAccent.Blue,
            dimmed = false
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LegendDot("$contributedCores team", MetricAccent.Blue)
            LegendDot("${availableCores - contributedCores} free", MetricAccent.Neutral)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status.enabled) {
                OutlinedButton(onClick = onStop, enabled = actionBusy == null) {
                    Text("Stop Team Mining")
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = actionBusy == null,
                    colors = zButtonColors()
                ) {
                    Text("Start Team Mining")
                }
            }
            ActionProgress(actionBusy)
        }
        val local = team.members[status.localAddress]
        KeyValueRow("Assigned share target", "${local?.shareDifficultyBits ?: 0} bits")
        KeyValueRow("Accepted work", (local?.totalWork ?: 0).toString())
        HintText(
            "Share difficulty adjusts automatically for a steady submission rate. Team " +
                "Mining takes precedence over solo mining on this same fullnode."
        )
    }
}

@Composable
private fun MembersPanel(status: TeamMiningStatus, team: MiningTeam) {
    val members = team.members.values.sortedWith(
        compareByDescending<com.zsilent.core.models.TeamMiningMember> {
            it.active && !it.expelled
        }.thenBy { it.address }
    )
    Panel(
        title = "Members",
        meta = "${members.count { it.active && !it.expelled }} active"
    ) {
        if (members.isEmpty()) {
            EmptyState("No members have been synchronized yet.")
        } else {
            members.forEach { member ->
                val roles = buildList {
                    if (member.address == status.localAddress) add("you")
                    if (member.address == team.coordinator) add("coordinator")
                    if (member.address == team.backupCoordinator) add("backup")
                    if (member.expelled) add("expelled")
                    else if (!member.active) add("inactive")
                }
                KeyValueRow(
                    shortAddress(member.address),
                    listOf(
                        roles.joinToString(", ").ifBlank { "member" },
                        formatHashrate(member.estimatedHashrateHps),
                        "${member.acceptedShares} shares",
                        "${member.blocksPaid} paid"
                    ).joinToString(" · ")
                )
            }
        }
    }
}

@Composable
private fun TeamExitPanel(
    status: TeamMiningStatus,
    actionBusy: String?,
    onLeave: () -> Unit
) {
    val coordinator = status.team?.coordinator == status.localAddress
    Panel(title = "Membership", meta = if (coordinator) "coordinator" else "member") {
        OutlinedButton(
            onClick = onLeave,
            enabled = !coordinator && actionBusy == null
        ) {
            Text("Leave team")
        }
        HintText(
            if (coordinator) {
                "The coordinator cannot leave abruptly because that would discard the handoff. " +
                    "Stop Team Mining; the signed backup promotes automatically after the timeout " +
                    "if this node goes offline."
            } else {
                "Leaving sends a signed request to the coordinator. Keep both nodes online until " +
                    "your membership disappears from this screen."
            }
        )
    }
}

@Composable
private fun LoadingPanel() {
    Panel(title = "Connecting to Team Mining", meta = "local node") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Reading team state…", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NodeOfflinePanel() {
    Panel(title = "Local node offline", meta = "waiting") {
        EmptyState("Start the local Zoka node. Team Mining never uses a third-party RPC.")
    }
}

@Composable
private fun UnsupportedNodePanel() {
    Panel(title = "Node update required", meta = "unsupported") {
        ErrorText("This node does not expose /team-mining/status.")
        HintText(
            "Rebuild and restart the local node from the matching release. A 404 here means " +
                "the running binary predates Team Mining; it is not an empty team."
        )
    }
}

@Composable
private fun ActionProgress(actionBusy: String?) {
    if (actionBusy != null) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

/** How much of the machine to contribute to the team worker. */
private enum class PowerLevel(val label: String) {
    Low("Low"),
    Normal("Normal"),
    Max("Max");

    fun coresFrom(available: Int): Int = when (this) {
        Low -> (available / 4).coerceAtLeast(1)
        Normal -> (available / 2).coerceAtLeast(1)
        Max -> available
    }
}

private sealed interface TeamMiningFeed {
    data object Loading : TeamMiningFeed
    data object NodeOffline : TeamMiningFeed
    data object Unsupported : TeamMiningFeed
    data class Loaded(val status: TeamMiningStatus) : TeamMiningFeed
    data class Failed(val message: String) : TeamMiningFeed
}

private fun shortAddress(address: String): String =
    if (address.length <= 20) address
    else "${address.take(10)}…${address.takeLast(8)}"

/** Hashes per second in the largest unit that keeps the figure readable. */
private fun formatHashrate(hps: Long): String = when {
    hps <= 0 -> "0 H/s"
    hps >= 1_000_000_000 -> "%.2f GH/s".format(hps / 1_000_000_000.0)
    hps >= 1_000_000 -> "%.2f MH/s".format(hps / 1_000_000.0)
    hps >= 1_000 -> "%.2f kH/s".format(hps / 1_000.0)
    else -> "$hps H/s"
}
