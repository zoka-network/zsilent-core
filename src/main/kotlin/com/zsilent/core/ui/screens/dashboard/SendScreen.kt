package com.zsilent.core.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.zsilent.core.app.AppState
import com.zsilent.core.design.ZSpacing
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZCard
import com.zsilent.core.ui.components.ZSecondaryButton
import com.zsilent.core.ui.components.ZTextField
import com.zsilent.core.wallet.PrivacyMode

@Composable
fun SendScreen(appState: AppState) {
    val draft = appState.sendDraft
    Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.lg)) {
        ZCard {
            Text("Private send", style = MaterialTheme.typography.titleLarge)
            ZTextField(
                value = draft.recipient,
                onValueChange = { appState.updateSendDraft(recipient = it) },
                label = "Recipient address",
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
                ZTextField(
                    value = draft.amount,
                    onValueChange = { appState.updateSendDraft(amount = it) },
                    label = "Amount",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                ZTextField(
                    value = draft.fee,
                    onValueChange = { appState.updateSendDraft(fee = it) },
                    label = "Fee",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.sm)) {
                PrivacyMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.privacyMode == mode,
                        onClick = { appState.updateSendDraft(privacyMode = mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
            ZButton(
                text = "Review transfer",
                onClick = { appState.updateSendDraft(confirmOpen = true) },
                enabled = draft.recipient.startsWith("zka1") && draft.amount.isNotBlank()
            )
        }

        if (draft.confirmOpen) {
            ZCard {
                Text("Confirm", style = MaterialTheme.typography.titleLarge)
                Text("To ${draft.recipient.take(18)}...")
                Text("Amount ${draft.amount} ZKA")
                Text("Fee ${draft.fee}")
                Text("Mode ${draft.privacyMode.label}")
                Row(horizontalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
                    ZSecondaryButton("Cancel", onClick = { appState.updateSendDraft(confirmOpen = false) })
                    ZButton("Sign on device", onClick = { appState.updateSendDraft(confirmOpen = false) })
                }
            }
        }
    }
}
