package com.zsilent.core.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zsilent.core.app.AppState
import com.zsilent.core.app.Screen
import com.zsilent.core.design.ZGreen
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSecondaryButton

@Composable
fun DashboardScreen(appState: AppState) {
    val account = appState.account
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZCard(Modifier.weight(1.4f)) {
                Text("Total balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (appState.balancesHidden) "••••••" else account.balance,
                    style = MaterialTheme.typography.displaySmall
                )
                Text("Locked ${account.lockedBalance}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ZCard(Modifier.weight(1f)) {
                Text("Privacy status", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.sm)) {
                    Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = ZGreen)
                    Text(account.privacyMode.label)
                }
                Text("Balances stay private", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        ZCard {
            Text("Quick actions", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
                ZButton("Send", onClick = { appState.navigate(Screen.Send) })
                ZSecondaryButton("Receive", onClick = { appState.navigate(Screen.Receive) })
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            ZCard(Modifier.weight(1f)) {
                Text("Recent activity", style = MaterialTheme.typography.titleLarge)
                appState.activity.forEach {
                    Text("${it.title}  ${it.amount}")
                    Text(it.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ZCard(Modifier.weight(1f)) {
                Text("Network status", style = MaterialTheme.typography.titleLarge)
                Text(appState.network.node)
                Text("Height ${appState.network.height}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Peers ${appState.network.peers} · Sync ${appState.network.sync}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
