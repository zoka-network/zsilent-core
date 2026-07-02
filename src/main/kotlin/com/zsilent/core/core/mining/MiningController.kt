package com.zsilent.core.core.mining

import com.zsilent.core.models.MiningSnapshot
import com.zsilent.core.models.NodeSyncStatus
import com.zsilent.core.models.PendingPrivateTransfers
import com.zsilent.core.models.RuntimeLog
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.core.process.ZokaProcessKiller
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class MiningController(private val cli: ZokaCli) {
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

    private val _syncStatus = MutableStateFlow(NodeSyncStatus())
    val syncStatus: StateFlow<NodeSyncStatus> = _syncStatus

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
                delay(2_000)
            }
        }
    }

    fun start(address: String, password: String, threads: Int, network: String = ZokaNetworkConfig.Network) {
        if (cli.zoka == null) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("mining", "Zoka CLI binary not found. Build target/release/zoka first."))
            return
        }
        if (!address.startsWith("zka1")) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("mining", "Mining requires a full zka1 reward address."))
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
            val sync = readSyncStatus().getOrElse { error ->
                _controlledByApp.value = false
                _status.value = ServiceStatus.Error
                _logs.tryEmit(RuntimeLog("mining", "Cannot start mining: local node sync-status is unavailable (${sanitize(error.message ?: "unknown error")})."))
                return@launch
            }
            _syncStatus.value = sync
            if (!sync.readyToMine) {
                _controlledByApp.value = false
                _status.value = ServiceStatus.Error
                _logs.tryEmit(RuntimeLog("mining", sync.miningGateReason()))
                return@launch
            }
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
                _logs.tryEmit(RuntimeLog("mining", "Could not start mining after sync gate passed: mining-control RPC rejected or was unreachable."))
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

    private fun readSyncStatus(): Result<NodeSyncStatus> = runCatching {
        val rpc = ZokaNetworkConfig.LocalRpcUrl.ifBlank { "http://127.0.0.1:3000" }.trimEnd('/')
        val conn = java.net.URL("$rpc/sync/status").openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 2500
        conn.readTimeout = 2500
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (code !in 200..299) {
            error("sync-status RPC returned HTTP $code")
        }
        codec.decodeFromString<NodeSyncStatus>(body)
    }

    fun close() {
        ZokaProcessKiller.terminate(process)
        ZokaProcessKiller.terminateManagedZokaProcesses(cli)
        process = null
        _controlledByApp.value = false
    }

    private suspend fun refresh(network: String = ZokaNetworkConfig.Network) {
        readSyncStatus().onSuccess { sync ->
            _syncStatus.value = sync
        }

        readSnapshot(network).onSuccess { mining ->
            snapshotFailures = 0
            _snapshot.value = mining
            _status.value = when {
                process?.isAlive == true -> ServiceStatus.Online
                mining.running -> ServiceStatus.Online
                else -> ServiceStatus.Offline
            }
        }.onFailure { error ->
            snapshotFailures += 1
            val processAlive = process?.isAlive == true
            val previous = _snapshot.value
            if (processAlive) {
                _snapshot.value = previous.copy(running = true)
                _status.value = ServiceStatus.Online
                emitMetricsWarning("Mining metrics are temporarily unavailable while the miner is running: ${sanitize(error.message ?: "unknown error")}")
            } else {
                _snapshot.value = previous.copy(running = false, hashrateHps = 0)
                _status.value = ServiceStatus.Offline
                if (snapshotFailures >= 3) {
                    emitMetricsWarning("Mining metrics were refreshed as offline after repeated read failures: ${sanitize(error.message ?: "unknown error")}")
                }
            }
        }

        readPendingTransfers(network).onSuccess {
            _pendingTransfers.value = it
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
