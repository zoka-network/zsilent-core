package com.zsilent.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  Sistema de diseño portado de ZokaNetwork/zoka-whisper-desk.
//
//  Ver docs/GUIA_CAMBIO_VISUAL.md para el mapeo completo y las decisiones.
//
//  Tres cosas que conviene saber antes de tocar este archivo:
//
//  1. Los grises son neutros puros (croma 0). No llevan tinte.
//
//  2. En oscuro, background/surface/surface-2 son casi indistinguibles a ojo
//     (#000000 / #020202 / #070707). Es intencional: la separación entre
//     superficies la producen los BORDES, no los rellenos. Si un panel "no se
//     ve", falta el borde de 1dp en `outlineVariant` — no aclarar el relleno.
//
//  3. El acento es acromático: blanco en oscuro, casi negro en claro. El color
//     existe únicamente como los cuatro estados semánticos de ZSemanticColors.
// ─────────────────────────────────────────────────────────────────────────────

private val Ink              = Color(0xFF000000)
private val Paper            = Color(0xFFF8F8F8)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary                 = Paper,
    onPrimary               = Ink,
    primaryContainer        = Color(0xFF1B1B1B),
    onPrimaryContainer      = Paper,
    secondary               = Color(0xFF070707),
    onSecondary             = Paper,
    secondaryContainer      = Color(0xFF0F0F0F),
    onSecondaryContainer    = Paper,
    tertiary                = Color(0xFFA7AEBB),
    onTertiary              = Ink,
    background              = Ink,
    onBackground            = Paper,
    surface                 = Color(0xFF020202),
    onSurface               = Paper,
    surfaceVariant          = Color(0xFF090909),
    onSurfaceVariant        = Color(0xFF868686),
    outline                 = Color(0xFF333333),
    outlineVariant          = Color(0xFF1B1B1B),
    error                   = Color(0xFFFF645F),
    onError                 = Ink,
    errorContainer          = Color(0xFF2A0F0E),
    onErrorContainer        = Color(0xFFFF645F),
    inverseSurface          = Paper,
    inverseOnSurface        = Ink,
    surfaceContainerLowest  = Ink,
    surfaceContainerLow     = Color(0xFF020202),
    surfaceContainer        = Color(0xFF020202),
    surfaceContainerHigh    = Color(0xFF070707),
    surfaceContainerHighest = Color(0xFF0F0F0F),
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary                 = Color(0xFF090909),
    onPrimary               = Color(0xFFFCFCFC),
    primaryContainer        = Color(0xFFD7D7D7),
    onPrimaryContainer      = Color(0xFF090909),
    secondary               = Color(0xFFEBEBEB),
    onSecondary             = Color(0xFF090909),
    secondaryContainer      = Color(0xFFEBEBEB),
    onSecondaryContainer    = Color(0xFF090909),
    tertiary                = Color(0xFF6B727E),
    onTertiary              = Color(0xFFFCFCFC),
    background              = Color(0xFFFCFCFC),
    onBackground            = Color(0xFF090909),
    surface                 = Color(0xFFF5F5F5),
    onSurface               = Color(0xFF090909),
    surfaceVariant          = Color(0xFFEBEBEB),
    onSurfaceVariant        = Color(0xFF555555),
    outline                 = Color(0xFFB7B7B7),
    outlineVariant          = Color(0xFFD7D7D7),
    error                   = Color(0xFFD73337),
    onError                 = Color(0xFFFFFFFF),
    errorContainer          = Color(0xFFF7D9DA),
    onErrorContainer        = Color(0xFFD73337),
    inverseSurface          = Color(0xFF090909),
    inverseOnSurface        = Color(0xFFFCFCFC),
    surfaceContainerLowest  = Color(0xFFFFFFFF),
    surfaceContainerLow     = Color(0xFFFCFCFC),
    surfaceContainer        = Color(0xFFF5F5F5),
    surfaceContainerHigh    = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFFFFFFF),
)

// ── Semánticos ───────────────────────────────────────────────────────────────
// Material 3 solo tiene `error`. Los otros tres estados y las dos superficies
// que no encajan en el esquema viajan por acá.

@Immutable
data class ZSemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    /** Borde de mayor contraste, para hover y separadores fuertes. */
    val borderStrong: Color,
    /** Superficie por encima de `surface`: popovers, diálogos, hover. */
    val elevated: Color,
)

private val DarkSemantics = ZSemanticColors(
    success      = Color(0xFF6CD092),
    warning      = Color(0xFFEFCA61),
    danger       = Color(0xFFFF645F),
    info         = Color(0xFFA7AEBB),
    borderStrong = Color(0xFF333333),
    elevated     = Color(0xFF0F0F0F),
)

