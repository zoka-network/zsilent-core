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
    val manifestPath: String = "",
    val minimumClientVersion: String = ""
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
    val desktop: DesktopConfig = DesktopConfig(),
    @SerialName("minimum_client_version")
    val minimumClientVersion: String = ""
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
    // Sin respaldo a un servidor que ya no existe.
    //
    // Esto apuntaba a `rpc.zoka.network:3000`, pensado para que el host
    // pudiera rotar sin recompilar. Pero la red paso a ser serverless el
    // 2026-07-09 y ese nombre ya ni siquiera resuelve en DNS: cada ZSilent
    // instalado lo intenta en cada ciclo de consulta y falla, para siempre.
    //
    // El manifiesto es coherente con eso -- `public_rpc` viene vacio --, asi
    // que la cadena de respaldo tenia que terminar en "no hay", no en un host
    // muerto. Vacio esta contemplado en los dos usos: `ZokaCli` ya consulta
    // `isNotBlank()` antes de pasarlo, y la llamada del tablero vive dentro de
    // un `runCatching`. Un despliegue que quiera volver a tener RPC publico lo
    // pone en el manifiesto o en `ZSILENT_REMOTE_RPC_URL`.
    private const val DEFAULT_REMOTE_RPC_URL = ""

    // Zenith es la testnet de Zoka Quantic desde octubre de 2026. Su chain id
    // lleva "mainnet" por historia y no se puede renombrar: de el sale el hash
    // del genesis. Por eso `Network` sigue valiendo "mainnet" para esta cadena
    // (es la palabra que se le pasa al nodo con --network, y los nodos viejos
    // no conocen otra) y lo que se le muestra a la persona sale de
    // `DisplayName`/`IsTestnet`, nunca de `Network`.
    const val ZENITH_CHAIN_ID = "zoka-mainnet-zenith-v1"
    private const val DEFAULT_CHAIN_ID = ZENITH_CHAIN_ID
    private const val DEFAULT_NETWORK = "mainnet"

    /** App version, injected at build time via -Dzsilent.version (see build.gradle.kts). */
    val AppVersion: String = System.getProperty("zsilent.version")?.takeIf { it.isNotBlank() } ?: "dev"

    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var current: ZokaNetworkSettings = ZokaNetworkSettings()

    val Network: String
        get() = current.network
    /** True when the configured chain has no monetary value (Zenith included). */
    val IsTestnet: Boolean
        get() = isTestnet(current.chainId, current.network)
    /** Network name for people: titles, pills and onboarding. */
    val DisplayName: String
        get() = displayName(current.chainId, current.network)
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
    val MinimumClientVersion: String
        get() = current.minimumClientVersion

    /** Returns true if the running app version is below the network-required minimum. */
    fun isClientOutdated(): Boolean {
        val minimum = MinimumClientVersion.takeIf { it.isNotBlank() } ?: return false
        if (AppVersion == "dev") return false
        return compareVersions(AppVersion, minimum) < 0
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").mapNotNull { it.toIntOrNull() }
        val partsB = b.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(partsA.size, partsB.size)
        for (i in 0 until len) {
            val diff = (partsA.getOrElse(i) { 0 }) - (partsB.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

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
            ?: manifest?.chainId?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CHAIN_ID
        val network = envOrProperty("ZSILENT_NETWORK", "zsilent.network")
            ?: when {
                chainId.contains("testnet", ignoreCase = true) -> "testnet"
                chainId.contains("mainnet", ignoreCase = true) -> "mainnet"
                else -> DEFAULT_NETWORK
            }
        val remoteRpc = envOrProperty("ZSILENT_REMOTE_RPC_URL", "zsilent.remoteRpcUrl")
            ?: activeRpc.takeIf { it.isNotBlank() }
            ?: DEFAULT_REMOTE_RPC_URL
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
            manifestPath = manifestFile?.absolutePath.orEmpty(),
            minimumClientVersion = manifest?.minimumClientVersion.orEmpty()
        ).also(::configure)
    }

    // Sin configurar todavia (chain id vacio) se asume la cadena por defecto,
    // que es Zenith: mostrar "Mainnet" un instante seria justo el error que el
    // titulo tiene que evitar.
    fun isTestnet(chainId: String, network: String): Boolean {
        val chain = chainId.ifBlank { DEFAULT_CHAIN_ID }
        return chain == ZENITH_CHAIN_ID || network.equals("testnet", ignoreCase = true)
    }

    fun displayName(chainId: String, network: String): String {
        val chain = chainId.ifBlank { DEFAULT_CHAIN_ID }
        return when {
            chain == ZENITH_CHAIN_ID -> "Zenith testnet"
            network.equals("testnet", ignoreCase = true) -> "Testnet"
            else -> "Mainnet"
        }
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
