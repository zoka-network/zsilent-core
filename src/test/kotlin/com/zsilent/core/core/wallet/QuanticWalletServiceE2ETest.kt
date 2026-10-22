package com.zsilent.core.core.wallet

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.config.ZokaNetworkSettings
import com.zsilent.core.services.ZokaCli
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WalletService contra un nodo de Zoka Quantic de verdad (regtest), con las
 * respuestas reales de `zoka quantic-wallet`.
 *
 * Solo corre si el ensayo lo pide (`ZSILENT_QUANTIC_E2E_RPC`): lo arma
 * `interop/zsilent-wallet.sh`, que levanta el nodo minando, deja la wallet del
 * premine de regtest como `equipo` y apunta `ZOKA_HOME` a una carpeta de prueba.
 * Sin `ZOKA_HOME` no corre nunca, para no tocar las wallets reales.
 */
class QuanticWalletServiceE2ETest {
    private val rpc = System.getenv("ZSILENT_QUANTIC_E2E_RPC").orEmpty()
    private val zokaHome = System.getenv("ZOKA_HOME").orEmpty()
    private val zokaBin = System.getenv("ZSILENT_ZOKA_BIN").orEmpty()
    private val teamAddress = System.getenv("ZSILENT_QUANTIC_E2E_TEAM").orEmpty()

    @Test
    fun the_wallet_service_works_against_a_real_quantic_node() {
        if (rpc.isBlank() || zokaHome.isBlank() || zokaBin.isBlank() || teamAddress.isBlank()) return
        ZokaNetworkConfig.configure(
            ZokaNetworkSettings(
                network = "quantic-regtest",
                chainId = ZokaNetworkConfig.QUANTIC_REGTEST_CHAIN_ID,
                localRpcUrl = rpc
            )
        )
        val cli = ZokaCli(
            zoka = File(zokaBin),
            zokahd = null,
            projectRoot = File(zokaHome),
            resourcesRoot = null,
            trustedSetupDir = null,
            dataRoot = File(zokaHome)
        )
        val wallets = WalletService(cli)
        runBlocking {
            // Desbloqueo: la contraseña del equipo abre solo la del equipo.
            val listed = wallets.listWallets("e1".toCharArray())
            assertTrue(listed.privateWallets.any { it.address == teamAddress && it.unlocked }, "$listed")

            val team = wallets.balance(teamAddress, "e1".toCharArray())
            assertEquals(5_000_000_000_000L, team.balanceAtoms)
            assertEquals("5000000", team.balance)
            assertTrue(team.maxSendableAtoms in 1 until team.balanceAtoms)

            val fee = wallets.estimateFee(teamAddress, "e1".toCharArray(), "25")
            assertEquals("0.0002 ZKA", fee.fee)
            assertEquals(null, fee.problem)

            // Alta como la hace el onboarding: la frase primero, la wallet después.
            val phrase = wallets.generateRecoveryPhrase(QUANTIC_SEED_WORDS)
            assertEquals(24, phrase.phrase.split(" ").size)
            val alice = wallets.importFromSeed("alice", phrase.phrase.toCharArray(), "a1-segura".toCharArray())
            assertTrue(ZokaNetworkConfig.isAddress(alice.address), alice.address)

            // Dirección corta: la lista la trae, se publica en el nodo y el
            // equipo le paga a la corta (la resuelve el nodo).
            val aliceShort = wallets.listWallets().privateWallets.first { it.address == alice.address }.shortAddress
            assertTrue(ZokaNetworkConfig.isShortAddress(aliceShort), aliceShort)
            assertEquals(aliceShort, wallets.publishAddress(alice.address))
            assertEquals(alice.address, com.zsilent.core.rpc.ZokaRpcClient(rpc).shortAddress(aliceShort)?.address)

            val sent = wallets.sendPrivate(
                from = teamAddress,
                to = aliceShort,
                amount = "25",
                fee = "ignored",
                password = "e1".toCharArray(),
                memo = "hola desde ZSilent"
            )
            assertEquals(64, sent.hash.length)

            var received = 0L
            for (attempt in 0 until 60) {
                received = wallets.balance(alice.address, "a1-segura".toCharArray()).balanceAtoms
                if (received > 0) break
                delay(5_000)
            }
            assertEquals(25_000_000L, received, "Alice recibe los 25 ZKA")
            val history = wallets.history(alice.address, "a1-segura".toCharArray())
            val entry = history.transactions.single()
            assertEquals("received", entry.kind)
            assertEquals(25_000_000L, entry.amountAtoms)
            assertTrue(entry.timestamp > 0 && entry.confirmations >= 1, "$entry")

            // Contraseña nueva, frase, respaldo e importación.
            wallets.changePassphrase(alice.address, "a1-segura".toCharArray(), "a2-segura".toCharArray())
            assertEquals(25_000_000L, wallets.balance(alice.address, "a2-segura".toCharArray()).balanceAtoms)
            assertEquals(phrase.phrase, wallets.exportSecrets(alice.address, "a2-segura".toCharArray()).mnemonic)
            val backup = File(zokaHome, "alice-backup.json").absolutePath
            wallets.exportEncryptedBackup(alice.address, backup, "a2-segura".toCharArray())
            val imported = wallets.restoreEncryptedBackup(backup, "a2-segura".toCharArray(), importName = "alice-copia")
            assertEquals(alice.address, imported.address)
            assertEquals(alice.address, wallets.hybridReceive(alice.address, "x".toCharArray()).address)
        }
    }
}
