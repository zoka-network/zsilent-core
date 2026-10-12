package com.zsilent.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZokaNetworkConfigDisplayTest {

    // Zenith paso a ser la testnet de Zoka Quantic, pero su chain id y la
    // palabra que se le pasa al nodo siguen diciendo "mainnet". Lo que ve la
    // persona tiene que decir testnet igual: el titulo es lo primero que se
    // mira para saber si lo que hay en pantalla es dinero.
    @Test
    fun zenith_is_shown_as_testnet_even_though_the_cli_token_says_mainnet() {
        val chainId = ZokaNetworkConfig.ZENITH_CHAIN_ID

        assertEquals("Zenith testnet", ZokaNetworkConfig.displayName(chainId, "mainnet"))
        assertTrue(ZokaNetworkConfig.isTestnet(chainId, "mainnet"))
    }

    @Test
    fun before_the_manifest_loads_the_default_chain_is_still_zenith() {
        assertEquals("Zenith testnet", ZokaNetworkConfig.displayName("", ""))
        assertTrue(ZokaNetworkConfig.isTestnet("", ""))
    }

    // Lugar reservado para Zoka Quantic: una cadena con otro chain id que no
    // sea de prueba no se puede presentar como testnet.
    @Test
    fun another_chain_is_not_mislabeled_as_zenith() {
        assertEquals("Mainnet", ZokaNetworkConfig.displayName("zoka-quantic-v1", "mainnet"))
        assertFalse(ZokaNetworkConfig.isTestnet("zoka-quantic-v1", "mainnet"))
        assertEquals("Testnet", ZokaNetworkConfig.displayName("otra-testnet", "testnet"))
    }
}
