package com.zsilent.core.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zsilent.core.models.BlockSummary
import com.zsilent.core.models.BlockTransactions
import com.zsilent.core.models.MempoolTransaction
import com.zsilent.core.models.PrivateTxLookup
import com.zsilent.core.models.ViewScanResponse
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.ErrorText
import com.zsilent.core.ui.components.HintText
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.Stat
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.ThinDivider
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.components.zFieldColors
import com.zsilent.core.ui.theme.ZTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Chain explorer backed by the configured Zoka RPC.
 *
 * Every lookup goes through [ZSilentAppState.localRpc]. The normal setup points
 * it at the user's own node; the screen checks that assumption and warns when
 * the configured endpoint is remote. View-key scans are never allowed remotely.
 *
 * The explorer can only see what that node already has, so it reports sync
 * state honestly instead of rendering a stale tip as if it were current.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ExplorerLookup>(ExplorerLookup.Idle) }
    val scope = rememberCoroutineScope()
    val localEndpoint = remember(appState) { appState.localRpc.isLocalEndpoint() }
    val hasTipReference = ui.localRpcOnline && ui.networkHeight > 0L
    val atObservedTip = hasTipReference && ui.syncRemainingBlocks <= 0L
    // El resultado sale debajo del buscador, que en una ventana chica queda
    // fuera de la vista: al abrir un bloque de la lista, o una transacción
    // desde otra pantalla, la búsqueda terminaba sin que se viera nada.
    val resultView = remember { BringIntoViewRequester() }

    fun runLookup(raw: String) {
        val term = raw.trim()
        if (term.isEmpty()) return
        query = term
        result = ExplorerLookup.Loading
        scope.launch {
            result = withContext(Dispatchers.IO) { lookup(appState, term) }
        }
    }

    // Un cuadro nuevo ya ubicado en la pantalla: recién ahí tiene sentido
    // pedir que se vea.
    LaunchedEffect(result) {
        if (result == ExplorerLookup.Idle) return@LaunchedEffect
        withFrameNanos { }
        resultView.bringIntoView()
    }

    // Lo que otra pantalla mandó a buscar (el botón "Explorer" de una
    // transacción). Se borra antes de correrlo para que no se repita al volver.
    LaunchedEffect(ui.explorerRequest) {
        val request = ui.explorerRequest ?: return@LaunchedEffect
        appState.consumeExplorerRequest()
        runLookup(request)
    }

    // Los últimos bloques que guarda el nodo, pedidos de nuevo solo cuando
    // cambia la punta: LATEST_BLOCK_COUNT lecturas cada unos 3 minutos, lejos
    // del límite de 1.200 por minuto con el que el nodo corta a un cliente.
    // Si un pedido falla queda la lista anterior, con el error a la vista.
    var latestBlocks by remember { mutableStateOf<List<BlockSummary>>(emptyList()) }
    var latestError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ui.chainHeight, ui.localRpcOnline) {
        if (!ui.localRpcOnline || ui.chainHeight <= 0L) return@LaunchedEffect
        val tip = ui.chainHeight
        withContext(Dispatchers.IO) {
            runCatching {
                latestBlockNumbers(tip, LATEST_BLOCK_COUNT).mapNotNull { appState.localRpc.blockByHeight(it) }
            }
        }.onSuccess {
            latestBlocks = it
            latestError = null
        }.onFailure {
            latestError = it.message ?: it::class.simpleName ?: "Unknown error"
        }
    }

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "Explorer",
        title = "Zoka chain explorer",
        subtitle = "Inspect chain state through the configured node, with freshness and privacy limits shown clearly."
    ) {
        ExplorerHealthHero(
            ui = ui,
            localEndpoint = localEndpoint,
            hasTipReference = hasTipReference,
            atObservedTip = atObservedTip
        )

        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            StatusCard(
                "Node height",
                if (ui.localRpcOnline) ui.chainHeight.toString() else "--",
                if (ui.localRpcOnline) "reported by configured RPC" else "node unavailable",
                Modifier.weight(1f)
            )
            StatusCard(
                "Network tip",
                if (ui.localRpcOnline && ui.networkHeight > 0L) ui.networkHeight.toString() else "--",
                when {
                    !ui.localRpcOnline -> "not live while RPC is offline"
                    ui.networkHeight > 0L -> "observed network height"
                    else -> "waiting for reference"
                },
                Modifier.weight(1f)
            )
            StatusCard(
                "Remaining",
                when {
                    !ui.localRpcOnline -> "--"
                    !hasTipReference -> "--"
                    else -> ui.syncRemainingBlocks.toString()
                },
                when {
                    !ui.localRpcOnline -> "not live while RPC is offline"
                    !hasTipReference -> "network tip not reported"
                    atObservedTip -> "at observed network tip"
                    ui.networkSynced -> "within mining tolerance"
                    else -> "blocks to network tip"
                },
                Modifier.weight(1f)
            )
            StatusCard(
                "Peers",
                if (ui.localRpcOnline) ui.localConnectedPeers.toString() else "--",
                when {
                    !ui.localRpcOnline -> "node unavailable"
                    ui.localConnectedPeers > 0L -> "connected via p2p"
                    else -> "node is isolated"
                },
                Modifier.weight(1f)
            )
            StatusCard(
                "Pending",
                if (ui.localRpcOnline) ui.pendingPrivateTransfers.count.toString() else "--",
                if (ui.localRpcOnline) "private transfer queue" else "not live while RPC is offline",
                Modifier.weight(1f)
            )
        }

        ExplorerSupplyPanel(ui)

        ExplorerHealthNotice(
            ui = ui,
            localEndpoint = localEndpoint,
            hasTipReference = hasTipReference,
            atObservedTip = atObservedTip
        )

        Panel(title = "Chain lookup", meta = if (localEndpoint) "local RPC" else "remote RPC") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Height, block hash, address, or transaction hash") },
                    singleLine = true,
                    enabled = ui.localRpcOnline,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    modifier = Modifier.weight(1f)
                )
                ZButton(
                    onClick = { runLookup(query) },
                    enabled = ui.localRpcOnline && query.isNotBlank(),
                    variant = ZButtonVariant.Accent
                ) {
                    Icon(Icons.Outlined.Search, null, modifier = Modifier.size(14.dp))
                    ZButtonText("Open")
                }
            }
            HintText(
                "Digits are a block height. zka1... and zpriv:... are looked up as addresses. " +
                    "A hexadecimal hash is tried as a block, then as a private transaction " +
                    "(found with its block when your node keeps its body, as it does for " +
                    "the ones you sent), then as a pending transaction. To find payments " +
                    "you received, use the view-key scan below."
            )
        }

        // Sin búsqueda no se compone nada: un contenedor vacío sumaría un hueco.
        if (result != ExplorerLookup.Idle) {
            Column(
                Modifier.bringIntoViewRequester(resultView),
                verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
            ) {
                LookupResult(result, localEndpoint)
            }
        }

        ExplorerChainOverview(
            ui = ui,
            latestBlocks = latestBlocks,
            latestError = latestError,
            onOpenBlock = { runLookup(it.height.toString()) }
        )

        ViewKeyScanPanel(ui, appState)
    }
}

