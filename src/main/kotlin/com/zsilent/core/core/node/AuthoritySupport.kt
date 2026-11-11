package com.zsilent.core.core.node

import com.zsilent.core.services.ZokaCli
import java.io.File

/** File discovery only. Secret material is handled by the native node. */
internal object AuthoritySupport {
    fun directory(cli: ZokaCli, network: String, configured: String = System.getenv("ZSILENT_CHECKPOINT_AUTHORITY_DIR").orEmpty()): File? {
        val setting = configured.trim()
        if (setting.equals("none", ignoreCase = true)) return null
        val candidates = when {
            setting.isNotBlank() -> listOf(File(setting))
            // The v1 folder, this network's, and the v2 one: a restore there
            // keeps signing on the v3 testnet (the same keys, nothing copied).
            network == "testnet" -> listOf(
                File(cli.dataRoot, ".zoka/quantic-testnet"),
                cli.networkDir(network),
                File(cli.dataRoot, ".zoka/quantic-testnet-v2")
            )
            else -> emptyList()
        }
        // A partial restore must fail closed rather than silently stop signing.
        return candidates.firstOrNull { dir -> names.any { File(dir, it).isFile } }
    }

    fun restoreDirectory(cli: ZokaCli, network: String): File =
        directory(cli, network) ?: System.getenv("ZSILENT_CHECKPOINT_AUTHORITY_DIR")?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("none", ignoreCase = true) }?.let(::File)
            ?: cli.networkDir(network)

    fun protectionFlags(): List<String> =
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) listOf("--authority-protection", "windows-user")
        else emptyList()

    private val names = listOf("checkpoint-authority.key", "checkpoint-authority-mldsa.key")
}
