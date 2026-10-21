package com.zsilent.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZokaNetworkConfigDisplayTest {

    // Desde la 3.0 ZSilent es solo de Zoka Quantic. El título es lo primero que
    // se mira para saber si lo que hay en pantalla es dinero: la testnet tiene
    // que decir testnet.
    @Test
    fun the_quantic_testnet_is_shown_as_a_testnet_and_runs_as_quantic_testnet() {
        val chainId = ZokaNetworkConfig.QUANTIC_TESTNET_CHAIN_ID

        assertEquals("Zoka Quantic · Testnet", ZokaNetworkConfig.displayName(chainId, "quantic-testnet"))
        assertEquals("quantic testnet", ZokaNetworkConfig.shortName(chainId, "quantic-testnet"))
        assertTrue(ZokaNetworkConfig.isTestnet(chainId, "quantic-testnet"))
        assertEquals("quantic-testnet", ZokaNetworkConfig.networkForChain(chainId))
        assertEquals("zqtest1", ZokaNetworkConfig.addressPrefix("quantic-testnet"))
    }

    @Test
    fun before_the_manifest_loads_the_default_chain_is_the_testnet() {
        assertEquals("Zoka Quantic · Testnet", ZokaNetworkConfig.displayName("", ""))
        assertEquals("quantic testnet", ZokaNetworkConfig.shortName("", ""))
        assertTrue(ZokaNetworkConfig.isTestnet("", ""))
        assertEquals("quantic-testnet", ZokaNetworkConfig.networkForChain(""))
        assertEquals("zqtest1", ZokaNetworkConfig.addressPrefix(""))
    }

    // Solo la cadena de la mainnet se presenta como dinero; Zenith, que ZSilent
    // ya no sigue, tampoco.
    @Test
    fun only_the_mainnet_chain_is_not_a_testnet() {
        val mainnet = ZokaNetworkConfig.QUANTIC_MAINNET_CHAIN_ID
        assertEquals("Zoka Quantic", ZokaNetworkConfig.displayName(mainnet, "quantic-mainnet"))
        assertEquals("quantic", ZokaNetworkConfig.shortName(mainnet, "quantic-mainnet"))
        assertFalse(ZokaNetworkConfig.isTestnet(mainnet, "quantic-mainnet"))
        assertEquals("quantic-mainnet", ZokaNetworkConfig.networkForChain(mainnet))
        assertEquals("zq1", ZokaNetworkConfig.addressPrefix("quantic-mainnet"))
        assertTrue(ZokaNetworkConfig.isTestnet(ZokaNetworkConfig.ZENITH_CHAIN_ID, "mainnet"))
        assertEquals("zqregtest1", ZokaNetworkConfig.addressPrefix("quantic-regtest"))
    }
}
