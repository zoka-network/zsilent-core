package com.zsilent.core.rpc

import com.zsilent.core.models.ChainHeightResponse
import com.zsilent.core.models.NodeStats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ZokaRpcClient(private val baseUrl: String) {
    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(codec)
        }
    }

    suspend fun height(): Long {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        return client.get("$baseUrl/chain/height").body<ChainHeightResponse>().height
    }

    suspend fun stats(): NodeStats {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        return client.get("$baseUrl/stats").body()
    }

    suspend fun startMining(threads: Int) {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        client.post("$baseUrl/local/mining/start") {
            contentType(ContentType.Application.Json)
            setBody(LocalMiningControlRequest(threads.coerceAtLeast(1)))
        }
    }

    suspend fun stopMining() {
        require(baseUrl.isNotBlank()) { "RPC URL is not configured" }
        client.post("$baseUrl/local/mining/stop")
    }

    fun close() {
        client.close()
    }
}

@Serializable
private data class LocalMiningControlRequest(
    val threads: Int
)
