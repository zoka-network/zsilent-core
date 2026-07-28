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
import com.zsilent.core.models.ViewScanResponse
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

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Explorer",
        title = "Local-first block explorer",
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
                    label = { Text("Height, block hash, address, or pending tx hash") },
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
                "Digits are a block height. zka1… and zpriv:… are looked up as addresses. " +
                    "Anything else is tried as a block hash, then as a pending transaction. " +
                    "Confirmed transactions have no public index — use the view key scan " +
                    "below to find your own."
            )
        }

        LookupResult(result)

        ViewKeyScanPanel(ui, appState)

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
 * Finds the user's own private payments from their view scan key.
 *
 * Private transactions carry no readable recipient, so no hash lookup can find
 * them — the only way is to test outputs against a scan key. That key reveals
 * every payment its owner has ever received, which is why this panel refuses
 * to run at all unless the RPC points at a node on this machine.
 */
@Composable
private fun ViewKeyScanPanel(ui: ZSilentUiState, appState: ZSilentAppState) {
    var scanKey by remember { mutableStateOf("") }
    var scan by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val scope = rememberCoroutineScope()
    val local = remember(appState) { appState.localRpc.isLocalEndpoint() }

    Panel(
        title = "Find my private payments",
        meta = if (local) "view key, stays local" else "disabled"
    ) {
        if (!local) {
            Text(
                "Your RPC endpoint is not on this machine. A view key reveals every " +
                    "private payment you have received, so it is never sent to a remote " +
                    "node.",
                style = MaterialTheme.typography.bodySmall
            )
            return@Panel
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = scanKey,
                onValueChange = { scanKey = it.trim() },
                label = { Text("View scan key (hex)") },
                singleLine = true,
                enabled = ui.localRpcOnline,
                colors = zFieldColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    scan = ScanState.Running
                    scope.launch {
                        scan = withContext(Dispatchers.IO) {
                            try {
                                ScanState.Done(appState.localRpc.viewScanByKey(scanKey))
                            } catch (t: Throwable) {
                                ScanState.Failed(t.message ?: "Scan failed")
                            }
                        }
                    }
                },
                enabled = ui.localRpcOnline && scanKey.isNotBlank(),
                colors = zButtonColors()
            ) {
                Text("Scan")
            }
        }

        when (val current = scan) {
            ScanState.Idle -> HintText(
                "Private payments have no public recipient, so they cannot be found by " +
                    "hash. Your view key is the only way to recognise them — it is sent " +
                    "to your own node and nowhere else."
            )

            ScanState.Running -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Scanning archived transactions…", style = MaterialTheme.typography.bodySmall)
            }

            is ScanState.Failed -> ErrorText(current.message)

            is ScanState.Done -> {
                val result = current.result
                CopyableKeyValueRow("Derived address", result.address)
                KeyValueRow("Transactions scanned", result.scannedTransactions.toString())
                KeyValueRow("Outputs found", result.matchingOutputs.toString())
                KeyValueRow("Total", "${formatAtoms(result.totalAmountAtoms)} ZKA")
                if (result.matches.isEmpty()) {
                    EmptyState("No private outputs on this node match that key.")
                    HintText(
                        "If your node is still syncing, payments in blocks it has not " +
                            "reached yet cannot be found."
                    )
                } else {
                    result.matches.take(20).forEach { match ->
                        CopyableKeyValueRow(
                            "${formatAtoms(match.amount)} ZKA" +
                                if (match.isChange) " (change)" else "",
                            match.txHash
                        )
                    }
                    if (result.matches.size > 20) {
                        HintText("Showing the first 20 of ${result.matches.size}.")
                    }
                }
            }
        }
    }
}

private sealed interface ScanState {
    data object Idle : ScanState
    data object Running : ScanState
    data class Done(val result: ViewScanResponse) : ScanState
    data class Failed(val message: String) : ScanState
}

/** Atoms to a readable ZKA figure. Matches `economics::ATOMS_PER_ZKA`. */
private fun formatAtoms(atoms: Long): String {
    val whole = atoms / 1_000_000L
    val frac = atoms % 1_000_000L
    return if (frac == 0L) whole.toString()
    else "$whole.${frac.toString().padStart(6, '0').trimEnd('0')}"
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

        is ExplorerLookup.Account -> Panel(title = "Address", meta = "exists on chain") {
            CopyableKeyValueRow("Address", result.address)
            KeyValueRow("Status", "known to this node")
            HintText(
                "Balances are not published. The node confirms the address exists and " +
                    "nothing more — amounts and history are private by protocol."
            )
        }

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
    data class Account(val address: String) : ExplorerLookup
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
    } else if (term.startsWith("zka1") || term.startsWith("zpriv:")) {
        // Address shapes go straight to the account check; trying them as a
        // block hash first would just waste a round trip.
        val account = rpc.accountExists(term)
        if (account != null && account.valid) {
            ExplorerLookup.Account(account.address)
        } else {
            ExplorerLookup.NotFound(
                term,
                "This node has no account with that address. It may simply never have " +
                    "received funds, or your node may not have synced the block that " +
                    "created it."
            )
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
                        "hash — the node keeps no index for them. Open the block instead. " +
                        "To find your own private payments, use the view key scan below."
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
