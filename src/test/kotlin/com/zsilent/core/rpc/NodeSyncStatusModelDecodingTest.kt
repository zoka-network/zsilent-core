package com.zsilent.core.rpc

import com.zsilent.core.models.NodeSyncStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeSyncStatusModelDecodingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodes_real_p2p_sync_and_gossip_metrics() {
        val status = json.decodeFromString<NodeSyncStatus>(
            """
            {
              "status": "caught_up",
              "mode": "caught_up",
              "ready_to_mine": true,
              "local_height": 14555,
              "target_height": 14555,
              "lag": 0,
              "connected_peers": 1,
              "min_peers": 1,
              "startup_ready": true,
              "startup_phase": "ready",
              "rpc_fallback_active": false,
              "last_sync_source": "p2p",
              "last_sync_event": "Observed peer sync state at height 14555.",
              "last_sync_error": null,
              "last_block_at": 1785686572,
              "last_updated": 1785691281,
              "gossip_rx_block_response": 1152,
              "gossip_publish_err": 5
            }
            """.trimIndent()
        )

        assertEquals("caught_up", status.status)
        assertEquals("caught_up", status.mode)
        assertTrue(status.readyToMine)
        assertEquals("ready", status.startupPhase)
        assertEquals("p2p", status.lastSyncSource)
        assertEquals("Observed peer sync state at height 14555.", status.lastSyncEvent)
        assertNull(status.lastSyncError)
        assertEquals(1_785_691_281L, status.lastUpdated)
        assertEquals(1_152L, status.gossipRxBlockResponse)
        assertEquals(5L, status.gossipPublishErr)
    }
}
