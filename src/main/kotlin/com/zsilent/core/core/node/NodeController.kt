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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.Writer

class NodeController(private val cli: ZokaCli) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var process: Process? = null
    private var stopRequested = false
    private var lastStartParams: StartParams? = null
    private var restartAttempt = 0
    val readyFile: File = File(cli.dataRoot, "zsilent/fullnode-ready.json")

    /// Full node output persisted to disk. The in-app panel is capped and
    /// filtered; this is the complete record, and the path users can be
    /// pointed at when reporting a problem.
    val logFile: File = File(cli.dataRoot, "logs/node.log")
    private var logWriter: Writer? = null

    private data class StartParams(
        val password: String,
        val address: String,
        val network: String,
        val power: String,
        val autoMine: Boolean,
        val threads: Int?,
        val torProxy: String,
        val torOnly: Boolean,
        val externalAddrs: List<String>
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
        torOnly: Boolean = false,
        externalAddrs: List<String> = emptyList()
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
        val cleanExternalAddrs = externalAddrs.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanExternalAddrs.isNotEmpty() && !supportsExternalAddrFlag(binary)) {
            _status.value = ServiceStatus.Error
            _logs.tryEmit(RuntimeLog("node", "This zoka binary does not support --external-addr. Rebuild the Rust node before enabling managed onion reachability."))
            return
        }

        stopRequested = false
        lastStartParams = StartParams(password, address, network, power, autoMine, threads, torProxy, torOnly, cleanExternalAddrs)
        _status.value = ServiceStatus.Starting
        readyFile.parentFile?.mkdirs()
        if (readyFile.exists()) readyFile.delete()
        val command = mutableListOf(
            "fullnode",
            // The node defaults to a fluid streaming console (as of the
            // ZokaNetwork node build that follows v1.6.1); ZSilent parses
            // stdout for its own log panel (see streamLogs/DASHBOARD_NOISE
            // below), which is tuned to the older full-screen dashboard's
            // line format, so keep requesting that explicitly here.
            "--dashboard",
            "--network", network,
            "--password-env", "ZSILENT_NODE_PASSWORD",
            "--power", power,
            "--allow-isolated-start",
            "--sync-warmup-secs", "30",
            "--sync-rebroadcast-recent-blocks", "32",
            "--ready-file", readyFile.absolutePath
        )
        if (ZokaNetworkConfig.ManifestPath.isNotBlank()) {
            command += listOf("--network-config", ZokaNetworkConfig.ManifestPath)
        }
        if (ZokaNetworkConfig.BootstrapMultiaddr.isNotBlank()) {
            command += listOf("--seed", ZokaNetworkConfig.BootstrapMultiaddr)
        }
        // Always launch the node in node-only mode (--no-mine). Mining is toggled
        // at RUNTIME via the node's /mining RPC, so the node stays online + synced
        // while mining is turned on/off without a restart. The miner params are
        // still passed so the runtime-enabled miner uses them.
        command += "--no-mine"
        if (threads != null && threads > 0) {
            command += listOf("--threads", threads.toString())
        }
        command += listOf("--min-mine-peers", "1")
        // Every ZSilent node automatically serves its chain as an iroh snapshot
        // (QUIC + NAT hole-punching, no open port) so fresh nodes can bootstrap
        // in one transfer instead of crawling block-by-block IBD. Fully guarded
        // node-side: it never blocks startup. Fresh-node auto-fetch is on by
        // default in the binary, so the whole flow needs no user action.
        command += "--serve-iroh-snapshot"
        if (address.isNotBlank()) {
            command += listOf("--address", address)
        }
        if (torProxy.isNotBlank()) {
            command += listOf("--tor-proxy", torProxy)
        }
        if (torOnly) {
            command += "--tor-only"
        }
        cleanExternalAddrs.forEach { addr ->
            command += listOf("--external-addr", addr)
        }
        val builder = cli.processBuilder(binary, command)
        builder.environment()["ZSILENT_NODE_PASSWORD"] = password
        // Enable the runtime mining-control RPC (/mining/start|stop) so mining can
        // be toggled in-process without restarting the node.
        builder.environment()["ZOKA_API_ENABLE_MINING_CONTROL"] = "true"
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
            torOnly = params.torOnly,
            externalAddrs = params.externalAddrs
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
        openLogFile()
        try {
            BufferedReader(InputStreamReader(child.inputStream)).useLines { lines ->
                lines.forEach { line ->
                    val clean = sanitize(line)

                    // Everything goes to the log file, including the churn the
                    // panel drops. The in-app panel keeps only the last few
                    // hundred lines and scrolls far too fast to read while
                    // mining, which made real diagnosis impossible -- the file
                    // is the copy you can actually grep after the fact.
                    // Sanitized, not raw: this lands on disk and may get
                    // shared when asking for help.
                    if (clean.isNotBlank()) writeLogLine(clean)

                    // Drop repetitive network churn (failed seed dials,
                    // transport negotiation, warmup retries) BEFORE
                    // sanitizing. In a hybrid Tor+TCP node these fire many
                    // times a second while peers are still being found and
                    // flooded the panel; sanitize() also rewrites anything
                    // with "seed" to a redaction marker, so filtering after it
                    // can't tell noise apart.
                    if (isNetworkChurn(line)) return@forEach
                    if (clean.isNotBlank() && !shouldDropLine(clean)) {
                        _logs.tryEmit(RuntimeLog("node", clean))
                    }
                }
            }
        } finally {
            closeLogFile()
        }
    }

    @Synchronized
    private fun openLogFile() {
        closeLogFile()
        try {
            logFile.parentFile?.mkdirs()
            rotateLogIfOversized()
            logWriter = BufferedWriter(FileWriter(logFile, true))
            writeLogLine("--- node session started ---")
            // Surface the path in the panel too, so it's discoverable without
            // knowing where the data directory lives.
            _logs.tryEmit(RuntimeLog("node", "Full node log: ${logFile.absolutePath}"))
        } catch (_: Exception) {
            // Logging to disk is a diagnostic aid, never a reason to fail to
            // run the node.
            logWriter = null
        }
    }

    @Synchronized
    private fun writeLogLine(line: String) {
        val writer = logWriter ?: return
        try {
            writer.write("${LOG_TIMESTAMP.format(java.time.Instant.now())} $line")
            writer.write("\n")
            // Flushed per line on purpose: the lines worth having are the ones
            // written just before a crash or a hang.
            writer.flush()
        } catch (_: Exception) {
            logWriter = null
        }
    }

    @Synchronized
    private fun closeLogFile() {
        try {
            logWriter?.flush()
            logWriter?.close()
        } catch (_: Exception) {
            // nothing useful to do while tearing down
        }
        logWriter = null
    }

    /// Keep at most one previous log around, so a long-running node can't
    /// quietly fill the disk.
    private fun rotateLogIfOversized() {
        try {
            if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
                val previous = File(logFile.parentFile, "${logFile.name}.1")
                if (previous.exists()) previous.delete()
                logFile.renameTo(previous)
            }
        } catch (_: Exception) {
            // A rotation failure must not stop logging.
        }
    }

    private fun sanitize(line: String): String {
        val noAnsi = line.replace(Regex("\\[[;?0-9]*[ -/]*[@-~]"), "")
        val sensitive = listOf(
            "seed phrase", "recovery phrase", "mnemonic",
            "private key", "spend key", "scan key", "spending key",
            "password", "secret"
        )
        return if (sensitive.any { noAnsi.contains(it, ignoreCase = true) }) {
            "[redacted sensitive log line]"
        } else {
            noAnsi.trim()
        }
    }

    private fun isNetworkChurn(rawLine: String): Boolean {
        val stripped = rawLine.replace(Regex("\\[[;?0-9]*[ -/]*[@-~]"), "")
        return NETWORK_CHURN_MARKERS.any { stripped.contains(it, ignoreCase = true) }
    }

    private fun shouldDropLine(line: String): Boolean {
        if (line.isBlank()) return true
        if (line.all { it == '-' || it == '=' || it == ' ' || it == '|' }) return true
        return DASHBOARD_NOISE.any { line.startsWith(it) }
    }

    companion object {
        /// Rotate the node log past this size, keeping one previous file, so
        /// a node left running for weeks can't fill the disk.
        private const val MAX_LOG_BYTES = 8L * 1024 * 1024

        private val LOG_TIMESTAMP: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault())

        // Repetitive, non-actionable p2p connection churn. Dropped from the
        // panel entirely (the fluid mining console / dashboard summary line
        // still shows the aggregate peer count and last event).
        private val NETWORK_CHURN_MARKERS = listOf(
            "seed reconnect dial failed",
            "seed reconnect warmup failed",
            "seed dial started",
            "Tor mode only allows",
            "Unsupported resolved address",
            "failed to add Kademlia peer",
            "connection closed while pumping",
            "keep-alive timeout",
            "tried to dial local peer"
        )

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
        // "fullnode --help" is not recognized by this CLI's per-subcommand
        // handler -- it silently ignores --help and actually tries to launch
        // a fullnode (failing on the missing password) without ever printing
        // flag names, which made this probe always return false regardless
        // of the binary's real capability. The bare top-level --help banner
        // does list every subcommand's flags (including --tor-proxy /
        // --tor-only / --external-addr) and exits immediately, so use that.
        return helpContainsTorFlags(binary, listOf("--help"))
    }

    private fun supportsExternalAddrFlag(binary: File): Boolean {
        return runCatching {
            cli.withZokaCommandLock {
                val child = cli.processBuilder(binary, listOf("--help"))
                    .redirectErrorStream(true)
                    .start()
                val output = child.inputStream.bufferedReader().readText()
                child.waitFor()
                output.contains("--external-addr")
            }
        }.getOrDefault(false)
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
