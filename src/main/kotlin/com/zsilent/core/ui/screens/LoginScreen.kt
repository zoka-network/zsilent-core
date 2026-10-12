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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import com.zsilent.core.ui.components.Pill
import com.zsilent.core.ui.components.QuanticWordmark
import com.zsilent.core.ui.components.Truncate
import com.zsilent.core.ui.components.ZButton
import com.zsilent.core.ui.components.ZButtonSize
import com.zsilent.core.ui.components.ZButtonText
import com.zsilent.core.ui.components.ZButtonVariant
import com.zsilent.core.ui.components.ZInput
import com.zsilent.core.ui.components.ZLabel
import com.zsilent.core.ui.components.ZTone
import com.zsilent.core.ui.onboarding.NoirBrandBadge
import com.zsilent.core.ui.theme.ZTheme
import com.zsilent.core.ui.theme.zGrid
import com.zsilent.core.ui.theme.zNoise

// ─────────────────────────────────────────────────────────────────────────────
//  Login — portado de zoka-whisper-desk/src/components/zs/Login.tsx
//
//  Ver docs/GUIA_CAMBIO_VISUAL.md, etapa 3.
//
//  Este archivo tenía 24 colores literales, todos rotos en tema claro. Ahora
//  todo sale del tema. No volver a poner literales acá.
// ─────────────────────────────────────────────────────────────────────────────

private fun loginWalletOptions(ui: ZSilentUiState): List<Pair<String, String>> {
    if (ui.wallets.isNotEmpty()) return ui.wallets.map { it.name to it.address }
    val names = ui.managedWallets.filter { it.isNotBlank() }.distinct()
    return names.map { name -> name to "" }
}

/**
 * Línea de 1dp con degradado `transparent → accent → transparent` en el borde
 * superior del panel. Detalle chico del prototipo pero muy característico:
 * aparece en el login y en la tarjeta de balance del wallet.
 */
@Composable
private fun AccentTopLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary,
                        Color.Transparent,
                    )
                )
            )
    )
}

@Composable
fun LoginScreen(ui: ZSilentUiState, appState: ZSilentAppState) {
    var showReset by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    if (showReset) {
        LoginResetScreen(
            onCancel = { showReset = false },
            onConfirm = { if (appState.resetLocalWallet()) showReset = false }
        )
        return
    }
    if (showRestoreConfirm) {
        LoginRestoreScreen(
            onCancel = { showRestoreConfirm = false },
            onConfirm = { appState.resetSetup(); showRestoreConfirm = false }
        )
        return
    }

    LoginContent(
        activeWalletName = ui.activeWalletName,
        activeWalletAddress = ui.activeWalletAddress,
        wallets = loginWalletOptions(ui),
        lockMessage = ui.lockMessage,
        loginError = ui.loginError,
        busy = ui.loginBusy,
        zokaReady = ui.zokaReady,
        onSelectWallet = appState::selectWallet,
        onLogin = appState::login,
        onRestore = { showRestoreConfirm = true },
        onReset = { showReset = true },
    )
}

/**
 * Cuerpo visual del login, sin dependencias de `ZSilentAppState`.
 *
 * Se separó del wrapper con estado para poder renderizarlo en la galería
 * (`./gradlew.bat runGallery`) sin arrancar nodo ni servicios. Es el patrón que
 * conviene repetir en el resto de las pantallas a medida que se porten.
 */
