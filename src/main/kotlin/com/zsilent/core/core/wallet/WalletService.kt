package com.zsilent.core.core.wallet

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.FeeEstimate
import com.zsilent.core.models.LocalWallet
import com.zsilent.core.models.SendResult
import com.zsilent.core.models.WalletBalance
import com.zsilent.core.models.WalletHistory
import com.zsilent.core.models.WalletHistoryEntry
import com.zsilent.core.models.WalletListResponse
import com.zsilent.core.models.WalletReceiveInfo
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    val spendPublicKeyHex: String = "",
    /** Zoka Quantic: sees the wallet's payments and balance, cannot spend. */
    @SerialName("view_key")
    val viewKey: String = ""
)

/** Palabras de la frase de Zoka Quantic: 256 bits, 128 frente a Grover. */
const val QUANTIC_SEED_WORDS = 24

/**
 * Las wallets de Zoka Quantic v2, por `zoka quantic-pq-wallet`, que tiene las
 * acciones y las respuestas de la `quantic-wallet` de la v1. Las firmas son las
 * de antes para que el resto de la app no cambie: la wallet se elige por su
 * dirección (`--address`), la contraseña viaja por variable de entorno y la
 * frase nunca como argumento. Saldo, historial y envíos leen una copia de la
 * cadena que cada llamada pone al día con el nodo local (`--peer`).
 */
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
        val reply: QwList = runJson(quanticArgs("list", network), password)
        WalletListResponse(
            network = reply.network.ifBlank { network },
            privateWallets = reply.details.map { wallet ->
                LocalWallet(
                    name = wallet.name,
                    address = wallet.address,
                    shortAddress = wallet.shortAddress,
                    encrypted = true,
                    unlocked = wallet.unlocked
                )
            }
        )
    }

    /**
     * Publica la dirección completa de `wallet` en el directorio del nodo local,
     * para que le puedan pagar con la corta. La dirección es pública: no pide
     * contraseña. El sello de trabajo lo busca `quantic-wallet` (unos segundos).
     * Devuelve la dirección corta.
     */
    suspend fun publishAddress(
        wallet: String,
        network: String = ZokaNetworkConfig.Network
    ): String = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        val reply: QwPublished = runJson(quanticArgs("publish-address", network) + select(wallet), null)
        reply.shortAddress
    }

    suspend fun balance(
        wallet: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletBalance = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        val reply: QwBalance = runJson(quanticArgs("balance", network) + select(wallet), password)
        val total = reply.spendableAtoms + reply.immatureAtoms
        WalletBalance(
            wallet = reply.name,
            address = wallet.takeIf(ZokaNetworkConfig::isAddress).orEmpty(),
            balanceAtoms = reply.spendableAtoms,
            balance = formatZka(reply.spendableAtoms),
            lockedBalanceAtoms = reply.immatureAtoms,
            lockedBalance = formatZka(reply.immatureAtoms),
            totalBalanceAtoms = total,
            totalBalance = formatZka(total),
            maxSendableAtoms = reply.maxSendableAtoms
        )
    }

    /**
     * En Quantic la dirección completa es una sola (unos 1.760 caracteres en
     * la v2): no hay una segunda dirección híbrida que activar.
     */
    suspend fun hybridReceive(
        wallet: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletReceiveInfo = withContext(Dispatchers.IO) {
        password.fill('\u0000')
        require(wallet.isNotBlank()) { "wallet is required" }
        WalletReceiveInfo(wallet = wallet, address = wallet)
    }

    suspend fun history(
        wallet: String,
        password: CharArray,
        limit: Int = 12,
        network: String = ZokaNetworkConfig.Network
    ): WalletHistory = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        val reply: QwHistory = runJson(
            quanticArgs("history", network) + select(wallet) + listOf("--limit", limit.coerceIn(1, 100).toString()),
            password
        )
        WalletHistory(
            wallet = reply.name,
            count = reply.count,
            transactions = reply.transactions.map { tx ->
                WalletHistoryEntry(
                    txHash = tx.txid,
                    status = "confirmed",
                    timestamp = tx.timestamp ?: 0,
                    amountHidden = false,
                    feeHidden = false,
                    height = tx.height,
                    kind = tx.kind,
                    amountAtoms = tx.amountAtoms,
                    confirmations = tx.confirmations
                )
            }
        )
    }

    /**
     * La comisión de Quantic es fija por acción (0,0001 ZKA, mínimo 2): no hay
     * prioridad que elegir. Lo que cambia con el envío es cuántas notas gasta,
     * y eso lo sabe la wallet: por eso hace falta abrirla.
     */
    suspend fun estimateFee(
        wallet: String,
        password: CharArray,
        amount: String,
        network: String = ZokaNetworkConfig.Network
    ): FeeEstimate = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        require(amount.isNotBlank()) { "amount is required" }
        val reply: QwFee = runJson(
            quanticArgs("estimate-fee", network) + select(wallet) + listOf("--amount", amount.trim()),
            password
        )
        FeeEstimate(
            amount = amount.trim(),
            priority = "fixed",
            fee = "${formatZka(reply.feeAtoms ?: MIN_FEE_ATOMS)} ZKA",
            maxSendableAtoms = reply.maxSendableAtoms,
            problem = reply.reason.takeIf { !reply.enough }
        )
    }

    suspend fun sendPrivate(
        from: String,
        to: String,
        amount: String,
        fee: String,
        password: CharArray,
        memo: String = "",
        network: String = ZokaNetworkConfig.Network
    ): SendResult = withContext(Dispatchers.IO) {
        require(from.isNotBlank()) { "sender address is required" }
        require(to.isNotBlank()) { "recipient address is required" }
        require(amount.isNotBlank()) { "amount is required" }
        // `fee` se ignora: la comisión la fija el protocolo y la calcula la wallet.
        val args = quanticArgs("send", network) + select(from) +
            listOf("--to", to.trim(), "--amount", amount.trim()) +
            (memo.takeIf { it.isNotBlank() }?.let { listOf("--memo", it) } ?: emptyList())
        val reply: QwSend = runJson(args, password)
        SendResult(hash = reply.txid, from = from, to = to, queuedDb = null)
    }

    suspend fun createWallet(
        name: String,
        password: CharArray,
        seedWords: Int,
        network: String = ZokaNetworkConfig.Network
    ): WalletCreationResult = withContext(Dispatchers.IO) {
        require(name.isNotBlank()) { "wallet name is required" }
        val reply: QwCreated = runJson(quanticArgs("new", network) + listOf("--name", name), password)
        WalletCreationResult(name = reply.name, address = reply.address, recoveryPhrase = reply.recoveryPhrase)
    }

    /** Siempre 24 palabras en Quantic, se pida lo que se pida. */
    suspend fun generateRecoveryPhrase(
        seedWords: Int,
        network: String = ZokaNetworkConfig.Network
    ): RecoveryPhraseResult = withContext(Dispatchers.IO) {
        val reply: QwPhrase = runJson(quanticArgs("generate-phrase", network), null)
        RecoveryPhraseResult(words = reply.words, phrase = reply.recoveryPhrase)
    }

    suspend fun importFromSeed(
        name: String,
        seed: CharArray,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletCreationResult = withContext(Dispatchers.IO) {
        try {
            require(name.isNotBlank()) { "wallet name is required" }
            require(seed.isNotEmpty()) { "seed is required" }
            val reply: QwCreated = runJson(
                quanticArgs("restore", network) + listOf("--name", name, "--phrase-env", PHRASE_ENV),
                password,
                extraEnv = mapOf(PHRASE_ENV to String(seed))
            )
            WalletCreationResult(name = reply.name, address = reply.address, recoveryPhrase = null)
        } finally {
            seed.fill('\u0000')
        }
    }

    /** Copia la wallet tal como está guardada (cifrada con su contraseña). */
    suspend fun exportEncryptedBackup(
        wallet: String,
        outPath: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletBackupResult = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        require(outPath.isNotBlank()) { "backup path is required" }
        val normalizedPath = Path.of(outPath).toAbsolutePath().normalize().toString()
        val reply: QwBackup = runJson(quanticArgs("backup", network) + select(wallet) + listOf("--out", normalizedPath), password)
        WalletBackupResult(name = reply.name, address = wallet, path = reply.backup.ifBlank { normalizedPath })
    }

    suspend fun restoreEncryptedBackup(
        inPath: String,
        password: CharArray,
        importName: String? = null,
        network: String = ZokaNetworkConfig.Network
    ): WalletImportResult = withContext(Dispatchers.IO) {
        require(inPath.isNotBlank()) { "backup path is required" }
        val normalizedPath = Path.of(inPath).toAbsolutePath().normalize().toString()
        val name = importName?.trim()?.takeIf { it.isNotBlank() } ?: "restored"
        val reply: QwCreated = runJson(
            quanticArgs("import-backup", network) + listOf("--name", name, "--file", normalizedPath),
            password
        )
        WalletImportResult(name = reply.name, address = reply.address, path = normalizedPath)
    }

    /**
     * Lo único secreto que hay que respaldar en Quantic es la frase: las claves
     * salen de ella. Los campos de Zenith (claves de escaneo y gasto) van vacíos.
     */
    suspend fun exportSecrets(
        wallet: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletSecretsResult = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        val reply: QwShownPhrase = runJson(quanticArgs("show-phrase", network) + select(wallet), password)
        WalletSecretsResult(
            walletName = reply.name,
            walletType = "zoka-quantic",
            address = wallet,
            userAddress = wallet,
            mnemonic = reply.recoveryPhrase
        )
    }

    /**
     * La clave de vista (`zqtestview2…`): ve cada pago y el saldo de la wallet,
     * en ZSilent o en zokascan.cc/wallet, y no puede gastar. Es lo que en
     * Zenith era la clave de escaneo; en Quantic no hay clave de gasto aparte:
     * gastar pide la frase.
     */
    suspend fun exportViewKey(
        wallet: String,
        password: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): String = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        val reply: QwShownViewKey = runJson(quanticArgs("show-view-key", network) + select(wallet), password)
        require(reply.viewKey.isNotBlank()) { "this node does not export view keys; update ZSilent" }
        reply.viewKey
    }

    suspend fun changePassphrase(
        wallet: String,
        oldPassword: CharArray,
        newPassword: CharArray,
        network: String = ZokaNetworkConfig.Network
    ): WalletPassphraseChangeResult = withContext(Dispatchers.IO) {
        require(wallet.isNotBlank()) { "wallet is required" }
        try {
            val reply: QwChanged = runJson(
                quanticArgs("change-password", network) + select(wallet) + listOf("--new-password-env", NEW_PASSWORD_ENV),
                oldPassword,
                extraEnv = mapOf(NEW_PASSWORD_ENV to String(newPassword))
            )
            WalletPassphraseChangeResult(name = reply.name, address = wallet)
        } finally {
            newPassword.fill('\u0000')
        }
    }

    private fun quanticArgs(action: String, network: String): List<String> = buildList {
        val chosen = network.ifBlank { ZokaNetworkConfig.Network }
        add("quantic-pq-wallet")
        add(action)
        add("--network")
        add(chosen)
        add("--dir")
        add(cli.networkDir(chosen).absolutePath)
        add("--api")
        add(ZokaNetworkConfig.LocalRpcUrl)
        add("--peer")
        add(cli.nodeDescriptor(chosen).absolutePath)
        add("--json")
    }

    /** La wallet por su dirección; si no parece una, por su nombre. */
    private fun select(wallet: String): List<String> {
        val trimmed = wallet.trim()
        return if (ZokaNetworkConfig.isAddress(trimmed)) listOf("--address", trimmed) else listOf("--name", trimmed)
    }

    private inline fun <reified T> runJson(
        args: List<String>,
        password: CharArray?,
        extraEnv: Map<String, String> = emptyMap()
    ): T {
        val output = runCommand(args, password, extraEnv)
        // La respuesta es la última línea con JSON: el nodo no escribe nada más
        // en estos comandos, pero así un aviso suelto no rompe el desbloqueo.
        val line = output.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("{") } ?: output.trim()
        return json.decodeFromString(line)
    }

    private fun runCommand(args: List<String>, password: CharArray?, extraEnv: Map<String, String>): String {
        val zoka = requireNotNull(cli.zoka) { "zoka binary not found" }
        val command = if (password != null) args + listOf("--password-env", PASSWORD_ENV) else args
        try {
            return cli.withZokaCommandLock {
                cli.processBuilder(zoka, command)
                    .redirectErrorStream(true)
                    .apply {
                        if (password != null) environment()[PASSWORD_ENV] = String(password)
                        extraEnv.forEach { (key, value) -> environment()[key] = value }
                    }
                    .start()
                    .readTextAndWait()
            }
        } finally {
            password?.fill('\u0000')
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

    private fun commandFailureMessage(output: String, exit: Int): String {
        val clean = output.trim()
        if (clean.isNotBlank()) return clean
        return if (exit == 1073741819 || exit == -1073741819) {
            "zoka command crashed while reading wallet data. ZSilent will keep wallet access local; restart the app and retry. Exit code $exit."
        } else {
            "zoka command failed with exit code $exit"
        }
    }

    private companion object {
        const val PASSWORD_ENV = "ZSILENT_WALLET_PASSWORD"
        const val NEW_PASSWORD_ENV = "ZSILENT_NEW_WALLET_PASSWORD"
        const val PHRASE_ENV = "ZSILENT_RECOVERY_PHRASE"
        /** 2 acciones a 0,0001 ZKA: lo mínimo que paga un envío. */
        const val MIN_FEE_ATOMS = 200L
    }
}

private const val ATOMS_PER_ZKA = 1_000_000L

/** "12", "12.5", "0.0002": como el nodo, sin ceros de más. */
internal fun formatZka(atoms: Long): String {
    val whole = atoms / ATOMS_PER_ZKA
    val fraction = atoms % ATOMS_PER_ZKA
    if (fraction == 0L) return whole.toString()
    return "$whole." + fraction.toString().padStart(6, '0').trimEnd('0')
}

@Serializable
private data class QwListEntry(
    val name: String = "",
    val address: String = "",
    @SerialName("short_address") val shortAddress: String = "",
    val unlocked: Boolean = false
)

@Serializable
private data class QwPublished(@SerialName("short_address") val shortAddress: String = "")

@Serializable
private data class QwList(val network: String = "", val details: List<QwListEntry> = emptyList())

@Serializable
private data class QwBalance(
    val name: String = "",
    val height: Long = 0,
    @SerialName("spendable_atoms") val spendableAtoms: Long = 0,
    @SerialName("immature_atoms") val immatureAtoms: Long = 0,
    @SerialName("max_sendable_atoms") val maxSendableAtoms: Long = 0,
    @SerialName("node_behind") val nodeBehind: Boolean = false
)

@Serializable
private data class QwTx(
    val txid: String = "",
    val height: Long = 0,
    val kind: String = "",
    @SerialName("amount_atoms") val amountAtoms: Long = 0,
    val timestamp: Long? = null,
    val confirmations: Long = 0
)

@Serializable
private data class QwHistory(val name: String = "", val count: Int = 0, val transactions: List<QwTx> = emptyList())

@Serializable
private data class QwFee(
    val enough: Boolean = false,
    @SerialName("fee_atoms") val feeAtoms: Long? = null,
    @SerialName("max_sendable_atoms") val maxSendableAtoms: Long = 0,
    val reason: String? = null
)

@Serializable
private data class QwSend(val txid: String = "", @SerialName("fee_atoms") val feeAtoms: Long? = null)

@Serializable
private data class QwCreated(
    val name: String = "",
    val address: String = "",
    @SerialName("recovery_phrase") val recoveryPhrase: String? = null
)

@Serializable
private data class QwPhrase(val words: Int = QUANTIC_SEED_WORDS, @SerialName("recovery_phrase") val recoveryPhrase: String = "")

@Serializable
private data class QwShownPhrase(val name: String = "", @SerialName("recovery_phrase") val recoveryPhrase: String = "")

@Serializable
private data class QwShownViewKey(val name: String = "", @SerialName("view_key") val viewKey: String = "")

@Serializable
private data class QwBackup(val name: String = "", val backup: String = "")

@Serializable
private data class QwChanged(val name: String = "", val changed: Boolean = false)
