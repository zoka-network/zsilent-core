package com.zsilent.core.core.node

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.config.ZokaNetworkSettings
import com.zsilent.core.core.wallet.WalletService
import com.zsilent.core.rpc.ZokaRpcClient
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * NodeController, WalletService y el cliente de la API contra un nodo v2 de
 * verdad (regtest), en una carpeta temporal y con la API en otro puerto que el
 * de siempre: lo que hace ZSilent al arrancar, pedir el saldo y publicar.
 *
 * Corre solo si `ZSILENT_V2_E2E_ZOKA` nombra un `zoka` compilado con
 * `--features quantic-pq`: una corrida normal de pruebas no levanta nodos.
 */
class V2NodeWalletE2ETest {
    private val zokaBin = System.getenv("ZSILENT_V2_E2E_ZOKA").orEmpty()

    @Test
    fun the_v2_node_starts_and_the_wallet_reads_its_chain() {
        if (zokaBin.isBlank()) return
        val home = Files.createTempDirectory("zsilent-v2-e2e").toFile()
        ZokaNetworkConfig.configure(
            ZokaNetworkSettings(
                network = "regtest",
                chainId = ZokaNetworkConfig.QUANTIC_REGTEST_CHAIN_ID,
                localRpcUrl = "http://127.0.0.1:19140"
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
        val wallets = WalletService(cli)
        val node = NodeController(cli)
        val rpc = ZokaRpcClient(ZokaNetworkConfig.LocalRpcUrl)
        val password = "a1-segura"
        try {
            runBlocking {
                val alice = wallets.createWallet("alice", password.toCharArray(), 24)
                assertEquals(24, alice.recoveryPhrase.orEmpty().split(" ").size)
                assertTrue(ZokaNetworkConfig.isAddress(alice.address), alice.address)

                // En regtest el nodo mina enseguida el bloque 1, el premine.
                node.start(password = "", address = alice.address, network = "regtest")
                val status = withTimeout(90_000) {
                    var seen = runCatching { rpc.status() }.getOrNull()
                    while (seen == null || seen.height < 1) {
                        delay(500)
                        seen = runCatching { rpc.status() }.getOrNull()
                    }
                    seen
                }
                assertEquals(ZokaNetworkConfig.QUANTIC_REGTEST_CHAIN_ID, status.chainId)
                assertTrue(cli.nodeDescriptor("regtest").isFile)
                assertTrue(File(cli.networkDir("regtest"), "node/node.identity.secret").isFile)
                assertEquals("synced", rpc.syncStatus().status)
                assertEquals(1L, rpc.blockByHeight(1)?.height)
                assertEquals(5_000_000_000_000L, rpc.blockByHeight(1)?.rewardAtoms)

                val balance = wallets.balance(alice.address, password.toCharArray())
                assertEquals(5_000_000_000_000L, balance.balanceAtoms)
                val fee = wallets.estimateFee(alice.address, password.toCharArray(), "25")
                assertEquals("0.0002 ZKA", fee.fee)
                assertNull(fee.problem)
                val history = wallets.history(alice.address, password.toCharArray())
                assertEquals("coinbase", history.transactions.last().kind)
                assertEquals(1L, history.transactions.last().height)

                // La corta: el nodo no la conoce hasta que se publica.
                val listed = wallets.listWallets(password.toCharArray())
                val entry = listed.privateWallets.single()
                assertTrue(entry.unlocked)
                assertTrue(ZokaNetworkConfig.isShortAddress(entry.shortAddress), entry.shortAddress)
                assertNull(rpc.shortAddress(entry.shortAddress))
                assertEquals(entry.shortAddress, wallets.publishAddress(alice.address))
                assertEquals(alice.address, rpc.shortAddress(entry.shortAddress)?.address)
            }
        } finally {
            node.stop()
            rpc.close()
            ZokaNetworkConfig.configure(ZokaNetworkSettings())
        }
    }
}
