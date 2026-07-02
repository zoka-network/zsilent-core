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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState

// ─── "Zenith" theme colors for Login ───
private val LBg       = Color(0xFF0A0A0C)   // page background — near black
private val LCard     = Color(0xFF16171B)   // card bg — a touch lighter than bg
private val LBorder   = Color(0xFF2A2D33)   // card border
private val LText     = Color(0xFFFFFFFF)   // primary text — white
private val LMuted    = Color(0xFFB8BCC4)   // secondary text
private val LSubtle   = Color(0xFF7A7F88)   // tertiary / placeholder
private val LAccent   = Color(0xFFA8E6B0)   // pastel green accent
private val LBtnBg    = Color(0xFF050506)   // button background (near-black) — stays neutral
private val LBtnText  = Color(0xFFFFFFFF)   // button text (white)
private val LError    = Color(0xFFE2918F)   // soft desaturated rose
private val LSuccess  = Color(0xFFA8E6B0)   // success — pastel green
private val LCardGradient = Brush.verticalGradient(
    listOf(Color(0xFF1B1D22), Color(0xFF101114))
)

private fun loginWalletOptions(ui: ZSilentUiState): List<Pair<String, String>> {
    if (ui.wallets.isNotEmpty()) return ui.wallets.map { it.name to it.address }
    val names = ui.managedWallets.filter { it.isNotBlank() }.distinct()
    return names.map { name -> name to "" }
}

@Composable
fun LoginScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }

    if (showReset) {
        LoginResetScreen(onCancel = { showReset = false }, onConfirm = {
            password = ""
            if (appState.resetLocalWallet()) showReset = false
        })
        return
    }
    if (showRestoreConfirm) {
        LoginRestoreScreen(onCancel = { showRestoreConfirm = false }, onConfirm = {
            password = ""
            appState.resetSetup()
            showRestoreConfirm = false
        })
        return
    }

    val walletOptions = loginWalletOptions(ui)
    val showSelector = walletOptions.size > 1

    fun doLogin() {
        if (password.isNotBlank() && !ui.loginBusy) {
            attempts += 1
            appState.login(password.toCharArray())
            password = ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(LBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(min = 420.dp, max = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Brand ────────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.zsilent.core.ui.onboarding.NoirBrandBadge(size = 60.dp)
                Text("ZSilent Wallet", color = LText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Enter your password to unlock", color = LMuted, fontSize = 13.sp)
            }

            // ── Login card ───────────────────────────────────────────────
            val loginCardShape = RoundedCornerShape(20.dp)
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = loginCardShape,
                color = Color.Transparent,
                shadowElevation = 4.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(LCardGradient, loginCardShape)
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showSelector) {
                        Text("Select wallet", color = LSubtle, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                        LoginWalletSelector(
                            wallets = walletOptions,
                            activeName = ui.activeWalletName,
                            activeAddress = ui.activeWalletAddress,
                            onSelect = { name -> password = ""; attempts = 0; appState.selectWallet(name) }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(36.dp).background(Color(0xFF1C2B20), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = LAccent, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(ui.activeWalletName.ifBlank { "Main wallet" }, color = LText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                ui.activeWalletAddress.takeIf { it.isNotBlank() }?.let {
                                    Text("${it.take(10)}…${it.takeLast(8)}", color = LSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    ui.lockMessage?.takeIf { it.isNotBlank() }?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF1D1F24), RoundedCornerShape(10.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                            Text(it, color = Color(0xFFFFB74D), fontSize = 12.sp)
                        }
                    }

                    // Password field — always shows, button always black
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = LSubtle) },
                        singleLine = true,
                        isError = ui.loginError != null,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { doLogin() }),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    null, tint = LMuted, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LText, unfocusedTextColor = LText,
                            focusedContainerColor = Color(0xFF1D1F24), unfocusedContainerColor = Color(0xFF1D1F24),
                            focusedBorderColor = LAccent, unfocusedBorderColor = LBorder,
                            focusedLabelColor = LAccent, unfocusedLabelColor = LSubtle,
                            errorBorderColor = LError, errorTextColor = LText, cursorColor = LAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ui.loginError?.let { err ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = LError, modifier = Modifier.size(14.dp))
                            Text(err, color = LError, fontSize = 12.sp)
                        }
                    }

                    // Unlock button — always near-black, never gray
                    Button(
                        onClick = { doLogin() },
                        enabled = !ui.loginBusy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LBtnBg,
                            contentColor = LBtnText,
                            disabledContainerColor = Color(0xFF2A2D33), // slightly lighter when busy
                            disabledContentColor = LBtnText
                        )
                    ) {
                        Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (ui.loginBusy) "Unlocking…" else "Unlock wallet",
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }

                    // Footer links
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Restore from phrase", color = LAccent, fontSize = 12.sp,
                            modifier = Modifier.clickable { password = ""; showRestoreConfirm = true }.padding(8.dp))
                        Text("·", color = LSubtle, modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Reset wallet", color = LError.copy(alpha = 0.7f), fontSize = 12.sp,
                            modifier = Modifier.clickable { showReset = true }.padding(8.dp))
                    }
                }
            }

            // Network status dot
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(if (ui.rpcOnline) LSuccess else LError.copy(alpha = 0.5f), CircleShape))
                Text(if (ui.rpcOnline) "Network reachable" else "Network unreachable", color = LSubtle, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LoginWalletSelector(
    wallets: List<Pair<String, String>>,
    activeName: String,
    activeAddress: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        wallets.forEach { (name, address) ->
            val selected = name == activeName || (address.isNotBlank() && activeAddress.isNotBlank() && address == activeAddress)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (selected) Color(0xFF1C2B20) else Color(0xFF1D1F24), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, if (selected) LAccent else LBorder), RoundedCornerShape(10.dp))
                    .clickable { if (!selected) onSelect(name) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.AccountBalanceWallet, null,
                    tint = if (selected) LAccent else LMuted, modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = LText, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
                    if (address.isNotBlank()) Text("${address.take(10)}…${address.takeLast(8)}", color = LSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                if (selected) Box(Modifier.size(8.dp).background(LAccent, CircleShape))
            }
        }
    }
}

