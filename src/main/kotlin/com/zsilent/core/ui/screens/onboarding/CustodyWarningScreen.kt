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
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSectionTitle
import com.zsilent.core.ui.components.ZWarningBox

@Composable
fun CustodyWarningScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Self-custody", "Your wallet password protects this device. Your seed restores ownership.")
        ZWarningBox(
            title = "No recovery service",
            text = "If you lose both seed and backup, nobody can restore your funds."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            Switch(checked = true, onCheckedChange = {})
            Text("I understand that ZSilent cannot reset my wallet password or seed.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("Continue", appState::nextOnboarding)
        }
    }
}
