package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

private val RetroTextBoxFont = FontFamily(Font(dev.alenajam.monsterdialer.R.font.ui_pixel_font))

/** Reusable Game Boy-style text box for menus and other non-battle screens. */
@Composable
internal fun RetroTextBox(
    message: String,
    animationKey: Any? = message,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 104.dp,
    characterDelayMillis: Long = 8,
    pageHoldMillis: Long = 900,
    textScale: Float = 1f,
    onCompleted: () -> Unit = {},
) {
    val style = TextStyle(
        fontFamily = RetroTextBoxFont,
        fontSize = 18.sp * textScale,
        lineHeight = 21.sp * textScale,
        color = androidx.compose.ui.graphics.Color.Black,
    )
    val textMeasurer = rememberTextMeasurer()
    val lifecycleOwner = LocalLifecycleOwner.current
    RetroBoxFrame(modifier = modifier.fillMaxWidth(0.95f), height = height) {
        val textWidth = with(LocalDensity.current) {
            (maxWidth - 10.dp).roundToPx().coerceAtLeast(0)
        }
        val pages = remember(message, textWidth) {
            retroTextPages(message, textMeasurer, style, textWidth)
        }
        var displayedMessage by remember(animationKey, message) { mutableStateOf("") }
        LaunchedEffect(animationKey, message, pages, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                withFrameNanos { }
                displayedMessage = ""
                pages.forEachIndexed { pageIndex, page ->
                    page.indices.forEach { index ->
                        displayedMessage = page.take(index + 1)
                        delay(characterDelayMillis)
                    }
                    if (pageIndex < pages.lastIndex) delay(pageHoldMillis)
                }
                onCompleted()
            }
        }
        Text(
            text = displayedMessage,
            style = style,
            maxLines = 3,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(5.dp),
        )
    }
}

private fun retroTextPages(
    text: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle,
    maxWidth: Int,
): List<String> {
    if (text.isBlank()) return emptyList()
    fun fits(page: String) = !textMeasurer.measure(
        text = AnnotatedString(page),
        style = style,
        overflow = TextOverflow.Clip,
        maxLines = 3,
        constraints = Constraints(maxWidth = maxWidth),
    ).hasVisualOverflow

    val pages = mutableListOf<String>()
    var remaining = text.trim()
    while (remaining.isNotEmpty()) {
        if (fits(remaining)) {
            pages += remaining
            break
        }
        var end = remaining.length
        while (end > 1 && !fits(remaining.substring(0, end))) end--
        val wordBoundary = remaining.lastIndexOfAny(charArrayOf(' ', '\n', '\t'), end - 1)
        val pageEnd = if (wordBoundary > 0) wordBoundary else end
        pages += remaining.substring(0, pageEnd).trimEnd()
        remaining = remaining.substring(if (wordBoundary > 0) wordBoundary + 1 else pageEnd).trimStart()
    }
    return pages
}
