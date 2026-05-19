package com.zsilent.core.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSectionTitle

@Composable
fun PrivacyModeScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Privacy mode", "Choose privacy defaults for the first session.")
        ZCard {
            Toggle("Hide balances", appState.balancesHidden, appState::updateBalancesHidden)
            Toggle("Use full node", appState.localNode, appState::updateLocalNode)
            Toggle("Silent privacy mode", appState.privateMode, appState::updatePrivateMode)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("Continue", appState::nextOnboarding)
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
