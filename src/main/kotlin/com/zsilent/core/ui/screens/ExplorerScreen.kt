package com.zsilent.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.BlockSummary
import com.zsilent.core.models.BlockTransactions
import com.zsilent.core.models.MempoolTransaction
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.ErrorText
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.zButtonColors
import com.zsilent.core.ui.components.zFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Local chain explorer.
 *
 * Every lookup goes to the user's own node over [ZSilentAppState.localRpc] and
 * nowhere else — no public explorer, no third-party API. That is the whole
 * point of having it inside the wallet: browsing the chain must not tell a
 * remote server which blocks or transactions you were interested in.
 *
 * The cost of that choice is that the explorer can only see what the local
 * node already has, so it reports sync state honestly instead of rendering a
 * stale tip as if it were current.
 */
@Composable
fun ExplorerScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ExplorerLookup>(ExplorerLookup.Idle) }
    val scope = rememberCoroutineScope()

    fun runLookup(raw: String) {
        val term = raw.trim()
        if (term.isEmpty()) return
        result = ExplorerLookup.Loading
        scope.launch {
            result = withContext(Dispatchers.IO) { lookup(appState, term) }
        }
    }

    ScreenScaffold(
        title = "Explorer",
        subtitle = "Clean access to chain state without exposing wallet data."
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Height", ui.chainHeight.toString(), "current node tip", Modifier.weight(1f))
            StatusCard(
                "Peers",
                ui.localConnectedPeers.toString(),
                if (ui.localConnectedPeers > 0) "connected via p2p" else "isolated",
                Modifier.weight(1f)
            )
            StatusCard(
                "Sync",
                if (ui.networkSynced) "OK" else ui.syncRemainingBlocks.toString(),
                "blocks remaining",
                Modifier.weight(1f)
            )
            StatusCard(
                "Pending",
                ui.pendingPrivateTransfers.count.toString(),
                "private tx queue",
                Modifier.weight(1f)
            )
        }

        NodeAvailabilityNotice(ui)

        Panel(title = "Search", meta = "local node only") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Block height, block hash, or pending tx hash") },
                    singleLine = true,
                    enabled = ui.localRpcOnline,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { runLookup(query) },
                    enabled = ui.localRpcOnline && query.isNotBlank(),
                    colors = zButtonColors()
                ) {
                    Text("Open")
                }
            }
            HintText(
                "Digits are read as a block height. Anything else is tried as a block hash, " +
                    "then as a pending transaction."
            )
        }

        LookupResult(result)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(title = "Latest blocks", meta = "visual top 5", modifier = Modifier.weight(1f)) {
                val latestBlocks = latestBlockNumbers(ui.chainHeight, count = 5)
                if (latestBlocks.isEmpty()) {
                    EmptyState("No block height detected yet.")
                } else {
                    latestBlocks.forEach { height ->
                        KeyValueRow(
                            "Block #$height",
                            if (height == ui.chainHeight) "current tip" else "available"
                        )
                    }
                }
            }

            Panel(
                title = "Pending private transfers",
                meta = "${ui.pendingPrivateTransfers.count} queued",
                modifier = Modifier.weight(1f)
            ) {
                KeyValueRow("Queue", ui.pendingPrivateTransfers.count.toString())
                KeyValueRow(
                    "Node DB",
                    ui.pendingPrivateTransfers.db.ifBlank {
                        ".zoka/${ZokaNetworkConfig.Network}/blockchain.db"
                    }
                )
                if (ui.pendingPrivateTransfers.pendingPrivateTxHashes.isEmpty()) {
                    EmptyState("No pending private transfers.")
                } else {
                    ui.pendingPrivateTransfers.pendingPrivateTxHashes.take(5)
                        .forEachIndexed { index, hash ->
                            CopyableKeyValueRow("Tx ${index + 1}", hash)
                        }
                }
            }
        }
    }
}

/**
 * Tells the user when the explorer cannot see the whole chain, and why.
 *
 * Silence here would be the worst option: a node that is off or ten thousand
 * blocks behind still answers old heights perfectly well, so without this the
 * screen looks healthy while quietly serving a stale view.
 */
