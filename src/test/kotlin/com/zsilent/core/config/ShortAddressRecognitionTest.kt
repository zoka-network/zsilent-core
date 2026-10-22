package com.zsilent.core.config

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShortAddressRecognitionTest {
    // El vector fijo del nodo (`crates/zoka-quantic`, address.rs): la corta de
    // la testnet de las claves de prueba 3.
    private val short = "zqtest14v4DLNV3HyMWaCir6w4Hv4kG3G98eLZBU2HPk5tffYnmqgQwcj"

    @Test
    fun a_short_address_is_a_recipient_but_does_not_pick_a_wallet() {
        assertTrue(ZokaNetworkConfig.isShortAddress(short))
        assertTrue(ZokaNetworkConfig.isRecipient(short))
        // Elegir una wallet por dirección necesita la completa: con una corta,
        // `quantic-wallet --address` no encontraría ninguna.
        assertFalse(ZokaNetworkConfig.isAddress(short))
    }

    @Test
    fun a_full_address_stays_a_recipient_and_other_text_is_neither() {
        val full = "zqtest1" + "2".repeat(1727)
        assertTrue(ZokaNetworkConfig.isAddress(full))
        assertTrue(ZokaNetworkConfig.isRecipient(full))
        assertFalse(ZokaNetworkConfig.isShortAddress(full))
        for (text in listOf("", "zqtest1", "zq1" + short.removePrefix("zqtest1"), short.replace('v', '0'), short + "zz")) {
            assertFalse(ZokaNetworkConfig.isRecipient(text) && ZokaNetworkConfig.isShortAddress(text), text)
        }
        assertFalse(ZokaNetworkConfig.isShortAddress(short.replace('v', '0')), "0 is not Base58")
        assertFalse(ZokaNetworkConfig.isShortAddress("zq1" + short.removePrefix("zqtest1")), "another network's prefix")
    }
}
