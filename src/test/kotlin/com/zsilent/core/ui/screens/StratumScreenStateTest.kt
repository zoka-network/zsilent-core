package com.zsilent.core.ui.screens

import com.zsilent.core.config.ZokaNetworkConfig
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.models.StratumStatus
import com.zsilent.core.ui.ZSilentUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class StratumScreenStateTest {

    private val online = ZSilentUiState(
        stratumEnabled = true,
        nodeStatus = ServiceStatus.Online,
        localRpcOnline = true,
    )

    // La palabra grande no puede decir que hay mineros cuando el nodo solo
    // escucha, ni que escucha cuando el nodo no abrió el puerto.
    @Test
    fun the_banner_follows_what_the_node_reports() {
        assertEquals(StratumScreenState.Off, stratumScreenState(ZSilentUiState()))
        assertEquals(StratumScreenState.Off, stratumScreenState(online.copy(stratumEnabled = false)))
        assertEquals(StratumScreenState.Waiting, stratumScreenState(online.copy(localRpcOnline = false)))
        assertEquals(StratumScreenState.Waiting, stratumScreenState(online))
        assertEquals(
            StratumScreenState.NotListening,
            stratumScreenState(online.copy(stratum = StratumStatus(listening = null)))
        )
        // Pedido pero todavía sin abrir: arranca, no falló.
        assertEquals(
            StratumScreenState.Starting,
            stratumScreenState(online.copy(stratum = StratumStatus(configured = "0.0.0.0:3333")))
        )
        assertEquals(
            StratumScreenState.NotListening,
            stratumScreenState(
                online.copy(stratum = StratumStatus(configured = "0.0.0.0:3333", error = "port in use"))
            )
        )
        assertEquals(
            StratumScreenState.Ready,
            stratumScreenState(online.copy(stratum = StratumStatus(listening = "127.0.0.1:3333")))
        )
        assertEquals(
            StratumScreenState.Mining,
            stratumScreenState(online.copy(stratum = StratumStatus(listening = "127.0.0.1:3333", miners = 2)))
        )
    }

    @Test
    fun the_xmrig_line_points_at_this_node_with_rx0() {
        assertEquals(
            "xmrig -o 127.0.0.1:3333 -u zqtest1abc -p x -a rx/0",
            xmrigCommandLine("127.0.0.1", "zqtest1abc")
        )
        assertEquals("127.0.0.1:3333", ZokaNetworkConfig.stratumBind(allowLan = false))
        assertEquals("0.0.0.0:3333", ZokaNetworkConfig.stratumBind(allowLan = true))
    }
}