@Composable
private fun LoginResetScreen(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var ack by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    val canReset = ack && confirmText.trim() == "RESET"
    LoginConfirmationLayout(
        title = "Reset local wallet",
        body = "This removes the encrypted wallet from this device. Funds remain on the blockchain. You need your recovery phrase to restore access.",
        warningText = "Without the recovery phrase you will permanently lose access.",
        checkLabel = "I understand I need my recovery phrase to restore access.",
        checked = ack, onChecked = { ack = it },
        extra = {
            OutlinedTextField(
                value = confirmText, onValueChange = { confirmText = it.uppercase() },
                label = { Text("Type RESET to confirm", color = LSubtle) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LText, unfocusedTextColor = LText,
                    focusedContainerColor = Color(0xFF1D1F24), unfocusedContainerColor = Color(0xFF1D1F24),
                    focusedBorderColor = if (canReset) LSuccess else LAccent, unfocusedBorderColor = LBorder,
                    cursorColor = LAccent
                )
            )
        },
        cancelLabel = "Cancel", onCancel = onCancel,
        confirmLabel = "Reset wallet", onConfirm = onConfirm, confirmEnabled = canReset, isDanger = true
    )
}

@Composable
private fun LoginRestoreScreen(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var ack by remember { mutableStateOf(false) }
    LoginConfirmationLayout(
        title = "Restore from recovery phrase",
        body = "This clears the setup registration so you can re-enter your recovery phrase. Wallet files are NOT deleted. You will need your recovery phrase.",
        warningText = "Have your recovery phrase ready before continuing.",
        checkLabel = "I have my recovery phrase and understand the setup will be cleared.",
        checked = ack, onChecked = { ack = it }, extra = {},
        cancelLabel = "Cancel", onCancel = onCancel,
        confirmLabel = "Clear and restore", onConfirm = onConfirm, confirmEnabled = ack, isDanger = true
    )
}

@Composable
private fun LoginConfirmationLayout(
    title: String, body: String, warningText: String, checkLabel: String,
    checked: Boolean, onChecked: (Boolean) -> Unit,
    extra: @Composable () -> Unit,
    cancelLabel: String, onCancel: () -> Unit,
    confirmLabel: String, onConfirm: () -> Unit,
    confirmEnabled: Boolean, isDanger: Boolean
) {
    Box(Modifier.fillMaxSize().background(LBg), contentAlignment = Alignment.Center) {
        val dialogShape = RoundedCornerShape(20.dp)
        androidx.compose.material3.Surface(
            modifier = Modifier.widthIn(min = 460.dp, max = 580.dp),
            shape = dialogShape, color = Color.Transparent, shadowElevation = 4.dp
        ) {
            Column(
                Modifier
                    .background(LCardGradient, dialogShape)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(title, color = LText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(body, color = LMuted, fontSize = 13.sp)
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF1D1F24), RoundedCornerShape(10.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠", fontSize = 14.sp)
                    Text(warningText, color = Color(0xFFFFB74D), fontSize = 12.sp)
                }
                Row(
                    Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(20.dp)
                            .background(if (checked) LBtnBg else Color.Transparent, RoundedCornerShape(4.dp))
                            .border(BorderStroke(1.dp, if (checked) LBtnBg else LBorder), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (checked) Text("✓", color = LBtnText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(checkLabel, color = LText, fontSize = 12.sp)
                }
                extra()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onCancel, modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3D44), contentColor = LText)
                    ) { Text(cancelLabel, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = onConfirm, enabled = confirmEnabled,
                        modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDanger) LError else LBtnBg,
                            contentColor = LBtnText,
                            disabledContainerColor = Color(0xFFE5E7EB), disabledContentColor = LSubtle
                        )
                    ) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
