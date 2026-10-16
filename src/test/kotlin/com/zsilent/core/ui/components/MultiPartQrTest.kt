package com.zsilent.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiPartQrTest {
    /** Una zkq1 de prueba de 1.715 caracteres, como las reales. */
    private val zkq1 = "zkq1" + (0 until 1711).joinToString("") {
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"[it % 58].toString()
    }

    @Test
    fun a_short_text_stays_a_single_qr_without_header() {
        val zka1 = "zka1" + "x".repeat(100)
        assertEquals(listOf(zka1), MultiPartQr.split(zka1))
    }

    @Test
    fun a_zkq1_is_split_into_six_small_parts_exactly_like_zkapriv() {
        val parts = MultiPartQr.split(zkq1)
        assertEquals(6, parts.size)
        assertTrue(parts.all { it.length <= 320 })
        // Vector compartido con ZKAPriv (MultiPartQrTest de zkapriv/ui): si las
        // dos apps no arman las partes igual, ZKAPriv no lee lo que muestra ZSilent.
        assertEquals("82a56d5f", MultiPartQr.fingerprint(zkq1))
        assertEquals("zkqp:1/6:82a56d5f:zkq112345678", parts.first().take(30))
        assertEquals(zkq1, parts.joinToString("") { it.substringAfter("82a56d5f:") })
    }
}
