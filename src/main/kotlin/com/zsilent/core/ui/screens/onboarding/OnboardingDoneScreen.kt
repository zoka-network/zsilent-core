package com.zsilent.core.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSectionTitle

@Composable
fun OnboardingDoneScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Wallet ready", "Your encrypted wallet has been prepared.")
        ZCard {
            Text("Dashboard: balances, privacy and network state.")
            Text("Send: private transfer review before signing.")
            Text("Receive: QR and address sharing.")
            Text("Settings: backups, node and security.")
        }
        ZButton("Open dashboard", appState::finishOnboarding)
    }
}
