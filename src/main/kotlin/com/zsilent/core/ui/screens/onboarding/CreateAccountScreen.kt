package com.zsilent.core.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.CopyableKeyValueRow
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSectionTitle

@Composable
fun CreateAccountScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Create account", "Your encrypted account is ready to initialize.")
        ZCard {
            Text(appState.walletName)
            CopyableKeyValueRow("Address", appState.account.address)
            Text("Seed verified")
            Text("Wallet password configured")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("Create account", appState::nextOnboarding)
        }
    }
}
