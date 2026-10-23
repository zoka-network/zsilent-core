package com.zsilent.core.ui.screens

import com.zsilent.core.models.LocalWallet
import com.zsilent.core.models.MiningSnapshot
import com.zsilent.core.models.NodeSyncStatus
import com.zsilent.core.models.NodeStats
import com.zsilent.core.models.PeerInfo
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.models.WalletHistory
import com.zsilent.core.models.WalletHistoryEntry
import com.zsilent.core.ui.ZSilentUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardPresentationTest {
    @Test
    fun responsive_metric_columns_have_stable_breakpoints() {
        assertEquals(1, dashboardMetricColumns(519f))
        assertEquals(2, dashboardMetricColumns(520f))
        assertEquals(2, dashboardMetricColumns(899f))
        assertEquals(4, dashboardMetricColumns(900f))
    }

    @Test
    fun telemetry_starts_with_one_real_sample_and_caps_old_values() {
        var history = emptyList<DashboardTelemetrySample>()
        history = appendDashboardTelemetrySample(history, sample(at = 1_000L, gossip = 1L), limit = 3)
        assertEquals(listOf(1_000L), history.map { it.timestampMillis })

        history = appendDashboardTelemetrySample(history, sample(at = 2_000L, gossip = 2L), limit = 3)
        history = appendDashboardTelemetrySample(history, sample(at = 3_000L, gossip = 3L), limit = 3)
        history = appendDashboardTelemetrySample(history, sample(at = 4_000L, gossip = 4L), limit = 3)

        assertEquals(listOf(2_000L, 3_000L, 4_000L), history.map { it.timestampMillis })
    }

    @Test
    fun cumulative_gossip_counter_becomes_a_rate_and_reset_is_not_negative() {
        val samples = listOf(
            sample(at = 0L, gossip = 10L),
            sample(at = 5_000L, gossip = 15L),
            sample(at = 10_000L, gossip = 20L),
            sample(at = 15_000L, gossip = 2L)
        )

        val rates = dashboardCounterRatePerMinute(samples) { it.gossipResponses }

        assertEquals(60.0, rates[0], 0.0001)
        assertEquals(60.0, rates[1], 0.0001)
        assertEquals(0.0, rates[2], 0.0001)

        val invalidClock = dashboardCounterRatePerMinute(
            listOf(sample(at = 20_000L, gossip = 2L), sample(at = 19_000L, gossip = 8L))
        ) { it.gossipResponses }
        assertEquals(0.0, invalidClock.single(), 0.0001)
    }

    @Test
    fun live_healthy_runtime_is_distinct_from_isolated_and_stale_states() {
        val now = 100_000L
        val healthy = ZSilentUiState(
            zokaReady = true,
            nodeStatus = ServiceStatus.Online,
            localRpcOnline = true,
            torEnabled = true,
            torStatus = ServiceStatus.Online,
            localConnectedPeers = 2,
            peers = listOf(
                PeerInfo(peerId = "peer-a", transport = "tor"),
                PeerInfo(peerId = "peer-b", transport = "direct")
            ),
            nodeSyncStatus = NodeSyncStatus(
                status = "caught_up",
                localHeight = 200,
                targetHeight = 200,
                lag = 0,
                connectedPeers = 2,
                minPeers = 1
            ),
            nodeSyncStatusObservedAtMillis = now - 1_000L,
            peerTelemetryObservedAtMillis = now - 1_000L
        )

        val healthySummary = dashboardRuntimeSummary(healthy, now)
        assertEquals(DashboardHealth.Healthy, healthySummary.health)
        assertTrue(healthySummary.alerts.isEmpty())

        val isolated = dashboardRuntimeSummary(healthy.copy(localConnectedPeers = 0, peers = emptyList()), now)
        assertEquals(DashboardHealth.Degraded, isolated.health)
        assertTrue(isolated.alerts.any { it.title == "Local node has no peers" })

        val stale = dashboardRuntimeSummary(
            healthy.copy(
                nodeSyncStatusObservedAtMillis = now - 20_000L,
                peerTelemetryObservedAtMillis = now - 20_000L
            ),
            now
        )
        assertEquals(DashboardHealth.Monitoring, stale.health)
    }

    @Test
    fun current_service_error_drives_attention_while_clean_offline_state_is_idle() {
        val attention = dashboardRuntimeSummary(
            ZSilentUiState(
                zokaReady = true,
                nodeStatus = ServiceStatus.Error,
                torEnabled = false
            ),
            nowMillis = 10_000L
        )
        assertEquals(DashboardHealth.NeedsAttention, attention.health)
        assertTrue(attention.alerts.any { it.title == "Node needs attention" })

        val idle = dashboardRuntimeSummary(
            ZSilentUiState(
                zokaReady = true,
                nodeStatus = ServiceStatus.Offline,
                torEnabled = false
            ),
            nowMillis = 10_000L
        )
        assertEquals(DashboardHealth.Idle, idle.health)
        assertTrue(idle.alerts.isEmpty())
    }

    @Test
    fun transient_poll_failures_are_tolerated_but_third_failure_is_degraded() {
        val now = 50_000L
        val base = healthyState(now)
        assertEquals(
            DashboardHealth.Healthy,
            dashboardRuntimeSummary(base.copy(nodeSyncStatusConsecutiveFailures = 1, peerTelemetryConsecutiveFailures = 2), now).health
        )

        val unavailable = dashboardRuntimeSummary(
            base.copy(nodeSyncStatusConsecutiveFailures = 3, peerTelemetryConsecutiveFailures = 3),
            now
        )
        assertEquals(DashboardHealth.Degraded, unavailable.health)
        assertTrue(unavailable.alerts.any { it.title == "Sync telemetry is stale" })
        assertTrue(unavailable.alerts.any { it.title == "Peer telemetry is stale" })
    }

    @Test
    fun explicit_sync_error_and_tor_only_outage_can_never_be_healthy() {
        val now = 70_000L
        val base = healthyState(now)
        val syncError = dashboardRuntimeSummary(
            base.copy(nodeSyncStatus = base.nodeSyncStatus.copy(status = "error", lag = 0, lastSyncError = null)),
            now
        )
        assertEquals(DashboardHealth.NeedsAttention, syncError.health)
        assertEquals(com.zsilent.core.ui.components.ZTone.Err, dashboardSyncIndicator(syncErrorState(base), now).tone)
        assertEquals("sync error", dashboardSyncIndicator(syncErrorState(base), now).label)

        val torUnavailable = dashboardRuntimeSummary(
            base.copy(torOnly = true, torStatus = ServiceStatus.Offline),
            now
        )
        assertEquals(DashboardHealth.Degraded, torUnavailable.health)
        assertTrue(torUnavailable.alerts.any { it.title == "Tor-only route is unavailable" })
    }

    @Test
    fun unknown_or_offline_sync_status_cannot_be_promoted_by_zero_lag() {
        val now = 75_000L
        listOf("", "unknown", "offline").forEach { status ->
            val summary = dashboardRuntimeSummary(
                healthyState(now).copy(
                    nodeSyncStatus = healthyState(now).nodeSyncStatus.copy(
                        status = status,
                        readyToMine = false,
                        lag = 0
                    )
                ),
                now
            )
            assertEquals(DashboardHealth.Monitoring, summary.health, "status=$status")
        }
    }

    @Test
    fun genesis_is_a_known_height_and_remote_network_stats_never_become_local_height() {
        val now = 80_000L
        val genesis = healthyState(now).copy(
            chainHeight = 0,
            networkHeight = 0,
            nodeSyncStatus = healthyState(now).nodeSyncStatus.copy(
                localHeight = 0,
                targetHeight = 0,
                lag = 0
            )
        )
        assertTrue(dashboardLocalHeightKnown(genesis, now))
        assertTrue(dashboardTargetHeightKnown(genesis, now))
        assertEquals(0L, dashboardLocalHeight(genesis, now))
        assertEquals(0L, dashboardTargetHeight(genesis, now))

        val remoteOnly = ZSilentUiState(
            localRpcOnline = false,
            chainHeight = 0,
            stats = NodeStats(height = 999)
        )
        assertFalse(dashboardLocalHeightKnown(remoteOnly, now))
        assertEquals(0L, dashboardLocalHeight(remoteOnly, now))
    }

    @Test
    fun balance_projection_rejects_another_wallet_and_distinguishes_real_zero() {
        val walletA = LocalWallet(
            name = "wallet-a",
            address = "zka-a",
            balance = "0",
            lockedBalance = "0",
            totalBalance = "0"
        )
        val wrongSelection = dashboardBalancePresentation(
            ZSilentUiState(
                activeWalletName = "wallet-b",
                activeWalletAddress = "zka-b",
                wallets = listOf(walletA)
            )
        )
        assertFalse(wrongSelection.snapshotAvailable)

        val realZero = dashboardBalancePresentation(
            ZSilentUiState(
                activeWalletName = "wallet-a",
                activeWalletAddress = "zka-a",
                wallets = listOf(walletA)
            )
        )
        assertTrue(realZero.snapshotAvailable)
        assertEquals("0", realZero.totalText)
        assertEquals(0L, realZero.totalAtoms)
    }

    @Test
    fun action_policy_blocks_unsafe_transitions_and_names_busy_states() {
        val now = 90_000L
        val base = healthyState(now).copy(miningSyncReady = true)
        assertTrue(dashboardActionPolicy(base).miningEnabled)

        val minerStarting = dashboardActionPolicy(base.copy(miningStatus = ServiceStatus.Starting))
        assertFalse(minerStarting.miningEnabled)
        assertEquals("Starting miner", minerStarting.miningLabel)

        val torStarting = dashboardActionPolicy(base.copy(torStatus = ServiceStatus.Starting))
        assertFalse(torStarting.torEnabled)
        assertEquals("Starting Tor", torStarting.torLabel)

        val noBinaries = dashboardActionPolicy(ZSilentUiState(zokaReady = false, nodeStatus = ServiceStatus.Offline))
        assertFalse(noBinaries.nodeEnabled)

        val externalMiner = dashboardActionPolicy(
            ZSilentUiState(
                zokaReady = true,
                sessionUnlocked = true,
                nodeStatus = ServiceStatus.Offline,
                mining = MiningSnapshot(running = true)
            )
        )
        assertFalse(externalMiner.nodeEnabled)
        assertTrue(externalMiner.nodeReason.orEmpty().contains("external miner"))

        val reconciling = dashboardActionPolicy(base.copy(miningStatus = ServiceStatus.Online, mining = MiningSnapshot(running = false)))
        assertFalse(reconciling.miningEnabled)
        assertEquals("Updating miner", reconciling.miningLabel)

        val onlineRoute = dashboardActionPolicy(base.copy(torEnabled = true))
        assertFalse(onlineRoute.torEnabled)
        assertEquals("Tor route active", onlineRoute.torLabel)

        val fallbackRoute = dashboardActionPolicy(base.copy(torEnabled = true, torStatus = ServiceStatus.Offline))
        assertFalse(fallbackRoute.torEnabled)
        assertEquals("Tor requested · direct fallback", fallbackRoute.torLabel)

        val recoverableError = dashboardActionPolicy(
            base.copy(nodeStatus = ServiceStatus.Error, torStatus = ServiceStatus.Offline)
        )
        assertTrue(recoverableError.torEnabled)
        assertEquals("Use direct on start", recoverableError.torLabel)

        val remoteRpc = dashboardActionPolicy(base, rpcEndpointLocal = false)
        assertFalse(remoteRpc.miningEnabled)
        assertTrue(remoteRpc.miningReason.orEmpty().contains("remote"))

        listOf(ServiceStatus.Online, ServiceStatus.Starting, ServiceStatus.Stopping).forEach { minerStatus ->
            val uncertainOwner = dashboardActionPolicy(
                ZSilentUiState(
                    zokaReady = true,
                    sessionUnlocked = true,
                    nodeStatus = ServiceStatus.Offline,
                    miningStatus = minerStatus,
                    mining = MiningSnapshot(running = false)
                )
            )
            assertFalse(uncertainOwner.nodeEnabled, "miningStatus=$minerStatus")
        }
    }

    @Test
    fun rpc_provenance_changes_labels_without_turning_remote_telemetry_into_an_alert() {
        val now = 95_000L
        val remoteHealthy = dashboardRuntimeSummary(healthyState(now), now, rpcEndpointLocal = false)
        assertEquals(DashboardHealth.Healthy, remoteHealthy.health)
        assertTrue(remoteHealthy.detail.contains("configured RPC"))

        val remoteIsolated = dashboardRuntimeSummary(
            healthyState(now).copy(peers = emptyList(), localConnectedPeers = 0),
            now,
            rpcEndpointLocal = false
        )
        assertTrue(remoteIsolated.alerts.any { it.title == "Configured endpoint has no peers" })
    }

    @Test
    fun wallet_history_must_belong_to_the_resolved_active_wallet() {
        val walletA = LocalWallet(name = "wallet-a", address = "zka-a")
        val walletB = LocalWallet(name = "wallet-b", address = "zka-b")
        val historyA = WalletHistory(
            wallet = "wallet-a",
            count = 1,
            transactions = listOf(WalletHistoryEntry(txHash = "tx-a", status = "confirmed"))
        )

        assertFalse(
            dashboardHistoryMatchesSelection(
                ZSilentUiState(
                    activeWalletAddress = "zka-b",
                    wallets = listOf(walletA, walletB),
                    walletHistory = historyA
                )
            )
        )
        assertFalse(
            dashboardHistoryMatchesSelection(
                ZSilentUiState(wallets = listOf(walletA, walletB), walletHistory = historyA)
            )
        )
        assertTrue(
            dashboardHistoryMatchesSelection(
                ZSilentUiState(activeWalletAddress = "zka-a", wallets = listOf(walletA, walletB), walletHistory = historyA)
            )
        )
    }

    @Test
    fun transaction_status_uses_explicit_states_not_ambiguous_substrings() {
        assertEquals(com.zsilent.core.ui.components.ZTone.On, dashboardTransactionTone("confirmed"))
        assertEquals(com.zsilent.core.ui.components.ZTone.On, dashboardTransactionTone("completed"))
        assertEquals(com.zsilent.core.ui.components.ZTone.Warn, dashboardTransactionTone("unconfirmed"))
        assertEquals(com.zsilent.core.ui.components.ZTone.Warn, dashboardTransactionTone("not completed"))
        assertEquals(com.zsilent.core.ui.components.ZTone.Err, dashboardTransactionTone("cancelled"))
        assertEquals(com.zsilent.core.ui.components.ZTone.Info, dashboardTransactionTone("unknown"))
    }

    private fun healthyState(now: Long): ZSilentUiState = ZSilentUiState(
        zokaReady = true,
        nodeStatus = ServiceStatus.Online,
        localRpcOnline = true,
        torEnabled = true,
        torStatus = ServiceStatus.Online,
        localConnectedPeers = 2,
        peers = listOf(
            PeerInfo(peerId = "peer-a", transport = "tor"),
            PeerInfo(peerId = "peer-b", transport = "direct")
        ),
        nodeSyncStatus = NodeSyncStatus(
            status = "caught_up",
            localHeight = 200,
            targetHeight = 200,
            lag = 0,
            connectedPeers = 2,
            minPeers = 1
        ),
        nodeSyncStatusObservedAtMillis = now - 1_000L,
        peerTelemetryObservedAtMillis = now - 1_000L,
        mining = MiningSnapshot(running = false)
    )

    private fun syncErrorState(base: ZSilentUiState): ZSilentUiState = base.copy(
        nodeSyncStatus = base.nodeSyncStatus.copy(status = "error", lag = 0, lastSyncError = null)
    )

    private fun sample(at: Long, gossip: Long): DashboardTelemetrySample = DashboardTelemetrySample(
        timestampMillis = at,
        localHeight = 100,
        targetHeight = 100,
        localHeightKnown = true,
        targetHeightKnown = true,
        lag = 0,
        peers = 2,
        gossipResponses = gossip,
        syncFresh = true,
        peersFresh = true
    )
}
