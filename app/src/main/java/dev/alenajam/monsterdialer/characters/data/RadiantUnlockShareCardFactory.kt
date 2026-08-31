package dev.alenajam.monsterdialer.characters.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.alenajam.monsterdialer.R
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Creates a shareable celebratory image for a newly discovered radiant variant. */
@Singleton
class RadiantUnlockShareCardFactory @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) {
    fun create(characterName: String, frontSpritePath: String): android.net.Uri? {
        val sprite = BitmapFactory.decodeFile(frontSpritePath) ?: return null
        val launcherIcon = runCatching {
            context.applicationInfo.loadIcon(context.packageManager).toBitmap(128, 128)
        }.getOrNull()
        val card = Bitmap.createBitmap(CardWidth, CardHeight, Bitmap.Config.ARGB_8888)
        Canvas(card).apply {
            drawColor(Background)
            drawText(
                context.getString(R.string.radiant_unlock_share_card_title),
                CardWidth / 2f,
                152f,
                fittedCenteredPaint(context.getString(R.string.radiant_unlock_share_card_title), 52f, Accent, MaxHeaderWidth),
            )
            drawRect(360f, 192f, 720f, 200f, Paint().apply { color = Accent })
            drawBitmapAspectFit(sprite, Rect(190, 250, 890, 830), Paint().apply { isFilterBitmap = false })
            drawSparkles(this)
            drawText(characterName, CardWidth / 2f, 940f, fittedCenteredPaint(characterName, 88f, Color.WHITE, 790f))
            drawText(context.getString(R.string.radiant), CardWidth / 2f, 1_006f, centeredPaint(36f, Accent))

            drawRect(160f, 1_100f, 920f, 1_104f, Paint().apply { color = BrandDivider })
            drawBrand(this, launcherIcon)
        }
        val directory = File(context.cacheDir, "shared-radiant-unlocks").apply { mkdirs() }
        val file = File(directory, "radiant-${characterName.safeFileName()}-${System.currentTimeMillis()}.png")
        file.outputStream().use { output -> card.compress(Bitmap.CompressFormat.PNG, 100, output) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun centeredPaint(size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        textAlign = Paint.Align.CENTER
        typeface = pixelTypeface
    }

    private fun fittedCenteredPaint(text: String, size: Float, color: Int, maxWidth: Float): Paint = centeredPaint(size, color).apply {
        while (measureText(text) > maxWidth && textSize > 20f) {
            textSize -= 2f
        }
    }

    private fun brandPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        typeface = pixelTypeface
    }

    private fun drawSparkles(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Accent }
        listOf(180 to 334, 900 to 382, 206 to 724, 874 to 686).forEach { (x, y) ->
            canvas.drawRect(x - 26f, y - 4f, x + 26f, y + 4f, paint)
            canvas.drawRect(x - 4f, y - 26f, x + 4f, y + 26f, paint)
        }
    }

    private fun Canvas.drawBitmapAspectFit(bitmap: Bitmap, bounds: Rect, paint: Paint) {
        val scale = minOf(
            bounds.width().toFloat() / bitmap.width,
            bounds.height().toFloat() / bitmap.height,
        )
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        val left = bounds.left + (bounds.width() - width) / 2
        val top = bounds.top + (bounds.height() - height) / 2
        drawBitmap(bitmap, null, Rect(left, top, left + width, top + height), paint)
    }

    private fun drawBrand(canvas: Canvas, launcherIcon: Bitmap?) {
        val appName = context.getString(R.string.app_name)
        val wordmark = brandPaint()
        val iconSize = if (launcherIcon != null) 88 else 0
        val gap = if (launcherIcon != null) 26f else 0f
        val groupWidth = iconSize + gap + wordmark.measureText(appName)
        val left = (CardWidth - groupWidth) / 2f
        launcherIcon?.let { icon ->
            val iconBounds = Rect(left.toInt(), 1_160, (left + iconSize).toInt(), 1_248)
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(RectF(iconBounds), 20f, 20f, Path.Direction.CW) })
            canvas.drawBitmap(icon, null, iconBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
            canvas.restore()
        }
        canvas.drawText(appName, left + iconSize + gap, 1_220f, wordmark)
    }

    private fun String.safeFileName(): String = replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private companion object {
        const val Background = 0xFF100C21.toInt()
        const val BrandDivider = 0xFFBAA7EE.toInt()
        const val Accent = 0xFFF5D26B.toInt()
        const val MaxHeaderWidth = 790f
        const val CardWidth = 1080
        const val CardHeight = 1350
    }

    private val pixelTypeface by lazy { ResourcesCompat.getFont(context, R.font.pixel_operator) }
}
