package com.zsilent.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

// Smoke-gray dark theme — minimalist, white text, green accent for metrics.
private val ZInk        = Color(0xFF2B2D31)   // section background — strong smoke gray
private val ZSurface    = Color(0xFF35373C)   // card surface — lighter smoke gray
private val ZSurfaceElev= Color(0xFF3C3E44)   // input field bg
private val ZOutline    = Color(0xFF45474D)   // subtle border
private val ZOutlineStrong = Color(0xFF55575E)
private val ZText       = Color(0xFFFFFFFF)   // primary text — WHITE
private val ZTextMuted  = Color(0xFFC7C9CF)   // secondary text — light gray
private val ZTextSubtle = Color(0xFF9AA0A6)   // tertiary
private val ZAccent     = Color(0xFF4ADE80)   // green accent (metrics/charts)
private val ZAccentSoft = Color(0xFF2E4A38)   // green-tinted bg
private val ZAccentDeep = Color(0xFF22C55E)   // deeper green
private val ZError      = Color(0xFFEF5350)   // error red
private val ZBtnBlack   = Color(0xFF1C1C1E)   // button bg — near-black

@Composable
fun ScreenScaffold(
    title: String,       // kept for API compat, not shown
    subtitle: String,    // kept for API compat, not shown
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(ZInk)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) { content() }
    }
}

// Kept for compile compatibility — no longer draws dark bg
@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(ZInk))
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    meta: String? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ZSurface,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalContentColor provides ZText) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (title != null || meta != null) PanelHeader(title = title.orEmpty(), meta = meta)
                content()
            }
        }
    }
}

@Composable
fun PanelHeader(title: String, meta: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ZText)
        if (meta != null) {
            MetaBadge(meta)
        }
    }
}

@Composable
fun MetaBadge(meta: String) {
    val icon = metaIcon(meta)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = ZText, modifier = Modifier.size(17.dp))
        Text(meta, color = ZTextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun metaIcon(meta: String): androidx.compose.ui.graphics.vector.ImageVector? {
    val bi = com.zsilent.core.ui.onboarding.BootstrapIcons
    return when {
        meta.contains("lock", ignoreCase = true)    -> bi.Lock
        meta.contains("sync", ignoreCase = true) || meta.contains("import", ignoreCase = true) -> bi.ArrowClockwise
        meta.contains("pend", ignoreCase = true)    -> bi.Clock
        meta.contains("shield", ignoreCase = true) || meta.contains("private", ignoreCase = true) -> bi.ShieldCheck
        meta.contains("mine", ignoreCase = true)    -> bi.Lightning
        meta.contains("active", ignoreCase = true) || meta.contains("ok", ignoreCase = true) -> bi.CheckCircle
        meta.contains("link", ignoreCase = true)    -> bi.LinkIcon
        meta.contains("node", ignoreCase = true)    -> bi.Cpu
        meta.contains("send", ignoreCase = true)    -> bi.ArrowUpCircle
        meta.contains("device", ignoreCase = true) || meta.contains("sign", ignoreCase = true) -> bi.ShieldCheck
        else -> null
    }
}

@Composable
fun StatusCard(
    title: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = ZSurface,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                labelIcon(title)?.let { Icon(it, null, tint = ZText, modifier = Modifier.size(17.dp)) }
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = ZTextSubtle, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }
            Text(value, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                color = ZText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = ZTextSubtle)
        }
    }
}

/**
 * Maps a key-value label to a representative icon. Keyword based and
 * case-insensitive so it works across all sections automatically.
 */
fun labelIcon(label: String): androidx.compose.ui.graphics.vector.ImageVector? {
    val l = label.lowercase()
    val bi = com.zsilent.core.ui.onboarding.BootstrapIcons
    return when {
        l.contains("available") || l.contains("balance") || l.contains("wallet") || l.contains("account") -> bi.Wallet
        l.contains("locked")                              -> bi.Lock
        l.contains("private key") || l.contains("keys")   -> bi.Key
        l.contains("privacy") || l.contains("shield")     -> bi.ShieldCheck
        l.contains("sync")                                -> bi.ArrowClockwise
        l.contains("peer")                                -> bi.Diagram
        l.contains("bootstrap")                           -> bi.Diagram
        l.contains("rpc") || l.contains("endpoint")       -> bi.Server
        l.contains("height") || l.contains("block")       -> bi.Box
        l.contains("difficulty")                          -> bi.Speedometer
        l.contains("emission") || l.contains("supply") || l.contains("reward") -> bi.Coin
        l.contains("fee")                                 -> bi.Cash
        l.contains("pending") || l.contains("queue")      -> bi.Clock
        l.contains("miner") || l.contains("mining")       -> bi.Lightning
        l.contains("transaction") || l.contains("tx")     -> bi.ArrowLeftRight
        l.contains("binar")                               -> bi.Cpu
        l.contains("status")                              -> bi.InfoCircle
        l.contains("valid")                               -> bi.CheckCircle
        l.contains("db") || l.contains("storage")         -> bi.Hdd
        l.contains("gate")                                -> bi.CheckCircle
        l.contains("address") || l.contains("recipient")  -> bi.Hash
        l.contains("total")                               -> bi.Cash
        l.contains("network") || l.contains("node")       -> bi.Diagram
        l.contains("name")                                -> bi.Person
        l.contains("mode") || l.contains("type")          -> bi.InfoCircle
        l.contains("link")                                -> bi.LinkIcon
        else -> null
    }
}

