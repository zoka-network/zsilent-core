package com.zsilent.core.rpc

import com.zsilent.core.models.AccountCheck
import com.zsilent.core.models.BlockSummary
import com.zsilent.core.models.BlockTransactions
import com.zsilent.core.models.ChainHeightResponse
import com.zsilent.core.models.ClaimOfferResponse
import com.zsilent.core.models.CreateOfferRequest
import com.zsilent.core.models.JoinMiningTeamRequest
import com.zsilent.core.models.PaymentFailureSummary
import com.zsilent.core.models.ReportPaymentFailureRequest
import com.zsilent.core.models.ViewScanKeyRequest
import com.zsilent.core.models.ViewScanResponse
import com.zsilent.core.models.MempoolTransaction
import com.zsilent.core.models.ExchangeDiagnostics
import com.zsilent.core.models.OfferList
import com.zsilent.core.models.OfferSummary
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.NodeSyncStatus
import com.zsilent.core.models.PendingPrivateTransfers
import com.zsilent.core.models.PeersResponse
import com.zsilent.core.models.TeamMiningControlRequest
import com.zsilent.core.models.TeamMiningStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ZokaRpcClient(val baseUrl: String) {
    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // No ContentNegotiation plugin — avoids Ktor ServiceLoader conflict in
    // jpackage bundles. JSON is parsed manually with kotlinx.serialization.
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 5_000
        }
    }

    suspend fun height(): Long {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val text = client.get("$baseUrl/chain/height").bodyAsText()
        return codec.decodeFromString<ChainHeightResponse>(text).height
    }

    suspend fun stats(): NodeStats {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val text = client.get("$baseUrl/stats").bodyAsText()
        return codec.decodeFromString<NodeStats>(text)
    }

    // The node's own sync view (P2P mesh + RPC combined). Meaningful even
    // when the centralized bootstrap RPC is saturated or unreachable.
    suspend fun syncStatus(): NodeSyncStatus {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val text = client.get("$baseUrl/sync/status").bodyAsText()
        return codec.decodeFromString<NodeSyncStatus>(text)
    }

    // The local node's live mesh view: which p2p peers it is connected to and
    // over which transport (Tor onion / direct). Serverless replacement for a
    // central "who's online" directory — a node only knows its own peers.
    suspend fun peers(): PeersResponse {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val text = client.get("$baseUrl/peers").bodyAsText()
        return codec.decodeFromString<PeersResponse>(text)
    }

    /**
     * Read the running node's pending queue without spawning a second zoka
     * process (and therefore without repeatedly reopening RocksDB).
     */
    suspend fun pendingPrivateTransfers(): PendingPrivateTransfers {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val text = client.get("$baseUrl/private/pending").bodyAsText()
        return decodePendingPrivateTransfers(text, codec)
    }

    suspend fun startMining(threads: Int) {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val body = codec.encodeToString(LocalMiningControlRequest(threads.coerceAtLeast(1)))
        client.post("$baseUrl/local/mining/start") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun stopMining() {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        client.post("$baseUrl/local/mining/stop")
    }

    /**
     * GETs [path] and decodes it, returning null when the node answers 404.
     *
     * The distinction matters for the explorer: "this block does not exist"
     * is an answer worth showing, while "the node never replied" is a failure
     * the user has to be told about. Collapsing both into null would make an
     * unreachable node look like an empty chain, so transport failures still
     * propagate as exceptions.
     */
    private suspend inline fun <reified T> getOrNull(path: String): T? {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.get("$baseUrl$path")
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) {
            throw IllegalStateException("RPC $path failed: ${response.status}")
        }
        return codec.decodeFromString<T>(response.bodyAsText())
    }

    suspend fun latestBlock(): BlockSummary? = getOrNull("/blocks/latest")

    suspend fun blockByHeight(height: Long): BlockSummary? = getOrNull("/blocks/$height")

    suspend fun blockByHash(hash: String): BlockSummary? = getOrNull("/blocks/hash/$hash")

    suspend fun blockTransactions(height: Long): BlockTransactions? =
        getOrNull("/blocks/$height/transactions")

    /**
     * Looks up an *unconfirmed* transaction. The node keeps no tx-hash index
     * over mined blocks, so a hash that already made it into a block 404s here
     * — absence means "not pending", never "does not exist".
     */
    suspend fun mempoolTransaction(hash: String): MempoolTransaction? =
        getOrNull("/mempool/tx/$hash")

    /**
     * Whether the chain knows this address. The node returns existence only —
     * balance and nonce stay private — so this answers "is this address real"
     * and nothing more.
     */
    suspend fun accountExists(address: String): AccountCheck? =
        getOrNull("/accounts/$address")

    /**
     * True when this client points at a node on this machine.
     *
     * The view scan key reveals every private payment its owner has received.
     * Sending it to a remote node would hand that power to whoever runs it, so
     * the scan is refused unless the RPC is local.
     */
    fun isLocalEndpoint(): Boolean {
        val host = runCatching { java.net.URI(baseUrl).host }.getOrNull()?.lowercase()
        return host == "127.0.0.1" || host == "localhost" || host == "::1" || host == "[::1]"
    }

    /**
     * Finds private outputs belonging to [scanKeyHex].
     *
     * Refuses outright when the endpoint is not local, rather than trusting the
     * caller to have checked: this is the one request in the client that
     * carries a secret capable of deanonymising the user's whole history.
     */
    suspend fun viewScanByKey(scanKeyHex: String, limit: Int? = null): ViewScanResponse {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        check(isLocalEndpoint()) {
            "refusing to send a view scan key to a non-local node ($baseUrl)"
        }
        val response = client.post("$baseUrl/private/view-scan-key") {
            contentType(ContentType.Application.Json)
            setBody(codec.encodeToString(ViewScanKeyRequest(scanKeyHex, limit)))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(body.ifBlank { "view scan failed: ${response.status}" })
        }
        return codec.decodeFromString(body)
    }

    /**
     * Raised when the node answers 404 on an endpoint the app knows about.
     *
     * For the exchange this is expected rather than exceptional: a node built
     * before offers existed has no such route, and the UI must say "your node
     * is too old" instead of "there are no offers", which would be a lie about
     * the network.
     */
    class EndpointUnsupported(val path: String) : Exception("node has no $path endpoint")

    suspend fun teamMiningStatus(): TeamMiningStatus =
        getOrNull<TeamMiningStatus>("/team-mining/status")
            ?: throw EndpointUnsupported("/team-mining/status")

    suspend fun createMiningTeam(): TeamMiningStatus =
        postTeamMining("/team-mining/team")

    suspend fun joinMiningTeam(inviteCode: String): TeamMiningStatus =
        postTeamMining(
            "/team-mining/join",
            codec.encodeToString(JoinMiningTeamRequest(inviteCode.trim().uppercase()))
        )

    suspend fun leaveMiningTeam(): TeamMiningStatus =
        postTeamMining("/team-mining/leave")

    /** Disuelve el equipo. Solo el coordinador puede; el resto usa leave. */
    suspend fun disbandMiningTeam(): TeamMiningStatus =
        postTeamMining("/team-mining/disband")

    suspend fun startTeamMining(threads: Int): TeamMiningStatus =
        postTeamMining(
            "/team-mining/start",
            codec.encodeToString(TeamMiningControlRequest(threads.coerceAtLeast(1)))
        )

    suspend fun stopTeamMining(): TeamMiningStatus =
        postTeamMining("/team-mining/stop")

    private suspend fun postTeamMining(path: String, jsonBody: String? = null): TeamMiningStatus {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post("$baseUrl$path") {
            if (jsonBody != null) {
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw EndpointUnsupported(path)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(body.ifBlank { "team mining request failed: ${response.status}" })
        }
        return codec.decodeFromString(body)
    }

    /**
     * Offers this node knows about. Throws [EndpointUnsupported] when the node
     * predates the exchange.
     */
    suspend fun exchangeOffers(): OfferList =
        getOrNull<OfferList>("/p2p-exchange/offers")
            ?: throw EndpointUnsupported("/p2p-exchange/offers")

    /**
     * Integridad de la reserva que respalda cada oferta viva de este nodo.
     *
     * Solo habla de las ofertas propias: la reserva vive en la cartera del
     * vendedor y nadie más puede verla. Devuelve `null` en nodos anteriores al
     * endpoint, para que una versión vieja no rompa la pantalla.
     */
    suspend fun exchangeDiagnostics(): ExchangeDiagnostics? =
        getOrNull<ExchangeDiagnostics>("/p2p-exchange/diagnostics")

    /**
     * Pide una pasada de verificación ahora, sin esperar el ciclo de 10 s.
     *
     * Es comodidad, no corrección: el nodo revisa igual por su cuenta.
     */
    suspend fun exchangeCheckNow(): Boolean {
        if (baseUrl.isBlank()) return false
        return runCatching {
            client.post("$baseUrl/p2p-exchange/check-now").status.isSuccess()
        }.getOrDefault(false)
    }

    suspend fun createOffer(request: CreateOfferRequest): OfferSummary {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post("$baseUrl/p2p-exchange/offers") {
            contentType(ContentType.Application.Json)
            setBody(codec.encodeToString(request))
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw EndpointUnsupported("/p2p-exchange/offers")
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            // The node explains refusals (below minimum, too many open offers,
            // malformed TRON address); surface its wording rather than a
            // generic failure the user cannot act on.
            throw IllegalStateException(body.ifBlank { "offer rejected: ${response.status}" })
        }
        return codec.decodeFromString(body)
    }

    suspend fun claimOffer(offerId: String): ClaimOfferResponse {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post("$baseUrl/p2p-exchange/offers/$offerId/claim")
        if (response.status == HttpStatusCode.NotFound) {
            throw EndpointUnsupported("/p2p-exchange/offers/$offerId/claim")
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(body.ifBlank { "claim refused: ${response.status}" })
        }
        return codec.decodeFromString(body)
    }

    /**
     * El comprador suelta la oferta que había tomado y vuelve al mercado.
     *
     * Sin esto, arrepentirse dejaba la venta trabada los 45 minutos completos:
     * nadie más podía tomarla y el vendedor tampoco podía cancelarla, porque
     * cancelar exige que esté abierta.
     */
    suspend fun releaseClaim(offerId: String): Boolean {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post("$baseUrl/p2p-exchange/offers/$offerId/release-claim")
        if (response.status == HttpStatusCode.NotFound) {
            throw EndpointUnsupported("/p2p-exchange/offers/$offerId/release-claim")
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(body.ifBlank { "release refused: ${response.status}" })
        }
        return true
    }

    suspend fun cancelOffer(offerId: String): OfferSummary {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post("$baseUrl/p2p-exchange/offers/$offerId/cancel")
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(body.ifBlank { "cancellation refused: ${response.status}" })
        }
        return codec.decodeFromString(body)
    }

    suspend fun reportPaymentFailure(
        offerId: String,
        tronTxHash: String
    ): PaymentFailureSummary {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post(
            "$baseUrl/p2p-exchange/offers/$offerId/report-payment-failure"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                codec.encodeToString(
                    ReportPaymentFailureRequest(
                        tronTxHash = tronTxHash,
                        acknowledgePublicDisclosure = true
                    )
                )
            )
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(body.ifBlank { "report refused: ${response.status}" })
        }
        return codec.decodeFromString(body)
    }

    // Returns the Block JSON for a given height.
    // Same format as GET /blocks/:height/raw, accepted by POST /sync/block.
    suspend fun getRawBlock(height: Long): String {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        return client.get("$baseUrl/blocks/$height/raw").bodyAsText()
    }

    // Submit a raw BlockResponseMessage JSON to the node's /blocks/submit
    // endpoint. Returns true if the node accepted or already knew the block.
    suspend fun submitBlock(rawBlockJson: String): Boolean {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.post("$baseUrl/sync/block") {
            contentType(ContentType.Application.Json)
            setBody(rawBlockJson)
        }
        val status = response.status.value
        return status in 200..299
    }

    fun close() {
        client.close()
    }
}

@Serializable
private data class LocalMiningControlRequest(
    val threads: Int
)

@Serializable
private data class PendingPrivateTransfersResponse(
    val count: Int = 0,
    val pending: List<PendingPrivateTransferRef> = emptyList()
)

@Serializable
private data class PendingPrivateTransferRef(
    @kotlinx.serialization.SerialName("tx_hash")
    val txHash: String = ""
)

internal fun decodePendingPrivateTransfers(
    text: String,
    codec: Json = Json { ignoreUnknownKeys = true; isLenient = true }
): PendingPrivateTransfers {
    val response = codec.decodeFromString<PendingPrivateTransfersResponse>(text)
    val hashes = response.pending.map { it.txHash.trim() }.filter { it.isNotBlank() }.distinct()
    return PendingPrivateTransfers(
        count = maxOf(response.count, hashes.size),
        pendingPrivateTxHashes = hashes
    )
}
