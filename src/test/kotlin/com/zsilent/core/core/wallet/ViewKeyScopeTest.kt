package com.zsilent.core.core.wallet

import com.zsilent.core.core.privacy.PrivacyGuards
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewKeyScopeTest {
    @Test
    fun incoming_export_never_accepts_a_full_key_or_an_older_nodes_reply() {
        assertFailsWith<IllegalArgumentException> { ViewKeyScope.Incoming.validateExport("full", "zqtestview2fixture") }
        assertFailsWith<IllegalArgumentException> { ViewKeyScope.Incoming.validateExport("", "zqtestview2fixture") }
        assertFailsWith<IllegalArgumentException> { ViewKeyScope.Incoming.validateExport("incoming", "zqtestview2fixture") }
        assertEquals("zqtestviewin2fixture", ViewKeyScope.Incoming.validateExport("incoming", "zqtestviewin2fixture"))
    }

    @Test
    fun full_export_keeps_compatibility_but_does_not_mislabel_incoming() {
        assertEquals("zqregtestview2fixture", ViewKeyScope.Full.validateExport("", "zqregtestview2fixture"))
        assertFailsWith<IllegalArgumentException> { ViewKeyScope.Full.validateExport("full", "zqregtestviewin2fixture") }
    }

    @Test
    fun both_key_types_are_redacted_without_needing_a_field_name() {
        for (prefix in listOf("zq", "zqtest", "zqregtest")) {
            for (kind in listOf("view2", "viewin2")) {
                assertEquals("[redacted sensitive data]", PrivacyGuards.redactForLog("Unexpected $prefix${kind}fixture"))
            }
        }
    }
}
