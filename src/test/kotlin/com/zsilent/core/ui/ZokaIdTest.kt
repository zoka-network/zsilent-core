package com.zsilent.core.ui

import com.zsilent.core.ui.components.ZokaId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Los vectores de docs/ZOKA-ID.md del nodo: los mismos que prueban ZKAPriv, la web y ZokaScan. */
class ZokaIdTest {
    @Test
    fun codeAndMosaicMatchTheSpecVectors() {
        val vectors = listOf(
            Triple(
                "public-account:alice",
                "F4A4-595C-AF1B-A80B-914F",
                "432401304002143331324404132221311134034044240240320441133232224123324403241422013",
            ),
            Triple(
                "zqtest2ZokaIdExampleShortAddressForTheSpecVectors0000000",
                "7E6B-EFD1-09F3-E0AC-979C",
                "344213322211120313430011324432441024221440333433223012304030433043022104420412341",
            ),
        )
        for ((short, code, mosaic) in vectors) {
            assertEquals(code, ZokaId.code(short))
            assertEquals(mosaic, ZokaId.cells(short)!!.joinToString(""))
        }
    }

    @Test
    fun anInputThatIsNotAnAddressHasNoId() {
        assertNull(ZokaId.code(""))
        assertNull(ZokaId.code("bad id"))
        assertNull(ZokaId.cells("x".repeat(257)))
    }
}
