package com.zsilent.core.rpc

import com.zsilent.core.models.AccountCheck
import com.zsilent.core.models.ShortAddressLookup
import com.zsilent.core.models.BlockSummary
import com.zsilent.core.models.BlockTransactions
import com.zsilent.core.models.ChainHeightResponse
import com.zsilent.core.models.ViewScanKeyRequest
import com.zsilent.core.models.ViewScanResponse
import com.zsilent.core.models.MempoolTransaction
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.NodeSyncStatus
import com.zsilent.core.models.PendingPrivateTransfers
import com.zsilent.core.models.PeersResponse
import com.zsilent.core.models.PrivateTxLookup
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
     * Where a private transaction stands on the chain: "Confirmed" with the
     * block that holds it, "Pending", "Rejected"/"Spent" with the reason, or
     * "Unknown". This is the only way to place an already-mined transaction,
     * since the node indexes no confirmed tx hashes; it answers only for the
     * private transactions whose body it keeps. 404 means it has no body.
     */
    suspend fun privateTransaction(hash: String): PrivateTxLookup? =
        getOrNull("/private/tx/$hash")

    /**
     * Whether the chain knows this address. The node returns existence only —
     * balance and nonce stay private — so this answers "is this address real"
     * and nothing more.
     */
    suspend fun accountExists(address: String): AccountCheck? =
        getOrNull("/accounts/$address")

    /**
     * La dirección completa de una corta en el directorio del nodo
     * (`GET /quantic/address/<corta>`), o null si el nodo no la tiene. Para
     * ZSilent, que la tenga el nodo local quiere decir que ya está publicada.
     */
    suspend fun shortAddress(short: String): ShortAddressLookup? =
        getOrNull("/quantic/address/$short")

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
