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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var risksAccepted by remember { mutableStateOf(false) }

    if (!risksAccepted) {
        ExchangeRiskGate(onAccept = { risksAccepted = true })
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
                    "cannot be verified and the offer will expire instead of releasing."
            )
            RiskLine(
                "Offers expire after 45 minutes",
                "If a buyer does not pay within the window, the offer closes and your " +
                    "ZKA is released back to you."
            )
            RiskLine(
                "Start small",
                "This feature is new and has not been through a security review yet. " +
                    "Do not put an amount at risk that you would mind losing."
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
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        state = withContext(Dispatchers.IO) {
            try {
                OfferFeed.Loaded(appState.localRpc.exchangeOffers().offers)
            } catch (unsupported: ZokaRpcClient.EndpointUnsupported) {
                OfferFeed.Unsupported
            } catch (t: Throwable) {
                OfferFeed.Failed(t.message ?: "Could not reach the local node")
            }
        }
    }

    LaunchedEffect(ui.localRpcOnline) {
        if (ui.localRpcOnline) reload() else state = OfferFeed.NodeOffline
    }

    ScreenScaffold(
        title = "P2P Exchange",
        subtitle = "Sell ZKA for USDT, directly with another peer."
    ) {
        val offers = (state as? OfferFeed.Loaded)?.offers.orEmpty()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            StatusCard("Offers", offers.size.toString(), "visible to your node", Modifier.weight(1f))
            StatusCard(
                "Open",
                offers.count { it.status == "Open" }.toString(),
                "claimable now",
                Modifier.weight(1f)
            )
            StatusCard(
                "Tor",
                if (ui.torEnabled) "on" else "off",
                if (ui.torEnabled) "payment checks private" else "required to verify",
                Modifier.weight(1f)
            )
        }

        if (!ui.torEnabled) {
            Panel(title = "Tor is off", meta = "verification blocked") {
                Text(
                    "Payment checks query a public Tron API. With Tor off that request " +
                        "would expose your IP and the address you are watching, so the " +
                        "exchange will not run them. Enable Tor under Privacy.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
                enabled = ui.localRpcOnline,
                colors = zButtonColors()
            ) {
                Text("Publish offer")
            }
            HintText(
                "Publishing locks the ZKA in this wallet until the offer completes, " +
                    "expires, or you cancel it."
            )
        }

        OfferFeedPanel(state, onClaim = { offer ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    runCatching { appState.localRpc.claimOffer(offer.id) }
                }
                reload()
            }
        })
    }
}

@Composable
private fun OfferFeedPanel(state: OfferFeed, onClaim: (OfferSummary) -> Unit) {
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
            meta = "${state.offers.size} visible"
        ) {
            if (state.offers.isEmpty()) {
                EmptyState("No offers are being advertised by your peers right now.")
            } else {
                state.offers.forEach { offer -> OfferRow(offer, onClaim) }
            }
        }
    }
}

@Composable
private fun OfferRow(offer: OfferSummary, onClaim: (OfferSummary) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KeyValueRow(
            "${formatZka(offer.zkaAtoms)} ZKA",
            "for ${formatUsdt(offer.microUsdt)} USDT"
        )
        KeyValueRow("Status", offer.status)
        CopyableKeyValueRow("Seller", offer.sellerAddress)
        if (offer.status == "Open") {
            Button(onClick = { onClaim(offer) }, colors = zButtonColors()) {
                Text("Take this offer")
            }
        }
    }
}

private sealed interface OfferFeed {
    data object Loading : OfferFeed
    data object NodeOffline : OfferFeed
    /** The node has no exchange endpoints — an older build. */
    data object Unsupported : OfferFeed
    data class Loaded(val offers: List<OfferSummary>) : OfferFeed
    data class Failed(val message: String) : OfferFeed
}

private sealed interface OfferInput {
    data class Valid(val request: CreateOfferRequest) : OfferInput
    data class Invalid(val reason: String) : OfferInput
}

/**
 * Turns the form into a request, or explains why it cannot.
 *
 * Amounts are converted to integers here and never handled as Double beyond
 * parsing: the node matches payments on the integer value, so a rounding
 * difference between the two sides would decide real money.
 */
private fun buildOfferRequest(zka: String, usdt: String, tron: String): OfferInput {
    val zkaAmount = zka.toDoubleOrNull()
        ?: return OfferInput.Invalid("Enter how much ZKA you are selling.")
    val usdtAmount = usdt.toDoubleOrNull()
        ?: return OfferInput.Invalid("Enter how much USDT you want for it.")
    if (zkaAmount <= 0) return OfferInput.Invalid("The ZKA amount must be greater than zero.")
    if (usdtAmount <= 0) return OfferInput.Invalid("The USDT amount must be greater than zero.")
    if (!looksLikeTronAddress(tron)) {
        return OfferInput.Invalid(
            "That is not a TRC-20 address. It should be 34 characters starting with T."
        )
    }
    return OfferInput.Valid(
        CreateOfferRequest(
            zkaAtoms = Math.round(zkaAmount * ATOMS_PER_ZKA),
            microUsdt = Math.round(usdtAmount * MICRO_USDT_PER_USDT),
            usdtReceiveAddress = tron
        )
    )
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
