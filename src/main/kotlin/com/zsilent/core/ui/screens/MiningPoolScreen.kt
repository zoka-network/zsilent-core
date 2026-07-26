package com.zsilent.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.StatusCard

/**
 * Team mining.
 *
 * Small miners pool hashrate and take turns collecting the block reward. There
 * is no team wallet: the coinbase pays whichever member is next in line, so
 * nobody ever custodies anyone else's earnings.
 *
 * The protocol behind this does not exist yet. Rather than render placeholder
 * hashrates and share counts that would look live, the screen states plainly
 * what is not built — a mining screen that invents numbers is worse than one
 * that admits it has none, because miners make real decisions on those figures.
 */
@Composable
fun MiningPoolScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    ScreenScaffold(
        title = "Mining Pool",
        subtitle = "Combine hashrate with other miners and take turns on the reward."
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard(
                "Team mining",
                "off",
                "not built yet",
                Modifier.weight(1f)
            )
            StatusCard(
                "Your hashrate",
                if ((ui.miningStatus == ServiceStatus.Online)) "solo" else "idle",
                if ((ui.miningStatus == ServiceStatus.Online)) "mining on your own" else "not mining",
                Modifier.weight(1f)
            )
            StatusCard(
                "Members",
                "—",
                "no team",
                Modifier.weight(1f)
            )
        }

        Panel(title = "Not available yet", meta = "in development") {
            Text(
                "Team mining is being built. Nothing on this screen is live yet, and no " +
                    "team can be created or joined.",
                style = MaterialTheme.typography.bodyMedium
            )
            HintText(
                "Until it ships, mining from the Mining section works normally — you " +
                    "mine solo and keep every block you find."
            )
        }

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
                "Because whole blocks rotate rather than being split, short-term " +
                    "earnings are uneven and only average out across many blocks."
            )
            PlannedPoint(
                "Built for people who know each other",
                "A member who finds a block could rewrite it to pay themselves. The " +
                    "team sees that it happened and removes them, but nothing at " +
                    "protocol level can prevent it."
            )
            PlannedPoint(
                "Opt-in, always",
                "Team mining stays off unless you turn it on."
            )
        }

        Panel(title = "Today", meta = "solo mining") {
            KeyValueRow("Block reward", "12 ZKA to whoever mines the block")
            KeyValueRow("Your mining", if ((ui.miningStatus == ServiceStatus.Online)) "running" else "stopped")
            KeyValueRow("Node", if (ui.localRpcOnline) "online" else "offline")
        }
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
