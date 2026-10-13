package com.zsilent.core.rpc

import com.zsilent.core.models.WalletReceiveInfo
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletReceiveInfoDecodingTest {
    // La misma configuracion que WalletService usa para leer al CLI.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun without_activation_the_zkq1_is_known_but_not_active() {
        val info = json.decodeFromString<WalletReceiveInfo>(
            """{"wallet":"main","address":"zka1abc","view_only":false,
               "hybrid_address":"zkq1xyz","hybrid_activation_height":null}"""
        )

        assertEquals("zkq1xyz", info.hybridAddress)
        assertNull(info.hybridActivationHeight)
    }

    @Test
    fun with_activation_the_height_comes_through() {
        val info = json.decodeFromString<WalletReceiveInfo>(
            """{"wallet":"main","address":"zka1abc","view_only":false,
               "hybrid_address":"zkq1xyz","hybrid_activation_height":30000}"""
        )

        assertEquals(30000L, info.hybridActivationHeight)
    }

    @Test
    fun output_without_the_hybrid_fields_reads_as_no_zkq1() {
        val info = json.decodeFromString<WalletReceiveInfo>(
            """{"wallet":"main","address":"zka1abc","view_only":false}"""
        )

        assertNull(info.hybridAddress)
        assertNull(info.hybridActivationHeight)
    }
}
