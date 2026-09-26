package dev.alenajam.monsterdialer.onlineprofiles.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideDialog
import dev.alenajam.monsterdialer.characters.ui.RetroContactPickerScreen
import kotlinx.coroutines.launch

private val SharedProfileFont = FontFamily(Font(R.font.ui_pixel_font))
private val SharedProfileInk = Color(0xFF202020)
private val SharedProfilePaper = Color(0xFFF9F7FC)

/** Selects the local contact that should use a profile opened from a shared link. */
@Composable
fun SharedProfileImportScreen(
    viewModel: ContactCharacterSettingsViewModel,
    onNavigateBack: () -> Unit,
    onProfileLinked: () -> Unit,
) {
    var isChoosingContact by remember { mutableStateOf(false) }
    var isGuideOpen by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf(SharedProfileAction.ChooseContact) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val linkFailedMessage = stringResource(R.string.shared_profile_import_link_failed)
    BackHandler(enabled = !isChoosingContact, onBack = onNavigateBack)

    Surface(modifier = Modifier.fillMaxSize(), color = SharedProfilePaper) {
        if (isChoosingContact) {
            RetroContactPickerScreen(
                onNavigateBack = { isChoosingContact = false },
                onContactSelected = { contact ->
                    scope.launch {
                        if (viewModel.selectContactForPendingOnlineProfile(contact)) {
                            onProfileLinked()
                        } else {
                            Toast.makeText(context, linkFailedMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.battle_enemy_trainer),
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.shared_profile_import_heading),
                                fontFamily = SharedProfileFont,
                                fontSize = 20.sp,
                                lineHeight = 25.sp,
                                color = SharedProfileInk,
                            )
                            Text(
                                text = stringResource(R.string.shared_profile_import_description),
                                fontFamily = SharedProfileFont,
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                color = SharedProfileInk.copy(alpha = 0.78f),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            RetroSelectableRow(
                                selected = selectedAction == SharedProfileAction.ChooseContact,
                                onClick = {
                                    selectedAction = SharedProfileAction.ChooseContact
                                    isChoosingContact = true
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.shared_profile_import_choose_contact)
                                        .uppercase(Locale.ROOT),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                                    fontFamily = SharedProfileFont,
                                    fontSize = 17.sp,
                                    color = SharedProfileInk,
                                )
                            }
                            RetroSelectableRow(
                                selected = selectedAction == SharedProfileAction.Help,
                                onClick = {
                                    selectedAction = SharedProfileAction.Help
                                    isGuideOpen = true
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.retro_picker_guide),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                                    fontFamily = SharedProfileFont,
                                    fontSize = 17.sp,
                                    color = SharedProfileInk,
                                )
                            }
                        }
                    }
                }
                RetroFooter(
                    message = null,
                    animationKey = "shared-profile-import-actions",
                    backKey = stringResource(R.string.retro_key_b),
                    backLabel = stringResource(R.string.customized_contacts_back_action),
                    onBack = onNavigateBack,
                )
            }
            if (isGuideOpen) {
                ContextualGuideDialog(
                    contents = sharedOnlineProfileGuideContents(),
                    onDismiss = { isGuideOpen = false },
                )
            }
        }
    }
}

private enum class SharedProfileAction {
    ChooseContact,
    Help,
}