/**
 * One honest summary of whether this screen can answer current chain queries
 * without disclosing them to another machine. A reachable, synchronized remote
 * RPC is useful, but it is not labelled READY because it changes that privacy
 * guarantee.
 */
@Composable
private fun ExplorerHealthHero(
    ui: ZSilentUiState,
    localEndpoint: Boolean,
    hasTipReference: Boolean,
    atObservedTip: Boolean
) {
    val stateLabel = when {
        !ui.localRpcOnline -> "OFFLINE"
        !localEndpoint -> "REMOTE"
        !hasTipReference -> "WAITING"
        !atObservedTip -> "SYNCING"
        else -> "READY"
    }
    val stateTone = when {
        !ui.localRpcOnline -> ZTone.Off
        !localEndpoint -> ZTone.Warn
        !atObservedTip -> ZTone.Warn
        else -> ZTone.On
    }
    val stateColor = when (stateTone) {
        ZTone.On -> ZTheme.semantics.success
        ZTone.Warn -> ZTheme.semantics.warning
        ZTone.Err -> ZTheme.semantics.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val stateDetail = when {
        !ui.localRpcOnline ->
            "The configured RPC is not answering, so searches and private scans are unavailable."
        !localEndpoint ->
            "The configured RPC is reachable but remote. Regular searches leave this machine; " +
                "view-key scans remain blocked."
        !hasTipReference ->
            "The node is answering, but no observed network-tip reference is available yet. Stored blocks remain searchable."
        !atObservedTip ->
            "The node is available at height ${ui.chainHeight}, with ${ui.syncRemainingBlocks} " +
                "block(s) still missing from the observed network tip."
        else ->
            "The node is answering and its local chain is synchronized. Explorer data is current."
    }

    Panel(title = "Explorer status", meta = stateLabel.lowercase()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stateLabel, style = ZTheme.text.monoDisplay, color = stateColor)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    "rpc ${if (ui.localRpcOnline) "online" else "offline"}",
                    if (ui.localRpcOnline) ZTone.On else ZTone.Off
                )
                Pill(
                    when {
                        !ui.localRpcOnline -> "chain unavailable"
                        !hasTipReference -> "tip unavailable"
                        atObservedTip -> "chain at tip"
                        else -> "${ui.syncRemainingBlocks} behind"
                    },
                    when {
                        !ui.localRpcOnline -> ZTone.Off
                        atObservedTip -> ZTone.On
                        else -> ZTone.Warn
                    }
                )
                Pill(
                    if (ui.localRpcOnline) {
                        "${ui.localConnectedPeers} peer${if (ui.localConnectedPeers == 1L) "" else "s"}"
                    } else {
                        "peers unavailable"
                    },
                    when {
                        !ui.localRpcOnline -> ZTone.Off
                        ui.localConnectedPeers > 0L -> ZTone.On
                        else -> ZTone.Warn
                    }
                )
                Pill(
                    if (localEndpoint) "endpoint local" else "endpoint remote",
                    if (localEndpoint) ZTone.On else ZTone.Err
                )
            }
        }
        HintText(stateDetail)
    }
}