@Composable
private fun LabelWithIcon(label: String) {
    val icon = labelIcon(label)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, tint = ZText, modifier = Modifier.size(18.dp))
        Text(label, color = ZTextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelWithIcon(label)
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = ZText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun CopyableKeyValueRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelWithIcon(label)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                color = ZText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CopyIconButton(value)
        }
    }
}

@Composable
fun CopyIconButton(value: String, enabled: Boolean = value.isNotBlank()) {
    val clipboard = LocalClipboardManager.current
    IconButton(
        onClick = { clipboard.setText(AnnotatedString(value)) },
        enabled = enabled
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = singleLine,
        enabled = enabled,
        modifier = modifier,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        colors = zFieldColors(),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun QrCode(value: String, modifier: Modifier = Modifier) {
    val matrix = remember(value) {
        runCatching {
            QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 33, 33)
        }.getOrNull()
    }
    val foreground = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .size(220.dp)
            .background(background, MaterialTheme.shapes.large)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.large)
            .padding(12.dp)
    ) {
        Canvas(Modifier.size(196.dp)) {
            val qr = matrix ?: return@Canvas
            val cell = minOf(size.width / qr.width, size.height / qr.height)
            val offset = Offset(
                x = (size.width - cell * qr.width) / 2f,
                y = (size.height - cell * qr.height) / 2f
            )
            for (x in 0 until qr.width) {
                for (y in 0 until qr.height) {
                    drawRect(
                        color = if (qr[x, y]) foreground else Color.Transparent,
                        topLeft = Offset(offset.x + x * cell, offset.y + y * cell),
                        size = Size(cell, cell)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionEyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ZTextSubtle
    )
}

@Composable
fun StatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    val dot = if (active) ZAccent else ZTextSubtle
    Row(
        modifier = modifier
            .background(ZSurfaceElev, shape)
            .border(BorderStroke(1.dp, ZOutline), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dot, shape)
        )
        Text(text, style = MaterialTheme.typography.labelSmall, color = if (active) ZText else ZTextMuted)
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(vertical = 4.dp),
        color = ZTextMuted,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun ErrorText(text: String) {
    Text(text, color = ZError, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun ThinDivider() {
    HorizontalDivider(color = ZOutline, thickness = DividerDefaults.Thickness)
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    Text(
        text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun zFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ZText,
    unfocusedTextColor = ZText,
    focusedContainerColor = ZSurfaceElev,
    unfocusedContainerColor = ZSurfaceElev,
    disabledContainerColor = ZSurfaceElev,
    focusedBorderColor = ZAccent,
    unfocusedBorderColor = ZOutlineStrong,
    disabledBorderColor = ZOutline,
    focusedLabelColor = ZAccent,
    unfocusedLabelColor = ZTextMuted,
    cursorColor = ZAccent
)

// Black raised button with white text/icon (use with elevation at call site).
@Composable
fun zButtonColors() = ButtonDefaults.buttonColors(
    containerColor = ZBtnBlack,
    contentColor = Color(0xFFFFFFFF),
    disabledContainerColor = Color(0xFF3A3A3C),
    disabledContentColor = ZTextSubtle
)

// Elevation for the raised (relief) look on black buttons.
@Composable
fun zButtonElevation() = ButtonDefaults.buttonElevation(
    defaultElevation = 4.dp,
    pressedElevation = 1.dp,
    hoveredElevation = 6.dp
)

@Composable
fun zOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = ZBtnBlack,
    contentColor = Color(0xFFFFFFFF),
    disabledContainerColor = Color(0xFF3A3A3C),
    disabledContentColor = ZTextSubtle
)

// Minimalist black/white switch: black track + white thumb when ON,
// dark track + light-gray thumb when OFF. High contrast on smoke-gray.
@Composable
fun zSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor   = Color(0xFFFFFFFF),
    checkedTrackColor   = Color(0xFF1C1C1E),
    checkedBorderColor  = Color(0xFF1C1C1E),
    uncheckedThumbColor = Color(0xFFC7C9CF),
    uncheckedTrackColor = Color(0xFF45474D),
    uncheckedBorderColor= Color(0xFF55575E)
)
