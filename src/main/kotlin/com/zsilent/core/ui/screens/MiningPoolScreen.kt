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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import com.zsilent.core.ui.components.PanelHeader
import com.zsilent.core.ui.theme.ZTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.Icon
import com.zsilent.core.ui.components.ZInput
import com.zsilent.core.ui.components.ZLabel
import androidx.compose.foundation.layout.IntrinsicSize
import com.zsilent.core.ui.components.Stat
import com.zsilent.core.ui.components.Truncate
import com.zsilent.core.ui.components.CopyIconButton
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

        // La tarjeta "De donde salen estos numeros" se quito: ademas de sobrar
        // en esta pantalla, estaba en espanol y el resto de la interfaz esta en
        // ingles. La distincion entre dato real y simulado sigue viva en las
        // pills de cada metrica.

        if (status != null && team == null) {
            // Dos paneles lado a lado, como el prototipo: crear a la izquierda,
            // unirse con codigo a la derecha. Antes era un solo panel con el
            // campo de codigo arriba de los dos botones, lo que sugeria que
            // hacia falta un codigo tambien para crear.
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                Panel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    PanelHeader(
                        title = "Create a team",
                        subtitle = "Generate a shareable code peers can join"
                    )
                    HintText(
                        "Creates an ad-hoc mining group with pass-through payout. Membership is " +
                            "signed peer-to-peer, and the code stays yours until you disband it."
                    )
                    ZButton(
                        onClick = {
                            runAction("Team created.") {
                                appState.localRpc.createMiningTeam()
                            }
                        },
                        variant = ZButtonVariant.Accent,
                        enabled = actionBusy == null,
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp))
                        ZButtonText("Create team")
                    }
                }

                Panel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    PanelHeader(
                        title = "Join with code",
                        subtitle = "Enter a code from your peer",
                        right = { Pill(status.joinStatus.replace('_', ' '), ZTone.Default) }
                    )
                    ZLabel("Team code")
                    ZInput(
                        value = inviteCode,
                        onValueChange = {
                            inviteCode = it
                                .trim()
                                .uppercase()
                                .filter { char -> char.isLetterOrDigit() || char == '-' }
                        },
                        placeholder = "TEAM-XXXXXX",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZButton(
                            onClick = {
                                runAction("Join request sent; waiting for the coordinator.") {
                                    appState.localRpc.joinMiningTeam(inviteCode)
                                }
                            },
                            variant = ZButtonVariant.Outline,
                            enabled = inviteCode.isNotBlank() && actionBusy == null,
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Login, null, modifier = Modifier.size(14.dp))
                            ZButtonText("Join")
                        }
                        ActionProgress(actionBusy)
                    }
                    HintText(
                        if (status.joinStatus == "join_requested") {
                            "The signed join request is in gossip. Keep both nodes online; this " +
                                "screen updates when the coordinator accepts it."
                        } else {
                            "Invite codes travel through the live P2P mesh. The coordinator must " +
                                "be online; there is no central team directory."
                        }
                    )
                }
            }
        }

        if (status != null && team != null) {
            TeamSummaryCards(status, team)

            TeamIdentityPanel(status, team)

            TeamHashrateChart(
                hashrate = status.combinedHashrateHps,
                running = status.hashing,
                members = team.members.values.count { it.active && !it.expelled },
            )
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
                },
                onDisband = {
                    runAction("Team disbanded.") {
                        appState.localRpc.disbandMiningTeam()
                    }
                }
            )
        }

        // Al pie, en la misma posicion que el pre-flight de Mining: es material
        // de referencia, no un control. Arriba competia por atencion con crear
        // y unirse, que es lo que la pantalla viene a resolver.
        if (status != null) {
            HowTeamMiningWorks()
        }
    }
}

/**
 * "Como funciona el team mining", como tira de tres tarjetas al pie de la
 * pantalla.
 *
 * Antes esto era una compuerta con checkbox que bloqueaba crear y unirse. Se
 * saco el bloqueo, pero **no** el contenido: el punto del hijacking describe un
 * ataque que el protocolo detecta y no puede prevenir, y esconderlo antes de que
 * alguien ponga su hashrate en un equipo ajeno seria dejarlo entrar a ciegas.
 *
 * La geometria es la misma que la tira de estado de Mining: `IntrinsicSize.Min`
 * para que las tres tarjetas midan lo mismo de alto aunque el texto sea desigual,
 * y `weight(1f)` para repartir el ancho en partes iguales.
 */
@Composable
private fun HowTeamMiningWorks() {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
    ) {
        TeamRiskLine(
            "01 · Rewards are statistical",
            "Whole block rewards rotate by contributed work. A short run can be very " +
                "uneven; it only converges toward your share over many blocks.",
            Modifier.weight(1f)
        )
        TeamRiskLine(
            "02 · Hijacking is detectable, not preventable",
            "A member who finds a valid block can rewrite its coinbase to pay themselves. " +
                "The team detects it and expels them, but the network still accepts an " +
                "otherwise-valid block.",
            Modifier.weight(1f)
        )
        TeamRiskLine(
            "03 · Mine with people you know",
            "No team wallet, fee, escrow or recovery desk. The coordinator tracks work and " +
                "templates; it never holds your reward keys.",
            Modifier.weight(1f)
        )
    }
}

