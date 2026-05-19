package com.zsilent.core.ui.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Shield
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

@Composable
fun WelcomeScreen(appState: AppState) {
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        Text("ZSilent Core", style = MaterialTheme.typography.displaySmall)
        Text(
            "Private wallet, full node and mining control for Zoka.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            FeatureCard("Private keys", "Device secured", Icons.Outlined.Lock, Modifier.weight(1f))
            FeatureCard("Full node", "Run from desktop", Icons.Outlined.Memory, Modifier.weight(1f))
            FeatureCard("Privacy", "No telemetry", Icons.Outlined.Shield, Modifier.weight(1f))
        }
        ZButton("Start setup", onClick = appState::nextOnboarding)
    }
}

@Composable
private fun FeatureCard(
    title: String,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    ZCard(modifier) {
        Icon(icon, contentDescription = null, tint = ZGreen)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
