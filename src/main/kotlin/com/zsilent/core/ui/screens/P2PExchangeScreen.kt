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
    ScreenScaffold(
        title = "P2P Exchange",
        subtitle = "Read this before trading."
    ) {
        Panel(title = "Before you trade", meta = "read once") {
            Text(
                "Trading ZKA for USDT here happens directly between you and another " +
                    "person. Nobody holds the funds for you and nobody can reverse a " +
                    "mistake.",
                style = MaterialTheme.typography.bodyMedium
            )

            RiskLine(
                "The ZKA lock is not an escrow",
                "Your ZKA is marked reserved inside this wallet. That is bookkeeping, " +
                    "not a blockchain escrow: a modified client could spend it anyway. " +
                    "Trade with people you have reason to trust."
            )
            RiskLine(
                "There is no arbiter",
                "If a trade goes wrong there is no dispute process, no support desk and " +
                    "no way to claw funds back."
            )
            RiskLine(
                "Payment is verified on Tron",
                "Your node checks the USDT transfer over Tor. If Tor is down, payments " +
                    "cannot be verified. A claimed offer stays locked until a private " +
                    "check succeeds; the node never falls back to a direct connection."
            )
            RiskLine(
                "Offers expire after 45 minutes",
                "If no qualifying payment arrived within the window, the offer closes and " +
                    "your ZKA is released only after your node completes one final private " +
                    "Tron check."
            )
            RiskLine(
                "There is no amount cap",
                "The backend flow has had a dedicated review, but this is still an " +
                    "experimental mainnet feature with no testnet phase. Start with the " +
                    "minimum and never risk an amount you would mind losing."
            )

            Button(onClick = onAccept, colors = zButtonColors()) {
                Text("I understand — continue")
            }
        }
    }
}

@Composable
private fun RiskLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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

    ScreenScaffold(
        title = "P2P Exchange",
        subtitle = "Sell ZKA for USDT, directly with another peer."
    ) {
        val loaded = state as? OfferFeed.Loaded
        val offers = loaded?.value?.offers.orEmpty()
        val verification = loaded?.value?.verification ?: "unknown"
        val verificationReady = verification == "ready" || verification.startsWith("checking offer ")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Offers", offers.size.toString(), "visible to your node", Modifier.weight(1f))
            StatusCard(
                "Open",
                offers.count { it.status == "Open" }.toString(),
                "claimable now",
                Modifier.weight(1f)
            )
            StatusCard(
                "Verification",
                if (verificationReady) "ready" else "blocked",
                verificationLabel(verification),
                Modifier.weight(1f)
            )
        }

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

        Panel(title = "Publish an offer", meta = "you sell ZKA") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = zkaInput,
                    onValueChange = { zkaInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("ZKA to sell") },
                    singleLine = true,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = usdtInput,
                    onValueChange = { usdtInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("USDT you want") },
                    singleLine = true,
                    colors = zFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = tronAddress,
                onValueChange = { tronAddress = it.trim() },
                label = { Text("Your USDT address (TRC-20, starts with T)") },
                singleLine = true,
                colors = zFieldColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
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
                "Publishing locks the ZKA in this wallet until the offer completes, " +
                    "expires, or you cancel it."
            )
        }

        OfferFeedPanel(
            state = state,
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

        is OfferFeed.Loaded -> Panel(
            title = "Offers",
            meta = "${state.value.offers.size} relevant"
        ) {
            if (state.value.offers.isEmpty()) {
                EmptyState("No live offers or trades are relevant to this wallet right now.")
            } else {
                state.value.offers.forEach { offer ->
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
        KeyValueRow(
            "${formatZka(offer.zkaAtoms)} ZKA",
            "for ${formatUsdt(offer.microUsdt)} USDT"
        )
        KeyValueRow("Status", offer.status)
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
