package com.zsilent.core.core.node

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.core.process.ZokaProcessKiller
import com.zsilent.core.models.LogLevel
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

private val NODE_ANSI_CSI = Regex("\u001B\\[[;?0-9]*[ -/]*[@-~]")
private val NODE_ANSI_OSC = Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)")
private val EXPLICIT_DIAGNOSTIC_LEVEL = Regex(
    """(?:^|[\s\[])(?:warn(?:ing)?|error|err)(?=[\s:\]])""",
    RegexOption.IGNORE_CASE
)
private val NODE_NETWORK_CHURN_MARKERS = listOf(
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

/**
 * Labels emitted by `zoka fullnode --dashboard` on every refresh.
 *
 * These are presentation rows, not discrete runtime events. Matching remains
 * anchored to the beginning of a normalized line so an operational message
 * that merely mentions (for example) "connected peers" is not hidden.
 */
private val DASHBOARD_NOISE = listOf(
    "ZOKA Fullnode",
    "Session uptime",
    "Last event",
    "Local label",
    "Address",
    "Available",
    "Locked",
    "Total",
    "Transactions",
    "View-only",
    "Peer ID",
    "Listen address",
    "Seed peer",
    "Transport",
    "Connected peers",
    "Incompatible peers",
    "PBFT peers",
    "Pending private txs",
    "Recent avg block txs",
    "Next block reward",
    "Next halving",
    "Emitted supply",
    "Remaining supply",
    "Auto-mining",
    "Algorithm",
    "CPU power",
    "Mining height",
    "Synced height",
    "Warmup remaining",
    "Mode",
    "Reward target",
    "Coinbase maturity",
    "Session rewards",
    "Session mined blocks",
    "Hashrate estimate",
    "Difficulty",
    "Solve time avg",
    "Press Ctrl+C to stop."
)

private val DASHBOARD_SECTIONS = setOf("Mining", "Chain", "Network", "Wallet")
private val OPERATIONAL_ROW_SUFFIX = Regex(
    """^(?:changed|connected|disconnected|failed|recovered|accepted|rejected|started|stopped|switched|completed|advertised|degraded|restored|updated|rotated|unavailable|ready)\b""",
    RegexOption.IGNORE_CASE
)

private fun stripNodeTerminalCodes(line: String): String =
    line.replace(NODE_ANSI_OSC, "").replace(NODE_ANSI_CSI, "")

/** Visible-stream policy kept internal so its narrow matching can be tested. */
internal fun shouldDropNodePanelLine(line: String): Boolean {
    if (line.isBlank()) return true

    val withoutTerminalCodes = stripNodeTerminalCodes(line).trim()
    if (withoutTerminalCodes.isBlank()) return true
    if (EXPLICIT_DIAGNOSTIC_LEVEL.containsMatchIn(withoutTerminalCodes)) return false
    if (withoutTerminalCodes.matches(Regex("^[\\s|=\\-─━]+$"))) return true

    val normalized = withoutTerminalCodes
        .trimStart { it.isWhitespace() || it == '>' || it == '›' || it == '»' }
        .trim()

    if (DASHBOARD_SECTIONS.any { normalized.equals(it, ignoreCase = true) }) return true
    return DASHBOARD_NOISE.any { label ->
        if (normalized.equals(label, ignoreCase = true)) return@any true
        if (!normalized.startsWith("$label ", ignoreCase = true)) return@any false

        // A real event can legitimately start with a dashboard label (for
        // example "Connected peers changed from 1 to 2"). Preserve those;
        // `Last event` itself is the dashboard's continuously rewritten
        // summary and remains intentionally compacted.
        val suffix = normalized.substring(label.length).trimStart()
        label.equals("Last event", ignoreCase = true) ||
            !OPERATIONAL_ROW_SUFFIX.containsMatchIn(suffix)
    }
}

/** Compact only unlevelled connection churn; explicit WARN/ERROR stays visible. */
internal fun shouldDropNodeChurnLine(rawLine: String): Boolean {
    val stripped = stripNodeTerminalCodes(rawLine)
    if (EXPLICIT_DIAGNOSTIC_LEVEL.containsMatchIn(stripped)) return false
    return NODE_NETWORK_CHURN_MARKERS.any { stripped.contains(it, ignoreCase = true) }
}

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
    private val diskLog = RollingNodeLog(logFile, MAX_LOG_BYTES)

    private data class StartParams(
        val password: String,
        val address: String,
        val network: String,
        val power: String,
        val autoMine: Boolean,
        val threads: Int?,
        val torProxy: String,
        val torOnly: Boolean,
        val externalAddrs: List<String>,
        /**
         * Direcciones onion que el usuario agregó a mano.
         *
         * Viajan como `--seed` repetido, que es el mismo camino de las semillas
         * del manifiesto: el nodo las guarda en su peerstore y reintenta contra
         * ellas mientras no llegue al objetivo de vecinos. No hace falta nada
         * nuevo del lado del nodo.
         */
        val manualPeers: List<String>,
        /**
         * El modo con el que arranco este proceso.
         *
         * Viaja en los parametros porque el reinicio automatico vuelve a
         * llamar a `start` con ellos: sin esto, un nodo levantado antes del
         * login se reiniciaria pidiendo una contrasena que todavia no existe,
         * y se quedaria abajo hasta que el usuario iniciara sesion.
         */
        val syncOnly: Boolean
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
        externalAddrs: List<String> = emptyList(),
        manualPeers: List<String> = emptyList(),
        /**
         * Bajar bloques sin abrir ninguna wallet.
         *
         * Permite empezar a sincronizar apenas se abre ZSilent, mientras el
         * usuario todavia esta en el login. El nodo no desbloquea, no crea y
         * no escribe ninguna wallet: solo descarga y valida bloques. Cuando se
         * inicia sesion, ZSilent reinicia el nodo en modo completo.
         */
        syncOnly: Boolean = false
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
        // En modo solo-sincronizar no hay wallet que abrir, asi que tampoco
        // hay contrasena que pedir: exigirla aca seria volver a atar el
        // arranque al login, que es justo lo que este modo evita.
        if (!syncOnly && password.isBlank()) {
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
        lastStartParams = StartParams(
            password,
            address,
            network,
            power,
            autoMine,
            threads,
            torProxy,
            torOnly,
            cleanExternalAddrs,
            manualPeers,
            syncOnly
        )
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
        // Los pares manuales van después de la semilla del manifiesto y antes
        // de cualquier otra cosa: el nodo los trata igual que a una semilla,
        // así que quedan en el peerstore y sobreviven a los reinicios.
        for (peer in manualPeers) {
            val limpio = peer.trim()
            if (limpio.isNotEmpty()) {
                command += listOf("--seed", limpio)
            }
        }
        // Always launch the node in node-only mode (--no-mine). Mining is toggled
        // at RUNTIME via the node's /mining RPC, so the node stays online + synced
        // while mining is turned on/off without a restart. The miner params are
        // still passed so the runtime-enabled miner uses them.
        // La contrasena solo viaja cuando hay una wallet que abrir. En modo
        // solo-sincronizar no se pasa: el nodo no la necesita y asi no hay
        // ninguna credencial en juego mientras el usuario no inicio sesion.
        if (!syncOnly) {
            command += listOf("--password-env", "ZSILENT_NODE_PASSWORD")
        } else {
            command += "--sync-only"
        }
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
        // Sin wallet que abrir no se exporta ninguna credencial al proceso
        // hijo. Esto es lo que hace que el arranque previo al login no tenga
        // secretos en juego, ni siquiera en el entorno del proceso.
        if (!syncOnly) {
            builder.environment()["ZSILENT_NODE_PASSWORD"] = password
        }
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
                _logs.tryEmit(
                    RuntimeLog(
                        "node",
                        "Node stopped with exit code $exitCode",
                        level = if (expectedStop || exitCode == 0) LogLevel.Info else LogLevel.Warning
                    )
                )
                if (expectedStop) {
                    _status.value = ServiceStatus.Offline
                    restartAttempt = 0
                } else {
                    handleUnexpectedExit(exitCode)
                }
            }.onFailure {
                process = null
                stopRequested = false
                _logs.tryEmit(
                    RuntimeLog(
                        "node",
                        "Failed to start node: ${sanitize(it.message ?: "unknown error")}",
                        level = LogLevel.Error
                    )
                )
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
            _logs.tryEmit(
                RuntimeLog(
                    "node",
                    "Node exited unexpectedly $restartAttempt times in a row; giving up auto-restart. Toggle the node again from the panel.",
                    level = LogLevel.Error
                )
            )
            _status.value = ServiceStatus.Error
            restartAttempt = 0
            return
        }
        val backoffSeconds = (2 * restartAttempt).coerceAtMost(30).toLong()
        _logs.tryEmit(
            RuntimeLog(
                "node",
                "Node exited unexpectedly (code=$exitCode). Auto-restart attempt $restartAttempt in ${backoffSeconds}s...",
                level = LogLevel.Warning
            )
        )
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
            externalAddrs = params.externalAddrs,
            manualPeers = params.manualPeers,
            syncOnly = params.syncOnly
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
            diskLog.open()
            writeLogLine("--- node session started ---")
            // Surface the path in the panel too, so it's discoverable without
            // knowing where the data directory lives.
            _logs.tryEmit(RuntimeLog("node", "Full node log: ${logFile.absolutePath}"))
        } catch (_: Exception) {
            // Logging to disk is a diagnostic aid, never a reason to fail to
            // run the node.
            diskLog.close()
        }
    }

    @Synchronized
    private fun writeLogLine(line: String) {
        diskLog.append(line)
    }

    @Synchronized
    private fun closeLogFile() {
        diskLog.close()
    }

    private fun sanitize(line: String): String {
        val noAnsi = stripNodeTerminalCodes(line)
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

    private fun isNetworkChurn(rawLine: String): Boolean = shouldDropNodeChurnLine(rawLine)

    private fun shouldDropLine(line: String): Boolean = shouldDropNodePanelLine(line)

    companion object {
        /// Rotate the node log past this size, keeping one previous file, so
        /// a node left running for weeks can't fill the disk.
        private const val MAX_LOG_BYTES = 8L * 1024 * 1024

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
