package com.zsilent.core.core.storage

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// De la testnet v2 a la v3: las wallets y el setup pasan solos, la cadena no,
// y la v2 queda como estaba. Como en la PC: las wallets en la carpeta de Zoka
// (%APPDATA%\Zoka) y el setup en la de ZSilent Core.
class NetworkMigrationTest {
    private val root = kotlin.io.path.createTempDirectory("zsilent-migration").toFile()
    private val zoka = File(root, "Zoka")
    private val setup = File(root, "ZSilent Core")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun write(base: File, path: String, text: String) =
        File(base, path).apply { parentFile.mkdirs(); writeText(text) }

    @Test
    fun the_wallets_and_the_setup_pass_once_and_the_chain_stays_behind() {
        write(zoka, ".zoka/quantic-testnet-v2/wallets/alice.json", "alice")
        write(zoka, ".zoka/quantic-testnet-v2/wallets/team/bob.json", "bob")
        write(zoka, ".zoka/quantic-testnet-v2/node/db/CURRENT", "chain")
        write(zoka, ".zoka/quantic-testnet-v2/wallet-mirror/CURRENT", "mirror")
        write(setup, "setup-quantic-testnet-v2.json", "{\"done\":true}")

        val copied = NetworkMigration.fromV2(zoka, setup, "testnet")

        assertEquals(
            setOf("wallets/alice.json", "wallets/team${File.separator}bob.json", "setup-quantic-testnet-v3.json"),
            copied.toSet()
        )
        val v3 = File(zoka, ".zoka/quantic-testnet-v3")
        assertEquals("alice", File(v3, "wallets/alice.json").readText())
        assertEquals("bob", File(v3, "wallets/team/bob.json").readText())
        assertEquals("{\"done\":true}", File(setup, "setup-quantic-testnet-v3.json").readText())
        assertFalse(File(v3, "node").exists())
        assertFalse(File(v3, "wallet-mirror").exists())
        assertTrue(File(zoka, ".zoka/quantic-testnet-v2/wallets/alice.json").isFile)

        // A wallet deleted on v3 does not come back on the next start.
        File(v3, "wallets/alice.json").delete()
        assertTrue(NetworkMigration.fromV2(zoka, setup, "testnet").isEmpty())
        assertFalse(File(v3, "wallets/alice.json").exists())
    }

    @Test
    fun what_v3_already_has_is_kept() {
        write(zoka, ".zoka/quantic-testnet-v2/wallets/alice.json", "old")
        write(zoka, ".zoka/quantic-testnet-v3/wallets/alice.json", "new")
        write(setup, "setup-quantic-testnet-v2.json", "old")
        write(setup, "setup-quantic-testnet-v3.json", "new")

        assertTrue(NetworkMigration.fromV2(zoka, setup, "testnet").isEmpty())
        assertEquals("new", File(zoka, ".zoka/quantic-testnet-v3/wallets/alice.json").readText())
        assertEquals("new", File(setup, "setup-quantic-testnet-v3.json").readText())
    }

    @Test
    fun a_fresh_install_has_nothing_to_copy() {
        assertTrue(NetworkMigration.fromV2(zoka, setup, "").isEmpty())
        assertTrue(File(zoka, ".zoka/quantic-testnet-v3/migrated-from-v2").isFile)
    }
}
