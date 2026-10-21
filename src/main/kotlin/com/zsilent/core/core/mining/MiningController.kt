package com.zsilent.core.core.mining

import com.zsilent.core.models.MiningSnapshot
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.PendingPrivateTransfers
import com.zsilent.core.models.RuntimeLog
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.core.process.ZokaProcessKiller
import com.zsilent.core.services.ZokaCli
import com.zsilent.core.rpc.ZokaRpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

internal const val MINING_POLL_INTERVAL_MS = 10_000L
internal const val MINING_CLI_FALLBACK_INTERVAL_MS = 15L * 60L * 1_000L

class MiningController(
    private val cli: ZokaCli,
    private val localRpc: ZokaRpcClient,
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private var process: Process? = null
    private var pollingStarted = false
    private var stopRequested = false
    private var snapshotFailures = 0
    private var lastMetricsWarningMillis = 0L
    private val refreshMutex = Mutex()
    private val cliFallbackThrottle = PollingThrottle(MINING_CLI_FALLBACK_INTERVAL_MS)

    @Volatile
    private var pollingPaused = false

    fun pausePolling() {
        pollingPaused = true
    }

    fun resumePolling() {
        pollingPaused = false
    }

    private val _status = MutableStateFlow(ServiceStatus.Offline)
    val status: StateFlow<ServiceStatus> = _status

    private val _snapshot = MutableStateFlow(MiningSnapshot())
    val snapshot: StateFlow<MiningSnapshot> = _snapshot

    private val _pendingTransfers = MutableStateFlow(PendingPrivateTransfers())
    val pendingTransfers: StateFlow<PendingPrivateTransfers> = _pendingTransfers

    private val _controlledByApp = MutableStateFlow(false)
    val controlledByApp: StateFlow<Boolean> = _controlledByApp

    private val _logs = MutableSharedFlow<RuntimeLog>(extraBufferCapacity = 256)
    val logs: SharedFlow<RuntimeLog> = _logs

    fun startPolling() {
        if (pollingStarted) return
        pollingStarted = true
        scope.launch {
            while (true) {
                if (!pollingPaused) {
                    refresh()
                }
                delay(MINING_POLL_INTERVAL_MS)
            }
        }
    }

    fun start(address: String, password: String, threads: Int, network: String = ZokaNetworkConfig.Network) {
        if (cli.zoka == null) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("mining", "Zoka CLI binary not found. Build target/release/zoka first."))
            return
        }
        if (!ZokaNetworkConfig.isAddress(address)) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("mining", "Mining requires a full Zoka Quantic reward address."))
            return
        }
        if (password.isBlank()) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("mining", "Wallet password is required to unlock mining rewards locally."))
            return
        }
        if (threads < 1) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("mining", "Mining threads must be at least 1."))
            return
        }
        if (process?.isAlive == true) {
            _logs.tryEmit(RuntimeLog("mining", "Mining is already controlled by ZSilent."))
            return
        }

        scope.launch {
            _status.value = ServiceStatus.Starting
            stopRequested = false
            // Toggle the running node's IN-PROCESS miner via its /mining RPC. No
            // separate `zoka mine` process → no RocksDB lock fight with the node,
            // and the node stays online + synced while mining starts/stops.
            val ok = postMiningControl("/mining/start", threads)
            if (ok) {
                _controlledByApp.value = true
                _status.value = ServiceStatus.Online
                _logs.tryEmit(RuntimeLog("mining", "Mining started in-process with $threads thread(s); the node stays online."))
                refresh(network)
            } else {
                _controlledByApp.value = false
                _status.value = ServiceStatus.Error
                _logs.tryEmit(RuntimeLog("mining", "Could not start mining — is the node running? (mining-control RPC unreachable)"))
            }
        }
    }

    fun stop() {
        scope.launch {
            _status.value = ServiceStatus.Stopping
            stopRequested = true
            // Stop the node's in-process miner via RPC. The node keeps running.
            val ok = postMiningControl("/mining/stop", null)
            _controlledByApp.value = false
            if (ok) {
                _logs.tryEmit(RuntimeLog("mining", "Mining stopped via the node; the node stays online and synced."))
            } else {
                _logs.tryEmit(RuntimeLog("mining", "Could not reach the node to stop mining (it may already be off)."))
            }
            _status.value = ServiceStatus.Offline
            refresh()
        }
    }

    private fun postMiningControl(path: String, threads: Int?): Boolean = runCatching {
        val rpc = ZokaNetworkConfig.LocalRpcUrl.ifBlank { "http://127.0.0.1:3000" }.trimEnd('/')
        val conn = java.net.URL("$rpc$path").openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        val body = if (threads != null) "{\"threads\":$threads}" else "{}"
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        runCatching { (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes() } }
        code in 200..299
    }.getOrElse { false }

    fun close() {
        scope.cancel()
        ZokaProcessKiller.terminate(process)
        ZokaProcessKiller.terminateManagedZokaProcesses(cli)
        process = null
        _controlledByApp.value = false
    }

    private suspend fun refresh(network: String = ZokaNetworkConfig.Network) = refreshMutex.withLock {
        // The running node already owns RocksDB and exposes the equivalent live
        // data. Prefer those endpoints so a UI poll does not create two short-
        // lived zoka processes (and a new RocksDB LOG.old) every few seconds.
        val rpcStats = runCatching { localRpc.stats() }
        rpcStats.onSuccess(::applyRpcStats)

        val rpcPending = runCatching { localRpc.pendingPrivateTransfers() }
        rpcPending.onSuccess { _pendingTransfers.value = it }

        if (rpcStats.isSuccess && rpcPending.isSuccess) return@withLock

        // Compatibility/offline fallback for an older or unavailable local
        // RPC. Results stay cached between attempts, and the mutex ensures
        // start/stop/manual refreshes cannot overlap this expensive DB open.
        if (!cliFallbackThrottle.tryAcquire(clockMillis())) return@withLock

        if (rpcStats.isFailure) {
            readSnapshot(network).onSuccess { mining ->
                snapshotFailures = 0
                _snapshot.value = mining
                _status.value = if (mining.running) ServiceStatus.Online else ServiceStatus.Offline
            }.onFailure(::applySnapshotFailure)
        }

        if (rpcPending.isFailure) {
            readPendingTransfers(network).onSuccess { _pendingTransfers.value = it }
        }
    }

    private fun applyRpcStats(stats: NodeStats) {
        snapshotFailures = 0
        val previous = _snapshot.value
        _snapshot.value = previous.copy(
            running = stats.localMining,
            chainHeight = stats.height,
            difficultyBits = stats.difficulty.toInt(),
            hashrateHps = if (stats.localMining) stats.reportedHashrateHps else 0,
            totalSupplyEmitted = stats.emissionZka.toString()
        )
        _status.value = if (stats.localMining) ServiceStatus.Online else ServiceStatus.Offline
    }

    private fun applySnapshotFailure(error: Throwable) {
        snapshotFailures += 1
        val previous = _snapshot.value
        if (previous.running || _controlledByApp.value) {
            _status.value = ServiceStatus.Online
            if (snapshotFailures >= 3) {
                emitMetricsWarning("Mining metrics are temporarily unavailable while the miner is running: ${sanitize(error.message ?: "unknown error")}")
            }
        } else if (snapshotFailures >= 3) {
            _snapshot.value = previous.copy(running = false, hashrateHps = 0)
            _status.value = ServiceStatus.Offline
            emitMetricsWarning("Mining metrics remain cached after repeated read failures: ${sanitize(error.message ?: "unknown error")}")
        }
    }

    private fun readSnapshot(network: String): Result<MiningSnapshot> = runCatching {
        runJsonCommand("mine-status", "--network", network, "--json")
    }

    private fun readPendingTransfers(network: String): Result<PendingPrivateTransfers> = runCatching {
        runJsonCommand("pending", "--network", network, "--json")
    }

    private inline fun <reified T> runJsonCommand(vararg args: String): T {
        val zoka = cli.zoka ?: error("Zoka CLI binary not found")
        return cli.withZokaCommandLock {
            val child = cli.processBuilder(zoka, args.toList())
                .redirectErrorStream(true)
                .start()
            val finished = child.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                child.destroyForcibly()
                error("zoka ${args.firstOrNull().orEmpty()} timed out")
            }
            val output = child.inputStream.bufferedReader().readText()
            val exitCode = child.exitValue()
            if (exitCode != 0) {
                error(sanitize(output.ifBlank { "zoka exited with code $exitCode" }))
            }
            codec.decodeFromString(output.trim())
        }
    }

    private suspend fun streamLogs(child: Process) {
        BufferedReader(InputStreamReader(child.inputStream)).useLines { lines ->
            lines.forEach { line ->
                val clean = sanitize(line)
                if (clean.isNotBlank()) {
                    _logs.tryEmit(RuntimeLog("mining", clean))
                }
            }
        }
    }

    private fun sanitize(line: String): String {
        val sensitive = listOf("seed", "mnemonic", "private key", "spending", "password", "secret")
        val noAnsi = line.replace(Regex("\u001B\\[[;?0-9]*[ -/]*[@-~]"), "")
        return if (sensitive.any { noAnsi.contains(it, ignoreCase = true) }) {
            "[redacted sensitive log line]"
        } else {
            noAnsi.trim()
        }
    }

    private fun emitMetricsWarning(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastMetricsWarningMillis < 15_000L) return
        lastMetricsWarningMillis = now
        _logs.tryEmit(RuntimeLog("mining", message))
    }
}

internal class PollingThrottle(private val minimumIntervalMillis: Long) {
    private var lastAttemptMillis: Long? = null

    init {
        require(minimumIntervalMillis >= 0) { "minimumIntervalMillis cannot be negative" }
    }

    @Synchronized
    fun tryAcquire(nowMillis: Long): Boolean {
        val previous = lastAttemptMillis
        if (previous != null && nowMillis >= previous && nowMillis - previous < minimumIntervalMillis) {
            return false
        }
        lastAttemptMillis = nowMillis
        return true
    }
}
