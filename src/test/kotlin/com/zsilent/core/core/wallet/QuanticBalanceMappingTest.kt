package com.zsilent.core.core.wallet

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El saldo de `zoka quantic-pq-wallet balance --json` tal como lo arma el nodo
 * (`stark_wallet.rs`, acción "balance"), pasado a lo que muestra la app.
 *
 * Caso real del 3/11/2026: un envío al teléfono reservó la nota más grande
 * (el vuelto del premine) mientras esperaba un bloque. El nodo la informaba en
 * `reserved_atoms`, la app no la leía y el total bajó de 5.019.799,9994 ZKA
 * (lo que veía ZokaScan con la clave de vista) a 36.776,9998.
 */
class QuanticBalanceMappingTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun a_pending_send_keeps_its_reserved_notes_in_the_total() {
        val reply = json.decodeFromString<QwBalance>(
            """
            {"name":"zsilent","height":4009,"wallet_height":4009,"node_behind":false,"sync_error":null,
             "spendable_atoms":36776999800,"immature_atoms":0,"reserved_atoms":4983022999600,
             "spendable":"36776.9998","immature":"0","max_sendable_atoms":0,"notes":1652,
             "pending_payment":true}
            """.trimIndent()
        )

        val balance = reply.toWalletBalance("zsilent")

        assertEquals(36_776_999_800L, balance.balanceAtoms)
        assertEquals(4_983_022_999_600L, balance.reservedBalanceAtoms)
        assertEquals(5_019_799_999_400L, balance.totalBalanceAtoms)
        assertEquals("5019799.9994", balance.totalBalance)
        assertTrue(balance.pendingPayment)
    }

    @Test
    fun without_a_pending_send_the_total_is_spendable_plus_immature() {
        val reply = json.decodeFromString<QwBalance>(
            """{"name":"zsilent","spendable_atoms":12000000,"immature_atoms":24000000,
                "reserved_atoms":0,"max_sendable_atoms":11999800,"pending_payment":false}"""
        )

        val balance = reply.toWalletBalance("zsilent")

        assertEquals(36_000_000L, balance.totalBalanceAtoms)
        assertEquals(0L, balance.reservedBalanceAtoms)
        assertFalse(balance.pendingPayment)
    }

    /** Un nodo anterior que no manda los campos nuevos sigue andando. */
    @Test
    fun an_older_node_without_the_reservation_fields_still_decodes() {
        val reply = json.decodeFromString<QwBalance>(
            """{"name":"zsilent","spendable_atoms":5000000,"immature_atoms":0,"max_sendable_atoms":4999800}"""
        )

        val balance = reply.toWalletBalance("zsilent")

        assertEquals(5_000_000L, balance.totalBalanceAtoms)
        assertFalse(balance.pendingPayment)
    }
}
