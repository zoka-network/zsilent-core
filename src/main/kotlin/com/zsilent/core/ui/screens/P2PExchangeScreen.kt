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
import com.zsilent.core.models.CreateOfferRequest
import com.zsilent.core.models.OfferList
import com.zsilent.core.models.OfferSummary
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.EmptyState
import com.zsilent.core.ui.components.ErrorText
import com.zsilent.core.ui.components.HintText
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.components.Pill
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import com.zsilent.core.ui.theme.ZTheme
import com.zsilent.core.ui.components.ZLabel
import com.zsilent.core.ui.components.ZInput
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import com.zsilent.core.ui.components.KeyValueRow
import com.zsilent.core.ui.components.Panel
import com.zsilent.core.ui.components.ScreenScaffold
import com.zsilent.core.ui.components.StatusCard
import com.zsilent.core.ui.components.zButtonColors
import com.zsilent.core.ui.components.zFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

/** Atoms per ZKA, matching `economics::ATOMS_PER_ZKA` on the node. */
private const val ATOMS_PER_ZKA = 1_000_000L

/** Micro-USDT per USDT — TRC-20 carries six decimals. */
private const val MICRO_USDT_PER_USDT = 1_000_000L

/**
 * Sell ZKA for USDT, peer to peer.
 *
 * One direction only: the seller locks ZKA and names a USDT price, a buyer
 * pays, and the seller's node releases the ZKA once it has verified the
 * payment on Tron. There is no arbiter and no refund, which is why the risk
 * screen below is a gate rather than a footnote.
 */
@Composable
fun P2PExchangeScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    if (!ui.p2pExchangeRisksAccepted) {
        ExchangeRiskGate(onAccept = appState::acceptP2PExchangeRisks)
        return
    }
    ExchangeMain(ui, appState)
}

/**
 * Shown once before the user can reach the exchange.
 *
 * Every item here is a limitation that cannot be fixed at this layer, so the
 * user has to know about it before their funds are involved — not after.
 */
@Composable
private fun ExchangeRiskGate(onAccept: () -> Unit) {
    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "P2P Exchange",
        title = "Risk acknowledgement",
        subtitle = "Read this before trading."
    ) {
        Panel(title = "Before you trade", meta = "read once") {
            Text(
                "Trading ZKA for USDT here happens directly between you and another " +
                    "person. Nobody holds the funds for you and nobody can reverse a " +
                    "mistake.",
                style = MaterialTheme.typography.bodyMedium
            )

            ZButton(onClick = onAccept, variant = ZButtonVariant.Accent) {
                ZButtonText("I understand — continue")
            }
        }

        // Tarjetas de igual alto, como la tira de Mining Pool. Antes eran textos
        // sueltos apilados dentro de un solo panel y se leian como un muro.
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
        ) {
            RiskLine(
                "01 · The lock is not an escrow",
                "Your ZKA is marked reserved inside your own wallet. That is bookkeeping, " +
                    "not a blockchain escrow: a modified client could spend it anyway.",
                Modifier.weight(1f)
            )
            RiskLine(
                "02 · There is no arbiter",
                "No dispute process, no support desk, no way to claw funds back. Trade " +
                    "with people you have reason to trust.",
                Modifier.weight(1f)
            )
            RiskLine(
                "03 · Payment is verified over Tor",
                "Your node checks the USDT transfer privately. If Tor is down payments " +
                    "cannot be verified, and the node never falls back to a direct connection.",
                Modifier.weight(1f)
            )
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
        ) {
            RiskLine(
                "04 · Claims expire in 45 minutes",
                "If no qualifying payment arrives in the window the offer closes, and your " +
                    "ZKA is freed only after one final private Tron check.",
                Modifier.weight(1f)
            )
            RiskLine(
                "05 · Selling locks whole notes",
                "A private note cannot be split. Backing a 20 ZKA sale may immobilise a " +
                    "27.99 note; the difference returns as change when the trade closes.",
                Modifier.weight(1f)
            )
            RiskLine(
                "06 · Experimental on mainnet",
                "The backend flow has had a dedicated review, but there was no testnet " +
                    "phase. Start at the minimum and never risk what you would mind losing.",
                Modifier.weight(1f)
            )
        }
    }
}

