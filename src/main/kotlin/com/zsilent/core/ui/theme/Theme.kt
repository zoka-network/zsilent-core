package com.zsilent.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
//  Premium fintech dark palette — silent, technical, violet-accented.
//
//  Surfaces: deep blacks with a slight blue-grey lean (never pure black).
//  Accent : a single lilac/violet family used sparingly for active state,
//           progress, chart lines and small glows.
//  Text   : soft white / cool grey / deep grey for high/medium/low emphasis.
//  Errors : desaturated rose, success expressed via the same violet family
//           so the palette stays disciplined (no green/blue noise).
// ─────────────────────────────────────────────────────────────────────────────

// Brand violet family
private val LilacPrimary = Color(0xFFC084FC)          // main accent
private val LilacPrimaryOn = Color(0xFF231038)
private val LilacContainer = Color(0xFF5B2BAA)
private val LilacContainerOn = Color(0xFFE9D5FF)
private val LilacDeep = Color(0xFFA855F7)             // deeper violet for hover/press
private val LilacSoft = Color(0xFFD8B4FE)             // softer highlight
private val LilacWhisper = Color(0xFFE9D5FF)          // very light hover wash

// Secondary violet (used for active state contrast / progress)
private val VioletSecondary = Color(0xFFA78BFA)
private val VioletSecondaryOn = Color(0xFF1F1145)
private val VioletSecondaryContainer = Color(0xFF3E2C7A)
private val VioletSecondaryContainerOn = Color(0xFFDDD0FF)

// Tertiary — even softer lilac for info badges / charts
private val LavenderTertiary = Color(0xFFD8B4FE)
private val LavenderTertiaryOn = Color(0xFF2B1556)

// Error — desaturated, never saturated red
private val SoftError = Color(0xFFE08B8B)
private val SoftErrorOn = Color(0xFF45161A)
private val SoftErrorContainer = Color(0xFF6D2E33)
private val SoftErrorContainerOn = Color(0xFFFCD9D9)

// Surfaces — carbon greys with a hint of blue
private val SurfaceBase = Color(0xFF07080C)           // app background
private val SurfaceLowest = Color(0xFF050608)         // recessed
private val SurfaceLow = Color(0xFF0B0D12)            // card low elevation
private val SurfaceMid = Color(0xFF10131A)            // standard card
private val SurfaceHigh = Color(0xFF141822)           // elevated card
private val SurfaceHigher = Color(0xFF171B24)         // hover / focus
private val SurfaceTop = Color(0xFF1B202B)            // tooltip / modal
private val SurfaceTint = Color(0xFF0D1017)           // tinted variant

// Text emphasis on dark surfaces
private val TextHigh = Color(0xFFF4F4F5)              // primary text — soft white
private val TextMedium = Color(0xFF9CA3AF)            // secondary text — cool grey
private val TextLow = Color(0xFF6B7280)               // tertiary / disabled

// Outlines
private val OutlineSoft = Color(0xFF1E2430)           // hair lines between cards
private val OutlineMid = Color(0xFF252B38)
private val OutlineStrong = Color(0xFF2A3140)         // emphasized borders

private val Paper = Color(0xFFFFFFFF)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = LilacPrimary,
    onPrimary = LilacPrimaryOn,
    primaryContainer = LilacContainer,
    onPrimaryContainer = LilacContainerOn,
    inversePrimary = LilacDeep,

    secondary = VioletSecondary,
    onSecondary = VioletSecondaryOn,
    secondaryContainer = VioletSecondaryContainer,
    onSecondaryContainer = VioletSecondaryContainerOn,

    tertiary = LavenderTertiary,
    onTertiary = LavenderTertiaryOn,

    error = SoftError,
    onError = SoftErrorOn,
    errorContainer = SoftErrorContainer,
    onErrorContainer = SoftErrorContainerOn,

    background = SurfaceBase,
    onBackground = TextHigh,

    surface = SurfaceBase,
    onSurface = TextHigh,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = TextMedium,
    surfaceTint = LilacPrimary,
    inverseSurface = Color(0xFFE9E6EE),
    inverseOnSurface = Color(0xFF1B1A1F),

    outline = OutlineStrong,
    outlineVariant = OutlineSoft,
    scrim = Color(0xFF000000),
    surfaceBright = SurfaceTop,
    surfaceDim = SurfaceLowest,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceMid,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigher
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1A3B6B),
    secondary = Color(0xFF005047),
    tertiary = Color(0xFF315FAD),
    error = Color(0xFFB3261E),
    background = Paper,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F2F4),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF11151A),
    onSurface = Color(0xFF11151A),
    onSurfaceVariant = Color(0xFF626873),
    outline = Color(0xFFDADDE3)
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
fun ZSilentTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ZSilentTypography,
        shapes = ZSilentShapes,
        content = content
    )
}
