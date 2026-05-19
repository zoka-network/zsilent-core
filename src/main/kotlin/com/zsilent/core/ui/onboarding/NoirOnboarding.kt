package com.zsilent.core.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShieldMoon
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import com.zsilent.core.models.OnboardingStep
import com.zsilent.core.models.SeedWordCount
import com.zsilent.core.models.WalletSetupMode
import com.zsilent.core.ui.ZSilentAppState
import com.zsilent.core.ui.ZSilentUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
//  PALETTE
// ─────────────────────────────────────────────────────────────────────────────

// Premium fintech palette — deep blacks + lilac/violet accents.
// Names kept for compatibility with existing call sites.
internal val NoirInk = Color(0xFF07080C)              // app background
internal val NoirSurface = Color(0xFF10131A)          // standard card
internal val NoirSurfaceElev = Color(0xFF141822)      // elevated card / hover
internal val NoirOutline = Color(0xFF1E2430)          // hairline border
internal val NoirOutlineStrong = Color(0xFF2A3140)    // emphasized border
internal val NoirText = Color(0xFFF4F4F5)             // primary text (soft white)
internal val NoirTextMuted = Color(0xFF9CA3AF)        // secondary text (cool grey)
internal val NoirTextSubtle = Color(0xFF6B7280)       // tertiary / disabled
internal val NoirSilver = Color(0xFFE9D5FF)           // very light lilac highlight
internal val NoirAccent = Color(0xFFC084FC)           // primary lilac accent
internal val NoirSuccess = Color(0xFFA78BFA)          // active / positive (violet family)
internal val NoirError = Color(0xFFE08B8B)            // desaturated rose
internal val NoirInfo = Color(0xFFD8B4FE)             // soft lavender for info

