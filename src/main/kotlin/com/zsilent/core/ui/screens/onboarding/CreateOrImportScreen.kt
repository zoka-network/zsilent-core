package com.zsilent.core.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZGreen
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZSectionTitle
import com.zsilent.core.ui.components.ZTextField

@Composable
fun CreateOrImportScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZSectionTitle("Create or import", "Start fresh or restore an existing Zoka wallet.")
        ZTextField(
            value = appState.walletName,
            onValueChange = appState::updateWalletName,
            label = "Account name",
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZCard(Modifier.weight(1f).clickable { }) {
                Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = ZGreen)
                Text("Create wallet", style = MaterialTheme.typography.titleLarge)
                Text("Generate a new seed phrase.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ZCard(Modifier.weight(1f).clickable { }) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = ZGreen)
                Text("Import wallet", style = MaterialTheme.typography.titleLarge)
                Text("Paste a recovery phrase.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ZTextField(
            value = appState.importedSeed,
            onValueChange = appState::updateImportedSeed,
            label = "Import seed (optional)",
            minLines = 3,
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZSecondaryButton("Back", appState::previousOnboarding)
            ZButton("Continue", appState::nextOnboarding, enabled = appState.walletName.isNotBlank())
        }
    }
}
