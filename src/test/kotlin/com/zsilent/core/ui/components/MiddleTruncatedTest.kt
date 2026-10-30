package com.zsilent.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class MiddleTruncatedTest {
    @Test
    fun long_values_keep_their_start_and_end() {
        val key = "zqtestview2EB67sAsQA42WgVACjWB4X2fng2wr7iEQK3DHyrC89fTgHwuUBwBeD41Fmi8ZRrNzXbhencg1YTWrTLPPmdgtNNhh3ppGnEi1phWFSqy6fkiDPm4AL"
        assertEquals("zqtestview2EB67sAs…qy6fkiDPm4AL", middleTruncated(key))
        // Short values are shown whole.
        assertEquals("zqtest22m1iDEY", middleTruncated("zqtest22m1iDEY"))
        assertEquals("a".repeat(31), middleTruncated("a".repeat(31)))
        assertEquals("a".repeat(18) + "…" + "b".repeat(12), middleTruncated("a".repeat(18) + "c" + "b".repeat(13)))
    }
}
