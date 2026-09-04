package dev.alenajam.monsterdialer.characters.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.opendialer.core.common.ui.AppIcon

internal data class GuideContent(
    @param:StringRes val title: Int,
    @param:StringRes val message: Int,
    @param:StringRes val details: List<Int> = emptyList(),
)

@Composable
internal fun ContextualGuideButton(
    contents: List<GuideContent>,
    @StringRes contentDescription: Int = R.string.open_character_guide,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { isOpen = true }, modifier = modifier) {
        AppIcon(
            icon = LocalMonsterAppIcons.current.guide,
            contentDescription = stringResource(contentDescription),
        )
    }
    if (isOpen) {
        ContextualGuideDialog(contents = contents, onDismiss = { isOpen = false })
    }
}

@Composable
internal fun ContextualGuideDialog(contents: List<GuideContent>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(contents.first().title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                contents.forEachIndexed { index, content ->
                    GuideContentSection(content, showTitle = index != 0)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

internal fun radiantGuideContents(): List<GuideContent> = listOf(
    GuideContent(
        R.string.radiants_guide_title,
        R.string.radiants_guide_message,
        listOf(
            R.string.radiants_guide_availability,
            R.string.radiants_guide_unlock,
            R.string.radiants_guide_assign,
            R.string.radiants_guide_journal,
        ),
    ),
)

@Composable
private fun GuideContentSection(content: GuideContent, showTitle: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showTitle) {
            Text(stringResource(content.title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(parseGuideText(stringResource(content.message)), style = MaterialTheme.typography.bodyMedium)
        content.details.forEach { detail ->
            Text(parseGuideText(stringResource(detail)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun parseGuideText(text: String): AnnotatedString = buildAnnotatedString {
    var start = 0
    while (true) {
        val boldStart = text.indexOf("<b>", start)
        if (boldStart == -1) return@buildAnnotatedString append(text.substring(start))
        append(text.substring(start, boldStart))
        val contentStart = boldStart + 3
        val boldEnd = text.indexOf("</b>", contentStart)
        if (boldEnd == -1) return@buildAnnotatedString append(text.substring(boldStart))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(contentStart, boldEnd)) }
        start = boldEnd + 4
    }
}