private val LightSemantics = ZSemanticColors(
    success      = Color(0xFF0FA05C),
    warning      = Color(0xFFCF9B00),
    danger       = Color(0xFFD73337),
    info         = Color(0xFF6B727E),
    borderStrong = Color(0xFFB7B7B7),
    elevated     = Color(0xFFFFFFFF),
)

/**
 * Paleta categórica para las métricas del Dashboard.
 *
 * Son cinco series de identidad (altura, hashrate, balance, próximo desbloqueo,
 * último bloque), así que cada una necesita su propio tono: con colores
 * repetidos no se pueden distinguir de un vistazo, que era el problema.
 *
 * **No son los semánticos.** Verde, ámbar y rojo están reservados para estado y
 * reusarlos como serie haría que un gráfico parezca una advertencia.
 *
 * El orden es fijo y no se cicla. Los valores salieron de validar con el script
 * de la guía de visualización, y pasan las cinco comprobaciones en **los dos
 * temas**: banda de luminosidad, piso de croma, separación para daltonismo
 * (peor par adyacente ΔE 12.2 deutan), piso de visión normal (ΔE 18.2) y
 * contraste contra la superficie. Si se tocan, hay que revalidar — el orden
 * importa porque la comprobación mira pares adyacentes.
 */
@Immutable
data class ZMetricPalette(
    val series: List<Color> = listOf(
        Color(0xFF2E86E6),  // azul
        Color(0xFF00A48C),  // verde azulado
        Color(0xFF8A5CF0),  // violeta
        Color(0xFFE0559A),  // rosa
        Color(0xFFB87A2E),  // bronce
    )
) {
    /** Tono por posición. Nunca cicla: si se piden más, repite el último. */
    fun at(index: Int): Color = series[index.coerceIn(0, series.lastIndex)]
}

val LocalZMetricPalette = staticCompositionLocalOf { ZMetricPalette() }

val LocalZSemantics = staticCompositionLocalOf<ZSemanticColors> {
    error("ZSemanticColors pedido fuera de ZSilentTheme")
}

// ── Tipografía ───────────────────────────────────────────────────────────────
// Inter para interfaz, JetBrains Mono para todo dato: hashes, direcciones,
// montos, alturas, identificadores y estados.

val InterFamily = FontFamily(
    Font("font/Inter-Regular.ttf",  FontWeight.Normal),
    Font("font/Inter-Medium.ttf",   FontWeight.Medium),
    Font("font/Inter-SemiBold.ttf", FontWeight.SemiBold),
)

val MonoFamily = FontFamily(
    Font("font/JetBrainsMono-Regular.ttf", FontWeight.Normal),
    Font("font/JetBrainsMono-Medium.ttf",  FontWeight.Medium),
)

/** Alternativas estilísticas de Inter que usa el prototipo. */
private const val INTER_FEATURES = "ss01, cv11"

/** Cifras de ancho fijo y cero barrado — obligatorio para columnas de datos. */
private const val MONO_FEATURES = "tnum, zero"

private fun inter(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = INTER_FEATURES,
)

private fun mono(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = MONO_FEATURES,
)

private val ZTypography = Typography(
    displaySmall   = inter(36, FontWeight.SemiBold, 40, -0.5f),
    headlineLarge  = inter(30, FontWeight.SemiBold, 34, -0.4f),
    headlineMedium = inter(24, FontWeight.SemiBold, 28, -0.3f),
    headlineSmall  = inter(18, FontWeight.SemiBold, 24, -0.2f),
    titleLarge     = inter(16, FontWeight.SemiBold, 22, -0.2f),
    titleMedium    = inter(14, FontWeight.SemiBold, 20),
    titleSmall     = inter(13, FontWeight.Medium,   18),
    bodyLarge      = inter(14, FontWeight.Normal,   21),
    bodyMedium     = inter(13, FontWeight.Normal,   19),
    bodySmall      = inter(12, FontWeight.Normal,   17),
    labelLarge     = inter(12, FontWeight.Medium,   16, 0.5f),
    labelMedium    = inter(11, FontWeight.Medium,   15, 0.5f),
    labelSmall     = inter(10, FontWeight.Medium,   14, 0.5f),
)

/**
 * Estilos que Material 3 no tiene slot para representar.
 *
 * La firma visual del prototipo es la etiqueta mono en mayúscula con tracking
 * ancho. En CSS el tracking es relativo al tamaño (`0.18em`); acá ya viene
 * resuelto a `sp` absolutos — no re-derivarlo.
 *
 * Los estilos de esta clase NO aplican mayúscula por su cuenta: eso lo hace
 * cada componente con `.uppercase()` sobre el texto.
 */
