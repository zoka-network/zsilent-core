package com.zsilent.core.core.wallet

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.WalletBalance
import com.zsilent.core.models.FeeEstimate
import com.zsilent.core.models.SendResult
import com.zsilent.core.models.WalletHistory
import com.zsilent.core.models.WalletListResponse
import com.zsilent.core.services.ZokaCli
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Path

data class WalletSummary(
    val name: String,
    val address: String,
    val locked: Boolean,
    val balanceLabel: String
)

data class WalletCreationResult(
    val name: String,
    val address: String,
    val recoveryPhrase: String?
)

data class WalletBackupResult(
    val name: String,
    val address: String,
    val path: String
)

data class WalletImportResult(
    val name: String,
    val address: String,
    val path: String
)

data class WalletPassphraseChangeResult(
    val name: String,
    val address: String
)

data class RecoveryPhraseResult(
    val words: Int,
    val phrase: String
)

@Serializable
data class WalletSecretsResult(
    @SerialName("wallet_name")
    val walletName: String = "",
    @SerialName("wallet_type")
    val walletType: String = "",
    val address: String = "",
    @SerialName("user_address")
    val userAddress: String = "",
    val mnemonic: String? = null,
    @SerialName("scan_private_key_hex")
    val scanPrivateKeyHex: String = "",
    @SerialName("scan_public_key_hex")
    val scanPublicKeyHex: String = "",
    @SerialName("spend_private_key_hex")
    val spendPrivateKeyHex: String = "",
    @SerialName("spend_public_key_hex")
    val spendPublicKeyHex: String = ""
)

