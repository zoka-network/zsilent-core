package com.zsilent.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.security.MessageDigest
import kotlinx.coroutines.delay

/**
 * Textos largos en varios QR chicos que se muestran en secuencia.
 *
 * Una `zkq1` tiene 1.715 caracteres: en un solo QR son unos 141 módulos por
 * lado y la cámara de un teléfono no lo lee desde la pantalla (15/10/2026:
 * ZKAPriv no leía el QR de ZSilent). Partida en trozos de [PART_CHARS], cada QR
 * queda chico y se lee al instante; ZKAPriv junta las partes y comprueba el
 * resultado con su huella.
 *
 * Formato de cada parte: `zkqp:<i>/<n>:<huella>:<trozo>`, con `i` desde 1 y la
 * huella = los primeros 8 hex del SHA-256 del texto completo. Es el mismo
 * formato que lee ZKAPriv (`com.zoka.zkapriv.ui.qrscanner.MultiPartQr`); las
 * pruebas de las dos apps comparten un vector para que no se separen.
 */
object MultiPartQr {
    const val PREFIX = "zkqp:"
    const val PART_CHARS = 300
    /** Hasta este largo un solo QR se lee bien y no se parte. */
    const val SPLIT_ABOVE_CHARS = 400
    const val MAX_PARTS = 64

    fun fingerprint(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    /** Las partes a mostrar; un texto corto queda en una sola, sin cabecera. */
    fun split(text: String, partChars: Int = PART_CHARS): List<String> {
        require(partChars > 0) { "partChars must be positive" }
        if (text.length <= SPLIT_ABOVE_CHARS) return listOf(text)
        val chunks = text.chunked(partChars)
        require(chunks.size <= MAX_PARTS) { "text too long for a multi-part QR" }
        val huella = fingerprint(text)
        return chunks.mapIndexed { index, chunk ->
            "$PREFIX${index + 1}/${chunks.size}:$huella:$chunk"
        }
    }
}

/**
 * Un texto largo como QR animado: sus partes ([MultiPartQr]) se alternan cada
 * [frameMillis]. Un texto corto queda como un QR común y quieto.
 *
 * Las partes van con módulos cuadrados y corrección M: son chicas, así que el
 * margen extra de lectura no cuesta tamaño, y una cámara mediocre apuntando a
 * un monitor lo necesita.
 */
@Composable
fun AnimatedQrCode(
    value: String,
    modifier: Modifier = Modifier,
    boxSize: Dp = 320.dp,
    frameMillis: Long = 500L,
) {
    val parts = remember(value) { MultiPartQr.split(value) }
    var frame by remember(value) { mutableIntStateOf(0) }
    LaunchedEffect(parts) {
        if (parts.size > 1) {
            while (true) {
                delay(frameMillis)
                frame = (frame + 1) % parts.size
            }
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QrCode(
            parts[frame.coerceIn(0, parts.lastIndex)],
            errorCorrection = ErrorCorrectionLevel.M,
            boxSize = boxSize,
            squareModules = true
        )
        if (parts.size > 1) {
            Text(
                "Part ${frame + 1} of ${parts.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
