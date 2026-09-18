package dev.alenajam.monsterdialer.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alenajam.monsterdialer.R

private val RetroSearchPixelFont = FontFamily(Font(R.font.ui_pixel_font))
private val RetroSearchInk = Color(0xFF202020)

/** A Game Boy-style command with consistent horizontal insets. */
@Composable
internal fun RetroSearchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp),
            fontFamily = RetroSearchPixelFont,
            fontSize = 18.sp,
            color = RetroSearchInk,
        )
    }
}

/** A framed retro search field that renders its query and block cursor inline. */
@Composable
internal fun RetroSearchBar(
    label: String,
    query: String,
    focusRequester: FocusRequester,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RetroDoubleBorderBox(
        modifier = modifier.fillMaxWidth(),
        height = 56.dp,
    ) {
        RetroSearchInput(
            label = label,
            query = query,
            focusRequester = focusRequester,
            onQueryChanged = onQueryChanged,
        )
    }
}

@Composable
private fun RetroSearchInput(
    label: String,
    query: String,
    focusRequester: FocusRequester,
    onQueryChanged: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val cursorTransition = rememberInfiniteTransition(label = "retro-search-cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "retro-search-cursor-alpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                fontFamily = RetroSearchPixelFont,
                fontSize = 18.sp,
                color = RetroSearchInk,
            )
            Text(
                text = query.uppercase(),
                fontFamily = RetroSearchPixelFont,
                fontSize = 18.sp,
                color = RetroSearchInk,
            )
            Text(
                text = "█",
                modifier = Modifier.graphicsLayer(alpha = cursorAlpha),
                fontFamily = RetroSearchPixelFont,
                fontSize = 18.sp,
                color = RetroSearchInk,
            )
        }
    }
}
