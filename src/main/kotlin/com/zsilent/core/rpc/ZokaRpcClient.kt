package com.zsilent.core.rpc

import com.zsilent.core.models.ChainHeightResponse
import com.zsilent.core.models.NodeStats
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
