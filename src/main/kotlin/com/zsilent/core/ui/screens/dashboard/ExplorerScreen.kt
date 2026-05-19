package com.zsilent.core.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSectionTitle

@Composable
fun ExplorerScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Explorer", "Clean local view of network state.")
        ZCard {
            Text("Latest block", style = MaterialTheme.typography.titleLarge)
            Text("#${appState.network.height}")
            Text("Node: ${appState.network.node}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ZCard {
            Text("Search")
            Text("Block, transaction and address search will live here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
