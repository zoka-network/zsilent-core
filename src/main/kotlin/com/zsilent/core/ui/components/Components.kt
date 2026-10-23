package com.zsilent.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zsilent.core.ui.theme.MonoFamily
import com.zsilent.core.ui.theme.ZTheme
import com.zsilent.core.ui.theme.ZEasing
import com.zsilent.core.ui.theme.Z_DURATION_FAST
import com.zsilent.core.ui.theme.zPulseAlpha
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Primitivos portados de zoka-whisper-desk/src/components/zs/primitives.tsx
//
//  Ver docs/GUIA_CAMBIO_VISUAL.md, etapa 0b.
//
//  Dos reglas al editar este archivo:
//
//  1. Ningún color literal. Todo sale de MaterialTheme.colorScheme o de
//     ZTheme.semantics. Este archivo tenía 28 Color(0x…) hardcodeados que
//     rompían el modo claro (decisión D2).
//
//  2. Ningún espaciado fijo. Todo relleno, separación y alto de control sale de
//     ZTheme.spacing, porque la densidad es configurable (decisión D4).
// ─────────────────────────────────────────────────────────────────────────────

/** Tonos de estado del prototipo. Mapean a los cuatro semánticos del tema. */
enum class ZTone { Default, On, Warn, Err, Off, Info }

@Composable
private fun ZTone.foreground(): Color = when (this) {
    ZTone.Default -> MaterialTheme.colorScheme.onSurfaceVariant
    ZTone.On      -> ZTheme.semantics.success
    ZTone.Warn    -> ZTheme.semantics.warning
    ZTone.Err     -> ZTheme.semantics.danger
    ZTone.Off     -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    ZTone.Info    -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun ZTone.container(): Color = when (this) {
    ZTone.Default, ZTone.Off -> MaterialTheme.colorScheme.surfaceContainerHigh
    ZTone.Info               -> MaterialTheme.colorScheme.surfaceContainerHigh
    else                     -> foreground().copy(alpha = 0.10f)
}

@Composable
private fun ZTone.borderColor(): Color = when (this) {
    ZTone.Default, ZTone.Off -> MaterialTheme.colorScheme.outlineVariant
    ZTone.Info               -> ZTheme.semantics.borderStrong
    else                     -> foreground().copy(alpha = 0.30f)
}

/** `true` en los tonos cuyo punto late. */
private val ZTone.pulses: Boolean
    get() = this == ZTone.On || this == ZTone.Warn || this == ZTone.Err

// ── Contenedores ─────────────────────────────────────────────────────────────

@Composable
fun ScreenScaffold(
    title: String,       // se conserva por compatibilidad de API, no se muestra
    subtitle: String,    // idem
    content: @Composable () -> Unit
) {
    val spacing = ZTheme.spacing
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screen, vertical = spacing.panel),
            verticalArrangement = Arrangement.spacedBy(spacing.gap)
        ) { content() }
    }
}

@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}

/**
 * Contenedor base. CSS del prototipo: `rounded-lg border border-border
 * bg-surface/60`.
 *
 * El relleno translúcido es intencional: sobre el fondo con retícula deja pasar
 * un rastro de la trama. La separación visual la produce el borde, no el
 * relleno — ver la nota de Theme.kt.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    meta: String? = null,
    contentPadding: PaddingValues = PaddingValues(ZTheme.spacing.panel),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(ZTheme.spacing.gap)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (title != null || meta != null) PanelHeader(title = title.orEmpty(), meta = meta)
            content()
        }
    }
}

/**
 * Cabecera de panel: etiqueta en mono mayúscula, subtítulo opcional y un slot a
 * la derecha.
 *
 * `meta` se mantiene por compatibilidad y se renderiza en el slot derecho.
 */
@Composable
fun PanelHeader(
    title: String,
    meta: String? = null,
    subtitle: String? = null,
    right: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title.uppercase(),
                style = ZTheme.text.panelLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        when {
            right != null -> right()
            meta != null  -> MetaBadge(meta)
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
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
        }
        Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = ZTheme.text.mono)
    }
}

private fun metaIcon(meta: String): ImageVector? {
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

/** Encabezado de sección: eyebrow acentuado, título y bajada. */
@Composable
fun SectionTitle(eyebrow: String, title: String, sub: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            eyebrow.uppercase(),
            style = ZTheme.text.eyebrow,
            color = MaterialTheme.colorScheme.primary
        )
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        if (sub != null) {
            Text(
                sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }
    }
}

// ── Datos ────────────────────────────────────────────────────────────────────