/** Only renders conditions that materially limit freshness, access or privacy. */
@Composable
private fun ExplorerHealthNotice(
    ui: ZSilentUiState,
    localEndpoint: Boolean,
    hasTipReference: Boolean,
    atObservedTip: Boolean
) {
    when {
        !ui.localRpcOnline -> Panel(title = "Explorer unavailable", meta = "RPC offline") {
            ErrorText("The configured node did not answer the RPC health check.")
            HintText(
                if (localEndpoint) {
                    "Check the node from the Node section. Chain data is not queried while RPC is offline."
                } else {
                    "Review the configured remote RPC endpoint. ZSilent cannot start or repair a node on another machine."
                }
            )
        }

        !localEndpoint -> Panel(title = "Privacy warning", meta = "remote RPC configured") {
            ErrorText("The configured RPC endpoint is not on this machine.")
            HintText(
                "Regular lookups may reveal which blocks or addresses you inspect. View-key scans " +
                    "remain disabled so the scan key is never sent to that endpoint."
            )
        }

        !hasTipReference -> Panel(title = "Network tip unavailable", meta = "waiting for reference") {
            Text(
                "The configured node is answering at height ${ui.chainHeight}, but it has not reported an observed network tip yet.",
                style = MaterialTheme.typography.bodySmall
            )
            HintText("Stored blocks remain searchable; freshness cannot be asserted until a tip reference is reported.")
        }

        !atObservedTip -> Panel(
            title = "Recent chain data incomplete",
            meta = "${ui.syncRemainingBlocks} block(s) behind"
        ) {
            Text(
                "Your node is at height ${ui.chainHeight} while the observed network tip is " +
                    "${ui.networkHeight}. Heights not stored by the configured node cannot be opened yet.",
                style = MaterialTheme.typography.bodySmall
            )
            HintText("Older blocks already stored by the node remain searchable while synchronization continues.")
        }

        ui.localConnectedPeers <= 0L -> Panel(title = "No connected peers", meta = "node isolated") {
            Text(
                "RPC and chain sync currently report healthy, but this node has no live P2P peers.",
                style = MaterialTheme.typography.bodySmall
            )
            HintText("The explorer can read stored blocks; new network data depends on peer connectivity returning.")
        }
    }
}

