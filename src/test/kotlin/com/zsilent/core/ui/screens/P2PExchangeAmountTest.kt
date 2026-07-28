package com.zsilent.core.ui.screens

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class P2PExchangeAmountTest {

    @Test
    fun decimal_amounts_convert_to_exact_six_decimal_units() {
        assertEquals(1L, decimalUnitsExact(BigDecimal("0.000001"), 1_000_000))
        assertEquals(1_234_567L, decimalUnitsExact(BigDecimal("1.234567"), 1_000_000))
        assertEquals(2_000_000L, decimalUnitsExact(BigDecimal("2.0"), 1_000_000))
    }

    @Test
    fun a_seventh_decimal_is_rejected_instead_of_rounded() {
        assertFailsWith<ArithmeticException> {
            decimalUnitsExact(BigDecimal("1.0000001"), 1_000_000)
        }
    }

    @Test
    fun values_outside_the_wire_long_are_rejected() {
        assertFailsWith<ArithmeticException> {
            decimalUnitsExact(BigDecimal("9223372036854.775808"), 1_000_000)
        }
    }
}
