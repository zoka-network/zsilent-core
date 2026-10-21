package com.zsilent.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * La gente comparte direcciones onion por chat de formas muy distintas: el
 * multiaddr completo, la dirección cruda, con `.onion`, sin puerto, en
 * mayúsculas. Aceptar solo una forma haría que la mitad de los intentos
 * fallaran sin motivo aparente; aceptar cualquier cosa guardaría basura que el
 * nodo marcaría en vano para siempre.
 */
class ManualPeerNormalizationTest {

    private fun normalizar(entrada: String): String? = normalizeManualPeer(entrada)

    @Test
    fun `acepta un multiaddr ya formado`() {
        val addr = "/onion3/" + "a".repeat(56) + ":28080"
        assertEquals(addr, normalizar(addr))
    }

    @Test
    fun `acepta la direccion cruda con y sin sufijo onion`() {
        val host = "b".repeat(56)
        // Sin puerto, el P2P de la testnet de Quantic.
        val esperado = "/onion3/$host:28180"
        assertEquals(esperado, normalizar("$host.onion:28180"))
        assertEquals(esperado, normalizar("$host:28180"))
        assertEquals(esperado, normalizar(host))
    }

    @Test
    fun `normaliza mayusculas y espacios`() {
        val host = "c".repeat(56)
        assertEquals("/onion3/$host:28180", normalizar("  ${host.uppercase()}.onion  "))
    }

    @Test
    fun `respeta un puerto distinto`() {
        val host = "d".repeat(56)
        assertEquals("/onion3/$host:9999", normalizar("$host.onion:9999"))
    }

    @Test
    fun `rechaza lo que no es una direccion onion`() {
        assertNull(normalizar(""))
        assertNull(normalizar("   "))
        assertNull(normalizar("no-es-una-direccion"))
        // Una onion v3 son exactamente 56 caracteres: 55 no alcanza.
        assertNull(normalizar("e".repeat(55) + ".onion"))
    }
}