/**
 * Métrica del prototipo: etiqueta chica en mono mayúscula, valor grande en mono
 * y una pista opcional.
 */
@Composable
fun Stat(
    label: String,
    value: String,
    hint: String? = null,
    tone: ZTone = ZTone.Default,
    mono: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val valueColor = when (tone) {
        ZTone.Default -> MaterialTheme.colorScheme.onSurface
        ZTone.Info    -> MaterialTheme.colorScheme.primary
        else          -> tone.foreground()
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = ZTheme.text.fieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (mono) ZTheme.text.monoLarge else MaterialTheme.typography.titleLarge,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/** Insignia de estado con punto. Los tonos activos laten. */
@Composable
fun Pill(
    text: String,
    tone: ZTone = ZTone.Default,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraSmall
    val fg = tone.foreground()
    val pulseAlpha = if (tone.pulses) zPulseAlpha() else 1f
    Row(
        modifier = modifier
            .background(tone.container(), shape)
            .border(BorderStroke(1.dp, tone.borderColor()), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tone != ZTone.Default && tone != ZTone.Off) {
            Box(
                Modifier
                    .size(6.dp)
                    .alpha(pulseAlpha)
                    .background(
                        if (tone == ZTone.Info) fg.copy(alpha = 0.6f) else fg,
                        CircleShape
                    )
            )
        }
        Text(text.uppercase(), style = ZTheme.text.pill, color = fg)
    }
}

/**
 * Trunca por el medio conservando cabeza y cola. Para hashes, direcciones y
 * endpoints. El prototipo usa 10/6 por defecto.
 */
@Composable
fun Truncate(
    value: String,
    head: Int = 10,
    tail: Int = 6,
    modifier: Modifier = Modifier,
    style: TextStyle = ZTheme.text.mono,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val shown = remember(value, head, tail) {
        if (value.length <= head + tail + 1) value
        else value.take(head) + "…" + value.takeLast(tail)
    }
    Text(shown, modifier = modifier, style = style, color = color, maxLines = 1)
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
        style = ZTheme.text.mono,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Controles ────────────────────────────────────────────────────────────────

enum class ZButtonVariant { Default, Ghost, Accent, Danger, Outline }
enum class ZButtonSize { Small, Medium, Large }

/**
 * Botón del prototipo: texto en mayúscula con tracking, borde de 1dp y sin
 * elevación. Cinco variantes.
 */
@Composable
fun ZButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ZButtonVariant = ZButtonVariant.Default,
    size: ZButtonSize = ZButtonSize.Medium,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val danger = ZTheme.semantics.danger
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // Accent copia el botón de zoka.network: contorno rosa con texto claro, que
    // se rellena de rosa (texto oscuro) al pasar el mouse.
    val container = when (variant) {
        ZButtonVariant.Default -> scheme.surfaceContainerHigh
        ZButtonVariant.Ghost   -> Color.Transparent
        ZButtonVariant.Accent  -> if (hovered && enabled) scheme.primary else Color.Transparent
        ZButtonVariant.Danger  -> danger.copy(alpha = if (hovered) 0.20f else 0.10f)
        ZButtonVariant.Outline -> Color.Transparent
    }
    val fg = when (variant) {
        ZButtonVariant.Default -> scheme.onSurface
        ZButtonVariant.Ghost   -> if (hovered) scheme.onSurface else scheme.onSurfaceVariant
        ZButtonVariant.Accent  -> if (hovered && enabled) scheme.onPrimary else scheme.onSurface
        ZButtonVariant.Danger  -> danger
        ZButtonVariant.Outline -> scheme.onSurface
    }
    val borderTarget = when (variant) {
        ZButtonVariant.Default -> if (hovered) ZTheme.semantics.borderStrong else scheme.outlineVariant
        ZButtonVariant.Ghost   -> Color.Transparent
        ZButtonVariant.Accent  -> scheme.primary
        ZButtonVariant.Danger  -> danger.copy(alpha = 0.40f)
        ZButtonVariant.Outline -> ZTheme.semantics.borderStrong
    }
    val border by animateColorAsState(borderTarget, tween(), label = "z-button-border")

    val height = when (size) {
        ZButtonSize.Small  -> 28.dp
        ZButtonSize.Medium -> ZTheme.spacing.control
        ZButtonSize.Large  -> ZTheme.spacing.control + 4.dp
    }
    val horizontal = when (size) {
        ZButtonSize.Small  -> 10.dp
        ZButtonSize.Medium -> 14.dp
        ZButtonSize.Large  -> 20.dp
    }
    val shape = MaterialTheme.shapes.small
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = modifier
            .heightIn(min = height)
            .alpha(alpha)
            .background(container, shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
            ) { onClick() }
            .padding(horizontal = horizontal),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            content()
        }
    }
}

