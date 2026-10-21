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

    // Desde la 3.0 ZSilent es solo de Zoka Quantic (decisión del 21/10/2026):
    // Zenith queda en las versiones anteriores y en la línea de comandos.
    // `Network` es la palabra que se le pasa al nodo con --network.
    const val ZENITH_CHAIN_ID = "zoka-mainnet-zenith-v1"
    const val QUANTIC_TESTNET_CHAIN_ID = "zoka-quantic-testnet-v1"
    const val QUANTIC_MAINNET_CHAIN_ID = "zoka-quantic-v1"
    const val QUANTIC_REGTEST_CHAIN_ID = "zoka-quantic-regtest-v1"
    private const val DEFAULT_CHAIN_ID = QUANTIC_TESTNET_CHAIN_ID
    private const val DEFAULT_NETWORK = "quantic-testnet"

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
    /** Network name for people: titles, dialogs and onboarding. */
    val DisplayName: String
        get() = displayName(current.chainId, current.network)
    /** Lowercase compact form for status pills ("quantic testnet"). */
    val ShortName: String
        get() = shortName(current.chainId, current.network)
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
            ?: networkForChain(chainId)
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

    /** La palabra de `--network` para una cadena de Quantic. */
    fun networkForChain(chainId: String): String = when (chainId.ifBlank { DEFAULT_CHAIN_ID }) {
        QUANTIC_MAINNET_CHAIN_ID -> "quantic-mainnet"
        QUANTIC_REGTEST_CHAIN_ID -> "quantic-regtest"
        else -> DEFAULT_NETWORK
    }

    /** El puerto P2P del nodo en esta red (el del servicio onion y de los pares). */
    val P2pPort: Int
        get() = p2pPort(current.network)

    fun p2pPort(network: String): Int = when (network.ifBlank { DEFAULT_NETWORK }) {
        "quantic-mainnet" -> 28380
        "quantic-regtest" -> 28280
        else -> 28180
    }

    /** Cómo empiezan las direcciones de la red configurada (`zqtest1…`). */
    val AddressPrefix: String
        get() = addressPrefix(current.network)

    fun addressPrefix(network: String): String = when (network.ifBlank { DEFAULT_NETWORK }) {
        "quantic-mainnet" -> "zq1"
        "quantic-regtest" -> "zqregtest1"
        else -> "zqtest1"
    }

    /**
     * Si `text` parece una dirección de esta red. Es el filtro rápido de la
     * interfaz; la que decide es la del nodo, que comprueba el checksum.
     */
    fun isAddress(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.length > AddressPrefix.length + 64 && trimmed.startsWith(AddressPrefix)
    }

    // Sin configurar todavía (chain id vacío) se asume la cadena por defecto,
    // la testnet: mostrar "Mainnet" un instante sería justo el error que el
    // título tiene que evitar.
    fun isTestnet(chainId: String, network: String): Boolean {
        return chainId.ifBlank { DEFAULT_CHAIN_ID } != QUANTIC_MAINNET_CHAIN_ID
    }

    fun displayName(chainId: String, network: String): String {
        val chain = chainId.ifBlank { DEFAULT_CHAIN_ID }
        return when {
            chain == QUANTIC_MAINNET_CHAIN_ID -> "Zoka Quantic"
            chain == QUANTIC_REGTEST_CHAIN_ID -> "Zoka Quantic · Regtest"
            else -> "Zoka Quantic · Testnet"
        }
    }

    fun shortName(chainId: String, network: String): String {
        val chain = chainId.ifBlank { DEFAULT_CHAIN_ID }
        return when {
            chain == QUANTIC_MAINNET_CHAIN_ID -> "quantic"
            chain == QUANTIC_REGTEST_CHAIN_ID -> "quantic regtest"
            else -> "quantic testnet"
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
        val network = envOrProperty("ZSILENT_NETWORK", "zsilent.network") ?: DEFAULT_NETWORK
        val name = "$network-network.v1.json"
        return listOfNotNull(
            resourcesRoot?.let { File(it, "docs/$name") },
            resourcesRoot?.let { File(it, "config/$name") },
            resourcesRoot?.let { File(it, name) },
            File(projectRoot, "docs/$name"),
            File(projectRoot, "config/$name"),
            File(projectRoot, name)
        ).firstOrNull { it.exists() }
    }

    private fun envOrProperty(envName: String, propertyName: String): String? {
        return System.getenv(envName)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(propertyName)?.takeIf { it.isNotBlank() }
    }
}
