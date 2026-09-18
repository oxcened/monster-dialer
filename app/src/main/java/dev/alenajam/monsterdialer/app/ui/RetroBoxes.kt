package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared four-layer Game Boy box frame. */
@Composable
internal fun RetroDoubleBorderBox(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    content: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier)
            .drawBehind {
                val step = 2.dp.toPx()

                fun pixelPath(inset: Float): Path = Path().apply {
                    // One square missing-pixel step per corner, matching the GSC frame.
                    moveTo(inset + step, inset)
                    lineTo(size.width - inset - step, inset)
                    lineTo(size.width - inset - step, inset + step)
                    lineTo(size.width - inset, inset + step)
                    lineTo(size.width - inset, size.height - inset - step)
                    lineTo(size.width - inset - step, size.height - inset - step)
                    lineTo(size.width - inset - step, size.height - inset)
                    lineTo(inset + step, size.height - inset)
                    lineTo(inset + step, size.height - inset - step)
                    lineTo(inset, size.height - inset - step)
                    lineTo(inset, inset + step)
                    lineTo(inset + step, inset + step)
                    close()
                }

                drawPath(pixelPath(0f), Color.Black)
                drawRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 4.dp.toPx(),
                        size.height - 4.dp.toPx(),
                    ),
                )
                drawRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 10.dp.toPx(),
                        size.height - 10.dp.toPx(),
                    ),
                )
                drawRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 18.dp.toPx(),
                        size.height - 18.dp.toPx(),
                    ),
                )
            }
            .padding(11.dp),
        content = content,
    )
}

/** Shared profile-style panel with the standard GSC outer margin and inner padding. */
@Composable
internal fun RetroProfilePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF202020), RectangleShape)
            .background(Color.White, RectangleShape)
            .padding(12.dp),
    ) {
        content()
    }
}

/** Reusable gray GSC menu-window border with a single outer missing-pixel notch. */
@Composable
internal fun RetroMenuBorder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .drawBehind {
                // Keep the complete chrome close to the dialogue frame's depth, but use
                // the much tighter gray/black proportions of a GSC menu window.
                val step = 3.dp.toPx()

                fun pixelPath(offset: Float): Path = Path().apply {
                    // One square missing-pixel notch per corner, matching the dialogue frame.
                    moveTo(offset + step, offset)
                    lineTo(size.width - offset - step, offset)
                    lineTo(size.width - offset - step, offset + step)
                    lineTo(size.width - offset, offset + step)
                    lineTo(size.width - offset, size.height - offset - step)
                    lineTo(size.width - offset - step, size.height - offset - step)
                    lineTo(size.width - offset - step, size.height - offset)
                    lineTo(offset + step, size.height - offset)
                    lineTo(offset + step, size.height - offset - step)
                    lineTo(offset, size.height - offset - step)
                    lineTo(offset, offset + step)
                    lineTo(offset + step, offset + step)
                    close()
                }

                // GSC menu chrome: pale outer casing, darker inset edge, then a crisp black keyline.
                drawPath(pixelPath(0f), Color(0xFFC8C8C0))
                drawRect(
                    color = Color(0xFF686860),
                    topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 8.dp.toPx(),
                        size.height - 8.dp.toPx(),
                    ),
                )
                drawRect(
                    color = Color(0xFF202020),
                    topLeft = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 7.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 14.dp.toPx(),
                        size.height - 14.dp.toPx(),
                    ),
                )
                drawRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 20.dp.toPx(),
                        size.height - 20.dp.toPx(),
                    ),
                )
            }
    ) {
        content()
    }
}

/** Compact padded box that uses the reusable gray GSC menu-window border. */
@Composable
internal fun RetroMenuWindow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    RetroMenuBorder(modifier = modifier) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            content()
        }
    }
}
