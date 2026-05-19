package com.zsilent.core.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSectionTitle
import com.zsilent.core.ui.components.ZTextField

@Composable
fun SeedVerifyScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Verify phrase", "Confirm two words from your recovery phrase.")
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZTextField(
                value = appState.seedCheckA,
                onValueChange = appState::updateSeedCheckA,
                label = "Word #3",
                modifier = Modifier.weight(1f)
            )
            ZTextField(
                value = appState.seedCheckB,
                onValueChange = appState::updateSeedCheckB,
                label = "Word #9",
                modifier = Modifier.weight(1f)
            )
        }
        if (appState.seedCheckA.isNotBlank() || appState.seedCheckB.isNotBlank()) {
            Text(if (appState.seedVerified()) "Seed verified." else "Words do not match yet.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("Continue", appState::nextOnboarding, enabled = appState.seedVerified())
        }
    }
}
