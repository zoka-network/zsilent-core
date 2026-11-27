package com.zsilent.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

/**
 * El Zoka ID de una dirección (docs/ZOKA-ID.md del nodo): el mosaico (v1) y el
 * código de 80 bits (v2) de su forma corta. La misma dirección da el mismo ID
 * en ZSilent, ZKAPriv, la billetera web y ZokaScan: quien paga lo compara con
 * el que le lee quien cobra.
 */
object ZokaId {
    const val SIDE = 9
    private const val SEED_DOMAIN = "ZKAPriv/avatar/v1/seed\u0000"
    private const val GRID_DOMAIN = "ZKAPriv/avatar/v1/grid\u0000"

    /** Los colores por valor, iguales en los dos temas. */
    val palette = listOf(Color(0xFF080809), Color(0xFF303036), Color(0xFFAAAAAF), Color(0xFFFAFAFA), Color(0xFFF2ABD0))
    val background = Color(0xFF101012)

    private fun valid(short: String) = short.length in 1..256 && short.all {
        it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in ":._-"
    }

    private fun seed(short: String): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
        hash.update(SEED_DOMAIN.toByteArray(Charsets.UTF_8))
        return hash.digest(short.toByteArray(Charsets.UTF_8))
    }

    /** Cinco grupos de cuatro: los dos primeros son el código v1 de ZKAPriv 4.1.0. */
    fun code(short: String): String? {
        if (!valid(short)) return null
        val hex = seed(short).take(10).joinToString("") { "%02X".format(it.toInt() and 0xff) }
        return hex.chunked(4).joinToString("-")
    }

    /** Los 81 valores del mosaico, de 0 a 4, por filas. */
    fun cells(short: String): List<Int>? {
        if (!valid(short)) return null
        val seed = seed(short)
        val hash = MessageDigest.getInstance("SHA-256")
        val cells = ArrayList<Int>(SIDE * SIDE)
        var counter = 0
        while (cells.size < SIDE * SIDE) {
            hash.update(GRID_DOMAIN.toByteArray(Charsets.UTF_8))
            hash.update(seed)
            val block = hash.digest(byteArrayOf(
                (counter ushr 24).toByte(), (counter ushr 16).toByte(), (counter ushr 8).toByte(), counter.toByte(),
            ))
            counter++
            for (byte in block) {
                val value = byte.toInt() and 0xff
                // 255 es divisible por 5: descartarlo deja los cinco valores parejos.
                if (value < 255) cells.add(value % 5)
                if (cells.size == SIDE * SIDE) break
            }
        }
        return cells
    }
}

@Composable
fun ZokaIdMosaic(short: String?, size: Dp = 40.dp) {
    val cells = remember(short) { short?.let(ZokaId::cells) }
    Canvas(Modifier.size(size)) {
        val side = this.size.minDimension
        drawRoundRect(ZokaId.background, size = Size(side, side), cornerRadius = CornerRadius(side * 0.06f))
        val pad = side * 0.03f
        val gap = side * 0.013f
        val cell = (side - 2 * pad - (ZokaId.SIDE - 1) * gap) / ZokaId.SIDE
        for (i in 0 until ZokaId.SIDE * ZokaId.SIDE) {
            val color = cells?.let { ZokaId.palette[it[i]] } ?: Color(0xFF26262B)
            val x = pad + (i % ZokaId.SIDE) * (cell + gap)
            val y = pad + (i / ZokaId.SIDE) * (cell + gap)
            drawRect(color, topLeft = Offset(x, y), size = Size(cell, cell))
        }
    }
}

/** El mosaico, una etiqueta y el código, en una fila. */
@Composable
fun ZokaIdView(short: String?, label: String = "Zoka ID", size: Dp = 40.dp) {
    val code = remember(short) { short?.let(ZokaId::code) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ZokaIdMosaic(short, size)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(code ?: "—", fontFamily = FontFamily.Monospace, fontSize = 14.sp, letterSpacing = 0.5.sp, maxLines = 1)
        }
    }
}
