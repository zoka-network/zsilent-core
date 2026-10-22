package com.zsilent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.zsilent.core.core.mining.MiningController
import com.zsilent.core.core.node.NodeController
import com.zsilent.core.core.storage.SetupStore
import com.zsilent.core.models.LocalWallet
import com.zsilent.core.models.WalletBalance
import com.zsilent.core.models.WalletHistory
import com.zsilent.core.models.WalletHistoryEntry
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.services.ZokaCli
import com.zsilent.core.ui.screens.ReceiveScreen
import com.zsilent.core.ui.screens.SendScreen
import com.zsilent.core.ui.screens.WalletScreen
import com.zsilent.core.ui.theme.ZSilentTheme
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Capturas de la wallet de Zoka Quantic (enviar, recibir, historial) con datos
 * de ejemplo, fuera de pantalla: sin nodo, sin binario y sin tocar la
 * configuración real (el estado de la app se arma sin `start()`, que es lo que
 * cierra procesos y lee el setup). Solo con `ZSILENT_SCREENSHOTS_DIR`.
 */
class QuanticWalletScreenshotsTest {

    private val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private fun fakeAddress(seed: Int): String =
        "zqtest1" + (0 until 1723).map { alphabet[(it * 31 + seed * 7) % alphabet.length] }.joinToString("")

    @Test
    fun renders_the_quantic_wallet_screens_when_requested() {
        val dir = System.getenv("ZSILENT_SCREENSHOTS_DIR")?.takeIf { it.isNotBlank() }?.let(::File) ?: return
        dir.mkdirs()
        val home = Files.createTempDirectory("zsilent-shots").toFile()
        val cli = ZokaCli(zoka = null, zokahd = null, projectRoot = home, resourcesRoot = null, trustedSetupDir = null, dataRoot = home)
        val rpc = ZokaRpcClient("")
        val appState = ZSilentAppState(
            cli = cli,
            rpc = rpc,
            localRpc = rpc,
            nodeController = NodeController(cli),
            miningController = MiningController(cli, rpc),
            setupStore = SetupStore(File(home, "setup.json").toPath())
        )
        val main = fakeAddress(1)
        val savings = fakeAddress(2)
        val ui = appState.uiState.copy(
            appLocked = false,
            walletLocked = false,
            walletName = "main",
            activeWalletName = "main",
            activeWalletAddress = main,
            activeShortAddressPublished = false,
            wallets = listOf(
                LocalWallet(
                    name = "main", address = main, balanceAtoms = 1_084_000_200, balance = "1084.0002", unlocked = true,
                    // El vector fijo del nodo: una corta de verdad, para que el QR sea el real.
                    shortAddress = "zqtest14v4DLNV3HyMWaCir6w4Hv4kG3G98eLZBU2HPk5tffYnmqgQwcj"
                ),
                LocalWallet(name = "savings", address = savings, balanceAtoms = 25_000_000, balance = "25", unlocked = true)
            ),
            walletBalance = WalletBalance(
                wallet = "main", address = main,
                balanceAtoms = 1_084_000_200, balance = "1084.0002",
                lockedBalanceAtoms = 24_000_000, lockedBalance = "24",
                totalBalanceAtoms = 1_108_000_200, totalBalance = "1108.0002",
                maxSendableAtoms = 1_083_999_700
            ),
            walletHistory = WalletHistory(
                wallet = "main",
                count = 4,
                transactions = listOf(
                    entry("19e5f35a1f0d1d9e", "received", 1_000_000_000, 41, 1),
                    entry("54bfd7f518e42361", "coinbase", 12_000_000, 40, 2),
                    entry("04b3e5da5f75900c", "sent", 250_000_200, 33, 9),
                    entry("7a1c90e2b5d34f88", "coinbase", 12_000_200, 30, 12)
                )
            )
        )
        for (dark in listOf(true, false)) {
            val theme = if (dark) "oscuro" else "claro"
            shot(dir, "wallet-$theme", dark) { WalletScreen(ui, appState) }
            shot(dir, "enviar-$theme", dark) { SendScreen(ui, appState) }
            shot(dir, "recibir-$theme", dark) { ReceiveScreen(ui, appState) }
        }
    }

    private fun entry(hash: String, kind: String, atoms: Long, height: Long, confirmations: Long) =
        WalletHistoryEntry(
            txHash = hash.repeat(4),
            status = "confirmed",
            timestamp = 1_792_600_000L + height * 120,
            amountHidden = false,
            feeHidden = false,
            height = height,
            kind = kind,
            amountAtoms = atoms,
            confirmations = confirmations
        )

    private fun shot(dir: File, name: String, dark: Boolean, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = 1280, height = 1000, density = Density(1f)) {
            ZSilentTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    content()
                }
            }
        }
        try {
            scene.render(0)
            val bytes = scene.render(2_000_000_000).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(dir, "$name.png").writeBytes(bytes)
            assertTrue(bytes.size > 10_000, "captura vacia: $name")
        } finally {
            scene.close()
        }
    }
}