/**
 * Una tarjeta de la tira anterior.
 *
 * El `modifier` tiene que llegar al `Panel`: si se queda sin aplicar, los
 * `weight(1f)` del Row se pierden y las tarjetas se reparten el ancho por
 * contenido — la tercera queda de un caracter de ancho y el texto se apila en
 * vertical.
 */
@Composable
private fun TeamRiskLine(title: String, body: String, modifier: Modifier = Modifier) {
    Panel(modifier = modifier.fillMaxHeight()) {
        ZLabel(title)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/**
 * Hashrate combinado del equipo en vivo. Mismo grafico que Mining pero en verde,
 * para que se distinga de un vistazo cual de las dos pantallas estas mirando.
 *
 * El subtitulo dice cuantos miembros activos hay porque el reparto depende de
 * eso: la misma barra con dos miembros o con diez significa cosas distintas
 * para lo que te toca.
 */
@Composable
private fun TeamHashrateChart(hashrate: Long, running: Boolean, members: Int) {
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
    val bar = ZTheme.metrics.liveTeam

    Panel {
        PanelHeader(
            title = "Live team hashrate",
            subtitle = "Last ~20 seconds · " + members + " active member(s)",
            right = { Pill(if (running) "hashing" else "idle", if (running) ZTone.On else ZTone.Off) }
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
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
    }
}

/**
 * Las cuatro tarjetas de arriba del prototipo: codigo, hashrate propio,
 * hashrate del equipo y participacion estimada.
 *
 * La participacion se marca como estimacion y no como dato: sale de comparar tu
 * trabajo contra el del equipo, y ambos se mueven. Mostrarla como un numero
 * firme invitaria a planificar sobre algo que cambia con cada share.
 */
@Composable
private fun TeamSummaryCards(status: TeamMiningStatus, team: MiningTeam) {
    val local = team.members[status.localAddress]
    val activeWork = team.members.values
        .filter { it.active && !it.expelled }
        .sumOf { it.totalWork }
    val sharePct = if (activeWork > 0 && local != null) {
        100.0 * local.totalWork.toDouble() / activeWork.toDouble()
    } else {
        0.0
    }

    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
    ) {
        Panel(modifier = Modifier.weight(1f)) {
            ZLabel("Team code")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Truncate(
                    team.inviteCode,
                    head = 12, tail = 4,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CopyIconButton(team.inviteCode)
            }
        }
        Panel(modifier = Modifier.weight(1f)) {
            Stat(
                "Your hashrate",
                formatHashrate(local?.estimatedHashrateHps ?: 0),
                hint = if (status.hashing) "submitting shares" else "worker stopped"
            )
        }
        Panel(modifier = Modifier.weight(1f)) {
            Stat(
                "Team hashrate",
                formatHashrate(status.combinedHashrateHps),
                hint = "" + team.members.values.count { it.active && !it.expelled } + " active member(s)"
            )
        }
        Panel(modifier = Modifier.weight(1f)) {
            Stat(
                "Your share",
                String.format("%.1f%%", sharePct),
                hint = "estimate · moves with every share"
            )
        }
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
    onLeave: () -> Unit,
    onDisband: () -> Unit,
) {
    val coordinator = status.team?.coordinator == status.localAddress
    val successor = status.team?.backupCoordinator
    var confirmDisband by remember { mutableStateOf(false) }

    Panel(title = "Membership", meta = if (coordinator) "coordinator" else "member") {
        if (!coordinator) {
            ZButton(
                onClick = onLeave,
                variant = ZButtonVariant.Outline,
                enabled = actionBusy == null,
            ) { ZButtonText("Leave team") }
            HintText(
                "Leaving sends a signed request to the coordinator. Keep both nodes online " +
                    "until your membership disappears from this screen."
            )
            return@Panel
        }

        // Coordinador: salir traspasa la coordinacion al backup. Disolver es un
        // camino aparte y con confirmacion, porque afecta a todo el equipo y no
        // solo a quien lo pulsa.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZButton(
                onClick = onLeave,
                variant = ZButtonVariant.Outline,
                enabled = successor != null && actionBusy == null,
            ) { ZButtonText("Hand over and leave") }
            ZButton(
                onClick = { confirmDisband = true },
                variant = ZButtonVariant.Danger,
                enabled = actionBusy == null,
            ) { ZButtonText("Disband team") }
        }

        HintText(
            if (successor != null) {
                "Leaving promotes the backup coordinator and you stay out as a regular member. " +
                    "The team keeps mining."
            } else {
                "There is nobody to hand over to yet, so leaving is not possible. Disbanding is " +
                    "the only way out while you are alone in the team."
            }
        )

        if (confirmDisband) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        ZTheme.semantics.danger.copy(alpha = 0.08f),
                        MaterialTheme.shapes.small
                    )
                    .border(
                        BorderStroke(1.dp, ZTheme.semantics.danger.copy(alpha = 0.3f)),
                        MaterialTheme.shapes.small
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ErrorText("Disband this team?")
                HintText(
                    "The team code stops working and every member loses the group. Creating a " +
                        "team again gives you a NEW code — this one is not recoverable, so anyone " +
                        "holding it will not be able to rejoin."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZButton(
                        onClick = { confirmDisband = false },
                        variant = ZButtonVariant.Ghost,
                        size = ZButtonSize.Small,
                    ) { ZButtonText("Cancel") }
                    ZButton(
                        onClick = { confirmDisband = false; onDisband() },
                        variant = ZButtonVariant.Danger,
                        size = ZButtonSize.Small,
                    ) { ZButtonText("Disband") }
                }
            }
        }
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
