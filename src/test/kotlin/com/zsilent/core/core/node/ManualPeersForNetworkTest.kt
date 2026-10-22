package com.zsilent.core.core.node

import kotlin.test.Test
import kotlin.test.assertEquals

class ManualPeersForNetworkTest {
    // El 22/10/2026 el nodo de la testnet de Quantic marcaba dos onion de
    // Zenith (28080) que venían guardadas del setup de la 2.1. Las redes no se
    // mezclan: a cada nodo solo le llegan los pares de su puerto.
    @Test
    fun only_the_peers_on_this_networks_port_reach_its_node() {
        val zenithOnion = "/onion3/" + "a".repeat(56) + ":28080"
        val testnetOnion = "/onion3/" + "b".repeat(56) + ":28180"
        val testnetIp = "/ip4/192.168.1.20/tcp/28180/p2p/12D3KooWQ9BXbCWQVXyUxR9wV4TgBq2ueeWaG1spopUhND7t6fPy"
        val zenithIp = "/ip4/192.168.1.20/tcp/28080"
        val mainnetDns = "/dns4/seed.example/tcp/28380"
        val saved = listOf(zenithOnion, "  $testnetOnion  ", testnetIp, zenithIp, mainnetDns, "   ")

        assertEquals(listOf(testnetOnion, testnetIp), manualPeersForNetwork(saved, "quantic-testnet"))
        assertEquals(listOf(mainnetDns), manualPeersForNetwork(saved, "quantic-mainnet"))
    }
}
