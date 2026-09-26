package dev.alenajam.monsterdialer.onlineprofiles.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideDialog
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

private val LinkedProfileFont = FontFamily(Font(R.font.ui_pixel_font))
private val LinkedProfileInk = Color(0xFF202020)

/** Shows and removes the Online Profile associated with the selected contact. */
@Composable
fun ColumnScope.LinkedOnlineProfileContent(
    viewModel: ContactCharacterSettingsViewModel,
) {
    val profileId by viewModel.linkedOnlineProfileId.collectAsStateWithLifecycle()
    var confirmUnlink by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) }
    var selectedAction by remember(profileId) {
        mutableStateOf(if (profileId == null) LinkedProfileAction.Help else LinkedProfileAction.Unlink)
    }
    val navigator = LocalSettingsSubpageNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (profileId == null) {
            LinkedProfileCard(
                title = stringResource(R.string.linked_online_profile_none),
                message = stringResource(R.string.linked_online_profile_link_hint),
                isLinked = false,
            )
        } else {
            LinkedProfileCard(
                title = stringResource(R.string.linked_online_profile_active_title),
                message = stringResource(R.string.linked_online_profile_active_message),
                isLinked = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.linked_online_profile_id_label),
                    fontFamily = LinkedProfileFont,
                    fontSize = 15.sp,
                    color = LinkedProfileInk.copy(alpha = 0.72f),
                )
                Text(
                    text = requireNotNull(profileId),
                    fontFamily = LinkedProfileFont,
                    fontSize = 16.sp,
                    color = LinkedProfileInk,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (profileId != null) {
                RetroSelectableRow(
                    selected = selectedAction == LinkedProfileAction.Unlink,
                    onClick = {
                        selectedAction = LinkedProfileAction.Unlink
                        confirmUnlink = true
                    },
                ) {
                    Text(
                        text = stringResource(R.string.unlink_online_profile_action).uppercase(Locale.ROOT),
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                        fontFamily = LinkedProfileFont,
                        fontSize = 17.sp,
                        color = LinkedProfileInk,
                    )
                }
            }
            RetroSelectableRow(
                selected = selectedAction == LinkedProfileAction.Help,
                onClick = {
                    selectedAction = LinkedProfileAction.Help
                    guideOpen = true
                },
            ) {
                Text(
                    text = stringResource(R.string.retro_picker_guide).uppercase(Locale.ROOT),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    fontFamily = LinkedProfileFont,
                    fontSize = 17.sp,
                    color = LinkedProfileInk,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        RetroFooter(
            message = null,
            animationKey = "linked-online-profile",
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.customized_contacts_back_action),
            onBack = { navigator?.navigateBack() },
        )
    }
    if (guideOpen) {
        ContextualGuideDialog(
            contents = sharedOnlineProfileGuideContents(),
            onDismiss = { guideOpen = false },
        )
    }
    if (confirmUnlink) {
        RetroConfirmationDialog(
            title = stringResource(R.string.unlink_online_profile_confirmation_title),
            message = stringResource(R.string.unlink_online_profile_confirmation_message),
            noLabel = stringResource(R.string.cancel),
            yesLabel = stringResource(R.string.unlink_online_profile),
            fontFamily = LinkedProfileFont,
            onDismissRequest = { confirmUnlink = false },
            onConfirm = {
                confirmUnlink = false
                viewModel.unlinkOnlineProfile()
            },
        )
    }
}

private enum class LinkedProfileAction {
    Unlink,
    Help,
}

@Composable
private fun LinkedProfileCard(title: String, message: String, isLinked: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.player),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            colorFilter = if (isLinked) {
                null
            } else {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            },
        )
        Text(
            text = title,
            fontFamily = LinkedProfileFont,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            color = LinkedProfileInk,
        )
        Text(
            text = message,
            fontFamily = LinkedProfileFont,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = LinkedProfileInk.copy(alpha = 0.72f),
        )
    }
}