/** Una tarjeta de la tira de riesgos. El `modifier` tiene que llegar al `Panel`. */
@Composable
private fun RiskLine(title: String, body: String, modifier: Modifier = Modifier) {
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
 * Semáforo de una operación.
 *
 * Verde sólo para lo que termino bien. Ámbar para lo que sigue en curso y
 * todavia puede salir de cualquier manera. Rojo para lo que se corto.
 *
 * `Expired` va en rojo y no en gris a proposito: para el que estaba esperando
 * cobrar, una ventana vencida es un final malo, no un estado neutro.
 */
private fun offerTone(status: String): ZTone = when (status) {
    "Released" -> ZTone.On
    "Open", "Claimed", "PaymentDetected" -> ZTone.Warn
    "Cancelled", "Expired" -> ZTone.Err
    else -> ZTone.Default
}

private fun offerIcon(status: String): ImageVector = when (status) {
    "Released" -> Icons.Outlined.CheckCircle
    "Open" -> Icons.Outlined.Schedule
    "Claimed", "PaymentDetected" -> Icons.Outlined.HourglassEmpty
    "Cancelled" -> Icons.Outlined.Cancel
    "Expired" -> Icons.Outlined.ErrorOutline
    else -> Icons.Outlined.Schedule
}

/** Etiqueta corta y en minuscula, como el resto de las pills de la app. */
private fun offerStatusLabel(status: String): String = when (status) {
    "PaymentDetected" -> "payment seen"
    else -> status.lowercase()
}

/** Estado global de la seccion, para el cartel de arriba. */
private enum class ExchangeState { Ready, Warming, Blocked, Offline }

/**
 * Cartel de estado, hermano de los de Node, Mining y Mining Pool.
 *
 * `READY` exige las tres condiciones a la vez: nodo en linea, Tor encendido y
 * verificacion de pagos disponible. Faltando cualquiera no se puede completar
 * un trato, y decir "listo" seria invitar a bloquear fondos que despues no se
 * van a poder liberar.
 */
@Composable
private fun ExchangeStateBanner(
    state: ExchangeState,
    nodeOnline: Boolean,
    torOn: Boolean,
    verificationReady: Boolean,
    peers: Int,
    verificationDetail: String,
) {
    val label = when (state) {
        ExchangeState.Ready -> "READY"
        ExchangeState.Warming -> "WARMING"
        ExchangeState.Blocked -> "BLOCKED"
        ExchangeState.Offline -> "OFFLINE"
    }
    val tint = when (state) {
        ExchangeState.Ready -> ZTheme.semantics.success
        ExchangeState.Warming -> ZTheme.semantics.warning
        ExchangeState.Blocked -> ZTheme.semantics.danger
        ExchangeState.Offline -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Panel(title = "Exchange", meta = label.lowercase()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = ZTheme.text.monoDisplay, color = tint)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        "node ${if (nodeOnline) "online" else "offline"}",
                        if (nodeOnline) ZTone.On else ZTone.Err
                    )
                    Pill("tor ${if (torOn) "on" else "off"}", if (torOn) ZTone.On else ZTone.Err)
                    Pill(
                        "payments ${if (verificationReady) "verifiable" else "blocked"}",
                        if (verificationReady) ZTone.On else ZTone.Warn
                    )
                    Pill("$peers peer(s)", if (peers > 0) ZTone.On else ZTone.Warn)
                }
            }
        }
        HintText(
            when (state) {
                ExchangeState.Offline ->
                    "Start the node from the Node section. Offers travel over its peer connections."
                ExchangeState.Blocked ->
                    "Tor is off. Payment verification runs over Tor and the node never falls back " +
                        "to a direct connection, so a claimed offer could not be settled."
                ExchangeState.Warming -> verificationDetail
                ExchangeState.Ready ->
                    if (peers == 0) {
                        "Everything is set, but no peers are connected — your offers reach nobody " +
                            "and you see nobody else's until the mesh has someone in it."
                    } else {
                        "Node, Tor and payment verification are all up. Offers you publish reach " +
                            "$peers peer(s)."
                    }
            }
        )
    }
}

/** Una operacion terminada ya no cambia de estado. */
private fun isFinished(offer: OfferSummary): Boolean =
    offer.status == "Released" || offer.status == "Expired" || offer.status == "Cancelled"

/** Criterio de orden del mercado. */
private enum class OfferSort(val label: String) {
    Price("price"),
    Amount("amount"),
    Newest("new"),
}

