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
import androidx.compose.material.icons.outlined.ContentCopy
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

// Premium fintech palette (mirrors NoirOnboarding.kt) so the shared
// components and the noir primitives stay visually consistent.
private val ZInk = Color(0xFF07080C)
private val ZSurface = Color(0xFF10131A)
private val ZSurfaceElev = Color(0xFF141822)
private val ZOutline = Color(0xFF1E2430)
private val ZOutlineStrong = Color(0xFF2A3140)
private val ZText = Color(0xFFF4F4F5)
private val ZTextMuted = Color(0xFF9CA3AF)
private val ZTextSubtle = Color(0xFF6B7280)
private val ZAccent = Color(0xFFC084FC)
private val ZAccentSoft = Color(0xFFD8B4FE)
private val ZAccentDeep = Color(0xFFA78BFA)
private val ZError = Color(0xFFE08B8B)

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZInk)
    ) {
        GridBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "ZSILENT WALLET",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZTextSubtle
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ZText
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZTextMuted
                )
            }
            content()
        }
    }
}

@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    // Premium fintech background: a deep carbon canvas with two soft lilac
    // glows + subtle bottom vignette. No grid, no animations.
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(ZAccent.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(w * 0.18f, h * 0.18f),
                radius = (if (w > h) w else h) * 0.55f
            ),
            size = Size(w, h)
        )
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(ZAccentSoft.copy(alpha = 0.035f), Color.Transparent),
                center = Offset(w * 0.82f, h * 0.85f),
                radius = (if (w > h) w else h) * 0.45f
            ),
            size = Size(w, h)
        )
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF03040A).copy(alpha = 0.55f)),
                startY = h * 0.55f,
                endY = h
            ),
            size = Size(w, h)
        )
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    meta: String? = null,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ZOutline),
        colors = CardDefaults.cardColors(containerColor = ZSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides ZText) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (title != null || meta != null) {
                    PanelHeader(title = title.orEmpty(), meta = meta)
                }
                content()
            }
        }
    }
}

@Composable
fun PanelHeader(title: String, meta: String? = null) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ZText
        )
        if (meta != null) {
            Text(
                meta.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ZTextSubtle
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, ZOutline),
        colors = CardDefaults.cardColors(containerColor = ZSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ZTextSubtle
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                color = ZText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = ZTextMuted
            )
        }
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
        Text(
            label,
            color = ZTextMuted,
            style = MaterialTheme.typography.bodySmall
        )
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
        Text(
            label,
            color = ZTextMuted,
            style = MaterialTheme.typography.bodySmall
        )
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
    focusedLabelColor = ZAccentSoft,
    unfocusedLabelColor = ZTextMuted,
    cursorColor = ZAccent
)

@Composable
fun zButtonColors() = ButtonDefaults.buttonColors(
    containerColor = ZAccent,
    contentColor = Color(0xFF231038),
    disabledContainerColor = ZSurfaceElev,
    disabledContentColor = ZTextSubtle
)

@Composable
fun zOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = Color.Transparent,
    contentColor = ZText,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = ZTextSubtle
)
