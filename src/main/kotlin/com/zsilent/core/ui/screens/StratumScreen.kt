package com.zsilent.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyButton
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.ThinDivider
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.onboarding.NoirScreenShell
import com.zsilent.core.ui.theme.ZTheme
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Stratum: XMRig, u otro minero `rx/0`, mina contra el nodo de esta PC.
 *
 * El servidor es del nodo (`--stratum`, docs/QUANTIC-POOL-DE-MINERIA.md en el
 * repo del nodo): cada bloque que encuentra un minero le paga a la dirección
 * con la que entra. ZSilent lo enciende, muestra lo que informa el nodo y arma
 * la línea de XMRig. No trae ni lanza XMRig: es un programa aparte, y los
 * antivirus lo tratan como minero.
 */
@Composable
fun StratumScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    val state = stratumScreenState(ui)
    val shortAddress = ui.wallets.firstOrNull { it.address == ui.activeWalletAddress }
        ?.shortAddress
        ?.takeIf { it.isNotBlank() }
    LaunchedEffect(shortAddress, ui.localRpcOnline) {
        if (shortAddress != null && ui.localRpcOnline) appState.refreshShortAddressPublished()
    }

    NoirScreenShell(
        eyebrow = "Mining",
        title = "Stratum for XMRig",
        subtitle = "Let XMRig mine against your own node. Every block a miner finds pays the address it logs in with."
    ) {
        StratumStateBanner(ui, state, appState)

        val status = ui.stratum
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Miners", (status?.miners ?: 0).toString(), "connected now", Modifier.weight(1f))
            StatusCard(
                "Shares",
                (status?.acceptedShares ?: 0).toString(),
                "${status?.rejectedShares ?: 0} rejected",
                Modifier.weight(1f)
            )
            StatusCard("Blocks", (status?.blocksFound ?: 0).toString(), "found since the node started", Modifier.weight(1f))
            StatusCard(
                "Port",
                ZokaNetworkConfig.StratumPort.toString(),
                if (ui.stratumAllowLan) "this PC + local network" else "this PC only",
                Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)) {
            ConnectXmrigPanel(
                ui = ui,
                shortAddress = shortAddress,
                onPublish = appState::publishActiveAddress,
                modifier = Modifier.weight(1f)
            )
            Panel(title = "Before you start", meta = "read once", modifier = Modifier.weight(1f)) {
                HintText(
                    "XMRig is a separate program, published at xmrig.com. ZSilent does not include it " +
                        "and never starts it."
                )
                HintText(
                    "Antivirus programs, Windows Defender included, flag CPU miners such as XMRig as a " +
                        "threat and stop them. Whether to allow it is your call; if you do, use a copy from " +
                        "the official site and allow only that folder."
                )
                HintText(
                    "This is solo mining through your node, not a shared pool: a block pays its whole " +
                        "reward to the miner that found it, and nothing is split."
                )
                HintText("ZSilent's own miner and the Stratum can run together, but they share the CPU.")
            }
        }
    }
}

/** Lo que se lee de un vistazo arriba de todo. */
internal enum class StratumScreenState(val label: String) {
    Off("OFF"),
    Waiting("WAITING"),
    NotListening("NOT LISTENING"),
    Ready("READY"),
    Mining("MINING"),
}

internal fun stratumScreenState(ui: ZSilentUiState): StratumScreenState {
    val nodeOnline = ui.nodeStatus == ServiceStatus.Online && ui.localRpcOnline
    val status = ui.stratum
    return when {
        !ui.stratumEnabled -> StratumScreenState.Off
        !nodeOnline || status == null -> StratumScreenState.Waiting
        status.listening.isNullOrBlank() -> StratumScreenState.NotListening
        status.miners > 0 -> StratumScreenState.Mining
        else -> StratumScreenState.Ready
    }
}

