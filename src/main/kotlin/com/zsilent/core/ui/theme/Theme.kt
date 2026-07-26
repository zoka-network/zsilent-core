package com.zsilent.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  "Zenith" — the single ZSilent palette: near-black surfaces, white text,
//  pastel-green accent. Matches ZKAPriv.
//
//  There is deliberately no light/dark pair. Everything reading
//  MaterialTheme.colorScheme.* directly (sync progress bars, mining screen,
//  the Send wallet picker, …) resolves against this one scheme, so the whole
//  app stays visually consistent.
//
//  Kept as a `darkColorScheme` because that is the Material3 plumbing for a
//  dark-surface palette — the name refers to the machinery, not to a theme
//  the user can switch away from.
// ─────────────────────────────────────────────────────────────────────────────

private val ZenithScheme: ColorScheme = darkColorScheme(
    primary             = Color(0xFFA8E6B0),   // pastel green accent
    onPrimary           = Color(0xFF0A0A0C),
    primaryContainer    = Color(0xFF1C2B20),
    onPrimaryContainer  = Color(0xFFA8E6B0),
    secondary           = Color(0xFF7FCB8E),
    onSecondary         = Color(0xFF0A0A0C),
    secondaryContainer  = Color(0xFF1C2B20),
    onSecondaryContainer= Color(0xFFA8E6B0),
    tertiary            = Color(0xFF9CB8F2),
    onTertiary          = Color(0xFF0A0A0C),
    error               = Color(0xFFE2918F),
    onError             = Color(0xFF2B0F0E),
    errorContainer      = Color(0xFF3A1F1E),
    onErrorContainer    = Color(0xFFF2D4D2),
    background          = Color(0xFF0A0A0C),   // near black
    onBackground        = Color(0xFFFFFFFF),
    surface             = Color(0xFF16171B),   // a touch lighter than bg
    onSurface           = Color(0xFFFFFFFF),
    surfaceVariant      = Color(0xFF1D1F24),
    onSurfaceVariant    = Color(0xFFB8BCC4),
    outline             = Color(0xFF3A3D44),
    outlineVariant      = Color(0xFF2A2D33),
    inverseSurface      = Color(0xFFE9EAEC),
    inverseOnSurface    = Color(0xFF1F2126),
    surfaceContainerLowest  = Color(0xFF050506),
    surfaceContainerLow     = Color(0xFF101114),
    surfaceContainer        = Color(0xFF16171B),
    surfaceContainerHigh    = Color(0xFF1D1F24),
    surfaceContainerHighest = Color(0xFF2A2D33)
)

// Premium-fintech corner radii. Cards lean large (24-28dp) so they read as
// distinct modules; small controls keep moderate roundness.
private val ZSilentShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val ZSilentTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun ZSilentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZenithScheme,
        typography = ZSilentTypography,
        shapes = ZSilentShapes,
        content = content
    )
}
