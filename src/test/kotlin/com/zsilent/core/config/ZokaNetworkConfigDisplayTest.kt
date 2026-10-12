package com.zsilent.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZokaNetworkConfigDisplayTest {

    // La cadena Zenith paso a ser la testnet de Zoka Quantic y ZSilent se
    // presenta como cliente de Quantic, pero su chain id y la palabra que se le
    // pasa al nodo siguen diciendo "mainnet". Lo que ve la persona tiene que
    // decir testnet igual: el titulo es lo primero que se mira para saber si lo
    // que hay en pantalla es dinero.
    @Test
    fun the_current_chain_is_shown_as_the_quantic_testnet_even_though_the_cli_token_says_mainnet() {
        val chainId = ZokaNetworkConfig.ZENITH_CHAIN_ID

        assertEquals("Zoka Quantic · Testnet", ZokaNetworkConfig.displayName(chainId, "mainnet"))
        assertEquals("quantic testnet", ZokaNetworkConfig.shortName(chainId, "mainnet"))
        assertTrue(ZokaNetworkConfig.isTestnet(chainId, "mainnet"))
    }

    @Test
    fun before_the_manifest_loads_the_default_chain_is_still_the_testnet() {
        assertEquals("Zoka Quantic · Testnet", ZokaNetworkConfig.displayName("", ""))
        assertEquals("quantic testnet", ZokaNetworkConfig.shortName("", ""))
        assertTrue(ZokaNetworkConfig.isTestnet("", ""))
    }

    // Lugar reservado para la mainnet de Zoka Quantic: una cadena con otro chain
    // id que no sea de prueba no se puede presentar como testnet.
    @Test
    fun another_chain_is_not_mislabeled_as_the_testnet() {
        assertEquals("Mainnet", ZokaNetworkConfig.displayName("zoka-quantic-v1", "mainnet"))
        assertEquals("mainnet", ZokaNetworkConfig.shortName("zoka-quantic-v1", "mainnet"))
        assertFalse(ZokaNetworkConfig.isTestnet("zoka-quantic-v1", "mainnet"))
        assertEquals("Testnet", ZokaNetworkConfig.displayName("otra-testnet", "testnet"))
    }
}