@Composable
private fun NodeAvailabilityNotice(ui: ZSilentUiState) {
    when {
        !ui.localRpcOnline -> Panel(title = "Local node offline", meta = "explorer unavailable") {
            Text(
                "The explorer reads only from your own node, and it is not responding. " +
                    "Start the node from the Node section to browse the chain.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        !ui.networkSynced -> Panel(title = "Still syncing", meta = "${ui.syncRemainingBlocks} behind") {
            Text(
                "Your node is at height ${ui.chainHeight} and still catching up. " +
                    "Recent blocks are not on this machine yet, so lookups above that " +
                    "height will report nothing found until the sync finishes.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LookupResult(result: ExplorerLookup) {
    when (result) {
        ExplorerLookup.Idle -> Unit

        ExplorerLookup.Loading -> Panel(title = "Searching") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Asking the local node…", style = MaterialTheme.typography.bodySmall)
            }
        }

        is ExplorerLookup.Found -> BlockDetail(result.block, result.transactions)

        is ExplorerLookup.PendingTx -> PendingTxDetail(result.tx)

        is ExplorerLookup.NotFound -> Panel(title = "Not found", meta = "no match") {
            Text(
                "Nothing on this node matches \"${result.term}\".",
                style = MaterialTheme.typography.bodySmall
            )
            HintText(result.hint)
        }

        is ExplorerLookup.Failed -> Panel(title = "Lookup failed", meta = "node error") {
            ErrorText(result.message)
            HintText(
                "This is a problem reaching the node, not proof that the block or " +
                    "transaction is missing."
            )
        }
    }
}

@Composable
private fun BlockDetail(block: BlockSummary, transactions: BlockTransactions?) {
    Panel(title = "Block #${block.height}", meta = "confirmed") {
        CopyableKeyValueRow("Hash", block.hash)
        CopyableKeyValueRow("Previous", block.previousHash)
        CopyableKeyValueRow("State root", block.stateRoot)
        KeyValueRow("Timestamp", formatTimestamp(block.timestamp))
        KeyValueRow("Nonce", block.nonce.toString())
        KeyValueRow(
            "Difficulty",
            block.difficultyBits?.let { "$it bits" } ?: "not recorded (genesis)"
        )
        KeyValueRow("Size", "${block.sizeBytes} bytes")
        KeyValueRow("Reward", block.rewardZkaDisplay)
        KeyValueRow("Public transactions", block.transactionsCount.toString())
        KeyValueRow("Private transactions", block.privateTxCount.toString())
        KeyValueRow("Private nullifiers", block.privateNullifierCount.toString())
        KeyValueRow("Private coinbase", if (block.privateCoinbase) "yes" else "no")
    }

    if (transactions != null && transactions.transactions.isNotEmpty()) {
        Panel(
            title = "Transactions in block #${block.height}",
            meta = "${transactions.transactionsCount} total"
        ) {
            transactions.transactions.take(25).forEachIndexed { index, tx ->
                CopyableKeyValueRow("Tx ${index + 1}", tx.hash)
            }
            if (transactions.transactions.size > 25) {
                HintText("Showing the first 25 of ${transactions.transactions.size}.")
            }
            HintText(
                "Hashes only — sender, receiver and amount are private by protocol and " +
                    "the node does not serve them."
            )
        }
    }
}

@Composable
private fun PendingTxDetail(tx: MempoolTransaction) {
    Panel(title = "Pending transaction", meta = tx.status.ifBlank { "pending" }) {
        CopyableKeyValueRow("Hash", tx.hash)
        KeyValueRow("Seen at", formatTimestamp(tx.timestamp))
        KeyValueRow("Status", "waiting in mempool — not yet in a block")
        HintText(
            "Once mined, this hash will no longer resolve here: the node keeps no " +
                "transaction index over confirmed blocks."
        )
    }
}

/** What a single explorer search produced. */
private sealed interface ExplorerLookup {
    data object Idle : ExplorerLookup
    data object Loading : ExplorerLookup
    data class Found(val block: BlockSummary, val transactions: BlockTransactions?) : ExplorerLookup
    data class PendingTx(val tx: MempoolTransaction) : ExplorerLookup
    data class NotFound(val term: String, val hint: String) : ExplorerLookup
    data class Failed(val message: String) : ExplorerLookup
}

/**
 * Resolves one search term against the local node.
 *
 * All-digits is treated as a height; anything else is tried as a block hash and
 * then as a pending transaction. A confirmed transaction hash cannot be
 * resolved at all — the node indexes transactions only while they sit in the
 * mempool — so that case gets an explicit hint rather than a bare "not found",
 * which would wrongly suggest the transaction never existed.
 */
private suspend fun lookup(appState: ZSilentAppState, term: String): ExplorerLookup = try {
    val rpc = appState.localRpc
    val height = term.toLongOrNull()

    if (height != null) {
        val block = rpc.blockByHeight(height)
        if (block == null) {
            ExplorerLookup.NotFound(
                term,
                "No block at height $height on this node. If the chain is taller than " +
                    "your local tip, the block exists but has not synced yet."
            )
        } else {
            ExplorerLookup.Found(block, runCatching { rpc.blockTransactions(height) }.getOrNull())
        }
    } else {
        val block = rpc.blockByHash(term)
        if (block != null) {
            ExplorerLookup.Found(
                block,
                runCatching { rpc.blockTransactions(block.height) }.getOrNull()
            )
        } else {
            val pending = rpc.mempoolTransaction(term)
            if (pending != null) {
                ExplorerLookup.PendingTx(pending)
            } else {
                ExplorerLookup.NotFound(
                    term,
                    "Not a block hash on this node, and not a pending transaction. " +
                        "Transactions already mined into a block cannot be looked up by " +
                        "hash — the node keeps no index for them. Open the block instead."
                )
            }
        }
    }
} catch (t: Throwable) {
    ExplorerLookup.Failed(t.message ?: t::class.simpleName ?: "Unknown error")
}

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

/** Node timestamps are seconds since epoch; milliseconds would land in year 56000. */
private fun formatTimestamp(epochSeconds: Long): String =
    if (epochSeconds <= 0) "unknown"
    else runCatching { timestampFormatter.format(Instant.ofEpochSecond(epochSeconds)) }
        .getOrElse { epochSeconds.toString() }

private fun latestBlockNumbers(height: Long, count: Int): List<Long> {
    if (height < 0) return emptyList()
    val first = (height - count + 1).coerceAtLeast(0)
    return (height downTo first).toList()
}
