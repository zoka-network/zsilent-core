package com.zsilent.core.core.node

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.core.process.ZokaProcessKiller
import com.zsilent.core.models.RuntimeLog
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class NodeController(private val cli: ZokaCli) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var process: Process? = null
    private var stopRequested = false
    private var lastStartParams: StartParams? = null
    private var restartAttempt = 0

    private data class StartParams(
        val password: String,
        val address: String,
        val network: String,
        val power: String,
        val autoMine: Boolean,
        val threads: Int?,
        val torProxy: String,
        val torOnly: Boolean
    )

    private val _status = MutableStateFlow(ServiceStatus.Offline)
    val status: StateFlow<ServiceStatus> = _status

    private val _logs = MutableSharedFlow<RuntimeLog>(extraBufferCapacity = 256)
    val logs: SharedFlow<RuntimeLog> = _logs

    fun start(
        password: String,
        address: String = "",
        network: String = ZokaNetworkConfig.Network,
        power: String = "low",
        autoMine: Boolean = false,
        threads: Int? = null,
        torProxy: String = "",
        torOnly: Boolean = false
    ) {
        val binary = cli.zoka
        if (process?.isAlive == true) {
            _logs.tryEmit(RuntimeLog("node", "Node is already controlled by ZSilent."))
            return
        }
        if (binary == null) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("node", "zoka binary not found. Build target/release/zoka first."))
            return
        }
        if (password.isBlank()) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("node", "Wallet password is required to start the node."))
            return
        }
        if ((torProxy.isNotBlank() || torOnly) && !supportsTorFlags(binary)) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("node", "This zokahd binary does not support Tor flags. Rebuild with cargo build --release --features randomx."))
            return
        }

        stopRequested = false
        lastStartParams = StartParams(password, address, network, power, autoMine, threads, torProxy, torOnly)
        _status.value = ServiceStatus.Starting
        val command = mutableListOf(
            "fullnode",
            "--network", network,
            "--password-env", "ZSILENT_NODE_PASSWORD",
            "--power", power,
            "--allow-isolated-start",
            "--sync-warmup-secs", "30",
            "--sync-rebroadcast-recent-blocks", "32"
        )
        if (ZokaNetworkConfig.ManifestPath.isNotBlank()) {
            command += listOf("--network-config", ZokaNetworkConfig.ManifestPath)
        }
        if (ZokaNetworkConfig.BootstrapMultiaddr.isNotBlank()) {
            command += listOf("--seed", ZokaNetworkConfig.BootstrapMultiaddr)
        }
        if (!autoMine) {
            command += "--no-mine"
        }
        if (autoMine && threads != null && threads > 0) {
            command += listOf("--threads", threads.toString())
        }
        if (autoMine) {
            command += listOf("--min-mine-peers", "1")
        }
        if (address.isNotBlank()) {
            command += listOf("--address", address)
        }
        if (torProxy.isNotBlank()) {
            command += listOf("--tor-proxy", torProxy)
        }
        if (torOnly) {
            command += "--tor-only"
        }
        val builder = cli.processBuilder(binary, command)
        builder.environment()["ZSILENT_NODE_PASSWORD"] = password
        builder.redirectErrorStream(true)

        scope.launch {
            runCatching {
                val child = builder.start()
                process = child
                _status.value = ServiceStatus.Online
                _logs.tryEmit(
                    RuntimeLog(
                        "node",
                        if (autoMine) {
                            "Started full node with integrated mining. Waiting for RPC and sync warmup."
                        } else {
                            "Started full node in node-only mode. Waiting for RPC and sync warmup."
                        }
                    )
                )
                streamLogs(child)
                val exitCode = child.waitFor()
                val expectedStop = stopRequested
                process = null
                stopRequested = false
                _logs.tryEmit(RuntimeLog("node", "Node stopped with exit code $exitCode"))
                if (expectedStop) {
                    _status.value = ServiceStatus.Offline
                    restartAttempt = 0
                } else {
                    handleUnexpectedExit(exitCode)
                }
            }.onFailure {
                process = null
                stopRequested = false
                _logs.tryEmit(RuntimeLog("node", "Failed to start node: ${sanitize(it.message ?: "unknown error")}"))
                _status.value = ServiceStatus.Error
            }
        }
    }

    private suspend fun handleUnexpectedExit(exitCode: Int) {
        val params = lastStartParams
        if (params == null) {
            _status.value = if (exitCode == 0) ServiceStatus.Offline else ServiceStatus.Error
            return
        }
        restartAttempt += 1
        if (restartAttempt > 5) {
            _logs.tryEmit(RuntimeLog("node", "Node exited unexpectedly $restartAttempt times in a row; giving up auto-restart. Toggle the node again from the panel."))
            _status.value = ServiceStatus.Error
            restartAttempt = 0
            return
        }
        val backoffSeconds = (2 * restartAttempt).coerceAtMost(30).toLong()
        _logs.tryEmit(RuntimeLog("node", "Node exited unexpectedly (code=$exitCode). Auto-restart attempt $restartAttempt in ${backoffSeconds}s..."))
        _status.value = ServiceStatus.Starting
        kotlinx.coroutines.delay(backoffSeconds * 1000L)
        if (stopRequested) {
            _status.value = ServiceStatus.Offline
            return
        }
        start(
            password = params.password,
            address = params.address,
            network = params.network,
            power = params.power,
            autoMine = params.autoMine,
            threads = params.threads,
            torProxy = params.torProxy,
            torOnly = params.torOnly
        )
    }

    fun stop() {
        _status.value = ServiceStatus.Stopping
        stopRequested = true
        lastStartParams = null
        restartAttempt = 0
        ZokaProcessKiller.terminate(process)
        ZokaProcessKiller.terminateManagedZokaProcesses(cli)
        process = null
        _status.value = ServiceStatus.Offline
    }

    fun close() {
        stop()
    }

    private suspend fun streamLogs(child: Process) {
        BufferedReader(InputStreamReader(child.inputStream)).useLines { lines ->
            lines.forEach { line ->
                val clean = sanitize(line)
                if (clean.isNotBlank() && !shouldDropLine(clean)) {
                    _logs.tryEmit(RuntimeLog("node", clean))
                }
            }
        }
    }

    private fun sanitize(line: String): String {
        val noAnsi = line.replace(Regex("\\[[;?0-9]*[ -/]*[@-~]"), "")
        val sensitive = listOf("seed", "mnemonic", "private key", "spending", "password", "secret")
        return if (sensitive.any { noAnsi.contains(it, ignoreCase = true) }) {
            "[redacted sensitive log line]"
        } else {
            noAnsi.trim()
        }
    }

    private fun shouldDropLine(line: String): Boolean {
        if (line.isBlank()) return true
        if (line.all { it == '-' || it == '=' || it == ' ' || it == '|' }) return true
        return DASHBOARD_NOISE.any { line.startsWith(it) }
    }

    companion object {
        private val DASHBOARD_NOISE = listOf(
            "ZOKA Fullnode",
            "Session uptime",
            "Last event",
            "Wallet",
            "Local label",
            "Address",
            "Available",
            "Locked",
            "Total",
            "Transactions",
            "View-only",
            "Network",
            "Peer ID",
            "Listen address",
            "Seed peer",
            "Transport",
            "Connected peers",
            "Incompatible peers",
            "PBFT peers",
            "Chain",
            "Pending private txs",
            "Recent avg block txs",
            "Next block reward",
            "Emitted supply",
            "Remaining supply",
            "Next halving",
            "Mining",
            "Auto-mining",
            "Algorithm",
            "CPU power",
            "Mining height",
            "Synced height",
            "Warmup remaining",
            "Mode",
            "Reward target",
        )
    }

    private fun supportsTorFlags(binary: File): Boolean {
        return helpContainsTorFlags(binary, listOf("fullnode", "--help"))
    }

    private fun helpContainsTorFlags(binary: File, args: List<String> = listOf("--help")): Boolean {
        return runCatching {
            cli.withZokaCommandLock {
                val child = cli.processBuilder(binary, args)
                    .redirectErrorStream(true)
                    .start()
                val output = child.inputStream.bufferedReader().readText()
                child.waitFor()
                output.contains("--tor-proxy") && output.contains("--tor-only")
            }
        }.getOrDefault(false)
    }
}
