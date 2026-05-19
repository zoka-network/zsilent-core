package com.zsilent.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.zFieldColors
import com.zsilent.core.ui.components.zButtonColors

@Composable
fun ExplorerScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var blockQuery by remember { mutableStateOf(ui.chainHeight.takeIf { it > 0 }?.toString().orEmpty()) }
    val latestBlocks = latestBlockNumbers(ui.chainHeight, count = 5)

    ScreenScaffold(
        title = "Explorer",
        subtitle = "Clean access to chain state without exposing wallet data."
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Height", ui.chainHeight.toString(), "current node tip", Modifier.weight(1f))
            StatusCard("Bootstrap RPC", if (ui.rpcOnline) "Online" else "Offline", ZokaNetworkConfig.displayUrl(ui.networkRpcUrl), Modifier.weight(1f))
            StatusCard("Sync", if (ui.networkSynced) "OK" else ui.syncRemainingBlocks.toString(), "blocks remaining", Modifier.weight(1f))
            StatusCard("Pending", ui.pendingPrivateTransfers.count.toString(), "private tx queue", Modifier.weight(1f))
        }

        Panel(title = "Block lookup", meta = "0 - ${ui.chainHeight.coerceAtLeast(0)}") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = blockQuery,
                    onValueChange = { value -> blockQuery = value.filter { it.isDigit() } },
                    label = { Text("Block height") },
                    singleLine = true,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {},
                    enabled = blockQuery.toLongOrNull()?.let { it in 0..ui.chainHeight } == true,
                    colors = zButtonColors()
                ) {
                    Text("Open")
                }
            }
            val selected = blockQuery.toLongOrNull()
            Text(
                if (selected != null && selected in 0..ui.chainHeight) {
                    "Block #$selected is within the available chain range. Detailed block view will attach to the explorer RPC next."
                } else {
                    "Enter any height from 0 to ${ui.chainHeight.coerceAtLeast(0)}."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(title = "Latest blocks", meta = "visual top 5", modifier = Modifier.weight(1f)) {
                if (latestBlocks.isEmpty()) {
                    EmptyState("No block height detected yet.")
                } else {
                    latestBlocks.forEach { height ->
                        KeyValueRow("Block #$height", if (height == ui.chainHeight) "current tip" else "available")
                    }
                }
            }

            Panel(title = "Pending private transfers", meta = "${ui.pendingPrivateTransfers.count} queued", modifier = Modifier.weight(1f)) {
                KeyValueRow("Queue", ui.pendingPrivateTransfers.count.toString())
                KeyValueRow("Node DB", ui.pendingPrivateTransfers.db.ifBlank { ".zoka/${ZokaNetworkConfig.Network}/blockchain.db" })
                if (ui.pendingPrivateTransfers.pendingPrivateTxHashes.isEmpty()) {
                    EmptyState("No pending private transfers.")
                } else {
                    ui.pendingPrivateTransfers.pendingPrivateTxHashes.take(5).forEachIndexed { index, hash ->
                        CopyableKeyValueRow("Tx ${index + 1}", hash)
                    }
                }
            }
        }
    }
}

private fun latestBlockNumbers(height: Long, count: Int): List<Long> {
    if (height < 0) return emptyList()
    val first = (height - count + 1).coerceAtLeast(0)
    return (height downTo first).toList()
}