// ─────────────────────────────────────────────────────────────────────────────
//  BACKGROUND
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Application background. A deep carbon surface (#07080C) with two extremely
 * soft lilac glows — one top-left, one bottom-right — and a vertical gradient
 * toward darker tones. The result is calm, premium and silent: enough depth
 * to never look flat, but no animation and no neon. Pure black is avoided.
 */
@Composable
fun NoirBackground(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(NoirInk)
    ) {
        val w = size.width
        val h = size.height
        // Soft top-left lilac wash (anchors the eye, fades quickly).
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NoirAccent.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.18f),
                radius = (w.coerceAtLeast(h)) * 0.55f
            ),
            size = Size(w, h)
        )
        // Mirror glow bottom-right, even softer, for compositional balance.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NoirInfo.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(w * 0.82f, h * 0.85f),
                radius = (w.coerceAtLeast(h)) * 0.45f
            ),
            size = Size(w, h)
        )
        // Subtle vertical vignette without resorting to pure black.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF03040A).copy(alpha = 0.55f)),
                startY = h * 0.55f,
                endY = h
            ),
            size = Size(w, h)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BUTTONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (enabled) Color(0xFFE8EAEE) else Color(0xFF24292F)
    val fg = if (enabled) NoirInk else NoirTextSubtle
    Row(
        modifier = modifier
            .background(bg, shape)
            .border(BorderStroke(1.dp, NoirOutlineStrong), shape)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (loading) "Working..." else label,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun SecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val border = if (enabled) NoirOutlineStrong else NoirOutline
    val fg = if (enabled) NoirSilver else NoirTextSubtle
    Row(
        modifier = modifier
            .background(Color.Transparent, shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    }
}

@Composable
fun DangerButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val color = if (enabled) NoirError else NoirError.copy(alpha = 0.4f)
    Row(
        modifier = modifier
            .background(Color.Transparent, shape)
            .border(BorderStroke(1.dp, color), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CARDS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoirCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(28.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .background(NoirSurface, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun SecurityWarningCard(
    title: String,
    detail: String,
    accent: Color = NoirError.copy(alpha = 0.85f)
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirSurfaceElev, shape)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.4f)), shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(title, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(detail, color = NoirTextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHELL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingShell(
    step: OnboardingStep,
    busy: Boolean,
    canContinue: Boolean,
    primaryLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    error: String?,
    canBack: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        NoirBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: brand + step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(NoirSurface, RoundedCornerShape(9.dp))
                            .border(BorderStroke(1.dp, NoirOutlineStrong), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Z", color = NoirText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("ZSILENT WALLET", color = NoirText, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp, fontSize = 13.sp)
                        Text("Zoka private gateway", color = NoirTextMuted, fontSize = 11.sp, letterSpacing = 0.4.sp)
                    }
                }
                StepIndicator(step)
            }

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                NoirCard(
                    modifier = Modifier
                        .widthIn(min = 520.dp, max = 760.dp)
                        .heightIn(min = 360.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        content()
                        error?.let {
                            SecurityWarningCard(
                                title = "Something went wrong",
                                detail = it,
                                accent = NoirError
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.widthIn(min = 520.dp, max = 760.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canBack) {
                    SecondaryButton(label = "Back", enabled = !busy, onClick = onBack)
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                PrimaryButton(
                    label = primaryLabel,
                    enabled = canContinue && !busy,
                    loading = busy,
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: OnboardingStep) {
    val steps = OnboardingStep.entries
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("STEP", color = NoirTextSubtle, fontSize = 11.sp, letterSpacing = 1.4.sp)
        Text(
            "${current.ordinal + 1}",
            color = NoirText,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text("/", color = NoirTextSubtle, fontFamily = FontFamily.Monospace)
        Text("${steps.size}", color = NoirTextMuted, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            steps.forEachIndexed { idx, _ ->
                val active = idx <= current.ordinal
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 3.dp)
                        .background(
                            if (active) NoirAccent else NoirOutline,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TYPOGRAPHY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScreenEyebrow(text: String) {
    Text(
        text.uppercase(),
        color = NoirTextSubtle,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        color = NoirText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = 0.2.sp
    )
}

@Composable
fun ScreenBody(text: String) {
    Text(
        text,
        color = NoirTextMuted,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  FORM ELEMENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoirPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isOk: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    val border = when {
        isError -> NoirError
        isOk -> NoirSuccess
        else -> NoirOutlineStrong
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = NoirTextMuted) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        modifier = modifier,
        trailingIcon = {
            Icon(
                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = null,
                tint = NoirSilver,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { visible = !visible }
            )
        },
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = NoirSurfaceElev,
            unfocusedContainerColor = NoirSurfaceElev,
            disabledContainerColor = NoirSurfaceElev,
            errorContainerColor = NoirSurfaceElev,
            focusedTextColor = NoirText,
            unfocusedTextColor = NoirText,
            focusedIndicatorColor = border,
            unfocusedIndicatorColor = border,
            cursorColor = NoirAccent
        )
    )
}

@Composable
fun PasswordStrengthMeter(password: String) {
    val score = passwordScore(password)
    val rules = passwordRules(password)
    val bars = 4
    val active = score.coerceIn(0, bars)
    val color = when (active) {
        0 -> NoirOutline
        1 -> NoirError
        2 -> Color(0xFFE2A348)
        3 -> Color(0xFF8FB48F)
        else -> NoirSuccess
    }
    val label = when (active) {
        0 -> "empty"
        1 -> "weak"
        2 -> "fair"
        3 -> "strong"
        else -> "excellent"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                repeat(bars) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (idx < active) color else NoirOutline, RoundedCornerShape(2.dp))
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(label.uppercase(), color = NoirTextMuted, fontSize = 10.sp, letterSpacing = 1.2.sp)
        }
        rules.forEach { (text, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (ok) Icons.Outlined.Check else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (ok) NoirSuccess else NoirTextSubtle,
                    modifier = Modifier.size(13.dp)
                )
                Text(text, color = if (ok) NoirSilver else NoirTextSubtle, fontSize = 12.sp)
            }
        }
    }
}

private fun passwordScore(p: String): Int {
    if (p.isEmpty()) return 0
    var s = 0
    if (p.length >= 8) s++
    if (p.length >= 12) s++
    if (p.any { it.isUpperCase() } && p.any { it.isLowerCase() }) s++
    if (p.any { it.isDigit() }) s++
    if (p.any { !it.isLetterOrDigit() }) s++
    return s.coerceAtMost(4)
}

private fun passwordRules(p: String): List<Pair<String, Boolean>> = listOf(
    "Minimum 8 characters" to (p.length >= 8),
    "Uppercase and lowercase" to (p.any { it.isUpperCase() } && p.any { it.isLowerCase() }),
    "At least one number" to p.any { it.isDigit() },
    "At least one symbol" to p.any { !it.isLetterOrDigit() }
)

@Composable
fun ResponsibilityCheck(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = NoirAccent,
                uncheckedColor = NoirOutlineStrong,
                checkmarkColor = NoirInk
            )
        )
        Text(label, color = NoirText, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEED WORD BOX
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SeedWordBox(
    index: Int,
    word: String,
    modifier: Modifier = Modifier,
    revealed: Boolean = true
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .background(NoirSurfaceElev, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "%02d".format(index),
            color = NoirTextSubtle,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Text(
            if (revealed) word else "•".repeat(word.length.coerceAtLeast(4)),
            color = if (revealed) NoirText else NoirTextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .alpha(if (revealed) 1f else 0.5f)
        )
    }
}

@Composable
fun EditableSeedWordBox(
    index: Int,
    word: String,
    onWordChange: (String) -> Unit,
    onPaste: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val border = if (word.isBlank() || isValidBip39Like(word)) NoirOutline else NoirError
    Row(
        modifier = modifier
            .background(NoirSurfaceElev, shape)
            .border(BorderStroke(1.dp, border), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("%02d".format(index), color = NoirTextSubtle, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        OutlinedTextField(
            value = word,
            onValueChange = { raw ->
                if (raw.contains(' ') && onPaste != null) {
                    onPaste(raw)
                } else {
                    onWordChange(raw.trim().lowercase())
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedTextColor = NoirText,
                unfocusedTextColor = NoirText,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = NoirAccent
            )
        )
    }
}

private fun isValidBip39Like(word: String): Boolean =
    word.isNotBlank() && word.length in 3..10 && word.all { it.isLetter() }

// ─────────────────────────────────────────────────────────────────────────────
//  RANDOM WORD CHIP (seed confirmation)
// ─────────────────────────────────────────────────────────────────────────────

enum class ChipState { Idle, Correct, Wrong }

@Composable
fun WordChip(
    word: String,
    state: ChipState,
    enabled: Boolean,
    shakeKey: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val border = when (state) {
        ChipState.Correct -> NoirSuccess
        ChipState.Wrong -> NoirError
        ChipState.Idle -> if (enabled) NoirOutlineStrong else NoirOutline
    }
    val bg = when (state) {
        ChipState.Correct -> NoirSuccess.copy(alpha = 0.10f)
        ChipState.Wrong -> NoirError.copy(alpha = 0.10f)
        ChipState.Idle -> NoirSurfaceElev
    }
    val fg = when {
        !enabled -> NoirTextSubtle
        state == ChipState.Correct -> NoirSuccess
        state == ChipState.Wrong -> NoirError
        else -> NoirText
    }

    // Shake animation when state == Wrong, keyed by shakeKey to retrigger.
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(shakeKey, state) {
        if (state == ChipState.Wrong) {
            val pattern = floatArrayOf(-8f, 8f, -6f, 6f, -3f, 3f, 0f)
            for (target in pattern) {
                offsetX.animateTo(target, tween(durationMillis = 38))
            }
        } else {
            offsetX.snapTo(0f)
        }
    }

    Row(
        modifier = Modifier
            .graphicsLayer { translationX = offsetX.value }
            .background(bg, shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(enabled = enabled && state != ChipState.Correct, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state == ChipState.Correct) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = NoirSuccess, modifier = Modifier.size(13.dp))
        }
        Text(word, color = fg, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STEP COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WelcomeStepNoir() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Welcome")
        ScreenTitle("Your private gateway to Zoka.")
        ScreenBody(
            "Create, restore and protect your wallet locally. Your keys never leave this device. " +
                "ZSilent runs the full node, mining engine and zero-knowledge transfer flow under your sole control."
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureBlock(
                Icons.Outlined.Lock,
                "Self-custody",
                "Keys, seeds and transfers stay on this machine.",
                Modifier.weight(1f)
            )
            FeatureBlock(
                Icons.Outlined.ShieldMoon,
                "Privacy first",
                "Stealth addresses, ring signatures, zk-SNARKs.",
                Modifier.weight(1f)
            )
            FeatureBlock(
                Icons.Outlined.AccountTree,
                "Full node",
                "Validate every block. No third-party trust.",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureBlock(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .background(NoirSurfaceElev, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NoirSilver, modifier = Modifier.size(20.dp))
        Text(title, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(text, color = NoirTextMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
fun SelfCustodyStepNoir(ui: ZSilentUiState, appState: ZSilentAppState) {
    var c1 by remember { mutableStateOf(ui.onboardingSelfCustodyConfirmed) }
    var c2 by remember { mutableStateOf(ui.onboardingSelfCustodyConfirmed) }
    var c3 by remember { mutableStateOf(ui.onboardingSelfCustodyConfirmed) }
    var c4 by remember { mutableStateOf(ui.onboardingSelfCustodyConfirmed) }

    LaunchedEffect(c1, c2, c3, c4) {
        val all = c1 && c2 && c3 && c4
        if (all != ui.onboardingSelfCustodyConfirmed) {
            appState.setOnboardingSelfCustodyConfirmed(all)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Responsibility")
        ScreenTitle("Self-custody acknowledgement")
        SecurityWarningCard(
            title = "Read carefully",
            detail = "Losing the recovery phrase means permanent loss of access to the wallet and its funds. " +
                "Nobody at Zoka can recover them for you. The password on this device only protects local access.",
            accent = NoirInfo
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ResponsibilityCheck("I understand that I am responsible for my wallet.", c1) { c1 = it }
            ResponsibilityCheck("I understand that losing my recovery phrase means losing access.", c2) { c2 = it }
            ResponsibilityCheck("I understand that nobody from Zoka can recover my funds.", c3) { c3 = it }
            ResponsibilityCheck("I will never share my recovery phrase.", c4) { c4 = it }
        }
    }
}

@Composable
fun WalletChoiceStepNoir(ui: ZSilentUiState, appState: ZSilentAppState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Account")
        ScreenTitle("Create or restore your wallet")
        ScreenBody("Pick how you want to start. You can change the name later.")

        OutlinedTextField(
            value = ui.onboardingWalletName,
            onValueChange = appState::setOnboardingWalletName,
            label = { Text("Account name", color = NoirTextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = NoirSurfaceElev,
                unfocusedContainerColor = NoirSurfaceElev,
                focusedTextColor = NoirText,
                unfocusedTextColor = NoirText,
                focusedIndicatorColor = NoirOutlineStrong,
                unfocusedIndicatorColor = NoirOutlineStrong,
                cursorColor = NoirAccent
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ChoiceCard(
                title = "Create new wallet",
                detail = "Generate a fresh recovery phrase. Recommended for new users.",
                selected = ui.walletSetupMode == WalletSetupMode.CreateNew,
                modifier = Modifier.weight(1f),
                onClick = { appState.setWalletSetupMode(WalletSetupMode.CreateNew) }
            )
            ChoiceCard(
                title = "Restore existing wallet",
                detail = "Enter a 12 or 24 word phrase you already own.",
                selected = ui.walletSetupMode == WalletSetupMode.ImportSeed,
                modifier = Modifier.weight(1f),
                onClick = { appState.setWalletSetupMode(WalletSetupMode.ImportSeed) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SeedCountPill("12 words", ui.seedWordCount == SeedWordCount.Twelve) { appState.setSeedWordCount(SeedWordCount.Twelve) }
            SeedCountPill("24 words", ui.seedWordCount == SeedWordCount.TwentyFour) { appState.setSeedWordCount(SeedWordCount.TwentyFour) }
        }

        if (ui.walletSetupMode == WalletSetupMode.ImportSeed) {
            RestoreSeedGrid(ui, appState)
        }
    }
}

@Composable
private fun SeedCountPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val border = if (selected) NoirAccent else NoirOutlineStrong
    val fg = if (selected) NoirText else NoirTextMuted
    Row(
        modifier = Modifier
            .background(if (selected) NoirSurfaceElev else Color.Transparent, shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    detail: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) NoirAccent else NoirOutline
    Column(
        modifier = modifier
            .background(if (selected) NoirSurfaceElev else NoirSurface, shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(detail, color = NoirTextMuted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun RestoreSeedGrid(ui: ZSilentUiState, appState: ZSilentAppState) {
    val wordCount = ui.seedWordCount.words
    val words = remember(ui.onboardingSeedPhrase, wordCount) {
        val parsed = ui.onboardingSeedPhrase
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        (0 until wordCount).map { idx -> parsed.getOrNull(idx) ?: "" }.toMutableStateList()
    }
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Paste recovery phrase") {
                val pasted = clipboard.getText()?.text.orEmpty()
                val parts = pasted.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                for (i in 0 until wordCount) {
                    words[i] = parts.getOrNull(i) ?: ""
                }
                appState.setOnboardingSeedPhrase(words.joinToString(" ").trim())
            }
            SecondaryButton("Clear") {
                for (i in 0 until wordCount) words[i] = ""
                appState.setOnboardingSeedPhrase("")
            }
        }
        // Render in 3 columns
        val cols = 3
        val rows = (wordCount + cols - 1) / cols
        repeat(rows) { rowIdx ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(cols) { colIdx ->
                    val idx = rowIdx * cols + colIdx
                    if (idx < wordCount) {
                        EditableSeedWordBox(
                            index = idx + 1,
                            word = words[idx],
                            onWordChange = { w ->
                                words[idx] = w
                                appState.setOnboardingSeedPhrase(words.joinToString(" ").trim())
                            },
                            onPaste = { raw ->
                                val parts = raw.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                                for (i in 0 until wordCount) {
                                    words[i] = parts.getOrNull(i) ?: ""
                                }
                                appState.setOnboardingSeedPhrase(words.joinToString(" ").trim())
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryRevealStepNoir(ui: ZSilentUiState) {
    val phrase = ui.onboardingRecoveryPhrase ?: ui.onboardingSeedPhrase
    if (ui.onboardingBusy || phrase.isBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenEyebrow("Recovery phrase")
            ScreenTitle("Generating recovery phrase...")
            ScreenBody("Deriving keys locally using a cryptographic RNG.")
        }
        return
    }
    val words = phrase.trim().split(Regex("\\s+"))
    var revealed by remember { mutableStateOf(false) }
    var unlockTime by remember { mutableStateOf(0L) }
    LaunchedEffect(revealed) {
        if (revealed) {
            unlockTime = System.currentTimeMillis() + 4_000L
        }
    }
    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(revealed) {
        while (revealed && now.value < unlockTime) {
            now.value = System.currentTimeMillis()
            delay(120)
        }
    }
    val canConfirm = revealed && now.value >= unlockTime

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Recovery phrase")
        ScreenTitle("Write these ${words.size} words down")
        ScreenBody(
            "Order matters. Store the phrase offline. Do not screenshot, do not paste into chats or email, " +
                "do not upload to a cloud service."
        )
        SecurityWarningCard(
            title = "This is the only way to recover your wallet",
            detail = "If you lose these words, the wallet and its funds cannot be recovered.",
            accent = NoirError
        )

        val cols = 4
        val rows = (words.size + cols - 1) / cols
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(rows) { rowIdx ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(cols) { colIdx ->
                        val idx = rowIdx * cols + colIdx
                        if (idx < words.size) {
                            SeedWordBox(
                                index = idx + 1,
                                word = words[idx],
                                revealed = revealed,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (revealed) {
                SecondaryButton("Hide phrase") { revealed = false }
            } else {
                SecondaryButton("Reveal phrase") { revealed = true }
            }
            if (revealed && !canConfirm) {
                Text(
                    "Read carefully. The continue button unlocks in a moment.",
                    color = NoirTextSubtle,
                    fontSize = 11.sp
                )
            } else if (canConfirm) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = NoirSuccess, modifier = Modifier.size(14.dp))
                    Text("Ready. Continue when you have it written down.", color = NoirTextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun VerifySeedStepNoir(ui: ZSilentUiState, appState: ZSilentAppState) {
    val phrase = (ui.onboardingRecoveryPhrase ?: ui.onboardingSeedPhrase).trim()
    val words = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size < 4) {
        ScreenEyebrow("Verify"); ScreenTitle("Recovery phrase is not ready.")
        return
    }
    val checks = remember(words.size) {
        when {
            words.size >= 24 -> listOf(4, 11, 17)
            words.size >= 12 -> listOf(2, 5, 8)
            else -> listOf(0, 1, words.size - 1)
        }
    }
    val targets = checks.map { it to words[it] }

    // distractor words from the phrase + a few permutations
    val pool = remember(words, checks) {
        val targetWords = checks.map { words[it] }.toSet()
        val others = words.filter { it !in targetWords }.distinct().shuffled().take(6)
        (targetWords + others).distinct().shuffled()
    }

    val progress = remember { mutableStateOf(0) }
    val correctSet = remember { mutableStateListOf<String>() }
    var lastWrong by remember { mutableStateOf<String?>(null) }
    var wrongTick by remember { mutableStateOf(0) }

    LaunchedEffect(progress.value) {
        if (progress.value >= targets.size) {
            // Commit verification by writing the first two expected words into
            // the existing onboarding state fields. canContinue() reads those.
            appState.setOnboardingSeedCheckFirst(words.getOrElse(checks[0]) { "" })
            appState.setOnboardingSeedCheckSecond(words.getOrElse(checks.getOrElse(1) { 0 }) { "" })
        } else {
            appState.setOnboardingSeedCheckFirst("")
            appState.setOnboardingSeedCheckSecond("")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Verify")
        ScreenTitle("Confirm your recovery phrase")
        ScreenBody("Tap the right word for each missing position, in order.")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            targets.forEachIndexed { idx, (position, _) ->
                val filled = idx < progress.value
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(NoirSurfaceElev, RoundedCornerShape(10.dp))
                        .border(
                            BorderStroke(1.dp, if (filled) NoirSuccess else NoirOutline),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Word #${position + 1}",
                        color = NoirTextSubtle,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        if (filled) correctSet[idx] else "—",
                        color = if (filled) NoirSuccess else NoirTextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Chip pool
        WrapRow(spacing = 8.dp) {
            for (word in pool) {
                val state = when {
                    word in correctSet -> ChipState.Correct
                    word == lastWrong -> ChipState.Wrong
                    else -> ChipState.Idle
                }
                WordChip(
                    word = word,
                    state = state,
                    enabled = state != ChipState.Correct,
                    shakeKey = wrongTick,
                    onClick = {
                        if (progress.value >= targets.size) return@WordChip
                        val expected = targets[progress.value].second
                        if (word == expected) {
                            correctSet.add(word)
                            progress.value = progress.value + 1
                            lastWrong = null
                        } else {
                            lastWrong = word
                            wrongTick++
                        }
                    }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Reset selection") {
                correctSet.clear()
                progress.value = 0
                lastWrong = null
                appState.setOnboardingSeedCheckFirst("")
                appState.setOnboardingSeedCheckSecond("")
            }
            if (progress.value >= targets.size) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = NoirSuccess, modifier = Modifier.size(14.dp))
                    Text("Phrase verified.", color = NoirSuccess, fontSize = 12.sp)
                }
            } else if (lastWrong != null) {
                Text("Incorrect word. Try again.", color = NoirError, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WrapRow(spacing: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        content()
    }
}

@Composable
fun CreatePasswordStepNoir(ui: ZSilentUiState, appState: ZSilentAppState) {
    val match = ui.onboardingPassword.isNotBlank() &&
        ui.onboardingPassword == ui.onboardingPasswordConfirm
    val mismatch = ui.onboardingPasswordConfirm.isNotBlank() &&
        ui.onboardingPassword != ui.onboardingPasswordConfirm
    val passwordOk = ui.onboardingPassword.length >= 8
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Local password")
        ScreenTitle("Encrypt this device")
        ScreenBody(
            "This password encrypts your wallet on this device. It does not replace your recovery phrase. " +
                "If you forget it, you can restore from the phrase."
        )

        NoirPasswordField(
            value = ui.onboardingPassword,
            onValueChange = appState::setOnboardingPassword,
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            isOk = passwordOk
        )
        NoirPasswordField(
            value = ui.onboardingPasswordConfirm,
            onValueChange = appState::setOnboardingPasswordConfirm,
            label = "Confirm password",
            modifier = Modifier.fillMaxWidth(),
            isError = mismatch,
            isOk = match
        )

        if (mismatch) {
            Text("Passwords do not match.", color = NoirError, fontSize = 12.sp)
        }

        PasswordStrengthMeter(ui.onboardingPassword)
    }
}

@Composable
fun AccountStepNoir(ui: ZSilentUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Account ready")
        if (ui.onboardingBusy || ui.onboardingWalletAddress.isBlank()) {
            ScreenTitle("Creating wallet...")
            ScreenBody("Encrypting keys, deriving stealth addresses, registering in local storage.")
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(NoirSuccess.copy(alpha = 0.16f), CircleShape)
                    .border(BorderStroke(1.dp, NoirSuccess.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = NoirSuccess, modifier = Modifier.size(18.dp))
            }
            ScreenTitle("Wallet created successfully.")
        }
        ScreenBody("Your wallet is encrypted locally and ready to use.")

        val shape = RoundedCornerShape(10.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NoirSurfaceElev, shape)
                .border(BorderStroke(1.dp, NoirOutline), shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DetailRow("Account", ui.onboardingWalletName.ifBlank { "main" })
            DetailRow("Receive address", ui.onboardingWalletAddress)
            DetailRow("Auto-lock", "${ui.onboardingAutoLockMinutes} minutes")
            DetailRow("Encryption", "device-local")
        }
        SecurityWarningCard(
            title = "Backup reminder",
            detail = "Make sure your recovery phrase is written down before you fund this wallet.",
            accent = NoirInfo
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NoirTextSubtle, fontSize = 11.sp, letterSpacing = 0.6.sp)
        Text(
            value,
            color = NoirText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun PrivacyStepNoir(ui: ZSilentUiState, appState: ZSilentAppState) {
    LaunchedEffect(Unit) {
        if (!ui.onboardingPrivacyConfirmed) {
            // No-op; user must explicitly toggle.
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Privacy")
        ScreenTitle("Privacy defaults")
        ScreenBody("Tune the privacy posture for first run. Everything below can be changed later.")

        ToggleRowNoir("Hide balances on screen", ui.balancesHidden) { appState.toggleBalances() }
        ToggleRowNoir("Run full node by default", ui.onboardingNodeEnabled, appState::setOnboardingNodeEnabled)
        ToggleRowNoir("Start mining after setup", ui.onboardingMiningEnabled, appState::setOnboardingMiningEnabled)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            ResponsibilityCheck(
                "Use privacy-first defaults: no telemetry, redacted logs, local-only secrets.",
                ui.onboardingPrivacyConfirmed,
                appState::setOnboardingPrivacyConfirmed
            )
        }
    }
}

@Composable
private fun ToggleRowNoir(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirSurfaceElev, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NoirText, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 18.dp)
                .background(if (checked) NoirAccent else NoirOutline, RoundedCornerShape(9.dp))
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (checked) NoirInk else NoirTextMuted, CircleShape)
            )
        }
    }
}

@Composable
fun TutorialStepNoir(ui: ZSilentUiState, appState: ZSilentAppState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Quick tour")
        ScreenTitle("What's inside the wallet")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TourRow("Wallet", "Accounts, backups, lock controls.")
            TourRow("Send / Receive", "Private transfers with zero-knowledge proofs.")
            TourRow("Node", "Full node, sync state, RPC settings.")
            TourRow("Mining", "Local PoW mining and reward queue.")
            TourRow("Privacy", "Tor routing, log redaction, key control.")
        }
        ResponsibilityCheck(
            "I am ready to open the dashboard.",
            ui.onboardingTutorialConfirmed,
            appState::setOnboardingTutorialConfirmed
        )
    }
}

@Composable
private fun TourRow(title: String, detail: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirSurfaceElev, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(detail, color = NoirTextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DashboardReadyStepNoir(ui: ZSilentUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenEyebrow("Ready")
        ScreenTitle("Everything is set up.")
        ScreenBody("Open the dashboard to start using ZSilent. Your wallet is encrypted and waiting.")
        val shape = RoundedCornerShape(10.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NoirSurfaceElev, shape)
                .border(BorderStroke(1.dp, NoirOutline), shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DetailRow("Account", ui.onboardingWalletName.ifBlank { "main" })
            DetailRow("Address", ui.onboardingWalletAddress.ifBlank { "not available" })
            DetailRow("Status", "locked, ready to unlock")
            DetailRow("Auto-lock", "${ui.onboardingAutoLockMinutes} min")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DASHBOARD PRIMITIVES (shared with the main sections)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Page shell for the main app sections (Dashboard, Wallet, etc.) — noir
 * background with brand eyebrow, big title and subtitle.
 */
@Composable
fun NoirScreenShell(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoirInk)
    ) {
        NoirBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(eyebrow.uppercase(), color = NoirTextSubtle, fontSize = 11.sp, letterSpacing = 1.8.sp)
                Text(title, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
                Text(subtitle, color = NoirTextMuted, fontSize = 12.sp)
            }
            content()
        }
    }
}

/** Compact status pill used in the dashboard top row. */
@Composable
fun NoirStatusPill(
    label: String,
    value: String,
    detail: String,
    tone: NoirStatusTone = NoirStatusTone.Neutral,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val dot = when (tone) {
        NoirStatusTone.Ok -> NoirSuccess
        NoirStatusTone.Warn -> Color(0xFFE0B260)
        NoirStatusTone.Bad -> NoirError
        NoirStatusTone.Neutral -> NoirSilver
    }
    Column(
        modifier = modifier
            .background(NoirSurface, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dot, CircleShape)
            )
            Text(label.uppercase(), color = NoirTextSubtle, fontSize = 10.sp, letterSpacing = 1.4.sp)
        }
        Text(value, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        Text(detail, color = NoirTextMuted, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

enum class NoirStatusTone { Ok, Warn, Bad, Neutral }

/** Numeric metric card with sparkline background, noir styled. */
@Composable
fun NoirMetricCard(
    label: String,
    value: String,
    samples: List<Double>,
    accent: Color = NoirSilver,
    hidden: Boolean = false,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .background(NoirSurface, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label.uppercase(), color = NoirTextSubtle, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text(
            if (hidden) "••••" else value,
            color = NoirText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        if (detail != null) {
            Text(detail, color = NoirTextMuted, fontSize = 10.sp, letterSpacing = 0.3.sp)
        }
        NoirSparkline(
            samples = samples,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
    }
}

@Composable
internal fun NoirSparkline(samples: List<Double>, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val maxV = (samples.maxOrNull() ?: 0.0).let { if (it <= 0.0) 1.0 else it }
        val minV = samples.minOrNull() ?: 0.0
        val span = (maxV - minV).coerceAtLeast(1e-9)
        val stepX = w / (samples.size - 1).coerceAtLeast(1)
        val pts = samples.mapIndexed { idx, v ->
            val nx = idx * stepX
            val ny = h - (((v - minV) / span) * h).toFloat().coerceIn(0f, h)
            Offset(nx, ny)
        }
        for (i in 1 until pts.size) {
            drawLine(
                color = accent.copy(alpha = 0.8f),
                start = pts[i - 1],
                end = pts[i],
                strokeWidth = 1.5f
            )
        }
        drawCircle(accent, radius = 2.5f, center = pts.last())
    }
}

/** Noir-styled section panel used to group dashboard content. */
@Composable
fun NoirSection(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .background(NoirSurface, shape)
            .border(BorderStroke(1.dp, NoirOutline), shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = NoirText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (meta != null) {
                Text(meta.uppercase(), color = NoirTextSubtle, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
        }
        content()
    }
}

/** Simple label : value row, noir styled. */
@Composable
fun NoirKeyValue(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NoirTextMuted, fontSize = 12.sp)
        Text(
            value,
            color = NoirText,
            fontSize = 12.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        )
    }
}

/**
 * Returns a human "Ns / Nm / Nh / Nd ago" string from a Unix timestamp in
 * seconds, or "—" when the source is zero.
 */
fun formatTimeAgo(unixSeconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (unixSeconds <= 0L) return "—"
    val deltaSec = (nowMillis / 1000L - unixSeconds).coerceAtLeast(0)
    return when {
        deltaSec < 60 -> "${deltaSec}s ago"
        deltaSec < 3_600 -> "${deltaSec / 60}m ago"
        deltaSec < 86_400 -> "${deltaSec / 3_600}h ago"
        else -> "${deltaSec / 86_400}d ago"
    }
}
