package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R

@Composable
fun CharactersHelpScreen() {
    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HelpSection(
            title = stringResource(R.string.characters_help_intro_title),
            message = stringResource(R.string.characters_help_intro_message)
        )

        HelpSection(
            title = stringResource(R.string.characters_help_assignments_title),
            message = stringResource(R.string.characters_help_assignments_message),
            bullets = listOf(
                stringResource(R.string.characters_help_assignments_player),
                stringResource(R.string.characters_help_assignments_contact)
            )
        )

        HelpSection(
            title = stringResource(R.string.characters_help_creating_title),
            message = stringResource(R.string.characters_help_creating_message),
            bullets = listOf(
                stringResource(R.string.characters_help_creating_sprites_front),
                stringResource(R.string.characters_help_creating_sprites_back)
            )
        )

        HelpSection(
            title = stringResource(R.string.characters_help_advanced_title),
            message = stringResource(R.string.characters_help_advanced_message)
        )

        HelpSection(
            title = stringResource(R.string.characters_help_packs_title),
            message = stringResource(R.string.characters_help_packs_message)
        )

        HelpSection(
            title = stringResource(R.string.characters_help_sharing_title),
            message = stringResource(R.string.characters_help_sharing_message),
            bullets = listOf(
                stringResource(R.string.characters_help_sharing_files_character),
                stringResource(R.string.characters_help_sharing_files_pack)
            ),
            footer = stringResource(R.string.characters_help_sharing_how_to)
        )

        HelpSection(
            title = stringResource(R.string.characters_help_radiants_title),
            message = stringResource(R.string.characters_help_radiants_message)
        )

        HelpSection(
            title = stringResource(R.string.characters_help_management_title),
            message = stringResource(R.string.characters_help_management_message)
        )
    }
}

@Composable
private fun HelpSection(
    title: String,
    message: String,
    bullets: List<String> = emptyList(),
    footer: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = parseHtmlBold(message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (bullets.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bullets.forEach { bullet ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = parseHtmlBold(bullet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (footer != null) {
            Text(
                text = parseHtmlBold(footer),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun parseHtmlBold(text: String): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        val boldStartTag = "<b>"
        val boldEndTag = "</b>"
        
        while (true) {
            val startIndex = text.indexOf(boldStartTag, lastIndex)
            if (startIndex == -1) {
                append(text.substring(lastIndex))
                break
            }
            append(text.substring(lastIndex, startIndex))
            val contentStart = startIndex + boldStartTag.length
            val endIndex = text.indexOf(boldEndTag, contentStart)
            if (endIndex == -1) {
                append(text.substring(startIndex))
                break
            }
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(text.substring(contentStart, endIndex))
            }
            lastIndex = endIndex + boldEndTag.length
        }
    }
}