/** Texto de botón, con la mayúscula y el tracking ya aplicados. */
@Composable
fun ZButtonText(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = LocalContentColor.current,
        maxLines = 1,
    )
}

/**
 * Marca de Zoka Quantic como en zoka.network: "ZOKA" en el color del texto y
 * "QUANTIC" en el rosa de acento, siempre en una sola línea. `suffix` agrega
 * un dato después de un punto medio (por ejemplo "TESTNET").
 */
@Composable
fun QuanticWordmark(
    style: TextStyle,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    suffixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = scheme.onSurface)) { append("ZOKA ") }
            withStyle(SpanStyle(color = scheme.primary)) { append("QUANTIC") }
            if (suffix != null) {
                withStyle(SpanStyle(color = suffixColor)) { append(" · $suffix") }
            }
        },
        style = style,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

/** Bajada de la marca, en el gris claro del lema de zoka.network (#C2C2C7). */
@Composable
fun QuanticTagline(modifier: Modifier = Modifier) {
    Text(
        "Private by design.",
        style = ZTheme.text.brandTagline,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        modifier = modifier,
    )
}

/** Etiqueta de campo: mono, mayúscula, tracking ancho. */
@Composable
fun ZLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = ZTheme.text.fieldLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Campo de texto del prototipo: alto fijo, relleno `surface-2`, texto mono y
 * borde que pasa a `primary` con el foco.
 *
 * Se construye sobre `BasicTextField` y no sobre `OutlinedTextField` porque el
 * de Material impone un alto mínimo de 56dp que pelea con los 40dp del diseño.
 */
@Composable
fun ZInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    mono: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.small
    val borderTarget = if (focused) scheme.primary.copy(alpha = 0.4f) else scheme.outlineVariant
    val border by animateColorAsState(borderTarget, tween(), label = "z-input-border")
    val style = (if (mono) ZTheme.text.mono else MaterialTheme.typography.bodyMedium)
        .copy(color = scheme.onSurface)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = ZTheme.spacing.control)
            .background(if (focused) ZTheme.semantics.elevated else scheme.surfaceContainerHigh, shape)
            .border(BorderStroke(1.dp, border), shape)
            .onFocusChanged { focused = it.isFocused },
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(scheme.primary),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = style,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(4.dp))
                    trailing()
                }
            }
        },
    )
}

/** Interruptor del prototipo: pista de 24×44dp con pastilla de 16dp. */
@Composable
fun ZToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val track = if (checked) scheme.primary.copy(alpha = 0.3f) else scheme.surfaceContainerHigh
    val borderColor = if (checked) scheme.primary else scheme.outlineVariant
    val thumb = if (checked) scheme.primary else scheme.onSurfaceVariant
    Box(
        modifier
            .size(width = 44.dp, height = 24.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .background(track, CircleShape)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(16.dp)
                .background(thumb, CircleShape)
        )
    }
}

/**
 * Selector segmentado de opción única.
 *
 * Nota: el prototipo no tiene un equivalente exacto — usa grupos de botones —
 * pero varias pantallas de ZSilent ya dependen de esta firma, así que se
 * conserva restilada.
 */
@Composable
fun SegmentedSelector(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Row(
        modifier
            .background(scheme.surfaceContainerHigh, shape)
            .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (active) scheme.primary.copy(alpha = 0.20f) else Color.Transparent,
                        MaterialTheme.shapes.extraSmall
                    )
                    .clickable(enabled = enabled) { onSelect(option) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option.uppercase(),
                    style = ZTheme.text.pill,
                    color = when {
                        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                        active   -> scheme.primary
                        else     -> scheme.onSurfaceVariant
                    },
                )
            }
        }
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ZLabel(label)
        ZInput(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(enabled = enabled) { visible = !visible }
                )
            },
        )
    }
}

/** Botón de copiar con confirmación de 1,2 s, como el del prototipo. */
@Composable
fun CopyButton(value: String, label: String = "Copy", modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { delay(1200); copied = false }
    }
    ZButton(
        onClick = { clipboard.setText(AnnotatedString(value)); copied = true },
        modifier = modifier,
        variant = ZButtonVariant.Default,
        size = ZButtonSize.Small,
        enabled = value.isNotBlank(),
    ) {
        Icon(
            if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = null,
            tint = if (copied) ZTheme.semantics.success else LocalContentColor.current,
            modifier = Modifier.size(12.dp)
        )
        ZButtonText(if (copied) "Copied" else label)
    }
}

