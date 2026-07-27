package com.zsilent.core.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Efectos ambientales y movimiento del prototipo whisper-desk.
//
//  Solo están los efectos que el prototipo usa de verdad. `zs-glow` y
//  `zs-mask-fade` están declarados en su CSS pero tienen cero usos, así que no
//  se portan — eran justamente los dos que no tienen equivalente directo en
//  Compose. Si algún día se necesitan, el glow requiere bajar a Skia.
//
//  Toda animación de este archivo respeta ZTheme.animations (decisión D4): si
//  está apagada, se devuelve el valor final sin animar.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Retícula ambiental de 40dp al 4% del color de texto. CSS: `.zs-grid`.
 *
 * Se dibuja con líneas y no con un patrón tileado porque son apenas unas
 * decenas por pantalla.
 */
fun Modifier.zGrid(color: Color, step: Dp = 40.dp, alpha: Float = 0.04f): Modifier =
    this.drawBehind {
        val stepPx = step.toPx()
        if (stepPx <= 0f) return@drawBehind
        val tint = color.copy(alpha = alpha)
        var x = 0f
        while (x <= size.width) {
            drawLine(tint, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += stepPx
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(tint, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += stepPx
        }
    }

/**
 * Grano de puntos de 3px al 2%. CSS: `.zs-noise`.
 *
 * Tilea un bitmap de 3×3 con un pixel encendido. Dibujarlo punto por punto
 * serían cientos de miles de `drawRect` por frame en una ventana grande.
 */
private fun noiseTile(color: Color): ImageBitmap {
    val bitmap = ImageBitmap(3, 3)
    val canvas = Canvas(bitmap)
    canvas.drawRect(0f, 0f, 1f, 1f, Paint().apply { this.color = color })
    return bitmap
}

@Composable
fun Modifier.zNoise(color: Color, alpha: Float = 0.02f): Modifier {
    val brush = remember(color, alpha) {
        ShaderBrush(
            ImageShader(
                noiseTile(color.copy(alpha = alpha)),
                TileMode.Repeated,
                TileMode.Repeated,
            )
        )
    }
    return this.drawBehind { drawRect(brush) }
}

/**
 * Alfa que late entre 1 y 0.35. CSS: `.zs-pulse`, ciclo de 1.6s.
 *
 * Se usa en los puntos de estado de las pills y de la sidebar.
 */
@Composable
fun zPulseAlpha(): Float {
    if (!ZTheme.animations) return 1f
    val transition = rememberInfiniteTransition(label = "zs-pulse")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "zs-pulse-alpha",
    ).value
}

/**
 * Desplazamiento vertical normalizado de −1 a 1 para el barrido. CSS:
 * `.zs-scan`, 3s lineal. Lo usa el diálogo de tomar oferta del Exchange.
 */
@Composable
fun zScanOffset(): Float {
    if (!ZTheme.animations) return 0f
    val transition = rememberInfiniteTransition(label = "zs-scan")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "zs-scan-offset",
    ).value
}

/**
 * Contenedor que reanima al cambiar de `target`. CSS: `.zs-in` aplicado con
 * `key={section}`, o sea entrada de 8dp hacia arriba más fundido.
 *
 * Se usa para el contenido de sección en el shell y para los pasos del
 * onboarding.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> ZAnimatedSwap(
    target: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    if (!ZTheme.animations) {
        content(target)
        return
    }
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(Z_DURATION_ENTER, easing = ZEasing)) +
                slideInVertically(tween(Z_DURATION_ENTER, easing = ZEasing)) { it / 12 })
                .togetherWith(fadeOut(tween(Z_DURATION_FAST)))
        },
        label = "zs-in",
    ) { content(it) }
}

/** Barra de scroll fina. CSS: `.scrollbar-thin`, 8px con pulgar redondeado. */
@Composable
fun zScrollbarStyle(): ScrollbarStyle {
    val thumb = ZTheme.semantics.borderStrong
    return ScrollbarStyle(
        minimalHeight = 24.dp,
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp),
        hoverDurationMillis = 300,
        unhoverColor = thumb.copy(alpha = 0.6f),
        hoverColor = thumb,
    )
}
