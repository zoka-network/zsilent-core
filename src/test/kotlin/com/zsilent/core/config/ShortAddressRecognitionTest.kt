package com.zsilent.core.config

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShortAddressRecognitionTest {
    // El vector fijo del nodo (`crates/zoka-pq-pool`, wallet.rs): la corta v2
    // de la testnet de la semilla de prueba 2.
    private val short = "zqtest22m1iDEYKpaemBFZMVj2k1SAzEV9BGL1uFXRZVABUrvAYuHQRbj"

    @Test
    fun a_short_address_is_a_recipient_but_does_not_pick_a_wallet() {
        assertTrue(ZokaNetworkConfig.isShortAddress(short))
        assertTrue(ZokaNetworkConfig.isRecipient(short))
        // Elegir una wallet por dirección necesita la completa: con una corta,
        // `quantic-pq-wallet --address` no encontraría ninguna.
        assertFalse(ZokaNetworkConfig.isAddress(short))
    }

    @Test
    fun a_full_address_stays_a_recipient_and_other_text_is_neither() {
        val full = "zqtest2" + "2".repeat(1756)
        assertTrue(ZokaNetworkConfig.isAddress(full))
        assertTrue(ZokaNetworkConfig.isRecipient(full))
        assertFalse(ZokaNetworkConfig.isShortAddress(full))
        for (text in listOf("", "zqtest2", "zq2" + short.removePrefix("zqtest2"), short.replace('v', '0'), short + "zz")) {
            assertFalse(ZokaNetworkConfig.isRecipient(text) && ZokaNetworkConfig.isShortAddress(text), text)
        }
        assertFalse(ZokaNetworkConfig.isShortAddress(short.replace('v', '0')), "0 is not Base58")
        assertFalse(ZokaNetworkConfig.isShortAddress("zq2" + short.removePrefix("zqtest2")), "another network's prefix")
    }
}

class ShortAddressQrSizeTest {
    // Con la corta, el QR de recibir es uno normal: 33 módulos por lado con
    // corrección M (versión 4), que cualquier cámara lee, más los 4 de margen
    // blanco de cada lado que dibuja QrCode. La completa daba 141 y no se leía
    // desde la pantalla (15/10/2026).
    @Test
    fun the_receive_qr_of_a_short_address_is_a_small_normal_qr() {
        val short = "zqtest22m1iDEYKpaemBFZMVj2k1SAzEV9BGL1uFXRZVABUrvAYuHQRbj"
        val margin = com.zsilent.core.ui.components.QR_QUIET_ZONE_MODULES
        val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
            short,
            com.google.zxing.BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
                com.google.zxing.EncodeHintType.MARGIN to margin,
            )
        )
        assertTrue(margin >= 4, "the standard asks for 4 modules of quiet zone")
        assertTrue(matrix.width - 2 * margin <= 33, "${matrix.width - 2 * margin} modules")
    }
}
