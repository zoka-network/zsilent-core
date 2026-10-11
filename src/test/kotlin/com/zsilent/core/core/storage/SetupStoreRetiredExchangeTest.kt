package com.zsilent.core.core.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SetupStoreRetiredExchangeTest {

    // El P2P Exchange se retiro en octubre de 2026, pero los archivos de setup
    // escritos antes siguen teniendo sus claves. Si el lector dejara de
    // ignorarlas, el usuario que alguna vez lo activo perderia todo su setup
    // al actualizar y volveria al onboarding.
    @Test
    fun setup_written_while_the_exchange_existed_still_loads() {
        val dir = Files.createTempDirectory("zsilent-retired-exchange")
        val path = dir.resolve("setup.json")
        Files.writeString(
            path,
            """
            {
              "onboardingComplete": true,
              "teamMiningEnabled": true,
              "p2pExchangeEnabled": true,
              "p2pExchangeRisksAccepted": true,
              "tronNetwork": "nile",
              "tronApiUrl": "https://nile.trongrid.io",
              "tronApiKey": "key-123",
              "tronFallbackUrls": "https://fallback.example",
              "manualPeers": ["abcdefghijklmnop.onion:30303"]
            }
            """.trimIndent()
        )

        val loaded = assertNotNull(SetupStore(path).load())

        assertTrue(loaded.onboardingComplete)
        assertTrue(loaded.teamMiningEnabled)
        assertEquals(listOf("abcdefghijklmnop.onion:30303"), loaded.manualPeers)
    }
}
