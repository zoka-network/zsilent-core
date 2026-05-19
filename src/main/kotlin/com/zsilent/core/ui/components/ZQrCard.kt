package com.zsilent.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zsilent.core.design.ZSpacing

@Composable
fun ZQrCard(
    address: String,
    modifier: Modifier = Modifier
) {
    ZCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(ZSpacing.md)) {
            Text("Receive address", style = MaterialTheme.typography.titleLarge)
            Box(
                modifier = Modifier
                    .size(248.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
            ) {
                QrCanvas(address, Modifier.size(248.dp))
            }
            Text(
                address,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CopyIconButton(address)
        }
    }
}

@Composable
private fun QrCanvas(value: String, modifier: Modifier = Modifier) {
    val matrix = remember(value) {
        runCatching {
            QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 33, 33)
        }.getOrNull()
    }
    Canvas(modifier) {
        val qr = matrix ?: return@Canvas
        val cell = minOf(size.width / qr.width, size.height / qr.height)
        val offset = Offset(
            x = (size.width - cell * qr.width) / 2f,
            y = (size.height - cell * qr.height) / 2f
        )
        for (x in 0 until qr.width) {
            for (y in 0 until qr.height) {
                drawRect(
                    color = if (qr[x, y]) Color.Black else Color.Transparent,
                    topLeft = Offset(offset.x + x * cell, offset.y + y * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}
