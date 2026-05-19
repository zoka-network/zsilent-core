package com.zsilent.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ZColorScheme = darkColorScheme(
    primary = ZAccent,
    onPrimary = ZBackground,
    secondary = ZGreen,
    onSecondary = ZBackground,
    background = ZBackground,
    onBackground = ZTextPrimary,
    surface = ZSurface,
    onSurface = ZTextPrimary,
    surfaceVariant = ZSurfaceHigh,
    onSurfaceVariant = ZTextSecondary,
    error = ZRed,
    outline = ZBorder
)

@Composable
fun ZSilentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZColorScheme,
        typography = ZTypography,
        shapes = ZShapes,
        content = content
    )
}
