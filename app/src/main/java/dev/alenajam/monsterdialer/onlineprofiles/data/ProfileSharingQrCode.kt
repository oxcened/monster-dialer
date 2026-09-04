package dev.alenajam.monsterdialer.onlineprofiles.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Encodes a profile sharing link as a QR-code matrix suitable for rendering at any size. */
object ProfileSharingQrCode {
    fun encode(sharingLink: String): QrCodeMatrix {
        val matrix = MultiFormatWriter().encode(
            sharingLink,
            BarcodeFormat.QR_CODE,
            MinimumSize,
            MinimumSize,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
        return QrCodeMatrix(
            width = matrix.width,
            height = matrix.height,
            darkModules = BooleanArray(matrix.width * matrix.height) { index ->
                matrix[index % matrix.width, index / matrix.width]
            },
        )
    }

    private const val MinimumSize = 1
}

data class QrCodeMatrix(
    val width: Int,
    val height: Int,
    private val darkModules: BooleanArray,
) {
    init {
        require(width > 0 && height > 0)
        require(darkModules.size == width * height)
    }

    operator fun get(x: Int, y: Int): Boolean = darkModules[y * width + x]
}
