package com.zsilent.core.ui

import com.zsilent.core.models.LocalWallet
import kotlin.test.Test
import kotlin.test.assertEquals

class ShownAddressTest {

    private val full = "zqtest12" + "x".repeat(1726)
    private val short = "zqtest15MnwVgjVzRRABprTurtK7rB2tUw4V7FUyfi66Mt1zJPXuv76Bp"

    // De una wallet propia se muestra (y se copia) la corta; la completa queda
    // en Receive. Sin corta conocida, o con una dirección ajena, la completa.
    @Test
    fun an_own_wallet_shows_its_short_address() {
        val ui = ZSilentUiState(
            wallets = listOf(
                LocalWallet(name = "zsilent-2", address = full, shortAddress = short),
                LocalWallet(name = "vieja", address = "zqtest1sin-corta", shortAddress = ""),
            )
        )
        assertEquals(short, ui.shownAddress(full))
        assertEquals("zqtest1sin-corta", ui.shownAddress("zqtest1sin-corta"))
        assertEquals("zqtest1ajena", ui.shownAddress("zqtest1ajena"))
        assertEquals("", ui.shownAddress(""))
    }
}
