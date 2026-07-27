package com.zsilent.core.rpc

import com.zsilent.core.models.TeamMiningStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TeamMiningModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodes_runtime_status_and_member_metrics() {
        val status = json.decodeFromString<TeamMiningStatus>(
            """
            {
              "enabled": true,
              "hashing": false,
              "threads": 4,
              "join_status": "joined",
              "local_address": "zka1alice",
              "combined_hashrate_hps": 42000,
              "local_accepted_shares": 12,
              "team_accepted_shares": 31,
              "estimated_turn": 2,
              "team": {
                "team_id": "00112233445566778899aabbccddeeff",
                "invite_code": "ZTM1-CODE-CHECK",
                "coordinator": "zka1bob",
                "backup_coordinator": "zka1alice",
                "epoch": 3,
                "revision": 19,
                "total_work": 900,
                "confirmed_blocks": 5,
                "active_template": {
                  "id": "template",
                  "height": 99,
                  "recipient_address": "zka1alice",
                  "network_difficulty_bits": 17
                },
                "members": {
                  "zka1alice": {
                    "address": "zka1alice",
                    "active": true,
                    "expelled": false,
                    "total_work": 300,
                    "accepted_shares": 12,
                    "blocks_paid": 1,
                    "share_difficulty_bits": 9,
                    "estimated_hashrate_hps": 12000
                  }
                }
              }
            }
            """.trimIndent()
        )

        assertTrue(status.enabled)
        assertFalse(status.hashing)
        assertEquals(4, status.threads)
        assertEquals(2, status.estimatedTurn)
        val team = assertNotNull(status.team)
        assertEquals(3, team.epoch)
        assertEquals("zka1alice", team.activeTemplate?.recipientAddress)
        assertEquals(12_000, team.members["zka1alice"]?.estimatedHashrateHps)
    }

    @Test
    fun decodes_not_joined_status_without_a_team() {
        val status = json.decodeFromString<TeamMiningStatus>(
            """{"enabled":false,"hashing":false,"join_status":"not_joined","local_address":"zka1alice","team":null}"""
        )

        assertEquals("not_joined", status.joinStatus)
        assertEquals(null, status.team)
    }
}
