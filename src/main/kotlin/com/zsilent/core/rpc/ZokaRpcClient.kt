package com.zsilent.core.rpc

import com.zsilent.core.models.AccountCheck
import com.zsilent.core.models.ShortAddressLookup
import com.zsilent.core.models.BlockSummary
import com.zsilent.core.models.BlockTransactions
import com.zsilent.core.models.ViewScanResponse
import com.zsilent.core.models.MempoolTransaction
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.NodeSyncStatus
import com.zsilent.core.models.PendingPrivateTransfers
import com.zsilent.core.models.PeerTransportCounts
import com.zsilent.core.models.PeersResponse
import com.zsilent.core.models.PrivateTxLookup
import com.zsilent.core.models.StratumStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

    /**
     * `GET /status` del nodo v2: altura, punta, emisión, pares, Stratum y
     * checkpoint en una sola lectura. Casi todo lo que muestra el tablero sale
     * de acá; las funciones de abajo lo traducen a los modelos de siempre.
     */
    suspend fun status(): V2Status {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        val response = client.get("$baseUrl/status")
        if (!response.status.isSuccess()) {
            throw IllegalStateException("RPC /status failed: ${response.status}")
        }
        return codec.decodeFromString(response.bodyAsText())
    }

    suspend fun height(): Long = status().height

    suspend fun stats(): NodeStats = status().toStats()

    // The node's own sync view: how far the outbound peers report their tips.
    suspend fun syncStatus(): NodeSyncStatus = status().toSyncStatus()

    // Only counts: the v2 node does not list its peers.
    suspend fun peers(): PeersResponse = status().toPeers()

    /** Los pagos del mempool del nodo: el v2 los cuenta, sin hashes que mostrar. */
    suspend fun pendingPrivateTransfers(): PendingPrivateTransfers =
        PendingPrivateTransfers(count = status().mempoolTransactions.toInt())

    /**
     * La testnet v2 no tiene minería integrada que prender: se mina con XMRig
     * contra el Stratum del nodo (`--stratum`).
     */
    suspend fun startMining(threads: Int) {
        throw UnsupportedOperationException(NO_INTEGRATED_MINING)
    }

    suspend fun stopMining() {
        throw UnsupportedOperationException(NO_INTEGRATED_MINING)
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

    suspend fun latestBlock(): BlockSummary? = blockByHeight(height())

    /** El resumen del bloque, sin los cuerpos de sus pagos (llevan las pruebas). */
    suspend fun blockByHeight(height: Long): BlockSummary? = getOrNull("/blocks/$height/summary")

    // The v2 node indexes blocks by height only, and its payments are private:
    // there is no hash index, no transaction list, no account to look up.
    suspend fun blockByHash(hash: String): BlockSummary? = null

    suspend fun blockTransactions(height: Long): BlockTransactions? = null

    suspend fun mempoolTransaction(hash: String): MempoolTransaction? = null

    suspend fun privateTransaction(hash: String): PrivateTxLookup? = null

    suspend fun accountExists(address: String): AccountCheck? = null

    /**
     * La dirección completa de una corta en el directorio del nodo local, sin
     * preguntarles a sus pares (`GET /address/<corta>?local=1`), o null si no
     * la tiene. Para ZSilent, que la tenga quiere decir que ya está publicada.
     */
    suspend fun shortAddress(short: String): ShortAddressLookup? =
        getOrNull("/address/$short?local=1")

    /** El servidor Stratum del nodo; nulo si no se abrió. */
    suspend fun stratumStatus(): StratumStatus? = status().stratum

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
        // Las wallets v2 escanean su propia copia de la cadena: el nodo nunca
        // recibe una clave de vista.
        throw UnsupportedOperationException("the v2 node has no view-key scan; wallets scan their own chain copy")
    }

    // Moving raw blocks between nodes over HTTP was a v1 sync path. The v2
    // nodes exchange blocks over their own authenticated connections.
    suspend fun getRawBlock(height: Long): String {
        throw UnsupportedOperationException("the v2 node syncs blocks with its peers, not through ZSilent")
    }

    suspend fun submitBlock(rawBlockJson: String): Boolean = false

    fun close() {
        client.close()
    }
}

private const val NO_INTEGRATED_MINING =
    "the Zoka Quantic v2 testnet has no built-in miner: mine with XMRig against the node's Stratum"

/** `GET /status` del nodo v2 (`src/quantic/stark_api.rs` del nodo). */
@Serializable
data class V2Status(
    @SerialName("chain_id") val chainId: String = "",
    val height: Long = 0,
    val hash: String = "",
    @SerialName("tip_timestamp") val tipTimestamp: Long = 0,
    @SerialName("issued_atoms") val issuedAtoms: Long = 0,
    @SerialName("mempool_transactions") val mempoolTransactions: Long = 0,
    val stratum: StratumStatus? = null,
    val peers: V2Peers = V2Peers(),
    val checkpoint: V2Checkpoint? = null,
    @SerialName("checkpoint_authority") val checkpointAuthority: Boolean = false,
    val directory: V2Directory = V2Directory()
)

@Serializable
data class V2Peers(
    val outbound: Long = 0,
    val inbound: Long = 0,
    val known: Long = 0,
    val pending: Long = 0,
    val relay: Boolean = false,
    /** La punta más alta que informa un par saliente; cero sin ninguno. */
    @SerialName("best_height") val bestHeight: Long = 0
) {
    val connected: Long
        get() = outbound + inbound
}

@Serializable
data class V2Checkpoint(
    val height: Long = 0,
    @SerialName("block_hash") val blockHash: String = "",
    /** Si la cadena canónica del nodo contiene el checkpoint. */
    val followed: Boolean = false
)

@Serializable
data class V2Directory(
    val addresses: Long = 0,
    @SerialName("published_here") val publishedHere: Long = 0
)

private const val ATOMS_PER_ZKA = 1_000_000.0

internal fun V2Status.toStats() = NodeStats(
    height = height,
    emissionZka = issuedAtoms / ATOMS_PER_ZKA,
    totalSupplyEmittedAtoms = issuedAtoms,
    lastBlockTime = tipTimestamp,
    connectedPeers = peers.connected,
    minersOnline = (stratum?.miners ?: 0).toLong(),
    localMining = false
)

/**
 * Sincronizado cuando ningún par saliente informa una punta más alta que la
 * propia (con un bloque de margen). Una semilla, que solo tiene sesiones
 * entrantes, es la punta: queda sincronizada.
 */
internal fun V2Status.toSyncStatus(nowSeconds: Long = System.currentTimeMillis() / 1000): NodeSyncStatus {
    val target = maxOf(height, peers.bestHeight)
    val lag = target - height
    val synced = lag <= 1
    return NodeSyncStatus(
        status = if (synced) "synced" else "syncing",
        mode = "stark-v2",
        readyToMine = synced,
        localHeight = height,
        targetHeight = target,
        lag = lag,
        connectedPeers = peers.connected,
        minPeers = 1,
        lastUpdated = nowSeconds
    )
}

// The v2 node reaches its peers over iroh, directly or through a relay, never
// over Tor; it reports counts only. "Direct" carries the sessions it opened and
// "other" the ones opened to it, which is how the screens label them.
internal fun V2Status.toPeers() = PeersResponse(
    connectedPeers = peers.connected,
    byTransport = PeerTransportCounts(direct = peers.outbound, other = peers.inbound)
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
