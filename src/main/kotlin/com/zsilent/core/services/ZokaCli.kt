package com.zsilent.core.services

import com.zsilent.core.config.ZokaNetworkConfig
import java.io.File
import java.util.concurrent.locks.ReentrantLock

data class ZokaCli(
    val zoka: File?,
    val zokahd: File?,
    val projectRoot: File,
    val resourcesRoot: File?,
    val trustedSetupDir: File?,
    val dataRoot: File
) {
    private val expectedTrustedSetupDigest = "219fdb5d5e47e115ca899d13a6de284df03c86eaaef05eb4ff4adb395cfb0d8e"
    private val commandMutex = ReentrantLock()

    val ready: Boolean = zoka?.exists() == true && zokahd?.exists() == true
    val workDir: File
        get() = dataRoot.apply { mkdirs() }

    /** La carpeta de la red v2: nodo, wallets y la copia de la cadena. */
    fun networkDir(network: String = ZokaNetworkConfig.Network): File =
        File(dataRoot, ZokaNetworkConfig.dataDir(network))

    /**
     * El descriptor del nodo local (`--descriptor-out`): por él la wallet
     * sincroniza su copia con una conexión directa, sin salir de la PC.
     */
    fun nodeDescriptor(network: String = ZokaNetworkConfig.Network): File =
        File(networkDir(network), "node/peer.json")

    fun <T> withZokaCommandLock(action: () -> T): T {
        // JVM-level mutex. A NIO FileChannel.lock() is exclusive *per JVM*, so a
        // second caller from another coroutine inside this JVM would throw
        // OverlappingFileLockException. ReentrantLock serialises callers
        // correctly and lets the same thread re-enter (needed when wallet
        // unlock chains into mine-status polling).
        commandMutex.lock()
        try {
            return action()
        } finally {
            commandMutex.unlock()
        }
    }

    fun processBuilder(binary: File, args: List<String>): ProcessBuilder {
        return ProcessBuilder(listOf(binary.absolutePath) + args)
            .directory(workDir)
            .apply {
                sanitizePathForNativeBinary(environment())
                applyRuntimeEnvironment(environment())
            }
    }

    private fun sanitizePathForNativeBinary(environment: MutableMap<String, String>) {
        // jpackage bundles its own MSVCP140/VCRUNTIME140 inside runtime/bin and
        // prepends that directory to PATH. A native subprocess (zoka.exe) that
        // links against the system C++ runtime will then load a mismatched DLL
        // and crash with c0000005. Strip the JRE bin entries so the loader
        // finds the correct system DLLs in System32 instead.
        val pathKey = environment.keys.firstOrNull { it.equals("Path", ignoreCase = true) } ?: "Path"
        val current = environment[pathKey] ?: return
        val javaHome = System.getProperty("java.home")?.let { File(it).absolutePath }
        val filtered = current.split(File.pathSeparator)
            .filter { entry ->
                val trimmed = entry.trim()
                if (trimmed.isEmpty()) return@filter false
                val lower = trimmed.lowercase()
                if (lower.contains("\\runtime\\bin")) return@filter false
                if (lower.contains("\\zsilent core\\runtime\\bin")) return@filter false
                if (javaHome != null && trimmed.startsWith(javaHome, ignoreCase = true)) return@filter false
                true
            }
            .joinToString(File.pathSeparator)
        environment[pathKey] = filtered
    }

    fun applyRuntimeEnvironment(environment: MutableMap<String, String>) {
        environment["ZOKA_MINER_LAUNCHER"] = "zsilent"
        environment["ZOKA_MINER_LAUNCHER_VERSION"] = ZokaNetworkConfig.AppVersion
        environment["ZSILENT_APP_VERSION"] = ZokaNetworkConfig.AppVersion
        val manifest = ZokaNetworkConfig.ManifestPath.takeIf { it.isNotBlank() }
        if (manifest != null) {
            environment["ZOKA_NETWORK_CONFIG"] = manifest
            environment["ZOKA_MAINNET_NETWORK_CONFIG"] = manifest
        }
        ZokaNetworkConfig.LocalRpcUrl.takeIf { it.isNotBlank() }?.let { localRpc ->
            environment["ZOKA_LOCAL_RPC_URL"] = localRpc
            environment["ZOKA_NODE_ENDPOINT"] = localRpc
            environment["ZOKA_CLI_NODE_ENDPOINT"] = localRpc
        }
        ZokaNetworkConfig.RemoteRpcUrl.takeIf { it.isNotBlank() }?.let { remoteRpc ->
            environment["ZOKA_REMOTE_RPC_URL"] = remoteRpc
        }
        trustedSetupDir
            ?.takeIf { it.exists() && it.isDirectory }
            ?.let { setupDir ->
                environment["ZOKA_TRUSTED_SETUP_DIR"] = setupDir.absolutePath
                environment["ZOKA_EXPECTED_SETUP_DIGEST"] = expectedTrustedSetupDigest
            }
    }

    companion object {
        fun discover(start: File = File(System.getProperty("user.dir"))): ZokaCli {
            val repoRoot = generateSequence(start.absoluteFile) { it.parentFile }
                .firstOrNull { File(it, "Cargo.toml").exists() }
            val resourcesRoot = System.getProperty("compose.application.resources.dir")
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.exists() && it.isDirectory }
            val dataRoot = defaultDataRoot().apply { mkdirs() }
            val projectRoot = repoRoot ?: dataRoot
            val trustedSetupDir = trustedSetupCandidates(resourcesRoot, repoRoot, dataRoot)
                .firstOrNull { it.exists() && it.isDirectory && File(it, "private_tx.params").isFile }

            val zoka = binaryCandidates(
                envName = "ZSILENT_ZOKA_BIN",
                propertyName = "zsilent.zokaBin",
                binaryNames = listOf("zoka.exe", "zoka"),
                resourcesRoot = resourcesRoot,
                repoRoot = repoRoot,
                dataRoot = dataRoot
            ).firstOrNull { it.exists() && it.isFile }

            val zokahd = binaryCandidates(
                envName = "ZSILENT_ZOKAHD_BIN",
                propertyName = "zsilent.zokahdBin",
                binaryNames = listOf("zokahd.exe", "zokahd"),
                resourcesRoot = resourcesRoot,
                repoRoot = repoRoot,
                dataRoot = dataRoot
            ).firstOrNull { it.exists() && it.isFile }

            return ZokaCli(
                zoka = zoka,
                zokahd = zokahd,
                projectRoot = projectRoot,
                resourcesRoot = resourcesRoot,
                trustedSetupDir = trustedSetupDir,
                dataRoot = dataRoot
            )
        }

        private fun trustedSetupCandidates(
            resourcesRoot: File?,
            repoRoot: File?,
            dataRoot: File
        ): List<File> {
            val configured = System.getenv("ZOKA_TRUSTED_SETUP_DIR")
                ?.takeIf { it.isNotBlank() }
                ?: System.getProperty("zsilent.trustedSetupDir")?.takeIf { it.isNotBlank() }
            return buildList {
                configured?.let { add(File(it)) }
                resourcesRoot?.let { root ->
                    add(File(root, "trusted-setup"))
                    add(File(root, "common/trusted-setup"))
                }
                add(File(dataRoot, "trusted-setup"))
                repoRoot?.let { root -> add(File(root, "trusted-setup")) }
            }
        }

        private fun binaryCandidates(
            envName: String,
            propertyName: String,
            binaryNames: List<String>,
            resourcesRoot: File?,
            repoRoot: File?,
            dataRoot: File
        ): List<File> {
            val configured = System.getenv(envName)
                ?.takeIf { it.isNotBlank() }
                ?: System.getProperty(propertyName)?.takeIf { it.isNotBlank() }
            return buildList {
                configured?.let { add(File(it)) }
                resourcesRoot?.let { root ->
                    binaryNames.forEach { name ->
                        add(File(root, "bin/$name"))
                        add(File(root, name))
                    }
                }
                binaryNames.forEach { name -> add(File(dataRoot, "bin/$name")) }
                repoRoot?.let { root ->
                    binaryNames.forEach { name ->
                        add(File(root, "target/release/$name"))
                        add(File(root, "target/debug/$name"))
                    }
                }
            }
        }

        /**
         * Shared Zoka home for CLI and ZSilent. Mirrors the resolution rules
         * in [setup_zoka_home] / [zoka_home] on the Rust side so that both
         * clients always operate on the same .zoka/<network> tree, regardless
         * of where the user launches them from.
         */
        private fun defaultDataRoot(): File {
            System.getenv("ZOKA_HOME")?.takeIf { it.isNotBlank() }?.let { return File(it) }
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            return when {
                osName.contains("win") -> {
                    val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                    if (appData != null) File(appData, "Zoka")
                    else File(System.getProperty("user.home"), ".zoka")
                }
                osName.contains("mac") -> File(
                    System.getProperty("user.home"),
                    "Library/Application Support/Zoka"
                )
                else -> {
                    val xdg = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
                    if (xdg != null) File(xdg, "zoka")
                    else File(System.getProperty("user.home"), ".local/share/zoka")
                }
            }
        }
    }
}
