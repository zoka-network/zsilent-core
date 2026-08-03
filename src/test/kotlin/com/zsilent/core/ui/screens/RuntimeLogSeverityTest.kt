package com.zsilent.core.ui.screens

import com.zsilent.core.models.LogLevel
import com.zsilent.core.models.RuntimeLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RuntimeLogSeverityTest {
    @Test
    fun preserves_embedded_warn_even_when_later_words_sound_like_errors() {
        val warnings = listOf(
            "2026-08-02T17:03:00.434933Z WARN zoka.iroh: chain serve connection failed error=connection lost",
            "2026-08-02T17:05:27.247318Z WARN noq_udp: sendmsg error: Os { code: 10051, kind: NetworkUnreachable }"
        )

        warnings.forEach { message ->
            assertEquals(LogLevel.Warning, effectiveLogLevel(RuntimeLog("node", message)))
        }
    }

    @Test
    fun respects_an_explicit_runtime_level_before_text_inference() {
        assertEquals(
            LogLevel.Warning,
            effectiveLogLevel(RuntimeLog("node", "operation failed", level = LogLevel.Warning))
        )
        assertEquals(
            LogLevel.Warning,
            effectiveLogLevel(
                RuntimeLog("node", "Node exited unexpectedly (code=1). Auto-restart attempt 1", level = LogLevel.Warning)
            )
        )
        assertEquals(
            LogLevel.Error,
            effectiveLogLevel(
                RuntimeLog("node", "Node exited unexpectedly six times; giving up", level = LogLevel.Error)
            )
        )
    }

    @Test
    fun respects_tor_structured_levels_before_failure_words() {
        val suffix = "Closed 1 streams for service [scrubbed].onion for reason resolve failed. " +
            "Fetch status: No more HSDir available to query."

        assertEquals(
            LogLevel.Info,
            effectiveLogLevel(RuntimeLog("tor", "Aug 02 15:23:22.000 [notice] $suffix"))
        )
        assertEquals(
            LogLevel.Warning,
            effectiveLogLevel(RuntimeLog("tor", "Aug 02 15:23:22.000 [warn] $suffix"))
        )
        assertEquals(
            LogLevel.Error,
            effectiveLogLevel(RuntimeLog("tor", "Aug 02 15:23:22.000 [err] $suffix"))
        )
    }

    @Test
    fun groups_repeated_tor_onion_resolution_notices_without_hiding_them() {
        val logs = (0 until 5).map { index ->
            RuntimeLog(
                source = "tor",
                message = "Aug 02 15:23:2$index.000 [notice] Closed 1 streams for service " +
                    "[scrubbed].onion for reason resolve failed. Fetch status: No more HSDir available to query.",
                timestampMillis = 5_000L - index
            )
        }

        val summaries = summarizeNetworkSignals(logs)
        val tor = summaries.single { it.kind == NetworkSignalKind.TorOnionResolution }
        assertEquals(5, tor.count)
        assertEquals(logs.first(), tor.latest)
        assertEquals(LogLevel.Info, tor.level)
        assertEquals(5, logs.size, "grouping must not delete the full-stream entries")
        assertNull(networkSignalKind(RuntimeLog("node", "DNS resolve failed")))
    }
}