class WalletService(private val cli: ZokaCli) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun available(): Boolean = cli.zoka?.exists() == true

    suspend fun listWallets(
        password: CharArray? = null,
        network: String = ZokaNetworkConfig.Network
    ): WalletListResponse = withContext(Dispatchers.IO) {
        runWalletJsonCommand(
            // ZSilent always runs its own embedded fullnode against the same
            // local chain DB, so a local-DB attempt here would just hit a
            // RocksDB lock conflict and fall back to RPC anyway. Going
            // straight to RPC skips that wasted round trip and gives a
            // single deterministic full scan instead of a partial one.
            args = listOf("wallet-list", "--network", network, "--json", "--rpc-only"),
            password = password
        )
    }

    suspend fun balance(
        wallet: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletBalance = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        runWalletJsonCommand(
            args = listOf("wallet-balance", "--address", wallet, "--network", network, "--json", "--rpc-only"),
            password = password
        )
    }

    suspend fun history(
        wallet: String,
        password: CharArray,
        limit: Int = 12,
        network: String = ZokaNetworkConfig.Network
    ): WalletHistory = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        runWalletJsonCommand(
            args = listOf(
                "wallet-history",
                "--address", wallet,
                "--network", network,
                "--rpc-only",
                "--limit", limit.coerceIn(1, 100).toString(),
                "--json"
            ),
            password = password
        )
    }

    suspend fun estimateFee(
        amount: String,
        priority: String = "normal",
        network: String = ZokaNetworkConfig.Network
    ): FeeEstimate = withContext(Dispatchers.IO) {
        require(amount.isNotBlank()) { "amount is required" }
        require(priority in setOf("low", "normal", "high")) { "priority must be low, normal, or high" }
        val output = runTextCommand(
            args = listOf(
                "estimate-fee",
                "--amount", amount,
                "--priority", priority,
                "--network", network
            ),
            password = null
        )
        FeeEstimate(
            amount = output.lineValue("Amount") ?: amount,
            priority = output.lineValue("Priority") ?: priority,
            fee = output.lineValue("Estimated fee") ?: "0 ZKA"
        )
    }

    suspend fun sendPrivate(
        from: String,
        to: String,
        amount: String,
        fee: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): SendResult = withContext(Dispatchers.IO) {
        require(from.isNotBlank()) { "sender address is required" }
        require(to.isNotBlank()) { "recipient address is required" }
        require(amount.isNotBlank()) { "amount is required" }
        val normalizedFee = fee.removeSuffix(" ZKA").trim().ifBlank { "0" }
        val output = runTextCommand(
            args = listOf(
                "tx-send",
                "--from", from,
                "--to", to,
                "--amount", amount,
                "--fee", normalizedFee,
                "--network", network,
                "--yes",
                "--password-env", "ZSILENT_WALLET_PASSWORD"
            ),
            password = password
        )
        val splitHashes = output.lineSequence()
            .mapNotNull { line ->
                line.substringAfter("hash=", missingDelimiterValue = "")
                    .substringBefore(" |")
                    .trim()
                    .takeIf { it.isNotBlank() }
            }
            .toList()
        val primaryHash = output.lineValue("Hash") ?: splitHashes.firstOrNull().orEmpty()
        SendResult(
            hash = primaryHash,
            transactionHashes = splitHashes.ifEmpty { listOf(primaryHash).filter { it.isNotBlank() } },
            from = output.lineValue("From") ?: from,
            to = output.lineValue("To") ?: to,
            queuedDb = output.lineValue("Queued for local node DB")
        )
    }

    suspend fun createWallet(
        name: String,
        password: CharArray,
        seedWords: Int,
        network: String = ZokaNetworkConfig.Network
    ): WalletCreationResult = withContext(Dispatchers.IO) {
        require(name.isNotBlank()) { "wallet name is required" }
        require(seedWords == 12 || seedWords == 24) { "seed words must be 12 or 24" }
        val zoka = requireNotNull(cli.zoka) { "zoka binary not found" }
        try {
            val output = cli.withZokaCommandLock {
                cli.processBuilder(
                    zoka,
                    listOf(
                        "wallet-create",
                        "--name", name,
                        "--network", network,
                        "--seed-words", seedWords.toString(),
                        "--password-env", "ZSILENT_WALLET_PASSWORD"
                    )
                )
                    .redirectErrorStream(true)
                    .apply {
                        environment()["ZSILENT_WALLET_PASSWORD"] = String(password)
                    }
                    .start()
                    .readTextAndWait()
            }

            WalletCreationResult(
                name = output.lineValue("Name") ?: name,
                address = output.lineValue("Address") ?: "",
                recoveryPhrase = output.lineValue("Recovery phrase")
            )
        } finally {
            password.fill('\u0000')
        }
    }

    suspend fun generateRecoveryPhrase(
        seedWords: Int,
        network: String = ZokaNetworkConfig.Network
    ): RecoveryPhraseResult = withContext(Dispatchers.IO) {
        require(seedWords == 12 || seedWords == 24) { "seed words must be 12 or 24" }
        val output = runTextCommand(
            args = listOf(
                "wallet-generate-seed",
                "--seed-words", seedWords.toString(),
                "--network", network
            ),
            password = null
        )
        RecoveryPhraseResult(
            words = output.lineValue("Seed words")?.toIntOrNull() ?: seedWords,
            phrase = output.lineValue("Recovery phrase") ?: error("Recovery phrase not returned by zoka")
        )
    }

    suspend fun importFromSeed(
        name: String,
        seed: CharArray,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletCreationResult = withContext(Dispatchers.IO) {
        val zoka = requireNotNull(cli.zoka) { "zoka binary not found" }
        try {
            require(name.isNotBlank()) { "wallet name is required" }
            require(seed.isNotEmpty()) { "seed is required" }
            val output = cli.withZokaCommandLock {
                cli.processBuilder(
                    zoka,
                    listOf(
                        "wallet-restore-from-seed",
                        "--name", name,
                        "--network", network,
                        "--password-env", "ZSILENT_WALLET_PASSWORD",
                        "--seed-env", "ZSILENT_RECOVERY_PHRASE"
                    )
                )
                    .redirectErrorStream(true)
                    .apply {
                        environment()["ZSILENT_WALLET_PASSWORD"] = String(password)
                        environment()["ZSILENT_RECOVERY_PHRASE"] = String(seed)
                    }
                    .start()
                    .readTextAndWait()
            }

            WalletCreationResult(
                name = output.lineValue("Name") ?: name,
                address = output.lineValue("Address") ?: "",
                recoveryPhrase = null
            )
        } finally {
            seed.fill('\u0000')
            password.fill('\u0000')
        }
    }

    suspend fun exportEncryptedBackup(
        wallet: String,
        outPath: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletBackupResult = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        require(outPath.isNotBlank()) { "backup path is required" }
        val normalizedPath = Path.of(outPath).toAbsolutePath().normalize().toString()
        val output = runTextCommand(
            args = listOf(
                "wallet-backup",
                "--address", wallet,
                "--out", normalizedPath,
                "--network", network,
                "--password-env", "ZSILENT_WALLET_PASSWORD"
            ),
            password = password
        )
        WalletBackupResult(
            name = output.lineValue("Name") ?: wallet,
            address = output.lineValue("Address") ?: wallet,
            path = output.lineValue("Backup created at") ?: normalizedPath
        )
    }

    suspend fun restoreEncryptedBackup(
        inPath: String,
        password: CharArray,
        importName: String? = null,
        network: String = ZokaNetworkConfig.Network
    ): WalletImportResult = withContext(Dispatchers.IO) {
        require(inPath.isNotBlank()) { "backup path is required" }
        val normalizedPath = Path.of(inPath).toAbsolutePath().normalize().toString()
        val command = buildList {
            add("wallet-restore-from-backup")
            add("--in")
            add(normalizedPath)
            importName?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
                add("--name")
                add(name)
            }
            add("--network")
            add(network)
            add("--password-env")
            add("ZSILENT_WALLET_PASSWORD")
        }
        val output = runTextCommand(
            args = command,
            password = password
        )
        WalletImportResult(
            name = output.lineValue("Name") ?: importName?.trim().orEmpty(),
            address = output.lineValue("Address") ?: "",
            path = normalizedPath
        )
    }

    suspend fun exportSecrets(
        wallet: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletSecretsResult = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        runWalletJsonCommand(
            args = listOf(
                "wallet-export-secrets",
                "--address", wallet,
                "--network", network,
                "--json",
                "--password-env", "ZSILENT_WALLET_PASSWORD"
            ),
            password = password
        )
    }

    suspend fun changePassphrase(
        wallet: String,
        oldPassword: CharArray,
        newPassword: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletPassphraseChangeResult = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        val zoka = requireNotNull(cli.zoka) { "zoka binary not found" }
        try {
            val output = cli.withZokaCommandLock {
                cli.processBuilder(
                    zoka,
                    listOf(
                        "wallet-change-passphrase",
                        "--address", wallet,
                        "--network", network,
                        "--old-password-env", "ZSILENT_OLD_WALLET_PASSWORD",
                        "--new-password-env", "ZSILENT_NEW_WALLET_PASSWORD"
                    )
                )
                    .redirectErrorStream(true)
                    .apply {
                        environment()["ZSILENT_OLD_WALLET_PASSWORD"] = String(oldPassword)
                        environment()["ZSILENT_NEW_WALLET_PASSWORD"] = String(newPassword)
                    }
                    .start()
                    .readTextAndWait()
            }

            WalletPassphraseChangeResult(
                name = output.lineValue("Name") ?: wallet,
                address = output.lineValue("Address") ?: wallet
            )
        } finally {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    private fun Process.readTextAndWait(): String {
        val output = inputStream.bufferedReader().readText()
        val exit = waitFor()
        if (exit != 0) {
            error(commandFailureMessage(output, exit))
        }
        return output
    }

    private fun String.lineValue(label: String): String? {
        return lineSequence()
            .firstOrNull { it.startsWith("$label:") }
            ?.substringAfter(":")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private inline fun <reified T> runWalletJsonCommand(
        args: List<String>,
        password: CharArray?
    ): T {
        val output = runTextCommand(args, password)
        return json.decodeFromString(output)
    }

    private fun runTextCommand(
        args: List<String>,
        password: CharArray?
    ): String {
        val zoka = requireNotNull(cli.zoka) { "zoka binary not found" }
        val command = if (password != null && args.none { it == "--password-env" }) {
            args + listOf("--password-env", "ZSILENT_WALLET_PASSWORD")
        } else {
            args
        }
        try {
            val output = cli.withZokaCommandLock {
                cli.processBuilder(zoka, command)
                    .redirectErrorStream(true)
                    .apply {
                        if (password != null) {
                            environment()["ZSILENT_WALLET_PASSWORD"] = String(password)
                        }
                    }
                    .start()
                    .readTextAndWait()
            }
            return output
        } finally {
            password?.fill('\u0000')
        }
    }

    private fun commandFailureMessage(output: String, exit: Int): String {
        val clean = output.trim()
        if (clean.isNotBlank()) return clean
        return if (exit == 1073741819 || exit == -1073741819) {
            "zoka command crashed while reading wallet data. ZSilent will keep wallet access local; restart the app and retry. Exit code $exit."
        } else {
            "zoka command failed with exit code $exit"
        }
    }
}