/**
 * Los últimos bloques del nodo y su cola privada.
 *
 * El panel de bloques antes listaba alturas calculadas desde la punta ("height
 * reference") sin pedirle nada al nodo: parecían bloques, no traían ningún dato
 * y no se podían abrir, así que el explorador daba la impresión de no andar.
 * Ahora cada fila es un bloque que el nodo devolvió, y se abre con un clic.
 */
@Composable
private fun ExplorerChainOverview(
    ui: ZSilentUiState,
    latestBlocks: List<BlockSummary>,
    latestError: String?,
    onOpenBlock: (BlockSummary) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Panel(
            title = "Latest blocks",
            meta = when {
                !ui.localRpcOnline -> "node unavailable"
                latestBlocks.isEmpty() -> "loading"
                else -> "read from your node"
            },
            modifier = Modifier.weight(1f)
        ) {
            when {
                latestBlocks.isEmpty() && !ui.localRpcOnline ->
                    EmptyState("Start the node to list its latest blocks.")
                latestBlocks.isEmpty() && latestError != null ->
                    ErrorText("The node did not return its latest blocks: $latestError")
                latestBlocks.isEmpty() ->
                    EmptyState("Asking the node for its latest blocks…")
                else -> {
                    latestBlocks.forEach { block ->
                        LatestBlockRow(block, isTip = block.height == ui.chainHeight) {
                            onOpenBlock(block)
                        }
                    }
                    when {
                        !ui.localRpcOnline ->
                            HintText("The node is not answering; these are the last blocks it returned.")
                        latestError != null ->
                            HintText("Could not refresh just now ($latestError); showing the last blocks received.")
                        else ->
                            HintText("Click a block to open it above.")
                    }
                }
            }
        }

        Panel(
            title = "Pending private transfers",
            meta = if (ui.localRpcOnline) {
                "${ui.pendingPrivateTransfers.count} queued"
            } else {
                "queue unavailable"
            },
            modifier = Modifier.weight(1f)
        ) {
            KeyValueRow(
                "Queue status",
                when {
                    !ui.localRpcOnline -> "unavailable"
                    ui.pendingPrivateTransfers.count == 0 -> "clear"
                    else -> "waiting for inclusion"
                }
            )
            KeyValueRow(
                if (ui.localRpcOnline) "Transactions" else "Last observed transactions",
                ui.pendingPrivateTransfers.count.toString()
            )
            KeyValueRow(
                "Node DB",
                ui.pendingPrivateTransfers.db.ifBlank { "not reported" }
            )
            if (ui.pendingPrivateTransfers.pendingPrivateTxHashes.isEmpty()) {
                EmptyState("No pending private transfers.")
            } else {
                ui.pendingPrivateTransfers.pendingPrivateTxHashes.take(5)
                    .forEachIndexed { index, hash ->
                        CopyableKeyValueRow(
                            if (ui.localRpcOnline) "Tx ${index + 1}" else "Last observed tx ${index + 1}",
                            hash
                        )
                    }
                if (ui.pendingPrivateTransfers.pendingPrivateTxHashes.size > 5) {
                    HintText(
                        "Showing 5 of ${ui.pendingPrivateTransfers.pendingPrivateTxHashes.size} queued transactions."
                    )
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
        title = "Private payment scan",
        meta = if (local) "view key stays local" else "disabled"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (local) "local endpoint" else "remote endpoint", if (local) ZTone.On else ZTone.Err)
            Pill(
                "rpc ${if (ui.localRpcOnline) "online" else "offline"}",
                if (ui.localRpcOnline) ZTone.On else ZTone.Off
            )
            Pill("read-only scan", ZTone.Info)
        }

        if (!local) {
            ErrorText(
                "Your RPC endpoint is not on this machine. A view key reveals every " +
                    "private payment you have received, so it is never sent to a remote " +
                    "node."
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
            ZButton(
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
                variant = ZButtonVariant.Accent
            ) {
                Icon(Icons.Outlined.Search, null, modifier = Modifier.size(14.dp))
                ZButtonText("Scan")
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
                ThinDivider()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Stat(
                        "Transactions scanned",
                        result.scannedTransactions.toString(),
                        "archived transactions checked",
                        modifier = Modifier.weight(1f)
                    )
                    Stat(
                        "Outputs found",
                        result.matchingOutputs.toString(),
                        "matching private outputs",
                        tone = if (result.matchingOutputs > 0) ZTone.On else ZTone.Default,
                        modifier = Modifier.weight(1f)
                    )
                    Stat(
                        "Total received",
                        if (ui.balancesHidden) "•••• ZKA" else "${formatAtoms(result.totalAmountAtoms)} ZKA",
                        if (ui.balancesHidden) "hidden by display privacy" else "matched by this scan key",
                        tone = if (!ui.balancesHidden && result.totalAmountAtoms > 0L) ZTone.On else ZTone.Default,
                        modifier = Modifier.weight(1f)
                    )
                }
                ThinDivider()
                if (result.matches.isEmpty()) {
                    EmptyState("No private outputs on this node match that key.")
                    HintText(
                        "If your node is still syncing, payments in blocks it has not " +
                            "reached yet cannot be found."
                    )
                } else {
                    result.matches.take(20).forEach { match ->
                        CopyableKeyValueRow(
                            (if (ui.balancesHidden) "Private output" else "${formatAtoms(match.amount)} ZKA") +
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

@Composable
private fun LookupResult(result: ExplorerLookup, localEndpoint: Boolean) {
    when (result) {
        ExplorerLookup.Idle -> Unit

        ExplorerLookup.Loading -> Panel(title = "Searching") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    if (localEndpoint) "Asking the local node…" else "Asking the configured remote node…",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        is ExplorerLookup.Found -> BlockDetail(result.block, result.transactions)

        is ExplorerLookup.PrivateTx -> {
            PrivateTxDetail(result.tx)
            result.block?.let { BlockDetail(it, transactions = null) }
        }

        is ExplorerLookup.PendingTx -> PendingTxDetail(result.tx)

        is ExplorerLookup.Account -> Panel(title = "Address", meta = "exists on chain") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("address found", ZTone.On)
                Pill("balances private", ZTone.Info)
            }
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Stat("Reward", block.rewardZkaDisplay, "coinbase reward", ZTone.Info, modifier = Modifier.weight(1f))
            Stat("Size", "${block.sizeBytes} B", "serialized block", modifier = Modifier.weight(1f))
            Stat(
                "Public tx",
                block.transactionsCount.toString(),
                "public transactions",
                modifier = Modifier.weight(1f)
            )
            Stat(
                "Private tx",
                block.privateTxCount.toString(),
                "private transactions",
                modifier = Modifier.weight(1f)
            )
        }
        ThinDivider()
        CopyableKeyValueRow("Hash", block.hash)
        CopyableKeyValueRow("Previous", block.previousHash)
        CopyableKeyValueRow("State root", block.stateRoot)
        KeyValueRow("Timestamp", formatTimestamp(block.timestamp))
        KeyValueRow("Nonce", block.nonce.toString())
        KeyValueRow(
            "Difficulty",
            block.difficultyBits?.let { "$it bits" } ?: "not recorded (genesis)"
        )
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("mempool", ZTone.Warn)
            Pill("not confirmed", ZTone.Off)
        }
        CopyableKeyValueRow("Hash", tx.hash)
        KeyValueRow("Seen at", formatTimestamp(tx.timestamp))
        KeyValueRow("Status", "waiting in mempool — not yet in a block")
        HintText(
            "Once mined, this hash will no longer resolve here: the node keeps no " +
                "transaction index over confirmed blocks."
        )
    }
}

/**
 * Una transacción privada según la cadena, con lo que su cuerpo ya tiene de
 * público: estado, bloque y hora. Emisor, receptor, monto y comisión no viajan
 * en claro, así que no hay nada de eso para mostrar.
 */
@Composable
private fun PrivateTxDetail(tx: PrivateTxLookup) {
    val status = tx.status.ifBlank { "Unknown" }
    val (tone, meaning) = when (status.lowercase()) {
        "confirmed" -> ZTone.On to (tx.blockHeight?.let { "included in block #$it" } ?: "included in a block")
        "pending" -> ZTone.Warn to "waiting to be mined"
        "rejected" -> ZTone.Err to "its note was already spent by another transaction; no miner will include it"
        "spent" -> ZTone.Warn to "its note no longer exists, and the node cannot tell which transaction spent it"
        else -> ZTone.Off to "the node cannot place it on the chain"
    }
    Panel(title = "Private transaction", meta = status.lowercase()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(status.lowercase(), tone)
            Pill("amounts private", ZTone.Info)
        }
        CopyableKeyValueRow("Hash", tx.txHash)
        KeyValueRow("Status", meaning)
        tx.blockHeight?.let { KeyValueRow("Block", "#$it") }
        KeyValueRow("Created", formatTimestamp(tx.timestamp))
        HintText(
            "Sender, receiver, amount and fee are private by protocol: no node " +
                "stores them in clear."
        )
    }
}

/** Una fila de "Latest blocks": un bloque que devolvió el nodo; se abre con un clic. */
@Composable
private fun LatestBlockRow(block: BlockSummary, isTip: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpen)
            .padding(horizontal = 6.dp, vertical = ZTheme.spacing.row / 2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#${block.height}", style = ZTheme.text.mono, color = MaterialTheme.colorScheme.onSurface)
        Text(
            formatBlockTime(block.timestamp),
            style = ZTheme.text.mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            shortBlockHash(block.hash),
            style = ZTheme.text.mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${block.transactionsCount} tx · ${block.privateTxCount} private",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (isTip) Pill("tip", ZTone.On)
    }
}

/** What a single explorer search produced. */
private sealed interface ExplorerLookup {
    data object Idle : ExplorerLookup
    data object Loading : ExplorerLookup
    data class Found(val block: BlockSummary, val transactions: BlockTransactions?) : ExplorerLookup
    /** [block] es el bloque que la contiene, si está confirmada y el nodo lo devolvió. */
    data class PrivateTx(val tx: PrivateTxLookup, val block: BlockSummary?) : ExplorerLookup
    data class PendingTx(val tx: MempoolTransaction) : ExplorerLookup
    data class Account(val address: String) : ExplorerLookup
    data class NotFound(val term: String, val hint: String) : ExplorerLookup
    data class Failed(val message: String) : ExplorerLookup
}

/**
 * Resolves one search term against the configured node.
 *
 * All-digits is treated as a height and address shapes go to the account
 * check. Anything else must be hexadecimal, because it is pasted into the URL
 * path; it is tried as a block hash, then as a private transaction and last as
 * a pending one. The node keeps no index of confirmed transaction hashes: a
 * private one is found only when the node keeps its body (it then reports the
 * block that holds it), so a miss gets an explicit hint rather than a bare
 * "not found", which would wrongly suggest the transaction never existed.
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
                    "the configured node's tip, the block exists but has not synced yet."
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
    } else if (term.startsWith("zkq1")) {
        ExplorerLookup.NotFound(
            term,
            "zkq1 addresses only receive private payments, which leave no public " +
                "record to look up. To find payments you received, use the view-key " +
                "scan below."
        )
    } else if (!HEX_TERM.matches(term)) {
        ExplorerLookup.NotFound(
            term,
            "Not a height, an address or a hash. Hashes are hexadecimal: copy them " +
                "with the copy button next to a block or a transaction."
        )
    } else {
        val block = rpc.blockByHash(term)
        if (block != null) {
            ExplorerLookup.Found(
                block,
                runCatching { rpc.blockTransactions(block.height) }.getOrNull()
            )
        } else {
            val privateTx = rpc.privateTransaction(term)
            if (privateTx != null) {
                val holder = privateTx.blockHeight?.let { height ->
                    runCatching { rpc.blockByHeight(height) }.getOrNull()
                }
                ExplorerLookup.PrivateTx(privateTx, holder)
            } else {
                val pending = rpc.mempoolTransaction(term)
                if (pending != null) {
                    ExplorerLookup.PendingTx(pending)
                } else {
                    ExplorerLookup.NotFound(
                        term,
                        "Not a block hash on this node, nor a transaction it knows. The " +
                            "node keeps no index of confirmed transaction hashes: it finds " +
                            "a private one only when it keeps its body, as it does for the " +
                            "ones sent from this node. To find payments you received, use " +
                            "the view-key scan below."
                    )
                }
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

/** Filas del panel "Latest blocks". */
private const val LATEST_BLOCK_COUNT = 8

/**
 * Lo único que se busca como hash. El término viaja pegado a la ruta de la URL,
 * así que cualquier otra cosa (barras, signos) ni siquiera llega al nodo.
 */
private val HEX_TERM = Regex("^[0-9a-fA-F]{16,128}$")

private val blockClockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

private val blockDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

/** La hora del bloque, con la fecha delante si no es de hoy. */
private fun formatBlockTime(epochSeconds: Long): String {
    if (epochSeconds <= 0) return "--:--:--"
    val instant = Instant.ofEpochSecond(epochSeconds)
    val zone = ZoneId.systemDefault()
    val sameDay = instant.atZone(zone).toLocalDate() == java.time.LocalDate.now(zone)
    return if (sameDay) blockClockFormatter.format(instant) else blockDayFormatter.format(instant)
}

private fun shortBlockHash(hash: String): String =
    if (hash.length <= 16) hash else "${hash.take(8)}…${hash.takeLast(6)}"

/**
 * ZKA en circulación: lo emitido, el techo y lo que falta.
 *
 * Va en el explorador porque es donde se busca el estado de la cadena. Antes
 * el dato existía únicamente en el panel de diagnóstico del nodo, entre una
 * docena de filas técnicas, y en la práctica no se encontraba.
 *
 * Se muestra desde `total_supply_emitted_atoms`, que es lo que la cadena lleva
 * contabilizado, y no desde `emission_zka`: aquél es un decimal recalculado
 * suponiendo que cada bloque pagó lo que le tocaba, y el punto flotante
 * redondea la última cifra. Con átomos enteros la cifra es exacta.
 */
@Composable
private fun ExplorerSupplyPanel(ui: ZSilentUiState) {
    val emitted = ui.stats.totalSupplyEmittedAtoms
    val max = ui.stats.maxSupplyAtoms
    // Un nodo anterior a 2.0 no envía estos campos. Ahí no se inventa nada: se
    // dice que no los reporta, en vez de mostrar un cero que se lee como dato.
    val reported = emitted > 0L && max > 0L

    Panel(
        title = "ZKA supply",
        meta = if (!ui.localRpcOnline) "node unavailable"
               else if (reported) "from chain state"
               else "not reported by this node"
    ) {
        if (!ui.localRpcOnline || !reported) {
            HintText(
                if (!ui.localRpcOnline) {
                    "Start the node to read the emitted supply from the chain."
                } else {
                    "This node does not report the emitted supply. Nodes from 2.0 onward do."
                }
            )
            return@Panel
        }

        val pct = emitted.toDouble() * 100.0 / max.toDouble()
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            StatusCard(
                "In circulation",
                formatZkaExact(emitted),
                "ZKA emitted so far",
                Modifier.weight(1f)
            )
            StatusCard(
                "Maximum",
                formatZkaExact(max),
                "hard cap, never exceeded",
                Modifier.weight(1f)
            )
            StatusCard(
                "Left to mine",
                formatZkaExact(ui.stats.remainingSupplyAtoms),
                "%.4f%% emitted".format(pct),
                Modifier.weight(1f)
            )
        }
        HintText(
            "Counted by the chain itself, block by block, not estimated from the height. " +
                "Emitted includes coinbase rewards that are still maturing and cannot be " +
                "spent yet."
        )
    }
}

/**
 * Átomos a ZKA sin perder una sola cifra.
 *
 * Aritmética entera a propósito: convertir a decimal antes de formatear
 * redondea el último dígito, y este número se muestra precisamente porque se
 * quiere exacto.
 */
private fun formatZkaExact(atoms: Long): String {
    val whole = atoms / ATOMS_PER_ZKA_EXPLORER
    val frac = atoms % ATOMS_PER_ZKA_EXPLORER
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$grouped.${frac.toString().padStart(6, '0')}"
}

private const val ATOMS_PER_ZKA_EXPLORER = 1_000_000L
