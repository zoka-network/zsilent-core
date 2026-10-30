package com.zsilent.core.core.node

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.config.ZokaNetworkSettings
import com.zsilent.core.core.tor.TorLaunchConfig
import com.zsilent.core.core.tor.TorSupervisor
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Modo Tor de punta a punta, como lo hace ZSilent: el supervisor levanta un
 * Tor cliente (solo SOCKS) y el nodo de la testnet arranca con `--tor`, en
 * una carpeta temporal y con la API en otro puerto, y tiene que llegar a la
 * red por la semilla .onion del manifiesto.
 *
 * Corre solo con `ZSILENT_TOR_E2E_ZOKA` (un `zoka` 4.0.3 o posterior) y
 * `ZSILENT_TOR_BIN` (un tor.exe): necesita la red Tor real.
 */
class TorModeE2ETest {
    private val zokaBin = System.getenv("ZSILENT_TOR_E2E_ZOKA").orEmpty()
    private val torBin = System.getenv("ZSILENT_TOR_BIN").orEmpty()

    @Test
    fun the_node_reaches_the_testnet_through_tor_alone() {
        if (zokaBin.isBlank() || torBin.isBlank()) return
        val home = Files.createTempDirectory("zsilent-tor-e2e").toFile()
        ZokaNetworkConfig.configure(
            ZokaNetworkSettings(
                network = "testnet",
                chainId = ZokaNetworkConfig.QUANTIC_TESTNET_CHAIN_ID,
                localRpcUrl = "http://127.0.0.1:19143"
            )
        )
        val cli = ZokaCli(
            zoka = File(zokaBin),
            zokahd = null,
            projectRoot = home,
            resourcesRoot = null,
            trustedSetupDir = null,
            dataRoot = home
        )
        val tor = TorSupervisor(cli)
        val node = NodeController(cli)
        val rpc = ZokaRpcClient(ZokaNetworkConfig.LocalRpcUrl)
        try {
            runBlocking {
                assertTrue(!node.holdsCheckpointAuthority("testnet"))
                val proxy = tor.start(TorLaunchConfig(socksPort = 19051, hiddenService = false))
                node.start(password = "", address = "", network = "testnet", torProxy = "${proxy.socksHost}:${proxy.socksPort}")
                val synced = withTimeout(600_000) {
                    var seen = runCatching { rpc.status() }.getOrNull()
                    while (seen == null || seen.height < 50) {
                        delay(2_000)
                        seen = runCatching { rpc.status() }.getOrNull()
                    }
                    seen
                }
                assertTrue(synced.height >= 50, "height ${synced.height}")
            }
        } finally {
            node.stop()
            tor.stop()
            rpc.close()
            ZokaNetworkConfig.configure(ZokaNetworkSettings())
        }
    }
}
