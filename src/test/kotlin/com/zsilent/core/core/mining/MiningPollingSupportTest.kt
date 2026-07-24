package com.zsilent.core.core.mining

import com.zsilent.core.rpc.decodePendingPrivateTransfers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiningPollingSupportTest {
    @Test
    fun cli_fallback_is_throttled_far_below_the_ui_poll_rate() {
        val throttle = PollingThrottle(MINING_CLI_FALLBACK_INTERVAL_MS)

        assertTrue(throttle.tryAcquire(1_000))
        assertFalse(throttle.tryAcquire(1_000 + MINING_POLL_INTERVAL_MS))
        assertFalse(throttle.tryAcquire(1_000 + MINING_CLI_FALLBACK_INTERVAL_MS - 1))
        assertTrue(throttle.tryAcquire(1_000 + MINING_CLI_FALLBACK_INTERVAL_MS))
        assertTrue(MINING_CLI_FALLBACK_INTERVAL_MS >= 15L * 60L * 1_000L)
    }

    @Test
    fun pending_rpc_response_maps_to_the_existing_ui_model() {
        val result = decodePendingPrivateTransfers(
            """{"count":2,"pending":[{"tx_hash":"hash-a","extra":1},{"tx_hash":"hash-b"}]}"""
        )

        assertEquals(2, result.count)
        assertEquals(listOf("hash-a", "hash-b"), result.pendingPrivateTxHashes)
    }
}