@Immutable
data class ZTextStyles(
    /** Eyebrow de sección — `SectionTitle`. 10sp / 0.28em. */
    val eyebrow: TextStyle,
    /** Título de panel — `PanelHeader`. 11sp / 0.18em. */
    val panelLabel: TextStyle,
    /** Etiqueta de campo — `ZLabel`, y etiqueta de `Stat`. 10sp / 0.18em. */
    val fieldLabel: TextStyle,
    /** Encabezado de grupo en la sidebar. 9sp / 0.24em. */
    val navGroup: TextStyle,
    /** Texto de pill. 10sp. */
    val pill: TextStyle,
    /** Dato monoespaciado por defecto. */
    val mono: TextStyle,
    /** Dato monoespaciado grande — valor de `Stat`, balances. */
    val monoLarge: TextStyle,
    /** Balance principal de la tarjeta de wallet. */
    val monoDisplay: TextStyle,
)

private val ZTextDefaults = ZTextStyles(
    eyebrow     = mono(10, FontWeight.Medium, 14, 2.8f),
    panelLabel  = mono(11, FontWeight.Medium, 15, 1.98f),
    fieldLabel  = mono(10, FontWeight.Medium, 14, 1.8f),
    navGroup    = mono(9,  FontWeight.Medium, 13, 2.16f),
    pill        = mono(10, FontWeight.Medium, 14, 0.5f),
    mono        = mono(12, FontWeight.Normal, 18),
    monoLarge   = mono(20, FontWeight.Normal, 24),
    monoDisplay = mono(36, FontWeight.Medium, 40),
)

val LocalZTextStyles = staticCompositionLocalOf { ZTextDefaults }

// ── Formas ───────────────────────────────────────────────────────────────────
// El prototipo solo define tres radios y nada supera los 10dp salvo lo que es
// explícitamente circular (puntos de estado, switches, badges redondos).

private val ZShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(6.dp),
    medium     = RoundedCornerShape(10.dp),
    large      = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

// ── Densidad ─────────────────────────────────────────────────────────────────
// Decisión D4: la densidad es configurable, así que ningún componente debe
// llevar espaciados fijos — todos leen de LocalZSpacing.

enum class ZDensity { Compact, Cozy }

@Immutable
data class ZSpacing(
    /** Relleno interno de `Panel`. */
    val panel: Dp,
    /** Separación entre paneles de una grilla. */
    val gap: Dp,
    /** Relleno vertical de una fila de lista o tabla. */
    val row: Dp,
    /** Alto de controles: `ZInput`, `ZButton` tamaño medio, desplegables. */
    val control: Dp,
    /** Relleno lateral del contenido de una sección. */
    val screen: Dp,
)

private val CompactSpacing = ZSpacing(panel = 16.dp, gap = 8.dp,  row = 6.dp,  control = 32.dp, screen = 16.dp)
private val CozySpacing    = ZSpacing(panel = 20.dp, gap = 12.dp, row = 10.dp, control = 40.dp, screen = 32.dp)

val LocalZSpacing = staticCompositionLocalOf { CozySpacing }

// ── Movimiento ───────────────────────────────────────────────────────────────

/** `cubic-bezier(0.2, 0.8, 0.2, 1)` del prototipo. */
val ZEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

const val Z_DURATION_FAST = 200
const val Z_DURATION_ENTER = 350
const val Z_DURATION_FADE = 400

/**
 * Decisión D4: si está en `false`, los helpers de animación devuelven el valor
 * final sin animar. Cada animación tiene que consultarlo — no asumir `true`.
 */
val LocalZAnimations = staticCompositionLocalOf { true }

// ── Tema ─────────────────────────────────────────────────────────────────────

@Composable
fun ZSilentTheme(
    darkTheme: Boolean = true,
    density: ZDensity = ZDensity.Cozy,
    animations: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalZSemantics provides if (darkTheme) DarkSemantics else LightSemantics,
        LocalZMetricPalette provides ZMetricPalette(),
        LocalZTextStyles provides ZTextDefaults,
        LocalZSpacing provides if (density == ZDensity.Compact) CompactSpacing else CozySpacing,
        LocalZAnimations provides animations,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = ZTypography,
            shapes = ZShapes,
            content = content,
        )
    }
}

/** Atajos de lectura, para no arrastrar los `LocalX.current` por todos lados. */
object ZTheme {
    val semantics: ZSemanticColors
        @Composable get() = LocalZSemantics.current
    val metrics: ZMetricPalette
        @Composable get() = LocalZMetricPalette.current
    val text: ZTextStyles
        @Composable get() = LocalZTextStyles.current
    val spacing: ZSpacing
        @Composable get() = LocalZSpacing.current
    val animations: Boolean
        @Composable get() = LocalZAnimations.current
}