@Composable
fun CopyIconButton(value: String, enabled: Boolean = value.isNotBlank()) {
    val clipboard = LocalClipboardManager.current
    IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }, enabled = enabled) {
        Icon(
            Icons.Outlined.ContentCopy,
            contentDescription = "Copy",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Filas y medidores ────────────────────────────────────────────────────────

/**
 * Acentos disponibles para métricas.
 *
 * La paleta nueva es acromática, así que estos seis colapsan sobre los cuatro
 * semánticos: se pierde la distinción entre Blue y Violet. Es intencional y
 * transitorio — cuando las pantallas que lo usan pasen al vocabulario de tonos
 * del prototipo, este enum se puede borrar.
 */
enum class MetricAccent { Green, Blue, Violet, Amber, Rose, Neutral }

@Composable
private fun MetricAccent.resolve(): Color = when (this) {
    MetricAccent.Green   -> ZTheme.semantics.success
    MetricAccent.Blue    -> MaterialTheme.colorScheme.primary
    MetricAccent.Violet  -> MaterialTheme.colorScheme.primary
    MetricAccent.Amber   -> ZTheme.semantics.warning
    MetricAccent.Rose    -> ZTheme.semantics.danger
    MetricAccent.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun StatusCard(
    title: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Panel(modifier = modifier, contentPadding = PaddingValues(ZTheme.spacing.panel)) {
        Stat(label = title, value = value, hint = detail)
    }
}

/**
 * Métrica con riel de acento, cifra grande y medidor opcional.
 *
 * `dimmed` la renderiza apagada para valores que todavía no son reales. Es un
 * control de honestidad visual: un placeholder con el mismo aspecto que una
 * lectura real invita a actuar sobre un número que nadie midió.
 */
@Composable
fun MetricTile(
    label: String,
    value: String,
    caption: String,
    accent: MetricAccent = MetricAccent.Green,
    fraction: Float? = null,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val tint = if (dimmed) scheme.onSurfaceVariant else accent.resolve()
    Row(
        modifier = modifier
            .background(scheme.surface.copy(alpha = 0.6f), shape)
            .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(tint.copy(alpha = if (dimmed) 0.35f else 1f))
        )
        Column(
            modifier = Modifier.padding(ZTheme.spacing.panel),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label.uppercase(), style = ZTheme.text.fieldLabel, color = scheme.onSurfaceVariant)
            Text(
                value,
                style = ZTheme.text.monoLarge,
                color = if (dimmed) scheme.onSurfaceVariant else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (fraction != null) MeterBar(fraction, accent, dimmed)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MeterBar(
    fraction: Float,
    accent: MetricAccent = MetricAccent.Green,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val safe = fraction.coerceIn(0f, 1f)
    val tint = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else accent.resolve()
    val shape = CircleShape
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
    ) {
        Box(
            Modifier
                .fillMaxWidth(safe)
                .height(6.dp)
                .background(tint.copy(alpha = if (dimmed) 0.4f else 1f), shape)
        )
    }
}

@Composable
fun LegendDot(label: String, accent: MetricAccent, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).background(accent.resolve(), CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun labelIcon(label: String): ImageVector? {
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
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = ZTheme.spacing.row / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelWithIcon(label)
        Text(
            value,
            style = ZTheme.text.mono,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun CopyableKeyValueRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = ZTheme.spacing.row / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabelWithIcon(label)
        Row(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = ZTheme.text.mono,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CopyIconButton(value)
        }
    }
}

// ── Estados y varios ─────────────────────────────────────────────────────────

@Composable
fun QrCode(
    value: String,
    modifier: Modifier = Modifier,
    // Una zkq1 tiene unos 1.715 caracteres: con Q no entra en ningun QR (el
    // maximo es 1.273 bytes), asi que esa usa L, mas tamaño y modulos
    // cuadrados, que se leen mejor cuando el codigo es denso.
    errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.Q,
    boxSize: Dp = 224.dp,
    squareModules: Boolean = false,
) {
    // Nivel de correccion Q (25%): al estilizar los modulos se pierde algo de
    // margen de lectura, y Q lo compensa de sobra. Con el nivel por defecto (L)
    // un QR con puntos redondeados puede volverse dificil de escanear con
    // camaras malas o poca luz.
    val matrix = remember(value, errorCorrection) {
        runCatching {
            QRCodeWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                0, 0,
                mapOf(
                    EncodeHintType.ERROR_CORRECTION to errorCorrection,
                    EncodeHintType.MARGIN to 1,
                )
            )
        }.getOrNull()
    }
    // El QR va SIEMPRE oscuro sobre claro, en los dos temas.
    //
    // Antes usaba onSurface sobre surfaceContainerHigh, o sea que en tema
    // oscuro quedaba invertido: modulos claros sobre fondo oscuro. Muchos
    // lectores de telefono no leen QR invertidos, y esto es una direccion de
    // cobro — que no se pueda escanear no es un problema estetico.
    //
    // El marco sigue el tema; solo la zona del codigo queda fija.
    val quiet = Color(0xFFFFFFFF)
    val foreground = Color(0xFF101010)
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .size(boxSize)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .padding(12.dp)
            .background(quiet, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val qr = matrix ?: return@Canvas
            val cell = minOf(size.width / qr.width, size.height / qr.height)
            val originX = (size.width - cell * qr.width) / 2f
            val originY = (size.height - cell * qr.height) / 2f

            // Los tres patrones de deteccion (esquinas) se dibujan aparte como
            // marcos redondeados. Si se dibujaran con puntos sueltos como el
            // resto, los lectores tardan mas en encontrarlos o directamente
            // fallan: son la referencia geometrica que usa el decodificador.
            fun isFinder(x: Int, y: Int): Boolean {
                val n = qr.width
                return (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
            }

            // Modulos de datos como puntos redondeados (o cuadrados llenos).
            val dot = if (squareModules) cell else cell * 0.86f
            val inset = (cell - dot) / 2f
            for (x in 0 until qr.width) {
                for (y in 0 until qr.height) {
                    if (!qr[x, y] || isFinder(x, y)) continue
                    drawRoundRect(
                        color = foreground,
                        topLeft = Offset(originX + x * cell + inset, originY + y * cell + inset),
                        size = Size(dot, dot),
                        cornerRadius = if (squareModules) CornerRadius.Zero else CornerRadius(dot / 2f, dot / 2f),
                    )
                }
            }

            // Esquinas: anillo exterior 7x7 redondeado + nucleo 3x3 redondeado.
            val n = qr.width
            listOf(0 to 0, n - 7 to 0, 0 to n - 7).forEach { (fx, fy) ->
                val left = originX + fx * cell
                val top = originY + fy * cell
                val outer = cell * 7f
                drawRoundRect(
                    color = foreground,
                    topLeft = Offset(left, top),
                    size = Size(outer, outer),
                    cornerRadius = CornerRadius(cell * 2f, cell * 2f),
                    style = Stroke(width = cell),
                )
                drawRoundRect(
                    color = foreground,
                    topLeft = Offset(left + cell * 2f, top + cell * 2f),
                    size = Size(cell * 3f, cell * 3f),
                    cornerRadius = CornerRadius(cell * 1f, cell * 1f),
                )
            }
        }
    }
}

@Composable
fun SectionEyebrow(text: String) {
    Text(
        text.uppercase(),
        style = ZTheme.text.eyebrow,
        color = MaterialTheme.colorScheme.primary
    )
}

/** Se conserva por compatibilidad. Lo nuevo es [Pill]. */
@Composable
fun StatusPill(text: String, active: Boolean, modifier: Modifier = Modifier) {
    Pill(text = text, tone = if (active) ZTone.On else ZTone.Off, modifier = modifier)
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, scheme.outlineVariant), MaterialTheme.shapes.medium)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon ?: Icons.Outlined.Inbox,
            contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Text(text, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        if (sub != null) {
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp)
            )
        }
    }
}

@Composable
fun ErrorText(text: String) {
    Text(text, color = ZTheme.semantics.danger, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun HintText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun ThinDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = DividerDefaults.Thickness)
}

// ── Colores heredados para llamadas Material que quedan por migrar ───────────
// Las pantallas que todavía usan Button/OutlinedTextField de Material los
// consumen. Se van borrando a medida que las etapas 1-14 migran a ZButton y
// ZInput.

@Composable
fun zFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = ZTheme.semantics.elevated,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
fun zButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
)

@Composable
fun zPastelButtonColors(accent: Color) = ButtonDefaults.buttonColors(
    containerColor = accent,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
)

/** El prototipo no usa elevación en ninguna parte. */
@Composable
fun zButtonElevation() = ButtonDefaults.buttonElevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    hoveredElevation = 0.dp
)

@Composable
fun zOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
)

@Composable
fun zSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor    = MaterialTheme.colorScheme.primary,
    checkedTrackColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    checkedBorderColor   = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor  = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceContainerHigh,
    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
)