@Composable
private fun SortControls(selected: OfferSort, onSelect: (OfferSort) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OfferSort.entries.forEach { sort ->
            val active = sort == selected
            Box(
                Modifier
                    .clickable { onSelect(sort) }
                    .background(
                        if (active) MaterialTheme.colorScheme.surfaceContainerHighest
                        else Color.Transparent,
                        MaterialTheme.shapes.small
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    sort.label.uppercase(),
                    style = ZTheme.text.eyebrow,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Estado de una operación: icono + palabra, en el color del semáforo.
 *
 * El icono acompaña al color en vez de reemplazarlo. Quien no distingue rojo de
 * verde —cerca de uno de cada doce hombres— necesita la forma, y el color solo
 * no le dice nada.
 */
@Composable
private fun StatusChip(status: String) {
    val tone = offerTone(status)
    val color = when (tone) {
        ZTone.On -> ZTheme.semantics.success
        ZTone.Warn -> ZTheme.semantics.warning
        ZTone.Err -> ZTheme.semantics.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            offerIcon(status),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(offerStatusLabel(status), style = ZTheme.text.eyebrow, color = color)
    }
}

/** Recorta una direccion para que entre en una celda sin romper la fila. */
private fun shortenAddress(address: String): String =
    if (address.length <= 18) address else address.take(15) + "…"

/** Celda de la tabla de ofertas. */
@Composable
private fun TableCell(text: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    Text(
        text,
        style = if (strong) ZTheme.text.mono else MaterialTheme.typography.bodySmall,
        color = if (strong) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Precio unitario: USDT por ZKA.
 *
 * Se calcula aquí y no viene del nodo, porque no es un dato del protocolo sino
 * una comodidad para comparar ofertas de tamaños distintos.
 */
private fun unitPriceLabel(offer: OfferSummary): String {
    if (offer.zkaAtoms <= 0L) return "—"
    val zka = BigDecimal(offer.zkaAtoms).divide(BigDecimal(ATOMS_PER_ZKA))
    val usdt = BigDecimal(offer.microUsdt).divide(BigDecimal(MICRO_USDT_PER_USDT))
    return usdt.divide(zka, 3, RoundingMode.HALF_UP).toPlainString()
}

/** Cabecera de la tabla de ofertas. */
@Composable
private fun OfferTableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZLabel("Selling", Modifier.weight(1f))
        ZLabel("Wants", Modifier.weight(1f))
        ZLabel("Price", Modifier.weight(0.8f))
        ZLabel("Seller", Modifier.weight(1.4f))
        ZLabel("Source", Modifier.weight(0.9f))
        ZLabel("Window", Modifier.weight(0.9f))
        Spacer(Modifier.width(150.dp))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Pestañas de la sección, con el mismo primitivo que usa Wallet. */
private enum class ExchangePage { Market, Sell, MyOffers, MyTrades, History }

@Composable
private fun ExchangeTabs(selected: ExchangePage, onSelect: (ExchangePage) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tabs = listOf(
        ExchangePage.Market to "market",
        ExchangePage.Sell to "sell zka",
        ExchangePage.MyOffers to "my offers",
        ExchangePage.MyTrades to "my trades",
        ExchangePage.History to "history",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEach { (page, label) ->
                val active = selected == page
                // width(IntrinsicSize.Max) es imprescindible: sin eso el
                // fillMaxWidth del subrayado expande la columna a todo el ancho
                // y la primera pestana se come la fila.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onSelect(page) }
                ) {
                    Text(
                        label.uppercase(),
                        style = ZTheme.text.eyebrow,
                        color = if (active) scheme.onSurface else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (active) scheme.primary else Color.Transparent,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

/**
 * Los cuatro pasos del intercambio, como tarjetas numeradas.
 *
 * No es decoración: el flujo tiene cuatro momentos con consecuencias distintas
 * —el bloqueo, la propagación, el pago, la verificación— y un usuario que no
 * los distingue manda USDT antes de tiempo o cancela creyendo que perdió algo.
 */
@Composable
private fun ExchangeFlowCards() {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
    ) {
        FlowStep(
            "1 · Lock",
            "Publishing an offer locks the ZKA in your wallet and takes it out of the " +
                "spendable balance. Notes are indivisible, so the amount locked is usually " +
                "larger than what you sell.",
            Modifier.weight(1f)
        )
        FlowStep(
            "2 · Gossip",
            "The offer floods to every peer running the same protocol version. There is no " +
                "central book: what you see is what your node was told.",
            Modifier.weight(1f)
        )
        FlowStep(
            "3 · Claim & pay",
            "A taker declares the Tron address they will pay from, then sends USDT (TRC-20) " +
                "to the address the seller declared. Paying from any other address cannot " +
                "be matched.",
            Modifier.weight(1f)
        )
        FlowStep(
            "4 · Verify & release",
            "The seller's node checks the transfer over Tor: sender, recipient and amount. " +
                "The amount may be equal or higher, never lower. Only then is the ZKA released.",
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun FlowStep(title: String, body: String, modifier: Modifier = Modifier) {
    Panel(modifier = modifier.fillMaxHeight()) {
        ZLabel(title)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExchangeMain(ui: ZSilentUiState, appState: ZSilentAppState) {
    var state by remember { mutableStateOf<OfferFeed>(OfferFeed.Loading) }
    var zkaInput by remember { mutableStateOf("") }
    var usdtInput by remember { mutableStateOf("") }
    var tronAddress by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        state = withContext(Dispatchers.IO) {
            try {
                OfferFeed.Loaded(appState.localRpc.exchangeOffers())
            } catch (unsupported: ZokaRpcClient.EndpointUnsupported) {
                OfferFeed.Unsupported
            } catch (t: Throwable) {
                OfferFeed.Failed(t.message ?: "Could not reach the local node")
            }
        }
    }

    LaunchedEffect(ui.localRpcOnline) {
        if (!ui.localRpcOnline) {
            state = OfferFeed.NodeOffline
            return@LaunchedEffect
        }
        while (isActive) {
            reload()
            delay(5_000)
        }
    }

    var page by remember { mutableStateOf(ExchangePage.Market) }
    var sort by remember { mutableStateOf(OfferSort.Price) }

    com.zsilent.core.ui.onboarding.NoirScreenShell(
        eyebrow = "P2P Exchange",
        title = "Peer-to-peer ZKA / USDT",
        subtitle = "Sellers lock ZKA locally, offers propagate by gossip, and payments are " +
            "verified on-chain before release."
    ) {
        val loaded = state as? OfferFeed.Loaded
        val offers = loaded?.value?.offers.orEmpty()
        val verification = loaded?.value?.verification ?: "unknown"
        val verificationReady = verification == "ready" || verification.startsWith("checking offer ")
        val mine = offers.filter { it.sellerAddress == ui.activeWalletAddress }
        // Lo inmovilizado por MIS ofertas vivas. Se suma reservedAtoms y no
        // zkaAtoms a proposito: es lo que realmente falta del saldo gastable.
        val escrowAtoms = mine
            .filter { it.status == "Open" || it.status == "Claimed" || it.status == "PaymentDetected" }
            .sumOf { if (it.reservedAtoms > 0) it.reservedAtoms else it.zkaAtoms }

        val exchangeState = when {
            !ui.localRpcOnline -> ExchangeState.Offline
            !ui.torEnabled -> ExchangeState.Blocked
            !verificationReady -> ExchangeState.Warming
            else -> ExchangeState.Ready
        }
        ExchangeStateBanner(
            state = exchangeState,
            nodeOnline = ui.localRpcOnline,
            torOn = ui.torEnabled,
            verificationReady = verificationReady,
            peers = ui.localConnectedPeers.toInt(),
            verificationDetail = "The node reports: ${verificationLabel(verification)}. " +
                "It will not query Tron directly, so selling stays blocked until this clears.",
        )

        ExchangeFlowCards()

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard(
                "Available",
                formatZka(ui.walletBalance?.balanceAtoms ?: 0L),
                "spendable ZKA",
                Modifier.weight(1f)
            )
            StatusCard(
                "In escrow",
                formatZka(escrowAtoms),
                "locked by your open offers",
                Modifier.weight(1f)
            )
            StatusCard(
                "Live offers",
                offers.count { it.status == "Open" }.toString(),
                "received via gossip",
                Modifier.weight(1f)
            )
            StatusCard(
                "Verification",
                if (verificationReady) "ready" else "blocked",
                verificationLabel(verification),
                Modifier.weight(1f)
            )
        }

        ExchangeTabs(selected = page, onSelect = { page = it })

        if (!verificationReady) {
            Panel(title = "Payment verification unavailable", meta = "publishing blocked") {
                Text(
                    "The local node reports: ${verificationLabel(verification)}. It will not " +
                        "query Tron directly or expire a claimed offer without a successful " +
                        "private check. Start/restart the node with Tor available before selling.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        actionError?.let { ErrorText(it) }
        actionNotice?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }

        if (page == ExchangePage.Sell) {
        Panel(title = "Publish an offer", meta = "you sell ZKA") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ZLabel("ZKA to sell")
                    ZInput(
                        value = zkaInput,
                        onValueChange = { zkaInput = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = "10",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ZLabel("USDT you want")
                    ZInput(
                        value = usdtInput,
                        onValueChange = { usdtInput = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = "1.00",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            ZLabel("Your USDT address (TRC-20, starts with T)")
            ZInput(
                value = tronAddress,
                onValueChange = { tronAddress = it.trim() },
                placeholder = "T…",
                modifier = Modifier.fillMaxWidth()
            )
            HintText(
                "This is the only address a payment is matched against. A buyer who pays " +
                    "anywhere else cannot be credited, and nobody can fix it afterwards."
            )
            formError?.let { ErrorText(it) }
            Button(
                onClick = {
                    val request = buildOfferRequest(zkaInput, usdtInput, tronAddress)
                    when (request) {
                        is OfferInput.Invalid -> formError = request.reason
                        is OfferInput.Valid -> {
                            formError = null
                            scope.launch {
                                val outcome = withContext(Dispatchers.IO) {
                                    runCatching { appState.localRpc.createOffer(request.request) }
                                }
                                outcome
                                    .onSuccess {
                                        zkaInput = ""; usdtInput = ""; reload()
                                    }
                                    .onFailure { formError = it.message }
                            }
                        }
                    }
                },
                enabled = ui.localRpcOnline && verificationReady,
                colors = zButtonColors()
            ) {
                Text("Publish offer")
            }
            HintText(
                "Publishing locks ZKA in this wallet until the offer completes, expires, " +
                    "or you cancel it. Notes cannot be split, so the amount immobilised is " +
                    "usually larger than what you sell — the difference comes back as change."
            )
        }
        }

        // Cada pestana filtra la misma lista en vez de pedir datos distintos:
        // el nodo entrega todo lo que conoce en una sola respuesta, y partirlo
        // en varias consultas solo agregaria formas de que se desincronicen.
        OfferFeedPanel(
            state = state,
            page = page,
            sort = sort,
            onSort = { sort = it },
            activeWalletAddress = ui.activeWalletAddress,
            onClaim = { offer ->
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching { appState.localRpc.claimOffer(offer.id) }
                    }
                    outcome
                        .onSuccess {
                            actionError = null
                            actionNotice =
                                "Claim request sent. Do not pay until the seller's signed acceptance appears."
                        }
                        .onFailure { actionError = it.message ?: "Claim request failed." }
                    reload()
                }
            },
            onCancel = { offer ->
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching { appState.localRpc.cancelOffer(offer.id) }
                    }
                    outcome
                        .onSuccess {
                            actionError = null
                            actionNotice = "Offer cancelled and its ZKA reservation released."
                        }
                        .onFailure { actionError = it.message ?: "Cancellation failed." }
                    reload()
                }
            },
            onReport = { offer, tronTxHash ->
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            appState.localRpc.reportPaymentFailure(offer.id, tronTxHash)
                        }
                    }
                    outcome
                        .onSuccess {
                            actionError = null
                            actionNotice =
                                "Failure report published with the disclosed Tron transaction hash."
                        }
                        .onFailure { actionError = it.message ?: "Failure report was refused." }
                    reload()
                }
            }
        )
    }
}

@Composable
private fun OfferFeedPanel(
    state: OfferFeed,
    page: ExchangePage,
    sort: OfferSort,
    onSort: (OfferSort) -> Unit,
    activeWalletAddress: String,
    onClaim: (OfferSummary) -> Unit,
    onCancel: (OfferSummary) -> Unit,
    onReport: (OfferSummary, String) -> Unit
) {
    when (state) {
        OfferFeed.Loading -> Panel(title = "Offers") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Loading offers…", style = MaterialTheme.typography.bodySmall)
            }
        }

        OfferFeed.NodeOffline -> Panel(title = "Local node offline", meta = "no offers") {
            Text(
                "Offers travel over your node's peer connections. Start the node from " +
                    "the Node section to see and publish offers.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Distinct from "no offers": claiming the network is empty when the
        // node simply cannot answer would be a lie the user acts on.
        OfferFeed.Unsupported -> Panel(title = "Node too old", meta = "exchange unavailable") {
            Text(
                "Your node does not expose the exchange endpoints yet, so this screen " +
                    "cannot list or publish offers. Update the node to use it.",
                style = MaterialTheme.typography.bodySmall
            )
            HintText("This is not the same as there being no offers — nothing was asked.")
        }

        is OfferFeed.Failed -> Panel(title = "Could not load offers", meta = "node error") {
            ErrorText(state.message)
        }

        is OfferFeed.Loaded -> {
            val mine = { o: OfferSummary -> o.sellerAddress == activeWalletAddress }
            val taken = { o: OfferSummary -> o.claimedBy == activeWalletAddress }
            val visible = when (page) {
                // El mercado muestra lo tomable de otros. Las propias se ven en
                // "my offers": ofrecerte tu propia venta no tiene sentido.
                ExchangePage.Market -> state.value.offers.filter { it.status == "Open" && !mine(it) }
                ExchangePage.Sell -> state.value.offers.filter { mine(it) && it.status == "Open" }
                // Lo terminado no invade las vistas de trabajo: cancelado,
                // vencido y liberado viven en History. Una lista donde lo vivo
                // se pierde entre lo muerto obliga a leerla entera cada vez.
                ExchangePage.MyOffers -> state.value.offers.filter { mine(it) && !isFinished(it) }
                ExchangePage.MyTrades -> state.value.offers.filter { taken(it) && !isFinished(it) }
                ExchangePage.History -> state.value.offers.filter {
                    isFinished(it) && (mine(it) || taken(it))
                }
            }
            val (title, empty) = when (page) {
                ExchangePage.Market -> "Offers on the mesh" to
                    "No offers reached this node yet. They arrive by gossip, so an isolated node sees none."
                ExchangePage.Sell -> "Your open offers" to "You have no offers published."
                ExchangePage.MyOffers -> "Your live offers" to
                    "Nothing of yours is live. Anything finished is under History."
                ExchangePage.MyTrades -> "Trades in progress" to
                    "No trade of yours is running. Anything finished is under History."
                ExchangePage.History -> "Finished" to "Nothing has closed yet."
            }
            val ordered = when (sort) {
                OfferSort.Price -> visible.sortedBy { o ->
                    if (o.zkaAtoms > 0) o.microUsdt.toDouble() / o.zkaAtoms.toDouble() else Double.MAX_VALUE
                }
                OfferSort.Amount -> visible.sortedByDescending { it.zkaAtoms }
                OfferSort.Newest -> visible.sortedByDescending { it.createdAt }
            }
            Panel(title = title, meta = "${visible.size}") {
            if (ordered.isEmpty()) {
                EmptyState(empty)
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SortControls(selected = sort, onSelect = onSort)
                }
                OfferTableHeader()
                ordered.forEach { offer ->
                    OfferRow(
                        offer = offer,
                        activeWalletAddress = activeWalletAddress,
                        onClaim = onClaim,
                        onCancel = onCancel,
                        onReport = onReport
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun OfferRow(
    offer: OfferSummary,
    activeWalletAddress: String,
    onClaim: (OfferSummary) -> Unit,
    onCancel: (OfferSummary) -> Unit,
    onReport: (OfferSummary, String) -> Unit
) {
    var nowSeconds by remember(offer.id) {
        mutableStateOf(System.currentTimeMillis() / 1_000)
    }
    var reportOpen by remember(offer.id) { mutableStateOf(false) }
    var tronTxHash by remember(offer.id) { mutableStateOf("") }
    var disclosureAccepted by remember(offer.id) { mutableStateOf(false) }
    LaunchedEffect(offer.id, offer.claimExpiresAt) {
        while (isActive) {
            nowSeconds = System.currentTimeMillis() / 1_000
            delay(1_000)
        }
    }
    val isSeller = offer.sellerAddress == activeWalletAddress
    val isBuyer = offer.claimedBy == activeWalletAddress
    val reportAfter = offer.claimExpiresAt?.plus(5 * 60)
    val canReport = isBuyer &&
        offer.status == "Claimed" &&
        reportAfter != null &&
        nowSeconds >= reportAfter

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Fila de tabla, como el mockup: las columnas se leen de un vistazo y
        // el detalle del trato se despliega debajo solo cuando hace falta.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell("${formatZka(offer.zkaAtoms)} ZKA", Modifier.weight(1f), strong = true)
            TableCell("${formatUsdt(offer.microUsdt)} USDT", Modifier.weight(1f), strong = true)
            // Precio derivado, no un campo del nodo: USDT por ZKA. Es lo que
            // deja comparar dos ofertas de tamanos distintos.
            TableCell(unitPriceLabel(offer), Modifier.weight(0.8f))
            TableCell(shortenAddress(offer.sellerAddress), Modifier.weight(1.4f))
            TableCell(if (isSeller) "local" else "mesh", Modifier.weight(0.9f))
            TableCell(
                offer.claimExpiresAt?.let { countdownLabel(it, nowSeconds) } ?: "—",
                Modifier.weight(0.9f)
            )
            Row(
                Modifier.width(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(offer.status)
                if (offer.status == "Open" && !isSeller) {
                    ZButton(
                        onClick = { onClaim(offer) },
                        variant = ZButtonVariant.Accent,
                        size = ZButtonSize.Small,
                    ) { ZButtonText("Take offer") }
                }
            }
        }
        // El excedente sobre lo vendido no es una comision ni una perdida: es
        // vuelto que todavia no existe, porque la nota no se parte hasta que la
        // operacion cierra. Se muestra solo al vendedor y solo cuando hay
        // diferencia, para no ensuciar la fila de quien compra.
        if (isSeller && offer.reservedAtoms > offer.zkaAtoms) {
            val excess = offer.reservedAtoms - offer.zkaAtoms
            KeyValueRow("Locked in your wallet", "${formatZka(offer.reservedAtoms)} ZKA")
            HintText(
                "${formatZka(excess)} ZKA more than you are selling. A private note cannot " +
                    "be split, so the whole note backs the offer; the surplus returns as " +
                    "change when the trade closes or you cancel."
            )
        }
        CopyableKeyValueRow("Seller", offer.sellerAddress)
        when {
            offer.status == "Open" && isSeller -> {
                OutlinedButton(onClick = { onCancel(offer) }) {
                    Text("Cancel open offer")
                }
            }
            offer.status == "Open" -> {
                Button(onClick = { onClaim(offer) }, colors = zButtonColors()) {
                    Text("Request this offer")
                }
                HintText("Wait for the seller's signed acceptance before sending USDT.")
            }
            isBuyer && offer.status == "Claimed" -> {
                Text(
                    "Claim accepted by the seller. You may now send the exact USDT amount.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                CopyableKeyValueRow("Send USDT (TRC-20) to", offer.usdtReceiveAddress)
                KeyValueRow("Amount", "${formatUsdt(offer.microUsdt)} USDT")
                offer.claimExpiresAt?.let {
                    KeyValueRow("Payment window", countdownLabel(it, nowSeconds))
                }
            }
            isBuyer && offer.status == "PaymentDetected" -> {
                Text(
                    "The seller verified the Tron payment. The ZKA release is being built or rebroadcast.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            isBuyer && offer.status == "Released" -> {
                Text(
                    "The seller released the ZKA. Your wallet will show it after the private transaction confirms.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (canReport && !reportOpen) {
            OutlinedButton(onClick = { reportOpen = true }) {
                Text("Report payment without release")
            }
        }
        if (reportOpen) {
            Text(
                "Public warning: this publishes your Tron transaction hash to the Zoka gossip network.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = tronTxHash,
                onValueChange = { tronTxHash = it.trim() },
                label = { Text("Tron transaction hash (64 hex characters)") },
                singleLine = true,
                colors = zFieldColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = disclosureAccepted,
                    onCheckedChange = { disclosureAccepted = it }
                )
                Text(
                    "I understand this permanently links my report to that public Tron transaction.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onReport(offer, tronTxHash)
                        reportOpen = false
                    },
                    enabled = disclosureAccepted && tronTxHash.length == 64 &&
                        tronTxHash.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' },
                    colors = zButtonColors()
                ) {
                    Text("Publish report")
                }
                OutlinedButton(onClick = { reportOpen = false }) {
                    Text("Cancel")
                }
            }
        }
        if (offer.claimedBy != null && !isBuyer) {
            CopyableKeyValueRow("Buyer", offer.claimedBy)
        }
        if (offer.status == "Expired") {
            HintText("This claim expired after the seller completed a final private Tron check.")
        }
        if (offer.status == "Cancelled") {
            HintText("The seller cancelled this offer before it was claimed.")
        }
    }
}

private sealed interface OfferFeed {
    data object Loading : OfferFeed
    data object NodeOffline : OfferFeed
    /** The node has no exchange endpoints — an older build. */
    data object Unsupported : OfferFeed
    data class Loaded(val value: OfferList) : OfferFeed
    data class Failed(val message: String) : OfferFeed
}

private sealed interface OfferInput {
    data class Valid(val request: CreateOfferRequest) : OfferInput
    data class Invalid(val reason: String) : OfferInput
}

/**
 * Turns the form into a request, or explains why it cannot.
 *
 * Amounts are converted directly from decimal text to integers: the node
 * matches payments on the integer value, so a floating-point rounding
 * difference between the two sides would decide real money.
 */
private fun buildOfferRequest(zka: String, usdt: String, tron: String): OfferInput {
    val zkaAmount = zka.toBigDecimalOrNull()
        ?: return OfferInput.Invalid("Enter how much ZKA you are selling.")
    val usdtAmount = usdt.toBigDecimalOrNull()
        ?: return OfferInput.Invalid("Enter how much USDT you want for it.")
    if (zkaAmount.signum() <= 0) {
        return OfferInput.Invalid("The ZKA amount must be greater than zero.")
    }
    if (usdtAmount.signum() <= 0) {
        return OfferInput.Invalid("The USDT amount must be greater than zero.")
    }
    if (!looksLikeTronAddress(tron)) {
        return OfferInput.Invalid(
            "That is not a TRC-20 address. It should be 34 characters starting with T."
        )
    }
    val zkaAtoms = runCatching { decimalUnitsExact(zkaAmount, ATOMS_PER_ZKA) }
        .getOrElse {
            return OfferInput.Invalid(
                "ZKA supports at most 6 decimal places and the amount is too large."
            )
        }
    val microUsdt = runCatching { decimalUnitsExact(usdtAmount, MICRO_USDT_PER_USDT) }
        .getOrElse {
            return OfferInput.Invalid(
                "USDT supports at most 6 decimal places and the amount is too large."
            )
        }
    return OfferInput.Valid(
        CreateOfferRequest(
            zkaAtoms = zkaAtoms,
            microUsdt = microUsdt,
            usdtReceiveAddress = tron
        )
    )
}

internal fun decimalUnitsExact(amount: BigDecimal, unitsPerWhole: Long): Long =
    amount
        .multiply(BigDecimal.valueOf(unitsPerWhole))
        .setScale(0, RoundingMode.UNNECESSARY)
        .longValueExact()

private fun verificationLabel(status: String): String = when {
    status == "ready" -> "Tor and Tron verification ready"
    status == "tor_unavailable" -> "Tor is unavailable"
    status == "unknown" -> "waiting for the local node"
    status.startsWith("checking offer ") -> "checking a claimed offer privately"
    else -> status.replace('_', ' ').ifBlank { "unknown node response" }
}

private fun countdownLabel(deadline: Long, now: Long): String {
    val remaining = deadline - now
    if (remaining <= 0) {
        return "window elapsed — final seller verification pending"
    }
    val minutes = remaining / 60
    val seconds = remaining % 60
    return "${minutes}m ${seconds.toString().padStart(2, '0')}s remaining"
}

/**
 * Same shape check the node applies. Catching it here means an Ethereum
 * address pasted by mistake is rejected before an offer is published against
 * an address that can never receive the payment.
 */
private fun looksLikeTronAddress(address: String): Boolean {
    val base58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    return address.length == 34 &&
        address.startsWith("T") &&
        address.all { base58.contains(it) }
}

private fun formatZka(atoms: Long): String {
    val whole = atoms / ATOMS_PER_ZKA
    val frac = atoms % ATOMS_PER_ZKA
    return if (frac == 0L) whole.toString() else "$whole.${frac.toString().padStart(6, '0').trimEnd('0')}"
}

private fun formatUsdt(micro: Long): String {
    val whole = micro / MICRO_USDT_PER_USDT
    val frac = micro % MICRO_USDT_PER_USDT
    return if (frac == 0L) whole.toString() else "$whole.${frac.toString().padStart(6, '0').trimEnd('0')}"
}
