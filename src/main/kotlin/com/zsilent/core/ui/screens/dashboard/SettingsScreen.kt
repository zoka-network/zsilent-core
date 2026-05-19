package com.zsilent.core.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSectionTitle

@Composable
fun SettingsScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Settings", "App preferences.")
        ZCard {
            Toggle("Hide balances", appState.balancesHidden, appState::updateBalancesHidden)
            Toggle("Full node", appState.localNode, appState::updateLocalNode)
            Toggle("Silent privacy mode", appState.privateMode, appState::updatePrivateMode)
        }
        ZCard {
            Text("Backup")
            Text("Encrypted JSON export will live here.")
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
        Switch(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
