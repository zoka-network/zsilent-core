package com.zsilent.core.core.storage

import com.zsilent.core.models.PersistedSetup
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetupStoreP2PExchangeSettingsTest {

    @Test
    fun legacy_setup_stays_opted_out_and_uses_trongrid() {
        val dir = Files.createTempDirectory("zsilent-p2p-legacy")
        val path = dir.resolve("setup.json")
        Files.writeString(path, """{"onboardingComplete":true}""")

        val loaded = SetupStore(path).load()!!

        assertFalse(loaded.p2pExchangeEnabled)
        assertFalse(loaded.p2pExchangeRisksAccepted)
        assertEquals("https://api.trongrid.io", loaded.tronApiUrl)
        assertEquals("", loaded.tronApiKey)
        assertEquals("", loaded.tronFallbackUrls)
    }

    @Test
    fun opt_in_consent_and_provider_settings_survive_restart() {
        val dir = Files.createTempDirectory("zsilent-p2p-settings")
        val store = SetupStore(dir.resolve("setup.json"))
        store.save(
            PersistedSetup(
                onboardingComplete = true,
                p2pExchangeEnabled = true,
                p2pExchangeRisksAccepted = true,
                tronApiUrl = "https://tron.example",
                tronApiKey = "key-123",
                tronFallbackUrls = "https://fallback-a.example,https://fallback-b.example"
            )
        )

        val loaded = store.load()!!

        assertTrue(loaded.p2pExchangeEnabled)
        assertTrue(loaded.p2pExchangeRisksAccepted)
        assertEquals("https://tron.example", loaded.tronApiUrl)
        assertEquals("key-123", loaded.tronApiKey)
        assertEquals(
            "https://fallback-a.example,https://fallback-b.example",
            loaded.tronFallbackUrls
        )
    }
}
