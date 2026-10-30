package com.zsilent.core.core.tor

import com.zsilent.core.models.RuntimeLog
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

// El servicio onion publica el puerto P2P del nodo de la red que sigue ZSilent
// (28180 en la testnet de Quantic; Zenith usaba 28080).
data class TorLaunchConfig(
    val socksHost: String = "127.0.0.1",
    val socksPort: Int = 19050,
    val hiddenServiceVirtualPort: Int = com.zsilent.core.config.ZokaNetworkConfig.P2pPort,
    val hiddenServiceTargetHost: String = "127.0.0.1",
    val hiddenServiceTargetPort: Int = com.zsilent.core.config.ZokaNetworkConfig.P2pPort,
    /**
     * Whether this Tor also hosts an onion service (the v1 node did). The v2
     * node in Tor mode only dials out: a client Tor with its SOCKS port alone.
     */
    val hiddenService: Boolean = true
)

data class TorLaunchResult(
    val torExe: File,
    val socksHost: String,
    val socksPort: Int,
    val onionHost: String,
    val onionMultiaddr: String,
    val torrc: File
)

data class TorRuntimeState(
    val status: ServiceStatus = ServiceStatus.Offline,
    val socksHost: String = "127.0.0.1",
    val socksPort: Int = 19050,
    val onionHost: String = "",
    val onionMultiaddr: String = "",
    val error: String? = null
)

