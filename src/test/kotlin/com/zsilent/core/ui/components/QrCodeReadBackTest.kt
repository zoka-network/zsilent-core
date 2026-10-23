package com.zsilent.core.ui.components

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class QrCodeReadBackTest {

    // Lo que dibuja QrCode (un rectángulo negro por corrida, en píxeles
    // enteros, sobre blanco) pintado en una imagen y leído con un lector de QR:
    // el texto que sale es la dirección corta, entera. El QR con puntos
    // redondos les costaba a los teléfonos (23/10/2026); este no puede romperse
    // sin que falle esta prueba.
    @Test
    fun the_drawn_receive_qr_reads_back_as_the_short_address() {
        val short = "zqtest15MnwVgjVzRRABprTurtK7rB2tUw4V7FUyfi66Mt1zJPXuv76Bp"
        val matrix = QRCodeWriter().encode(
            short,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to QR_QUIET_ZONE_MODULES,
            )
        )
        for (cell in listOf(3, 6)) {
            val side = matrix.width * cell
            val pixels = IntArray(side * side) { 0xFFFFFFFF.toInt() }
            for (run in qrDarkRuns(matrix)) {
                for (y in run.y * cell until (run.y + 1) * cell) {
                    for (x in run.from * cell until run.to * cell) {
                        pixels[y * side + x] = 0xFF000000.toInt()
                    }
                }
            }
            val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
            assertEquals(short, QRCodeReader().decode(image).text, "$cell px per module")
        }
    }
}
