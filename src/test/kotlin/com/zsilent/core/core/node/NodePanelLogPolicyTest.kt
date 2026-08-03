package com.zsilent.core.core.node

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodePanelLogPolicyTest {
    @Test
    fun drops_fullnode_dashboard_rows_including_terminal_title_and_sections() {
        val dashboardRows = listOf(
            "\u001B]0;ZOKA Fullnode 1.0\u0007ZOKA Fullnode 1.0 [ RUNNING ] [ NO-MINING ]",
            "Session uptime 00:29:49    Synced height 14555    Mining height 14555",
            "Last event Published block announce at height 14555.",
            "> Mining",
            "› Chain",
            "> Network",
            "> Wallet",
            "Coinbase maturity 60 block(s)",
            "Session rewards 0 ZKA",
            "Session mined blocks 0",
            "Hashrate estimate warming up",
            "Difficulty 17 bits",
            "Solve time avg 226.39s / last 212.99s",
            "Press Ctrl+C to stop."
        )

        dashboardRows.forEach { line ->
            assertTrue(shouldDropNodePanelLine(line), "expected dashboard row to be hidden: $line")
        }
    }

    @Test
    fun keeps_network_and_exchange_events_that_are_not_dashboard_rows() {
        val operationalRows = listOf(
            "Peer connected over Tor: 12D3KooW...",
            "P2P gossip offer accepted and forwarded",
            "iroh relay route selected",
            "Snapshot served to remote peer",
            "Connected peers changed from 1 to 2",
            "Transport degraded to relay-only",
            "Address advertised to gossip peers"
        )

        operationalRows.forEach { line ->
            assertFalse(shouldDropNodePanelLine(line), "expected operational event to remain visible: $line")
        }
    }

    @Test
    fun never_hides_explicit_warning_or_error_even_when_prefixed_like_dashboard() {
        assertFalse(
            shouldDropNodePanelLine("2026-08-02T17:05:27Z WARN Connected peers probe failed")
        )
        assertFalse(
            shouldDropNodePanelLine("Last event ERROR gossip publish rejected")
        )
        assertFalse(
            shouldDropNodePanelLine("2026-08-02T17:05:27Z WARN seed reconnect dial failed")
        )
        assertFalse(
            shouldDropNodeChurnLine("2026-08-02T17:05:27Z WARN seed reconnect dial failed")
        )
        assertTrue(shouldDropNodeChurnLine("seed reconnect dial failed: peer unavailable"))
    }

    @Test
    fun drops_only_exact_section_headers_not_messages_beginning_with_section_words() {
        assertFalse(shouldDropNodePanelLine("Network route switched to iroh relay"))
        assertFalse(shouldDropNodePanelLine("Wallet refresh completed"))
        assertFalse(shouldDropNodePanelLine("Chain serve connection failed"))
        assertFalse(shouldDropNodePanelLine("Mining control request accepted"))
    }
}
