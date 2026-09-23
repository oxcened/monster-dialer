package dev.alenajam.monsterdialer.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.opendialer.feature.settings.R as SettingsR

private val AboutPaper = Color(0xFFF9F7FC)
private val AboutInk = Color(0xFF202020)
private val AboutFont = FontFamily(Font(R.font.pixel_operator))

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun MonsterAboutScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
    val appName = stringResource(R.string.app_name)
    val entries = aboutEntries(context.packageName, version, appName)
    val firstLinkIndex = entries.indexOfFirst { it is AboutEntry.Link }.coerceAtLeast(0)
    var cursor by rememberSaveable { mutableIntStateOf(firstLinkIndex) }

    BackHandler(onBack = onNavigateBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AboutPaper)
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(SettingsR.string.screen_about_title).uppercase(),
                        fontFamily = AboutFont,
                        fontSize = 16.sp,
                        color = AboutInk,
                        modifier = Modifier.padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(entries) { index, entry ->
                    when (entry) {
                        is AboutEntry.Section -> AboutSectionLabel(entry.title)
                        is AboutEntry.Info -> AboutInfoRow(entry)
                        is AboutEntry.Link -> AboutLinkRow(
                            entry = entry,
                            selected = index == cursor,
                            onClick = {
                                cursor = index
                                uriHandler.openUri(entry.uri)
                            },
                        )
                    }
                }
            }
            RetroFastScroller(
                listState = listState,
                contentDescription = stringResource(SettingsR.string.screen_about_title),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
        RetroFooter(
            onBack = onNavigateBack,
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.back),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 2.dp),
        )
    }
}

private sealed interface AboutEntry {
    data class Section(val title: String) : AboutEntry
    data class Info(val title: String, val description: String) : AboutEntry
    data class Link(val title: String, val description: String, val uri: String) : AboutEntry
}

@Composable
private fun aboutEntries(packageName: String, version: String, appName: String): List<AboutEntry> = buildList {
    add(AboutEntry.Info(appName, stringResource(SettingsR.string.version, version)))
    add(AboutEntry.Info(packageName, ""))
    add(AboutEntry.Section(stringResource(SettingsR.string.about_section_developer)))
    add(AboutEntry.Link(
        stringResource(SettingsR.string.developer_credit),
        stringResource(SettingsR.string.developer_website, stringResource(SettingsR.string.developer_domain)),
        stringResource(SettingsR.string.url_developer),
    ))
    add(AboutEntry.Section(stringResource(SettingsR.string.about_section_community)))
    add(AboutEntry.Link(stringResource(SettingsR.string.join_discord), stringResource(SettingsR.string.discord_description), stringResource(SettingsR.string.url_discord)))
    add(AboutEntry.Link(stringResource(R.string.additional_translation_project_title), stringResource(SettingsR.string.contribute_translations_description, appName), stringResource(R.string.url_additional_translation_project)))
    add(AboutEntry.Link(stringResource(SettingsR.string.contribute_translations), stringResource(SettingsR.string.contribute_translations_description, stringResource(SettingsR.string.opendialer_name)), stringResource(SettingsR.string.url_crowdin_opendialer)))
    add(AboutEntry.Link(stringResource(SettingsR.string.feature_requests), stringResource(SettingsR.string.feature_requests_description, appName), stringResource(R.string.url_feature_requests)))
    add(AboutEntry.Section(stringResource(SettingsR.string.about_section_open_source)))
    add(AboutEntry.Link(stringResource(SettingsR.string.view_source_code), stringResource(SettingsR.string.open_source_description, appName), stringResource(R.string.url_github_opendialer)))
    add(AboutEntry.Link(stringResource(SettingsR.string.report_an_issue), stringResource(SettingsR.string.issues_description), stringResource(SettingsR.string.url_github_issues)))
    add(AboutEntry.Link(stringResource(SettingsR.string.contact_developer), stringResource(SettingsR.string.contact_email), stringResource(SettingsR.string.url_contact_email)))
    add(AboutEntry.Section(stringResource(SettingsR.string.about_section_credits)))
    add(AboutEntry.Link(stringResource(SettingsR.string.typography_credit_title), stringResource(SettingsR.string.font_credit_format, stringResource(R.string.font_name), stringResource(R.string.font_author)), stringResource(R.string.url_font)))
    add(AboutEntry.Link(stringResource(SettingsR.string.icon_credit_title), stringResource(SettingsR.string.icon_credit_format, stringResource(R.string.icon_designer)), stringResource(R.string.url_icon_designer)))
}

@Composable
private fun AboutSectionLabel(title: String) {
    Text(title.uppercase(), fontFamily = AboutFont, fontSize = 16.sp, color = AboutInk, modifier = Modifier.padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 4.dp))
}

@Composable
private fun AboutInfoRow(entry: AboutEntry.Info) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(dev.alenajam.monsterdialer.app.ui.RetroSelectionArrowSize))
        AboutText(entry.title, entry.description)
    }
}

@Composable
private fun AboutLinkRow(entry: AboutEntry.Link, selected: Boolean, onClick: () -> Unit) {
    RetroSelectableRow(selected = selected, onClick = onClick) {
        AboutText(entry.title, entry.description)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AboutText(title: String, description: String) {
    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(title, fontFamily = AboutFont, fontSize = 20.sp, color = AboutInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (description.isNotBlank()) {
            Text(description, fontFamily = AboutFont, fontSize = 16.sp, color = AboutInk.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
