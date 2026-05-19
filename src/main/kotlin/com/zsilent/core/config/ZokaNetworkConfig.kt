package com.zsilent.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ZokaNetworkSettings(
    val network: String = "",
    val chainId: String = "",
    val status: String = "",
    val localRpcUrl: String = "",
    val remoteRpcUrl: String = "",
    val cliRemoteRpcUrl: String = "",
    val bootstrapMultiaddr: String = "",
    val manifestPath: String = ""
)

@Serializable
private data class NetworkManifest(
    @SerialName("chain_id")
    val chainId: String = "",
    val status: String = "",
    @SerialName("seed_nodes")
    val seedNodes: List<SeedNode> = emptyList(),
    @SerialName("public_rpc")
    val publicRpc: List<RpcEndpoint> = emptyList(),
    val desktop: DesktopConfig = DesktopConfig()
)

@Serializable
private data class SeedNode(
    val multiaddr: String = "",
    val status: String = ""
)

@Serializable
private data class RpcEndpoint(
    val url: String = "",
    val status: String = ""
)

@Serializable
private data class DesktopConfig(
    @SerialName("local_rpc_url")
    val localRpcUrl: String = ""
)

object ZokaNetworkConfig {
    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var current: ZokaNetworkSettings = ZokaNetworkSettings()

    val Network: String
        get() = current.network
    val LocalRpcUrl: String
        get() = current.localRpcUrl
    val RemoteRpcUrl: String
        get() = current.remoteRpcUrl
    val CliRemoteRpcUrl: String
        get() = current.cliRemoteRpcUrl.ifBlank { current.remoteRpcUrl }
    val BootstrapMultiaddr: String
        get() = current.bootstrapMultiaddr
    val ChainId: String
        get() = current.chainId
    val Status: String
        get() = current.status
    val ManifestPath: String
        get() = current.manifestPath

    fun configure(settings: ZokaNetworkSettings) {
        current = settings
    }

    fun load(projectRoot: File, resourcesRoot: File? = null): ZokaNetworkSettings {
        val manifestFile = resolveManifestFile(projectRoot, resourcesRoot)
        val manifest = manifestFile
            ?.takeIf { it.exists() && it.isFile }
            ?.let { runCatching { codec.decodeFromString<NetworkManifest>(it.readText()) }.getOrNull() }

        val activeRpc = manifest?.publicRpc
            ?.firstOrNull { it.status.equals("active", ignoreCase = true) && it.url.isNotBlank() }
            ?.url
            .orEmpty()
        val activeSeed = manifest?.seedNodes
            ?.firstOrNull { it.status.equals("active", ignoreCase = true) && it.multiaddr.isNotBlank() }
            ?.multiaddr
            .orEmpty()
        val chainId = envOrProperty("ZSILENT_CHAIN_ID", "zsilent.chainId")
            ?: manifest?.chainId.orEmpty()
        val network = envOrProperty("ZSILENT_NETWORK", "zsilent.network")
            ?: when {
                chainId.contains("testnet", ignoreCase = true) -> "testnet"
                chainId.contains("mainnet", ignoreCase = true) -> "mainnet"
                else -> ""
            }
        val remoteRpc = envOrProperty("ZSILENT_REMOTE_RPC_URL", "zsilent.remoteRpcUrl")
            ?: activeRpc
        val cliRpc = envOrProperty("ZSILENT_CLI_RPC_URL", "zsilent.cliRpcUrl")
            ?: remoteRpc
        val localRpc = envOrProperty("ZSILENT_LOCAL_RPC_URL", "zsilent.localRpcUrl")
            ?: manifest?.desktop?.localRpcUrl.orEmpty()

        return ZokaNetworkSettings(
            network = network,
            chainId = chainId,
            status = manifest?.status.orEmpty(),
            localRpcUrl = localRpc.trimEnd('/'),
            remoteRpcUrl = remoteRpc.trimEnd('/'),
            cliRemoteRpcUrl = cliRpc.trimEnd('/'),
            bootstrapMultiaddr = envOrProperty("ZSILENT_BOOTSTRAP_MULTIADDR", "zsilent.bootstrapMultiaddr")
                ?: activeSeed,
            manifestPath = manifestFile?.absolutePath.orEmpty()
        ).also(::configure)
    }

    fun displayUrl(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .ifBlank { "not configured" }
    }

    private fun resolveManifestFile(projectRoot: File, resourcesRoot: File?): File? {
        val configured = envOrProperty("ZSILENT_NETWORK_MANIFEST", "zsilent.networkManifest")
        if (!configured.isNullOrBlank()) {
            return File(configured).let { if (it.isAbsolute) it else File(projectRoot, configured) }
        }
        return listOfNotNull(
            resourcesRoot?.let { File(it, "docs/mainnet-network.v1.json") },
            resourcesRoot?.let { File(it, "config/mainnet-network.v1.json") },
            resourcesRoot?.let { File(it, "mainnet-network.v1.json") },
            File(projectRoot, "docs/mainnet-network.v1.json"),
            File(projectRoot, "config/mainnet-network.v1.json"),
            File(projectRoot, "mainnet-network.v1.json")
        ).firstOrNull { it.exists() }
    }

    private fun envOrProperty(envName: String, propertyName: String): String? {
        return System.getenv(envName)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(propertyName)?.takeIf { it.isNotBlank() }
    }
}
