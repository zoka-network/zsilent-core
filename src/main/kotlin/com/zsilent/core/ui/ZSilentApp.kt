package com.zsilent.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zsilent.core.models.AppSection
import com.zsilent.core.models.ServiceStatus
import com.zsilent.core.ui.screens.DashboardScreen
import com.zsilent.core.ui.screens.ExplorerScreen
import com.zsilent.core.ui.screens.LoginScreen
import com.zsilent.core.ui.screens.LogsScreen
import com.zsilent.core.ui.screens.MiningScreen
import com.zsilent.core.ui.screens.NodeScreen
import com.zsilent.core.ui.screens.OnboardingScreen
import com.zsilent.core.ui.screens.PrivacyScreen
import com.zsilent.core.ui.screens.ReceiveScreen
import com.zsilent.core.ui.screens.SendScreen
import com.zsilent.core.ui.screens.SettingsScreen
import com.zsilent.core.ui.screens.WalletScreen

@Composable
fun ZSilentApp(appState: ZSilentAppState) {
    val uiState by appState.state.collectAsState()

    if (!uiState.onboardingComplete) {
        OnboardingScreen(uiState, appState)
        return
    }

    if (uiState.appLocked) {
        LoginScreen(uiState, appState)
        return
    }

    Row(Modifier.fillMaxSize()) {
        Sidebar(
            selected = uiState.selectedSection,
            onSelect = appState::select,
            uiState = uiState,
            onCloseSession = { appState.lockApp("Session closed.") }
        )

        when (uiState.selectedSection) {
            AppSection.Dashboard -> DashboardScreen(uiState, appState)
            AppSection.Wallet -> WalletScreen(uiState, appState)
            AppSection.Send -> SendScreen(uiState, appState)
            AppSection.Receive -> ReceiveScreen(uiState, appState)
            AppSection.Node -> NodeScreen(uiState, appState)
            AppSection.Mining -> MiningScreen(uiState, appState)
            AppSection.Privacy -> PrivacyScreen(uiState, appState)
            AppSection.Logs -> LogsScreen(uiState)
            AppSection.Explorer -> ExplorerScreen(uiState, appState)
            AppSection.Settings -> SettingsScreen(uiState, appState)
        }
    }
}

@Composable
private fun Sidebar(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
    uiState: ZSilentUiState,
    onCloseSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.small)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Z", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(
                    "zsilentcore",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    "Zoka private wallet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AppSection.entries.forEach { section ->
            val item = section.navItem()
            SidebarItem(
                selected = selected == section,
                icon = item.icon,
                label = item.label,
                onClick = { onSelect(section) }
            )
        }
        Spacer(Modifier.weight(1f))
        val runtimeActive = uiState.nodeStatus == ServiceStatus.Starting ||
            uiState.nodeStatus == ServiceStatus.Online ||
            uiState.miningStatus == ServiceStatus.Starting ||
            uiState.miningStatus == ServiceStatus.Online ||
            uiState.mining.running
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    MaterialTheme.shapes.medium
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onCloseSession,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Close session")
            }
            Text(
                if (runtimeActive) "Closing the wallet session stops node and mining processes." else "Wallet access will require the password again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun SidebarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val lightMode = MaterialTheme.colorScheme.background == Color.White
    val container = when {
        lightMode -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val content = when {
        lightMode -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        lightMode && selected -> MaterialTheme.colorScheme.secondary
        lightMode -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.outline
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .border(
                BorderStroke(1.dp, borderColor),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content)
        Text(label, color = content, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun AppSection.navItem(): NavItem = when (this) {
    AppSection.Dashboard -> NavItem("Dashboard", Icons.Outlined.Dashboard)
    AppSection.Wallet -> NavItem("Wallet", Icons.Outlined.AccountBalanceWallet)
    AppSection.Send -> NavItem("Send", Icons.AutoMirrored.Outlined.Send)
    AppSection.Receive -> NavItem("Receive", Icons.Outlined.QrCode)
    AppSection.Node -> NavItem("Node", Icons.Outlined.Memory)
    AppSection.Mining -> NavItem("Mining", Icons.Outlined.Bolt)
    AppSection.Privacy -> NavItem("Privacy", Icons.Outlined.Security)
    AppSection.Logs -> NavItem("Logs", Icons.Outlined.Terminal)
    AppSection.Explorer -> NavItem("Explorer", Icons.Outlined.Explore)
    AppSection.Settings -> NavItem("Settings", Icons.Outlined.Settings)
}
