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

    /**
     * El reloj del tablero avanza cada 5 s; el dato del nodo llega cada ~3 s.
     * Como 3 no divide a 5, lo normal es que el dato sea MAS NUEVO que el
     * ultimo tic y la resta de negativa.
     *
     * La condicion era `in 0L..VENTANA`, y un negativo queda fuera de ese
     * rango: el tablero declaraba "no disponible" justo cuando el dato era mas
     * fresco que nunca. Simulando las dos cadencias, pasaba el 60 % del
     * tiempo.
     */
    @Test
    fun `un dato mas nuevo que el reloj sigue siendo fresco`() {
        assertTrue(
            dashboardTelemetrySampleFresh(
                localRpcOnline = true,
                observedAtMillis = ahora + 3_000L,
                consecutiveFailures = 0,
                nowMillis = ahora
            ),
            "un dato posterior al ultimo tic del reloj es lo mas fresco que hay, no lo mas viejo"
        )
    }

    /**
     * El caso real completo: reloj cada 5 s, telemetria cada 3 s, durante
     * medio minuto. Antes 18 de esos 30 segundos daban "no disponible" con el
     * nodo respondiendo en milisegundos.
     */
    @Test
    fun `las dos cadencias juntas no producen ni un hueco falso`() {
        val base = ahora
        val huecos = (0 until 30).count { segundo ->
            val ultimoTic = base + (segundo / 5) * 5_000L
            val ultimaConsulta = base + (segundo / 3) * 3_000L
            !dashboardTelemetrySampleFresh(
                localRpcOnline = true,
                observedAtMillis = ultimaConsulta,
                consecutiveFailures = 0,
                nowMillis = ultimoTic
            )
        }
        kotlin.test.assertEquals(
            0,
            huecos,
            "con el nodo sano no puede haber un solo segundo de telemetria declarada muerta"
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
