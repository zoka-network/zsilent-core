package com.zsilent.core.ui.screens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El tablero titilaba: el gráfico se vaciaba y las tarjetas pasaban a "last
 * known" durante un instante, una y otra vez.
 *
 * La causa era que la frescura de las muestras exigía cero fallos seguidos,
 * así que **una sola** lectura tardía declaraba muerta la telemetría. Contra un
 * nodo que responde por Tor eso pasa a cada rato.
 */
class DashboardFlickerTest {

    private val ahora = 1_700_000_000_000L
    private val recien = ahora - 2_000L

    @Test
    fun `un tropiezo aislado no apaga la telemetria`() {
        assertTrue(
            dashboardTelemetrySampleFresh(
                localRpcOnline = true,
                observedAtMillis = recien,
                consecutiveFailures = 1,
                nowMillis = ahora
            ),
            "un solo fallo no puede vaciar el grafico"
        )
    }

    @Test
    fun `varios fallos seguidos si la apagan`() {
        assertFalse(
            dashboardTelemetrySampleFresh(
                localRpcOnline = true,
                observedAtMillis = recien,
                consecutiveFailures = 3,
                nowMillis = ahora
            ),
            "tres fallos seguidos ya no son un tropiezo"
        )
    }

    @Test
    fun `una lectura vieja se descarta aunque no haya fallado`() {
        assertFalse(
            dashboardTelemetrySampleFresh(
                localRpcOnline = true,
                observedAtMillis = ahora - 60_000L,
                consecutiveFailures = 0,
                nowMillis = ahora
            ),
            "pasada la ventana ya no es telemetria viva"
        )
    }

    @Test
    fun `con el nodo apagado no hay telemetria`() {
        assertFalse(
            dashboardTelemetrySampleFresh(
                localRpcOnline = false,
                observedAtMillis = recien,
                consecutiveFailures = 0,
                nowMillis = ahora
            )
        )
    }
}