@Composable
fun LoginContent(
    activeWalletName: String,
    activeWalletAddress: String,
    wallets: List<Pair<String, String>>,
    lockMessage: String?,
    loginError: String?,
    busy: Boolean,
    zokaReady: Boolean,
    onSelectWallet: (String) -> Unit,
    onLogin: (CharArray) -> Unit,
    onRestore: () -> Unit,
    onReset: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val showSelector = wallets.size > 1
    val scheme = MaterialTheme.colorScheme

    fun doLogin() {
        if (password.isNotBlank() && !busy) {
            onLogin(password.toCharArray())
            password = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .zNoise(scheme.onSurface)
            .zGrid(scheme.onSurface, alpha = 0.04f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(min = 400.dp, max = 448.dp),
            verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
        ) {
            // ── Marca, fuera del panel ───────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                NoirBrandBadge(size = 40.dp)
                Column {
                    Text("ZSilent", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                    QuanticWordmark(style = ZTheme.text.navGroup, suffix = "TESTNET · LOCKED")
                }
            }

            // ── Panel de desbloqueo ──────────────────────────────────────
            val shape = MaterialTheme.shapes.medium
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface.copy(alpha = 0.6f), shape)
                    .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
            ) {
                AccentTopLine()
                Column(
                    modifier = Modifier.padding(ZTheme.spacing.panel),
                    verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ZLabel("Active wallet")
                            if (!showSelector) {
                                Text(
                                    activeWalletName.ifBlank { "Main wallet" },
                                    style = ZTheme.text.mono,
                                    color = scheme.onSurface
                                )
                                activeWalletAddress.takeIf { it.isNotBlank() }?.let {
                                    Truncate(it, head = 10, tail = 8, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                        Pill("Zoka ready", ZTone.Info)
                    }

                    if (showSelector) {
                        LoginWalletSelector(
                            wallets = wallets,
                            activeName = activeWalletName,
                            activeAddress = activeWalletAddress,
                            onSelect = { name -> password = ""; onSelectWallet(name) }
                        )
                    }

                    lockMessage?.takeIf { it.isNotBlank() }?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Lock, null,
                                tint = ZTheme.semantics.warning,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = ZTheme.semantics.warning
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ZLabel("Password")
                        ZInput(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Enter password to unlock",
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailing = {
                                Icon(
                                    if (showPassword) Icons.Outlined.VisibilityOff
                                    else Icons.Outlined.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showPassword = !showPassword }
                                )
                            },
                        )
                        loginError?.let { err ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ErrorOutline, null,
                                    tint = ZTheme.semantics.danger,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZTheme.semantics.danger
                                )
                            }
                        }
                    }

                    ZButton(
                        onClick = { doLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ZButtonVariant.Accent,
                        size = ZButtonSize.Large,
                        enabled = !busy && password.isNotBlank(),
                    ) {
                        Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(14.dp))
                        ZButtonText(if (busy) "Unlocking" else "Unlock session")
                    }

                    HorizontalDivider(color = scheme.outlineVariant)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZButton(
                            onClick = { password = ""; onRestore() },
                            variant = ZButtonVariant.Ghost,
                            size = ZButtonSize.Small,
                        ) {
                            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(12.dp))
                            ZButtonText("Restore from phrase")
                        }
                        ZButton(
                            onClick = { password = ""; onReset() },
                            variant = ZButtonVariant.Danger,
                            size = ZButtonSize.Small,
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(12.dp))
                            ZButtonText("Wipe local data")
                        }
                    }
                }
            }

            // ── Pie, fuera del panel ─────────────────────────────────────
            //
            // Serverless: no hay red que pinguear antes de desbloquear — el nodo
            // conecta por P2P/Tor/iroh después —, así que se informa si el nodo
            // está listo para arrancar, no la alcanzabilidad de un RPC muerto.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "runtime · local-first",
                    style = ZTheme.text.mono,
                    color = scheme.onSurfaceVariant
                )
                Pill(
                    if (zokaReady) "Ready to connect (P2P)" else "Node binaries missing",
                    if (zokaReady) ZTone.On else ZTone.Err,
                )
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
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        wallets.forEach { (name, address) ->
            val selected = name == activeName ||
                (address.isNotBlank() && activeAddress.isNotBlank() && address == activeAddress)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (selected) scheme.primary.copy(alpha = 0.10f)
                        else scheme.surfaceContainerHigh,
                        shape
                    )
                    .border(
                        BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant),
                        shape
                    )
                    .clickable { if (!selected) onSelect(name) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet, null,
                    tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface
                    )
                    if (address.isNotBlank()) {
                        Truncate(address, head = 10, tail = 8, color = scheme.onSurfaceVariant)
                    }
                }
                if (selected) Box(Modifier.size(6.dp).background(scheme.primary, CircleShape))
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
        body = "This removes the encrypted wallet from this device. Funds remain on the " +
            "blockchain. You need your recovery phrase to restore access.",
        warningText = "Without the recovery phrase you will permanently lose access.",
        checkLabel = "I understand I need my recovery phrase to restore access.",
        checked = ack, onChecked = { ack = it },
        extra = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ZLabel("Type RESET to confirm")
                ZInput(
                    value = confirmText,
                    onValueChange = { confirmText = it.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "RESET",
                )
            }
        },
        cancelLabel = "Cancel", onCancel = onCancel,
        confirmLabel = "Reset wallet", onConfirm = onConfirm,
        confirmEnabled = canReset, isDanger = true
    )
}

@Composable
private fun LoginRestoreScreen(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var ack by remember { mutableStateOf(false) }
    LoginConfirmationLayout(
        title = "Restore from recovery phrase",
        body = "This clears the setup registration so you can re-enter your recovery " +
            "phrase. Wallet files are NOT deleted. You will need your recovery phrase.",
        warningText = "Have your recovery phrase ready before continuing.",
        checkLabel = "I have my recovery phrase and understand the setup will be cleared.",
        checked = ack, onChecked = { ack = it }, extra = {},
        cancelLabel = "Cancel", onCancel = onCancel,
        confirmLabel = "Clear and restore", onConfirm = onConfirm,
        confirmEnabled = ack, isDanger = true
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
    val scheme = MaterialTheme.colorScheme
    val warning = ZTheme.semantics.warning
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .zNoise(scheme.onSurface)
            .zGrid(scheme.onSurface, alpha = 0.04f),
        contentAlignment = Alignment.Center
    ) {
        val shape = MaterialTheme.shapes.medium
        Column(
            modifier = Modifier
                .widthIn(min = 440.dp, max = 560.dp)
                .background(scheme.surface.copy(alpha = 0.6f), shape)
                .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
                .padding(ZTheme.spacing.panel),
            verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isDanger) ZTheme.semantics.danger else scheme.onSurface
            )
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(warning.copy(alpha = 0.08f), MaterialTheme.shapes.small)
                    .border(BorderStroke(1.dp, warning.copy(alpha = 0.30f)), MaterialTheme.shapes.small)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline, null,
                    tint = warning, modifier = Modifier.size(14.dp)
                )
                Text(warningText, style = MaterialTheme.typography.bodySmall, color = warning)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onChecked(!checked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .background(
                            if (checked) scheme.primary else Color.Transparent,
                            MaterialTheme.shapes.extraSmall
                        )
                        .border(
                            BorderStroke(1.dp, if (checked) scheme.primary else scheme.outlineVariant),
                            MaterialTheme.shapes.extraSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        Icon(
                            Icons.Outlined.Check, null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(checkLabel, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
            }

            extra()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    variant = ZButtonVariant.Ghost,
                    size = ZButtonSize.Large,
                ) { ZButtonText(cancelLabel) }
                ZButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    variant = if (isDanger) ZButtonVariant.Danger else ZButtonVariant.Accent,
                    size = ZButtonSize.Large,
                    enabled = confirmEnabled,
                ) { ZButtonText(confirmLabel) }
            }
        }
    }
}
