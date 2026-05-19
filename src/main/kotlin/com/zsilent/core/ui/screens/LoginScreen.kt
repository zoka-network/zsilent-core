package com.zsilent.core.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.onboarding.DangerButton
import com.zsilent.core.ui.onboarding.NoirBackground
import com.zsilent.core.ui.onboarding.NoirCard
import com.zsilent.core.ui.onboarding.NoirPasswordField
import com.zsilent.core.ui.onboarding.PrimaryButton
import com.zsilent.core.ui.onboarding.ResponsibilityCheck
import com.zsilent.core.ui.onboarding.ScreenBody
import com.zsilent.core.ui.onboarding.ScreenEyebrow
import com.zsilent.core.ui.onboarding.ScreenTitle
import com.zsilent.core.ui.onboarding.SecondaryButton
import com.zsilent.core.ui.onboarding.SecurityWarningCard

private val NoirText = Color(0xFFEAEDF2)
private val NoirTextMuted = Color(0xFF858C99)
private val NoirTextSubtle = Color(0xFF4E5563)
private val NoirSurfaceElev = Color(0xFF11151B)
private val NoirOutline = Color(0xFF1E242C)
private val NoirOutlineStrong = Color(0xFF2A3140)
private val NoirAccent = Color(0xFFC8CDD7)
private val NoirError = Color(0xFFE05757)
private val NoirSuccess = Color(0xFF4FB585)
private val NoirInk = Color(0xFF05070A)

@Composable
fun LoginScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var password by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }

    if (showReset) {
        ResetWalletScreen(
            onCancel = { showReset = false },
            onConfirm = {
                password = ""
                appState.resetSetup()
                showReset = false
            }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        NoirBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(NoirSurfaceElev, RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, NoirOutlineStrong), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = NoirAccent, modifier = Modifier.size(22.dp))
                }
                Text("ZSILENT WALLET", color = NoirText, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, fontSize = 13.sp)
                Text("Unlock your local encrypted wallet.", color = NoirTextMuted, fontSize = 12.sp)
            }

            NoirCard(modifier = Modifier.widthIn(min = 460.dp, max = 540.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ScreenEyebrow("Session")
                    Text(
                        ui.activeWalletName.ifBlank { ui.walletName.ifBlank { "main" } },
                        color = NoirText,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp
                    )
                    ui.activeWalletAddress.takeIf { it.isNotBlank() }?.let { addr ->
                        Text(addr, color = NoirTextSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    ui.lockMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                        SecurityWarningCard(
                            title = "Locked",
                            detail = msg,
                            accent = NoirTextMuted
                        )
                    }

                    NoirPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Wallet password",
                        modifier = Modifier.fillMaxWidth(),
                        isError = ui.loginError != null
                    )

                    ui.loginError?.let { err ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = NoirError, modifier = Modifier.size(14.dp))
                            Text(err, color = NoirError, fontSize = 12.sp)
                        }
                        if (attempts >= 3) {
                            Text(
                                "Multiple failed attempts. Make sure the keyboard layout matches the one you used during setup.",
                                color = NoirTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PrimaryButton(
                            label = if (ui.loginBusy) "Unlocking..." else "Unlock",
                            onClick = {
                                if (password.isNotBlank() && !ui.loginBusy) {
                                    attempts += 1
                                    appState.login(password.toCharArray())
                                    password = ""
                                }
                            },
                            enabled = password.isNotBlank() && !ui.loginBusy,
                            loading = ui.loginBusy,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecondaryLink("Restore from recovery phrase") {
                            password = ""
                            appState.resetSetup()
                        }
                        SecondaryLink("Reset wallet", danger = true) { showReset = true }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusDot(label = if (ui.rpcOnline) "Network reachable" else "Network unreachable", ok = ui.rpcOnline)
            }
        }
    }
}

@Composable
private fun ResetWalletScreen(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var ack by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    val canReset = ack && confirmText.trim().equals("RESET", ignoreCase = false)

    Box(Modifier.fillMaxSize()) {
        NoirBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NoirCard(modifier = Modifier.widthIn(min = 500.dp, max = 640.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ScreenEyebrow("Reset wallet")
                    ScreenTitle("This removes the local encrypted wallet.")
                    ScreenBody(
                        "Resetting removes the encrypted wallet from this device. It does not delete funds " +
                            "from the blockchain. Only your recovery phrase can restore access afterwards."
                    )

                    SecurityWarningCard(
                        title = "Make sure you have your recovery phrase",
                        detail = "Without the recovery phrase you will not be able to restore access to this wallet.",
                        accent = NoirError
                    )

                    ResponsibilityCheck(
                        "I understand that I need my recovery phrase to restore access.",
                        ack
                    ) { ack = it }

                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it.uppercase() },
                        label = { Text("Type RESET to confirm", color = NoirTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = NoirSurfaceElev,
                            unfocusedContainerColor = NoirSurfaceElev,
                            focusedTextColor = NoirText,
                            unfocusedTextColor = NoirText,
                            focusedIndicatorColor = if (canReset) NoirSuccess else NoirOutlineStrong,
                            unfocusedIndicatorColor = NoirOutlineStrong,
                            cursorColor = NoirAccent
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(label = "Cancel", modifier = Modifier.weight(1f), onClick = onCancel)
                        DangerButton(
                            label = "Reset local wallet",
                            modifier = Modifier.weight(1f),
                            enabled = canReset,
                            onClick = onConfirm
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryLink(label: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) NoirError.copy(alpha = 0.85f) else NoirTextMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier
                .background(Color.Transparent, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StatusDot(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (ok) NoirSuccess else NoirError.copy(alpha = 0.5f), RoundedCornerShape(50))
        )
        Text(label, color = NoirTextSubtle, fontSize = 11.sp, letterSpacing = 0.8.sp)
    }
}