class TorSupervisor(private val cli: ZokaCli) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var process: Process? = null
    private var logJob: Job? = null
    @Volatile private var bootstrapped = false
    @Volatile private var lastHiddenService: Boolean? = null

    private val _state = MutableStateFlow(TorRuntimeState())
    val state: StateFlow<TorRuntimeState> = _state

    private val _logs = MutableSharedFlow<RuntimeLog>(extraBufferCapacity = 256)
    val logs: SharedFlow<RuntimeLog> = _logs

    suspend fun start(config: TorLaunchConfig = TorLaunchConfig()): TorLaunchResult = withContext(Dispatchers.IO) {
        cachedResult(config)?.let { return@withContext it }

        val torExe = discoverTorBinary()
            ?: throw IllegalStateException("tor.exe was not found. Package Tor Expert Bundle under common/tor/windows-x86_64 or set ZSILENT_TOR_BIN.")

        val torRoot = File(cli.dataRoot, "tor").apply { mkdirs() }
        val dataDir = File(torRoot, "data").apply { mkdirs() }
        val hiddenServiceDir = File(torRoot, "zoka-p2p").apply { mkdirs() }
        val torrc = File(torRoot, "torrc")
        writeTorrc(torrc, dataDir, hiddenServiceDir, config, torExe.parentFile)
        lastHiddenService = config.hiddenService

        stopProcessOnly()
        bootstrapped = false
        _state.value = TorRuntimeState(
            status = ServiceStatus.Starting,
            socksHost = config.socksHost,
            socksPort = config.socksPort
        )
        _logs.tryEmit(RuntimeLog("tor", "Starting managed Tor at ${config.socksHost}:${config.socksPort}."))

        val child = ProcessBuilder(torExe.absolutePath, "-f", torrc.absolutePath)
            .directory(torRoot)
            .redirectErrorStream(true)
            .start()
        process = child
        logJob = scope.launch { streamLogs(child) }

        // Readiness gate: the onion hostname exists AND the SOCKS port accepts
        // connections. Deliberately NOT gated on seeing "Bootstrapped 100%" in
        // the log stream -- that marker can be missed (log buffering, or a
        // warm DataDirectory that reconnects without re-emitting it), which
        // used to hang this for the full timeout and block the node from
        // starting at all. Once the hidden service descriptor exists and SOCKS
        // is up, hand back the onion; the node runs over clearnet in the
        // meantime and its onion dials start succeeding as Tor finishes
        // building circuits. Timeout shortened so a genuinely stuck Tor falls
        // back to the node's clearnet startup quickly.
        val hostnameFile = File(hiddenServiceDir, "hostname")
        val deadline = System.currentTimeMillis() + if (config.hiddenService) 40_000L else 90_000L
        while (System.currentTimeMillis() < deadline) {
            if (!child.isAlive) {
                val exit = runCatching { child.exitValue() }.getOrDefault(-1)
                val message = "Tor stopped before it was ready (exit=$exit)."
                _state.value = _state.value.copy(status = ServiceStatus.Error, error = message)
                throw IllegalStateException(message)
            }

            val onionHost = hostnameFile.readTextIfExists()
                ?.lineSequence()
                ?.map { it.trim() }
                ?.firstOrNull { it.endsWith(".onion") }
                .orEmpty()
            val socksReady = canConnect(config.socksHost, config.socksPort)
            // A client Tor is ready once its SOCKS port answers; the node
            // waits on its circuits (it allows 90 s for a handshake over Tor).
            if (!config.hiddenService && socksReady) {
                val result = TorLaunchResult(
                    torExe = torExe,
                    socksHost = config.socksHost,
                    socksPort = config.socksPort,
                    onionHost = "",
                    onionMultiaddr = "",
                    torrc = torrc
                )
                _state.value = TorRuntimeState(
                    status = ServiceStatus.Online,
                    socksHost = result.socksHost,
                    socksPort = result.socksPort
                )
                _logs.tryEmit(RuntimeLog("tor", "Managed Tor ready (client only) at ${config.socksHost}:${config.socksPort}."))
                return@withContext result
            }
            if (onionHost.isNotBlank() && socksReady) {
                val onionId = onionHost.removeSuffix(".onion")
                val onionMultiaddr = "/onion3/$onionId:${config.hiddenServiceVirtualPort}"
                val result = TorLaunchResult(
                    torExe = torExe,
                    socksHost = config.socksHost,
                    socksPort = config.socksPort,
                    onionHost = onionHost,
                    onionMultiaddr = onionMultiaddr,
                    torrc = torrc
                )
                _state.value = TorRuntimeState(
                    status = ServiceStatus.Online,
                    socksHost = result.socksHost,
                    socksPort = result.socksPort,
                    onionHost = result.onionHost,
                    onionMultiaddr = result.onionMultiaddr
                )
                _logs.tryEmit(RuntimeLog("tor", "Managed Tor ready with onion ${result.onionHost}."))
                return@withContext result
            }
            delay(500)
        }

        val message = "Tor SOCKS/onion was not ready within 40 seconds; starting the node without Tor for now."
        _state.value = _state.value.copy(status = ServiceStatus.Error, error = message)
        throw IllegalStateException(message)
    }

    @Synchronized
    fun stop() {
        stopProcessOnly()
        _state.value = TorRuntimeState()
    }

    fun discoverTorBinary(): File? {
        val configured = System.getenv("ZSILENT_TOR_BIN")
            ?.takeIf { it.isNotBlank() }
            ?: System.getProperty("zsilent.torBin")?.takeIf { it.isNotBlank() }
        return buildList {
            configured?.let { add(File(it)) }
            cli.resourcesRoot?.let { root ->
                add(File(root, "common/tor/windows-x86_64/tor.exe"))
                add(File(root, "tor/windows-x86_64/tor.exe"))
                add(File(root, "common/tor/windows-x86_64/Tor/tor.exe"))
                add(File(root, "tor/windows-x86_64/Tor/tor.exe"))
                add(File(root, "common/tor/windows-x86_64/tor/tor.exe"))
                add(File(root, "tor/windows-x86_64/tor/tor.exe"))
                add(File(root, "common/tor/windows-x86_64/bin/tor.exe"))
                add(File(root, "tor/windows-x86_64/bin/tor.exe"))
            }
            add(File(cli.dataRoot, "tor/bin/tor.exe"))
            add(File(cli.projectRoot, "third_party/tor/windows-x86_64/tor.exe"))
            add(File(cli.projectRoot, "third_party/tor/windows-x86_64/Tor/tor.exe"))
            add(File(cli.projectRoot, "third_party/tor/windows-x86_64/tor/tor.exe"))
            add(File(cli.projectRoot, "third_party/tor/windows-x86_64/bin/tor.exe"))
        }.firstOrNull { it.exists() && it.isFile }
    }

    private fun cachedResult(config: TorLaunchConfig): TorLaunchResult? {
        process?.takeIf { it.isAlive } ?: return null
        val torExe = discoverTorBinary() ?: return null
        if (!config.hiddenService) {
            // A Tor started for the other mode has the wrong torrc.
            if (lastHiddenService != false || !canConnect(config.socksHost, config.socksPort)) return null
            _state.value = TorRuntimeState(status = ServiceStatus.Online, socksHost = config.socksHost, socksPort = config.socksPort)
            return TorLaunchResult(torExe, config.socksHost, config.socksPort, "", "", File(File(cli.dataRoot, "tor"), "torrc"))
        }
        if (lastHiddenService == false) return null
        val hiddenServiceDir = File(File(cli.dataRoot, "tor"), "zoka-p2p")
        val onionHost = File(hiddenServiceDir, "hostname").readTextIfExists()
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.endsWith(".onion") }
            .orEmpty()
        if (onionHost.isBlank() || !canConnect(config.socksHost, config.socksPort)) return null
        val onionMultiaddr = "/onion3/${onionHost.removeSuffix(".onion")}:${config.hiddenServiceVirtualPort}"
        _state.value = TorRuntimeState(
            status = ServiceStatus.Online,
            socksHost = config.socksHost,
            socksPort = config.socksPort,
            onionHost = onionHost,
            onionMultiaddr = onionMultiaddr
        )
        return TorLaunchResult(
            torExe = torExe,
            socksHost = config.socksHost,
            socksPort = config.socksPort,
            onionHost = onionHost,
            onionMultiaddr = onionMultiaddr,
            torrc = File(File(cli.dataRoot, "tor"), "torrc")
        )
    }

    private fun writeTorrc(
        torrc: File,
        dataDir: File,
        hiddenServiceDir: File,
        config: TorLaunchConfig,
        bundleDir: File?
    ) {
        torrc.parentFile?.mkdirs()
        torrc.writeText(buildTorrcContent(dataDir, hiddenServiceDir, config, bundleDir))
    }

    companion object {
        /// Pure torrc builder, split out from the file write so it can be
        /// unit-tested without launching Tor.
        internal fun buildTorrcContent(
            dataDir: File,
            hiddenServiceDir: File,
            config: TorLaunchConfig,
            bundleDir: File?
        ): String {
            // The Expert Bundle ships geoip/geoip6 next to tor.exe. Point Tor
            // at them explicitly: otherwise it resolves the default GeoIPFile
            // path relative to the working directory, fails to find it, and
            // logs a warning on every start. Only emitted when the files are
            // actually present so a geoip-less bundle still produces a valid
            // torrc.
            val geoipLines = buildString {
                bundleDir?.let { dir ->
                    File(dir, "geoip").takeIf { it.isFile }?.let {
                        append("GeoIPFile ${torPath(it)}\n")
                    }
                    File(dir, "geoip6").takeIf { it.isFile }?.let {
                        append("GeoIPv6File ${torPath(it)}\n")
                    }
                }
            }
            val hiddenServiceLines = if (config.hiddenService) {
                """
                HiddenServiceDir ${torPath(hiddenServiceDir)}
                HiddenServiceVersion 3
                HiddenServicePort ${config.hiddenServiceVirtualPort} ${config.hiddenServiceTargetHost}:${config.hiddenServiceTargetPort}
                """.trimIndent() + "\n"
            } else {
                ""
            }
            return """
                SocksPort ${config.socksHost}:${config.socksPort}
                DataDirectory ${torPath(dataDir)}
            """.trimIndent() + "\n" + hiddenServiceLines + "Log notice stdout\n" + geoipLines
        }

        private fun torPath(file: File): String {
            val path = file.absolutePath.replace("\\", "/")
            return if (path.any { it.isWhitespace() }) "\"$path\"" else path
        }
    }

    private suspend fun streamLogs(child: Process) {
        child.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val clean = line.trim().take(2_000)
                if (clean.isBlank()) return@forEach
                if (clean.contains("Bootstrapped 100%", ignoreCase = true)) {
                    bootstrapped = true
                    _state.value = _state.value.copy(status = ServiceStatus.Online, error = null)
                }
                _logs.tryEmit(RuntimeLog("tor", clean))
            }
        }
    }

    @Synchronized
    private fun stopProcessOnly() {
        logJob?.cancel()
        logJob = null
        process?.let { child ->
            if (child.isAlive) {
                _logs.tryEmit(RuntimeLog("tor", "Stopping managed Tor."))
                child.destroy()
                if (!child.waitFor(5, TimeUnit.SECONDS)) {
                    child.destroyForcibly()
                }
            }
        }
        process = null
        bootstrapped = false
    }

    private fun canConnect(host: String, port: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 350)
            }
        }.isSuccess
    }

    private fun File.readTextIfExists(): String? {
        return runCatching { if (isFile) readText() else null }.getOrNull()
    }
}
