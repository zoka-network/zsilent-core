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
import com.zsilent.core.ui.components.ZPasswordField
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSectionTitle
import com.zsilent.core.ui.components.ZWarningBox

@Composable
fun PasswordScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Wallet password", "This password unlocks the wallet on this computer.")
        ZWarningBox(
            title = "Password is device-only",
            text = "It does not replace the recovery phrase. Keep both safe."
        )
        ZPasswordField(
            value = appState.password,
            onValueChange = appState::updatePassword,
            label = "Password",
            modifier = Modifier.fillMaxWidth()
        )
        ZPasswordField(
            value = appState.passwordConfirm,
            onValueChange = appState::updatePasswordConfirm,
            label = "Confirm password",
            modifier = Modifier.fillMaxWidth()
        )
        if (appState.password.isNotBlank()) {
            Text(if (appState.passwordValid()) "Password ready." else "Use 8+ characters and match both fields.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("Continue", appState::nextOnboarding, enabled = appState.passwordValid())
        }
    }
}
