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

data class TorLaunchConfig(
    val socksHost: String = "127.0.0.1",
    val socksPort: Int = 19050,
    val hiddenServiceVirtualPort: Int = 28080,
    val hiddenServiceTargetHost: String = "127.0.0.1",
    val hiddenServiceTargetPort: Int = 28080
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
        writeTorrc(torrc, dataDir, hiddenServiceDir, config)

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

        val hostnameFile = File(hiddenServiceDir, "hostname")
        val deadline = System.currentTimeMillis() + 90_000L
        while (System.currentTimeMillis() < deadline) {
            if (!child.isAlive) {
                val exit = runCatching { child.exitValue() }.getOrDefault(-1)
                val message = "Tor stopped before bootstrap (exit=$exit)."
                _state.value = _state.value.copy(status = ServiceStatus.Error, error = message)
                throw IllegalStateException(message)
            }

            val onionHost = hostnameFile.readTextIfExists()
                ?.lineSequence()
                ?.map { it.trim() }
                ?.firstOrNull { it.endsWith(".onion") }
                .orEmpty()
            val socksReady = canConnect(config.socksHost, config.socksPort)
            if (onionHost.isNotBlank() && socksReady && (bootstrapped || _state.value.status == ServiceStatus.Online)) {
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

        val message = "Tor did not finish bootstrap or create hostname within 90 seconds."
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
        config: TorLaunchConfig
    ) {
        torrc.parentFile?.mkdirs()
        torrc.writeText(
            """
            SocksPort ${config.socksHost}:${config.socksPort}
            DataDirectory ${torPath(dataDir)}
            HiddenServiceDir ${torPath(hiddenServiceDir)}
            HiddenServiceVersion 3
            HiddenServicePort ${config.hiddenServiceVirtualPort} ${config.hiddenServiceTargetHost}:${config.hiddenServiceTargetPort}
            Log notice stdout
            """.trimIndent() + "\n"
        )
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

    private fun torPath(file: File): String {
        val path = file.absolutePath.replace("\\", "/")
        return if (path.any { it.isWhitespace() }) "\"$path\"" else path
    }

    private fun File.readTextIfExists(): String? {
        return runCatching { if (isFile) readText() else null }.getOrNull()
    }
}
