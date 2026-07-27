package com.zsilent.core.core.storage

import com.zsilent.core.models.PersistedSetup
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards which Tron network a stale setup file resolves to.
 *
 * Unlike the Explorer flag, "unset must mean on" is backwards here: an old
 * setup file with no `tronNetwork` key must resolve to mainnet, matching the
 * node's own `ZOKA_TRON_NETWORK` parser. If it resolved to Nile instead, an
 * upgrade could silently start judging real trades against testnet USDT —
 * every genuine mainnet payment would look like non-payment.
 */
class SetupStoreTronNetworkTest {

    private fun storeInTempDir(): SetupStore {
        val dir = Files.createTempDirectory("zsilent-setup-tron-test")
        dir.toFile().deleteOnExit()
        return SetupStore(dir.resolve("setup.json"))
    }

    @Test
    fun setup_file_without_the_field_resolves_to_mainnet() {
        val dir = Files.createTempDirectory("zsilent-setup-tron-legacy")
        dir.toFile().deleteOnExit()
        val path = dir.resolve("setup.json")
        Files.writeString(
            path,
            """{"onboardingComplete": true, "walletName": "main"}"""
        )

        val loaded = SetupStore(path).load()

        assertTrue(loaded != null, "legacy setup file should still load")
        assertEquals(
            "mainnet",
            loaded.tronNetwork,
            "an unset network must never resolve to testnet"
        )
    }

    @Test
    fun selecting_nile_survives_a_save_and_reload() {
        val store = storeInTempDir()
        store.save(PersistedSetup(onboardingComplete = true, tronNetwork = "nile"))

        assertEquals("nile", store.load()!!.tronNetwork)
    }

    @Test
    fun switching_back_to_mainnet_survives_a_save_and_reload() {
        val store = storeInTempDir()
        store.save(PersistedSetup(onboardingComplete = true, tronNetwork = "nile"))
        store.save(PersistedSetup(onboardingComplete = true, tronNetwork = "mainnet"))

        assertEquals("mainnet", store.load()!!.tronNetwork)
    }
}