@Composable
private fun StratumStateBanner(ui: ZSilentUiState, state: StratumScreenState, appState: ZSilentAppState) {
    val tint = when (state) {
        StratumScreenState.Mining -> ZTheme.semantics.success
        StratumScreenState.Waiting, StratumScreenState.Ready -> ZTheme.semantics.warning
        StratumScreenState.NotListening -> ZTheme.semantics.danger
        StratumScreenState.Off -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val nodeChangingState = ui.nodeStatus == ServiceStatus.Starting || ui.nodeStatus == ServiceStatus.Stopping

    Panel(title = "Stratum server", meta = state.label.lowercase()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.label, style = ZTheme.text.monoDisplay, color = tint)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        "node ${if (ui.localRpcOnline) "online" else "offline"}",
                        if (ui.localRpcOnline) ZTone.On else ZTone.Off
                    )
                    Pill(
                        ui.stratum?.listening?.let { "listening $it" } ?: "not listening",
                        if (ui.stratum?.listening.isNullOrBlank()) ZTone.Off else ZTone.On
                    )
                    Pill("rx/0", ZTone.Info)
                }
            }
            ZButton(
                onClick = { appState.setStratumEnabled(!ui.stratumEnabled) },
                variant = if (ui.stratumEnabled) ZButtonVariant.Danger else ZButtonVariant.Accent,
                enabled = !nodeChangingState,
            ) {
                Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(14.dp))
                ZButtonText(if (ui.stratumEnabled) "Turn off" else "Turn on")
            }
        }
        ToggleRow("Accept miners from my local network", ui.stratumAllowLan) { appState.setStratumAllowLan(it) }
        HintText(
            when (state) {
                StratumScreenState.Off ->
                    "Off. Turning it on restarts the node with the Stratum open on port " +
                        "${ZokaNetworkConfig.StratumPort}; solo mining comes back on if it was on."
                StratumScreenState.Waiting -> "On. The Stratum opens when the node is online."
                StratumScreenState.NotListening ->
                    "The node did not open port ${ZokaNetworkConfig.StratumPort}. Another program may be " +
                        "using it, or the node is older than 3.0.2. Check Logs."
                StratumScreenState.Ready -> "Listening. Start XMRig with the line below."
                StratumScreenState.Mining -> "XMRig is connected and mining against your node."
            }
        )
        if (ui.stratumAllowLan) {
            HintText(
                "Open to your local network: other PCs connect to this PC's local address. The first " +
                    "time, Windows asks whether zoka.exe may accept connections; allow private networks only."
            )
        }
    }
}

@Composable
private fun ConnectXmrigPanel(
    ui: ZSilentUiState,
    shortAddress: String?,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val published = ui.activeShortAddressPublished == true
    // Con la corta publicada, el minero cobra exactamente a esta wallet. Sin
    // publicar, "x" no es una dirección y el nodo le paga a la suya, que es la
    // de la wallet activa cuando arrancó.
    val login = if (published && shortAddress != null) shortAddress else "x"
    val lanAddresses = remember(ui.stratumAllowLan) {
        if (ui.stratumAllowLan) localLanAddresses() else emptyList()
    }

    Panel(title = "Connect XMRig", meta = "same PC", modifier = modifier) {
        XmrigCommand(xmrigCommandLine("127.0.0.1", login))
        if (published) {
            HintText("Blocks pay the active wallet: XMRig logs in with its short address.")
        } else {
            HintText(
                "With \"-u x\" blocks pay the address the node mines to. Publish the active wallet's " +
                    "short address to log in with it instead."
            )
            if (shortAddress != null) {
                ZButton(
                    onClick = onPublish,
                    variant = ZButtonVariant.Outline,
                    size = ZButtonSize.Small,
                    enabled = ui.localRpcOnline && !ui.addressPublishBusy,
                ) {
                    ZButtonText(if (ui.addressPublishBusy) "Publishing…" else "Publish short address")
                }
            }
        }
        if (lanAddresses.isNotEmpty()) {
            ThinDivider()
            Text("From another PC", style = ZTheme.text.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
            lanAddresses.forEach { host -> KeyValueRow("This PC", "$host:${ZokaNetworkConfig.StratumPort}") }
            XmrigCommand(xmrigCommandLine(lanAddresses.first(), login))
        }
    }
}

@Composable
private fun XmrigCommand(command: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .padding(12.dp)
        ) {
            SelectionContainer {
                Text(command, style = ZTheme.text.mono, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        CopyButton(command, label = "Copy command")
    }
}

/** La línea de XMRig contra el Stratum de este nodo. */
internal fun xmrigCommandLine(host: String, login: String): String =
    "xmrig -o $host:${ZokaNetworkConfig.StratumPort} -u $login -p x -a rx/0"

/** Las direcciones IPv4 de esta PC en la red local (para minar desde otra PC). */
private fun localLanAddresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .filter { it.isSiteLocalAddress }
        .map { it.hostAddress }
        .distinct()
}.getOrDefault(emptyList())
