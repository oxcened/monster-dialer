package dev.alenajam.monsterdialer.app

import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroDialog
import dev.alenajam.monsterdialer.app.ui.RetroManualDialogueBox
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.opendialer.feature.appShell.SetupScreenCallbacks
import java.util.Locale

@Composable
internal fun MonsterSetupScreen(setup: SetupScreenCallbacks) {
    val appName = stringResource(R.string.app_name).uppercase(Locale.ROOT)
    val message = when {
        !setup.isDefaultPhoneApp -> stringResource(R.string.first_run_phone_app_description)
        else -> stringResource(R.string.first_run_setup_description)
    }
    val actionLabel = stringResource(R.string.first_run_next)
    var advanceSetup by remember { mutableStateOf<() -> Unit>({}) }
    var showRestrictedSettingsHelp by remember { mutableStateOf(false) }

    LaunchedEffect(setup.showDefaultPhoneRecovery) {
        if (setup.showDefaultPhoneRecovery) showRestrictedSettingsHelp = true
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.first_run_guide),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(128.dp),
                    )
                    RetroManualDialogueBox(
                        message = message,
                        modifier = Modifier.fillMaxWidth(),
                        animationKey = message,
                        onAdvanceActionChanged = { advanceSetup = it },
                        onMessageFinished = {
                            if (setup.isDefaultPhoneApp) {
                                setup.onEnableFullScreenIntent()
                            } else {
                                setup.onSetAsDefault()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RetroActionButton(
                    key = stringResource(R.string.retro_key_a),
                    label = actionLabel,
                    onClick = advanceSetup,
                )
                if (setup.showDefaultPhoneRecovery) {
                    RetroActionButton(
                        key = stringResource(R.string.retro_key_b),
                        label = stringResource(R.string.setup_default_phone_help_action),
                        onClick = { showRestrictedSettingsHelp = true },
                    )
                }
            }
        }
    }

    if (showRestrictedSettingsHelp) {
        RestrictedSettingsHelpDialog(
            appName = appName,
            onDismiss = { showRestrictedSettingsHelp = false },
            onOpenAppInfo = {
                showRestrictedSettingsHelp = false
                setup.onOpenAppInfo()
            },
        )
    }
}

@Composable
private fun RestrictedSettingsHelpDialog(
    appName: String,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val pixelFont = remember { FontFamily(Font(R.font.ui_pixel_font)) }
    val modalFont = remember { FontFamily(Font(R.font.pixel_operator)) }
    val bodyStyle = TextStyle(fontFamily = modalFont, fontSize = 18.sp, lineHeight = 23.sp)
    val openAppInfoLabel = stringResource(
        dev.alenajam.opendialer.feature.appShell.R.string.setup_default_phone_restricted_action,
    )
    val backLabel = stringResource(R.string.retro_action_back_label)

    RetroDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_help_intro, appName),
                style = bodyStyle,
            )
            Text(
                text = stringResource(R.string.setup_help_open_app_info),
                style = bodyStyle,
            )
            Text(
                text = setupHelpRichText(stringResource(R.string.setup_help_step_4)),
                style = bodyStyle,
            )
            Text(
                text = stringResource(R.string.setup_help_step_5),
                style = bodyStyle,
            )
            Image(
                painter = painterResource(R.drawable.restricted_settings_help),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .width(150.dp)
                    .aspectRatio(377f / 800f),
            )
            RetroSelectableRow(selected = true, onClick = onOpenAppInfo) {
                Text(
                    text = openAppInfoLabel.uppercase(Locale.ROOT),
                    fontFamily = pixelFont,
                    fontSize = 16.sp,
                    color = Color(0xFF202020),
                )
            }
            RetroSelectableRow(selected = false, onClick = onDismiss) {
                Text(
                    text = backLabel.uppercase(Locale.ROOT),
                    fontFamily = pixelFont,
                    fontSize = 16.sp,
                    color = Color(0xFF202020),
                )
            }
        }
    }
}

private fun setupHelpRichText(html: String): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return AnnotatedString.Builder().apply {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, StyleSpan::class.java).forEach { span ->
            if (span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    spanned.getSpanStart(span),
                    spanned.getSpanEnd(span),
                )
            }
        }
    }.toAnnotatedString()
}
