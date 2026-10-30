package com.zsilent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.core.mining.MiningController
import com.zsilent.core.core.node.NodeController
import com.zsilent.core.core.storage.SetupStore
import com.zsilent.core.models.LocalWallet
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.models.StratumStatus
import com.zsilent.core.rpc.V2Peers
import com.zsilent.core.rpc.V2Status
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.rpc.toSyncStatus
import com.zsilent.core.services.ZokaCli
import com.zsilent.core.ui.screens.DashboardScreen
import com.zsilent.core.ui.screens.MiningScreen
import com.zsilent.core.ui.screens.NodeScreen
import com.zsilent.core.ui.screens.PrivacyScreen
import com.zsilent.core.ui.screens.SettingsScreen
import com.zsilent.core.ui.screens.StratumScreen
import com.zsilent.core.ui.theme.ZSilentTheme
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Capturas de las pantallas que cambian con la red STARK v2 (nodo, minería,
 * Stratum, privacidad y tablero), con un nodo v2 de ejemplo en línea, fuera de
 * pantalla y sin binario. Solo con `ZSILENT_SCREENSHOTS_DIR`.
 */
class QuanticV2ScreensScreenshotsTest {

    @Test
    fun renders_the_v2_node_screens_when_requested() {
        val dir = System.getenv("ZSILENT_SCREENSHOTS_DIR")?.takeIf { it.isNotBlank() }?.let(::File) ?: return
        dir.mkdirs()
        val home = Files.createTempDirectory("zsilent-v2-shots").toFile()
        val zoka = File(home, "zoka.exe").apply { writeText("") }
        val zokahd = File(home, "zokahd.exe").apply { writeText("") }
        val cli = ZokaCli(zoka = zoka, zokahd = zokahd, projectRoot = home, resourcesRoot = null, trustedSetupDir = null, dataRoot = home)
        // Nada escucha en este puerto: las pantallas no llegan a leer nada.
        val rpc = ZokaRpcClient("http://127.0.0.1:1")
        val appState = ZSilentAppState(
            cli = cli,
            rpc = rpc,
            localRpc = rpc,
            nodeController = NodeController(cli),
            miningController = MiningController(cli, rpc),
            setupStore = SetupStore(File(home, "setup.json").toPath())
        )
        val status = V2Status(
            chainId = "zoka-quantic-testnet-v2",
            height = 1_284,
            tipTimestamp = 1_792_600_000L,
            issuedAtoms = 5_015_400_000_000L,
            stratum = StratumStatus(configured = "127.0.0.1:3333", listening = "127.0.0.1:3333", miners = 1, acceptedShares = 42, blocksFound = 3),
            peers = V2Peers(outbound = 2, inbound = 5, known = 9, relay = true, bestHeight = 1_284),
            checkpoint = com.zsilent.core.rpc.V2Checkpoint(height = 1_260, blockHash = "ab".repeat(32), followed = true),
            directory = com.zsilent.core.rpc.V2Directory(addresses = 14, publishedHere = 1)
        )
        val now = System.currentTimeMillis()
        val main = "zqtest2" + "3".repeat(1756)
        val ui = appState.uiState.copy(
            appLocked = false,
            walletLocked = false,
            sessionUnlocked = true,
            walletName = "main",
            activeWalletName = "main",
            activeWalletAddress = main,
            activeShortAddressPublished = true,
            wallets = listOf(
                LocalWallet(
                    name = "main", address = main, balanceAtoms = 36_000_000, balance = "36", unlocked = true,
                    shortAddress = "zqtest22m1iDEYKpaemBFZMVj2k1SAzEV9BGL1uFXRZVABUrvAYuHQRbj"
                )
            ),
            nodeStatus = ServiceStatus.Online,
            localRpcOnline = true,
            rpcOnline = true,
            chainHeight = status.height,
            networkHeight = status.height,
            networkSynced = true,
            miningSyncReady = true,
            localConnectedPeers = 7,
            peerDirectCount = 2,
            stratumEnabled = true,
            stratum = status.stratum,
            nodeSyncStatus = status.toSyncStatus(nowSeconds = 1_792_600_030L),
            stats = NodeStats(
                height = status.height,
                emissionZka = 5_015_400.0,
                totalSupplyEmittedAtoms = status.issuedAtoms,
                lastBlockTime = status.tipTimestamp,
                connectedPeers = 7,
                minersOnline = 1
            ),
            torStatus = ServiceStatus.Offline,
            zokaReady = true,
            nodeV2 = status,
            nodeEndpointId = "e1ca05f3ee539ae198938d8391f0cb45555d2bafa423dd086bc8ca7695fb8706",
            nodeKeyId = "3b2dc1593abcabb3d2807b61f5bd81a9b5e6555d27f2fe69ef7a2d06813af7b6",
            nodeSyncStatusObservedAtMillis = now,
            peerTelemetryObservedAtMillis = now
        )
        for (dark in listOf(true, false)) {
            val theme = if (dark) "oscuro" else "claro"
            shot(dir, "v2-tablero-$theme", dark) { DashboardScreen(ui, appState) }
            shot(dir, "v2-nodo-$theme", dark) { NodeScreen(ui, appState) }
            shot(dir, "v2-mineria-$theme", dark) { MiningScreen(ui, appState) }
            shot(dir, "v2-stratum-$theme", dark) { StratumScreen(ui, appState) }
            shot(dir, "v2-privacidad-$theme", dark) { PrivacyScreen(ui, appState) }
            shot(dir, "v2-ajustes-$theme", dark) { SettingsScreen(ui, appState) }
            // A full address and a view key, as long as the real ones, in a
            // column as wide as the Recovery card of a 1360 px window.
            shot(dir, "v2-fila-copiable-$theme", dark) {
                Column(Modifier.width(940.dp).padding(24.dp)) {
                    CopyableKeyValueRow("Wallet", "zqtest22Bzm" + "HsHe1V95nFuCN2gAyrKvko1ktrDmbSsLcBwk7NHbphWY2NWmU7j".repeat(30) + "ysLDbN")
                    CopyableKeyValueRow("View key", "zqtestview2EB67sAsQA42WgVACjWB4X2fng2wr7iEQK3DHyrC89fTgHwuUBwBeD41Fmi8ZRrNzXbhencg1YTWrTLPPmdgtNNhh3ppGnEi1phWFSqy6fkiDPm4ALq9Q")
                    CopyableKeyValueRow("Short", "zqtest22m1iDEY")
                }
            }
        }
    }

    private fun shot(dir: File, name: String, dark: Boolean, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = 1280, height = 1500, density = Density(1f)) {
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
