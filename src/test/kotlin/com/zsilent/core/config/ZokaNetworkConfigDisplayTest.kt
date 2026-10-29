package com.zsilent.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZokaNetworkConfigDisplayTest {

    // Desde la 3.0 ZSilent es solo de Zoka Quantic, y desde la 4.0 de la red
    // STARK v2. El título es lo primero que se mira para saber si lo que hay en
    // pantalla es dinero: la testnet tiene que decir testnet.
    @Test
    fun the_quantic_testnet_is_shown_as_a_testnet_and_runs_as_the_v2_testnet() {
        val chainId = ZokaNetworkConfig.QUANTIC_TESTNET_CHAIN_ID

        assertEquals("zoka-quantic-testnet-v2", chainId)
        assertEquals("Zoka Quantic · Testnet", ZokaNetworkConfig.displayName(chainId, "testnet"))
        assertEquals("quantic testnet", ZokaNetworkConfig.shortName(chainId, "testnet"))
        assertTrue(ZokaNetworkConfig.isTestnet(chainId, "testnet"))
        assertEquals("testnet", ZokaNetworkConfig.networkForChain(chainId))
        assertEquals("zqtest2", ZokaNetworkConfig.addressPrefix("testnet"))
        // Su carpeta no es la de la v1 (`.zoka/quantic-testnet`): conviven.
        assertEquals(".zoka/quantic-testnet-v2", ZokaNetworkConfig.dataDir("testnet"))
    }

    @Test
    fun before_the_manifest_loads_the_default_chain_is_the_testnet() {
        assertEquals("Zoka Quantic · Testnet", ZokaNetworkConfig.displayName("", ""))
        assertEquals("quantic testnet", ZokaNetworkConfig.shortName("", ""))
        assertTrue(ZokaNetworkConfig.isTestnet("", ""))
        assertEquals("testnet", ZokaNetworkConfig.networkForChain(""))
        assertEquals("zqtest2", ZokaNetworkConfig.addressPrefix(""))
        assertEquals("http://127.0.0.1:9140", ZokaNetworkConfig.LocalRpcUrl)
    }

    // El manifiesto de la v2 no trae `desktop.localRpcUrl`, y el cliente del
    // nodo local se arma con la URL de la configuración cargada, no con
    // `LocalRpcUrl`: sin la caída a 9140 quedaba vacía y la app nunca leía
    // /status ni refrescaba el saldo (4.0.0 y 4.0.1).
    @Test
    fun a_manifest_without_a_desktop_section_still_reaches_the_local_node() {
        val root = kotlin.io.path.createTempDirectory("zsilent-manifest").toFile()
        try {
            java.io.File(root, "docs").mkdirs()
            java.io.File(root, "docs/quantic-testnet-v2-network.v1.json").writeText(
                """{"schema":"zoka-quantic-stark-network-v1","status":"active",""" +
                    """"chain_id":"zoka-quantic-testnet-v2","seed_nodes":[]}"""
            )
            assertEquals("http://127.0.0.1:9140", ZokaNetworkConfig.load(root).localRpcUrl)
        } finally {
            root.deleteRecursively()
        }
    }

    // Solo la cadena de la mainnet se presenta como dinero; Zenith, que ZSilent
    // ya no sigue, tampoco.
    @Test
    fun only_the_mainnet_chain_is_not_a_testnet() {
        val mainnet = ZokaNetworkConfig.QUANTIC_MAINNET_CHAIN_ID
        assertEquals("Zoka Quantic · Mainnet", ZokaNetworkConfig.displayName(mainnet, "mainnet"))
        assertEquals("quantic mainnet", ZokaNetworkConfig.shortName(mainnet, "mainnet"))
        assertFalse(ZokaNetworkConfig.isTestnet(mainnet, "mainnet"))
        assertEquals("mainnet", ZokaNetworkConfig.networkForChain(mainnet))
        assertEquals("zq2", ZokaNetworkConfig.addressPrefix("mainnet"))
        assertTrue(ZokaNetworkConfig.isTestnet(ZokaNetworkConfig.ZENITH_CHAIN_ID, "mainnet"))
        assertEquals("zqregtest2", ZokaNetworkConfig.addressPrefix("regtest"))
        assertEquals("regtest", ZokaNetworkConfig.networkForChain(ZokaNetworkConfig.QUANTIC_REGTEST_CHAIN_ID))
    }
}
