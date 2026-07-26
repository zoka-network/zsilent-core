package com.zsilent.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.LegendDot
import com.zsilent.core.ui.components.MeterBar
import com.zsilent.core.ui.components.MetricAccent
import com.zsilent.core.ui.components.MetricTile
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.SegmentedSelector
import com.zsilent.core.ui.components.zFieldColors
import com.zsilent.core.ui.components.zSwitchColors

/**
 * Team mining.
 *
 * Small miners pool hashrate and take turns collecting the block reward. There
 * is no team wallet: the coinbase pays whichever member is next in line, so
 * nobody ever custodies anyone else's earnings.
 *
 * The pooling protocol is not built yet, so every team figure renders dimmed
 * through [MetricTile]'s `dimmed` flag. Styling an unmeasured number exactly
 * like a live one invites a miner to plan around it — the controls are laid
 * out and legible, but never dressed up as readings.
 */
@Composable
fun MiningPoolScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    val soloMining = ui.miningStatus == ServiceStatus.Online
    val availableCores = ui.mining.availableCpuThreads.coerceAtLeast(1)

    var teamEnabled by remember { mutableStateOf(false) }
    var powerLevel by remember { mutableStateOf(PowerLevel.Normal) }
    var teamCode by remember { mutableStateOf("") }

    val contributedCores = powerLevel.coresFrom(availableCores)
    val soloHashrate = ui.mining.hashrateHps

    ScreenScaffold(
        title = "Mining Pool",
        subtitle = "Combine hashrate with other miners and take turns on the reward."
    ) {
        // ── Live metrics ─────────────────────────────────────────────────
        // Solo figures are real; team figures are dimmed until the protocol
        // exists.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "Your hashrate",
                value = formatHashrate(soloHashrate),
                caption = if (soloMining) "mining solo right now" else "mining stopped",
                accent = if (soloMining) MetricAccent.Green else MetricAccent.Neutral,
                dimmed = !soloMining,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Contributed",
                value = "$contributedCores / $availableCores",
                caption = "cores you would share",
                accent = MetricAccent.Blue,
                fraction = contributedCores.toFloat() / availableCores.toFloat(),
                dimmed = !teamEnabled,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Team hashrate",
                value = "—",
                caption = "needs the pooling protocol",
                accent = MetricAccent.Violet,
                dimmed = true,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Next payout",
                value = "—",
                caption = "no team yet",
                accent = MetricAccent.Amber,
                dimmed = true,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Master control ───────────────────────────────────────────────
        Panel(
            title = "Team mining",
            meta = if (teamEnabled) "armed" else "off"
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Share hashrate with a team",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Off by default. Your solo mining is unaffected either way.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = teamEnabled,
                    onCheckedChange = { teamEnabled = it },
                    colors = zSwitchColors()
                )
            }

            if (teamEnabled) {
                HintText(
                    "Saved as a preference only. Nothing is shared yet — the pooling " +
                        "protocol still has to ship before this connects to anyone."
                )
            }
        }

        // ── Power allocation ─────────────────────────────────────────────
        Panel(title = "Power to contribute", meta = "$availableCores cores detected") {
            SegmentedSelector(
                options = PowerLevel.entries.map { it.label },
                selected = powerLevel.label,
                onSelect = { picked ->
                    powerLevel = PowerLevel.entries.first { it.label == picked }
                },
                enabled = teamEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            MeterBar(
                fraction = contributedCores.toFloat() / availableCores.toFloat(),
                accent = MetricAccent.Blue,
                dimmed = !teamEnabled
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                LegendDot("$contributedCores shared", MetricAccent.Blue)
                LegendDot("${availableCores - contributedCores} kept", MetricAccent.Neutral)
            }
            Text(
                powerLevel.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Team membership ──────────────────────────────────────────────
        Panel(title = "Your team", meta = "none joined") {
            OutlinedTextField(
                value = teamCode,
                onValueChange = { teamCode = it.trim().uppercase() },
                label = { Text("Team invite code") },
                singleLine = true,
                enabled = false,
                colors = zFieldColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            HintText("Joining opens once the pooling protocol ships.")
        }

        // ── Design notes ─────────────────────────────────────────────────
        Panel(title = "How it will work", meta = "design") {
            PlannedPoint(
                "Nobody holds your rewards",
                "There is no team wallet. Each block pays a member's own address " +
                    "directly, so no coordinator ever holds funds that belong to you."
            )
            PlannedPoint(
                "Turns follow contribution",
                "Members submit partial solutions to show how much work they did. " +
                    "Whoever is owed the most is named as the next block's recipient."
            )
            PlannedPoint(
                "Payouts even out over time",
                "Whole blocks rotate rather than being split, so short-term earnings " +
                    "are uneven and only average out across many blocks."
            )
            PlannedPoint(
                "Built for people who know each other",
                "A member who finds a block could rewrite it to pay themselves. The " +
                    "team sees it happened and removes them, but nothing at protocol " +
                    "level can prevent it."
            )
        }

        // ── What works today ─────────────────────────────────────────────
        Panel(title = "Today", meta = "solo mining") {
            KeyValueRow("Block reward", "12 ZKA to whoever mines the block")
            KeyValueRow("Blocks you mined", ui.mining.blocksMined.toString())
            KeyValueRow("Total rewards", "${ui.mining.totalRewards} ZKA")
            KeyValueRow("Your mining", if (soloMining) "running" else "stopped")
            KeyValueRow("Node", if (ui.localRpcOnline) "online" else "offline")
        }
    }
}

/** How much of the machine to hand to the team. */
private enum class PowerLevel(
    val label: String,
    val explanation: String
) {
    Low(
        "Low",
        "About a quarter of your cores. Leaves the machine comfortable for everyday use."
    ),
    Normal(
        "Normal",
        "About half your cores. A balance between contributing and keeping the machine responsive."
    ),
    Max(
        "Max",
        "Every core. Fastest contribution, and the machine will feel it."
    );

    /** Cores this level allocates, never zero and never above what exists. */
    fun coresFrom(available: Int): Int = when (this) {
        Low -> (available / 4).coerceAtLeast(1)
        Normal -> (available / 2).coerceAtLeast(1)
        Max -> available
    }
}

@Composable
private fun PlannedPoint(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Hashes per second in the largest unit that keeps the figure readable. */
private fun formatHashrate(hps: Long): String = when {
    hps <= 0 -> "0 H/s"
    hps >= 1_000_000_000 -> "%.2f GH/s".format(hps / 1_000_000_000.0)
    hps >= 1_000_000 -> "%.2f MH/s".format(hps / 1_000_000.0)
    hps >= 1_000 -> "%.2f kH/s".format(hps / 1_000.0)
    else -> "$hps H/s"
}
