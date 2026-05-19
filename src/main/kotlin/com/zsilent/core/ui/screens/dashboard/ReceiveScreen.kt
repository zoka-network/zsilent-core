package com.zsilent.core.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZQrCard

@Composable
fun ReceiveScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
            ZQrCard(appState.account.address, Modifier.weight(1f))
            ZCard(Modifier.weight(1f)) {
                Text("Receive privately", style = MaterialTheme.typography.titleLarge)
                Text("Share this address or QR code.")
                Text("Incoming funds appear after wallet scan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No private keys are exposed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
