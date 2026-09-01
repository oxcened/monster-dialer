package dev.alenajam.monsterdialer.characters.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
                120f,
                fittedCenteredPaint(context.getString(R.string.radiant_unlock_share_card_title), 52f, Accent, MaxHeaderWidth),
            )
            drawRect(360f, 156f, 720f, 164f, Paint().apply { color = Accent })
            drawBitmapAspectFit(sprite, Rect(190, 210, 890, 650), Paint().apply { isFilterBitmap = false })
            drawSparkles(this)
            drawText(characterName, CardWidth / 2f, 755f, fittedCenteredPaint(characterName, 88f, Foreground, 790f))
            drawText(context.getString(R.string.radiant), CardWidth / 2f, 808f, centeredPaint(36f, Accent))

            drawRect(160f, 860f, 920f, 864f, Paint().apply { color = BrandDivider })
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
        color = Foreground
        textAlign = Paint.Align.LEFT
        typeface = pixelTypeface
    }

    private fun drawSparkles(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Accent }
        listOf(180 to 280, 900 to 310, 206 to 592, 874 to 560).forEach { (x, y) ->
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
        val link = context.getString(R.string.radiant_unlock_share_card_link)
        val wordmark = brandPaint()
        val subtitle = brandSubtitlePaint()
        val iconSize = if (launcherIcon != null) 88 else 0
        val gap = if (launcherIcon != null) 26f else 0f
        val groupWidth = iconSize + gap + maxOf(wordmark.measureText(appName), subtitle.measureText(link))
        val left = (CardWidth - groupWidth) / 2f
        launcherIcon?.let { icon ->
            val iconTop = (BrandCenterY - iconSize / 2f).toInt()
            val iconBounds = Rect(left.toInt(), iconTop, (left + iconSize).toInt(), iconTop + iconSize)
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(RectF(iconBounds), 20f, 20f, Path.Direction.CW) })
            canvas.drawBitmap(icon, null, iconBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
            canvas.restore()
        }
        val textLeft = left + iconSize + gap
        val wordmarkMetrics = wordmark.fontMetrics
        val subtitleMetrics = subtitle.fontMetrics
        val textBlockHeight =
            wordmarkMetrics.descent - wordmarkMetrics.ascent + BrandLineGap +
                subtitleMetrics.descent - subtitleMetrics.ascent
        val wordmarkBaseline = BrandCenterY - textBlockHeight / 2f - wordmarkMetrics.ascent
        val subtitleBaseline = wordmarkBaseline + wordmarkMetrics.descent - subtitleMetrics.ascent + BrandLineGap
        canvas.drawText(appName, textLeft, wordmarkBaseline, wordmark)
        canvas.drawText(link, textLeft, subtitleBaseline, subtitle)
    }

    private fun brandSubtitlePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        color = BrandDivider
        textAlign = Paint.Align.LEFT
        typeface = pixelTypeface
    }

    private fun String.safeFileName(): String = replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private companion object {
        const val Background = 0xFFFFFBF2.toInt()
        const val Foreground = 0xFF261A3E.toInt()
        const val BrandDivider = 0xFF65547A.toInt()
        const val Accent = 0xFF9C6515.toInt()
        const val MaxHeaderWidth = 790f
        const val BrandCenterY = 935f
        const val BrandLineGap = 8f
        const val CardWidth = 1080
        const val CardHeight = 1080
    }

    private val pixelTypeface by lazy { ResourcesCompat.getFont(context, R.font.pixel_operator) }
}
