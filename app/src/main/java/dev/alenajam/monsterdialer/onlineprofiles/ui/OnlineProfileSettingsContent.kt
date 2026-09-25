package dev.alenajam.monsterdialer.onlineprofiles.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.VariantBackupSynchronizer
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuDialog
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.app.ui.RetroMenuWindow
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideDialog
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideButton
import dev.alenajam.monsterdialer.onlineprofiles.data.ProfileSharingLink
import dev.alenajam.monsterdialer.onlineprofiles.data.ProfileSharingQrCode
import dev.alenajam.monsterdialer.onlineprofiles.data.QrCodeMatrix
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val ProfilePixelFont = FontFamily(Font(R.font.ui_pixel_font))
private val ProfileInk = Color(0xFF202020)

@Composable
fun OnlineProfileSection(viewModel: OnlineProfileSettingsViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val working by viewModel.isWorking.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showRetentionCheckIn by viewModel.showRetentionCheckIn.collectAsStateWithLifecycle()
    val variantBackupEnabled by viewModel.variantBackupEnabled.collectAsStateWithLifecycle()
    val variantBackupStatus by viewModel.variantBackupStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val regeneratingDescription = stringResource(R.string.online_profile_regenerating)
    val enablingDescription = stringResource(R.string.online_profile_enabling)
    val deletingDescription = stringResource(R.string.online_profile_deleting)
    val clearingVariantBackupDescription = stringResource(R.string.online_profile_clearing_variant_backup)
    val deletingAccountDescription = stringResource(R.string.online_profile_deleting_account)
    val signingInDescription = stringResource(R.string.online_profile_signing_in)
    val keepingOnlineDescription = stringResource(R.string.online_profile_keeping_online)
    val googleSignInNotConfigured = stringResource(R.string.online_profile_google_sign_in_not_configured)
    val navigator = LocalSettingsSubpageNavigator.current
    val googleServerClientId = remember(resources) {
        resources.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?.let(resources::getString)
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeleteVariantBackup by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var confirmRegenerate by remember { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    val signOut: () -> Unit = {
        scope.launch {
            runCatching { GoogleProfileSignIn.clearCredentialState(context) }
            viewModel.signOut()
        }
    }
    androidx.compose.runtime.LaunchedEffect(viewModel, googleServerClientId) {
        viewModel.credentialRequest.collectLatest { request ->
            if (request == null) return@collectLatest
            val serverClientId = googleServerClientId
            if (serverClientId == null) {
                viewModel.failGoogleSignIn(googleSignInNotConfigured)
            } else {
                runCatching { GoogleProfileSignIn.idToken(context, serverClientId) }
                    .onSuccess { idToken ->
                        when (request) {
                            OnlineProfileCredentialRequest.SignIn -> viewModel.completeGoogleSignIn(idToken)
                            OnlineProfileCredentialRequest.DeleteAccount -> viewModel.deleteAccount(idToken)
                        }
                    }
                    .onFailure { exception -> viewModel.failGoogleSignIn(exception.message) }
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(error) {
        error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    val linkIsOn = profile != null
    val sharingLink = profile?.let { ProfileSharingLink.urlFor(it.publicProfileId) }
    val shareText = sharingLink?.let {
        stringResource(R.string.online_profile_share_text, it)
    }
    val shareTitle = stringResource(R.string.online_profile_share)
    var guideOpen by remember { mutableStateOf(false) }
    var selectedMenuIndex by remember { mutableStateOf(0) }
    val shareIndex = if (linkIsOn) 0 else -1
    val primaryIndex = if (linkIsOn) 1 else 0
    val backupIndex = if (isSignedIn) primaryIndex + 1 else -1
    val regenerateIndex = if (linkIsOn) {
        if (isSignedIn) backupIndex + 1 else primaryIndex + 1
    } else -1
    val signOutIndex = if (isSignedIn) {
        if (linkIsOn) regenerateIndex + 1 else backupIndex + 1
    } else -1
    val deleteAccountIndex = if (isSignedIn) signOutIndex + 1 else -1
    val helpIndex = when {
        isSignedIn -> deleteAccountIndex + 1
        linkIsOn -> regenerateIndex + 1
        else -> primaryIndex + 1
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RetroScreenHorizontalPadding,
                    vertical = 0.dp,
                ),
        ) {
            RetroMenuWindow {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(
                            if (linkIsOn) R.string.online_profile_link_on
                            else R.string.online_profile_link_off,
                        ),
                        fontFamily = ProfilePixelFont,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        color = ProfileInk,
                    )
                    if (linkIsOn) {
                        RetroSelectableRow(
                            selected = selectedMenuIndex == shareIndex,
                            enabled = !working,
                            onClick = {
                                selectedMenuIndex = shareIndex
                                showShareOptions = true
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.online_profile_share).uppercase(),
                                fontFamily = ProfilePixelFont,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                color = ProfileInk,
                            )
                        }
                    }
                    RetroSelectableRow(
                        selected = selectedMenuIndex == primaryIndex,
                        enabled = !working,
                        onClick = {
                            selectedMenuIndex = primaryIndex
                            if (linkIsOn) confirmDelete = true
                            else if (isSignedIn) viewModel.enable() else viewModel.signIn()
                        },
                    ) {
                        if (
                            operation == OnlineProfileOperation.SignIn ||
                            operation == OnlineProfileOperation.Enable ||
                            operation == OnlineProfileOperation.Delete
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = when (operation) {
                                    OnlineProfileOperation.SignIn -> signingInDescription.uppercase()
                                    OnlineProfileOperation.Enable -> enablingDescription.uppercase()
                                    else -> deletingDescription.uppercase()
                                },
                                modifier = Modifier.padding(start = 8.dp),
                                fontFamily = ProfilePixelFont,
                                fontSize = 14.sp,
                                color = ProfileInk.copy(alpha = 0.76f),
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    when {
                                        linkIsOn -> R.string.online_profile_delete
                                        isSignedIn -> R.string.online_profile_enable
                                        else -> R.string.online_profile_sign_in_action
                                    },
                                ).uppercase(),
                                fontFamily = ProfilePixelFont,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                color = if (linkIsOn) MaterialTheme.colorScheme.error else ProfileInk,
                            )
                        }
                    }
                    if (isSignedIn) {
                        RetroSelectableRow(
                            selected = selectedMenuIndex == backupIndex,
                            enabled = !working,
                            onClick = {
                                selectedMenuIndex = backupIndex
                                if (variantBackupEnabled) confirmDeleteVariantBackup = true
                                else viewModel.enableVariantBackup()
                            },
                        ) {
                            if (operation == OnlineProfileOperation.DeleteVariantBackup) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = clearingVariantBackupDescription.uppercase(),
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 14.sp,
                                    color = ProfileInk.copy(alpha = 0.76f),
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        if (variantBackupEnabled) R.string.online_profile_clear_variant_backup
                                        else R.string.variant_backup_enable,
                                    ).uppercase(),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    color = if (variantBackupEnabled) MaterialTheme.colorScheme.error else ProfileInk,
                                )
                            }
                        }
                        if (variantBackupStatus is VariantBackupSynchronizer.Status.Failed) {
                            RetroSelectableRow(
                                selected = false,
                                enabled = !working,
                                onClick = viewModel::retryVariantBackup,
                            ) {
                                Text(
                                    text = stringResource(R.string.variant_backup_retry).uppercase(),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (linkIsOn) {
                        RetroSelectableRow(
                            selected = selectedMenuIndex == regenerateIndex,
                            enabled = !working,
                            onClick = {
                                selectedMenuIndex = regenerateIndex
                                confirmRegenerate = true
                            },
                        ) {
                            if (operation == OnlineProfileOperation.Regenerate) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = regeneratingDescription.uppercase(),
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 14.sp,
                                    color = ProfileInk.copy(alpha = 0.76f),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.online_profile_regenerate).uppercase(),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    color = ProfileInk,
                                )
                            }
                        }
                    }
                    if (isSignedIn) {
                        RetroSelectableRow(
                            selected = selectedMenuIndex == signOutIndex,
                            enabled = !working,
                            onClick = {
                                selectedMenuIndex = signOutIndex
                                signOut()
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.online_profile_sign_out_google).uppercase(),
                                fontFamily = ProfilePixelFont,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                color = ProfileInk,
                            )
                        }
                        RetroSelectableRow(
                            selected = selectedMenuIndex == deleteAccountIndex,
                            enabled = !working,
                            onClick = {
                                selectedMenuIndex = deleteAccountIndex
                                confirmDeleteAccount = true
                            },
                        ) {
                            if (operation == OnlineProfileOperation.DeleteAccount) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = deletingAccountDescription.uppercase(),
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 14.sp,
                                    color = ProfileInk.copy(alpha = 0.76f),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.online_profile_delete_account).uppercase(),
                                    fontFamily = ProfilePixelFont,
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    RetroSelectableRow(
                        selected = selectedMenuIndex == helpIndex,
                        onClick = {
                            selectedMenuIndex = helpIndex
                            guideOpen = true
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.retro_picker_guide).uppercase(),
                            fontFamily = ProfilePixelFont,
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            color = ProfileInk,
                        )
                    }
                }
            }
        }
        RetroFooter(
            message = stringResource(
                if (linkIsOn) R.string.online_profile_linked_message
                else R.string.online_profile_not_linked_message,
            ),
            animationKey = linkIsOn,
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.customized_contacts_back_action),
            onBack = { navigator?.navigateBack() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    if (guideOpen) {
        ContextualGuideDialog(
            contents = ownedOnlineProfileGuideContents(),
            onDismiss = {
                guideOpen = false
                selectedMenuIndex = helpIndex
            },
        )
    }
    if (showShareOptions) {
        RetroContextMenuDialog(
            title = shareTitle,
            fontFamily = ProfilePixelFont,
            items = listOf(
                RetroContextMenuItem(stringResource(R.string.online_profile_share_link)) {
                    showShareOptions = false
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, requireNotNull(shareText))
                    }, shareTitle))
                },
                RetroContextMenuItem(stringResource(R.string.online_profile_qr_action)) {
                    showShareOptions = false
                    showQrCode = true
                },
                RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                    showShareOptions = false
                },
            ),
            onDismissRequest = { showShareOptions = false },
        )
    }
    if (showQrCode) {
        ProfileSharingQrCodeSheet(
            sharingLink = requireNotNull(sharingLink),
            onDismiss = { showQrCode = false },
        )
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.online_profile_delete)) },
        text = { Text(stringResource(R.string.online_profile_delete_message)) },
        confirmButton = {
            TextButton(
                onClick = { confirmDelete = false; viewModel.delete() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.online_profile_delete))
            }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmDeleteVariantBackup) AlertDialog(
        onDismissRequest = { confirmDeleteVariantBackup = false },
        title = { Text(stringResource(R.string.online_profile_clear_variant_backup)) },
        text = { Text(stringResource(R.string.online_profile_clear_variant_backup_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    confirmDeleteVariantBackup = false
                    viewModel.deleteVariantBackup()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.online_profile_clear_variant_backup))
            }
        },
        dismissButton = { TextButton(onClick = { confirmDeleteVariantBackup = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmRegenerate) AlertDialog(
        onDismissRequest = { confirmRegenerate = false },
        title = { Text(stringResource(R.string.online_profile_regenerate)) },
        text = { Text(stringResource(R.string.online_profile_regenerate_message)) },
        confirmButton = {
            TextButton(
                onClick = { confirmRegenerate = false; viewModel.regenerate() },
            ) {
                Text(stringResource(R.string.online_profile_regenerate))
            }
        },
        dismissButton = { TextButton(onClick = { confirmRegenerate = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmDeleteAccount) AlertDialog(
        onDismissRequest = { confirmDeleteAccount = false },
        title = { Text(stringResource(R.string.online_profile_delete_account)) },
        text = { Text(stringResource(R.string.online_profile_delete_account_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    confirmDeleteAccount = false
                    viewModel.requestAccountDeletion()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.online_profile_delete_account))
            }
        },
        dismissButton = { TextButton(onClick = { confirmDeleteAccount = false }) { Text(stringResource(R.string.cancel)) } },
    )
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileSharingQrCodeSheet(
    sharingLink: String,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.online_profile_qr_code_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            ProfileSharingQrCodeImage(
                sharingLink = sharingLink,
                modifier = Modifier.size(256.dp),
            )
            Text(
                stringResource(R.string.online_profile_qr_code_description),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f),
            )
        }
    }
}

@Composable
private fun ProfileSharingQrCodeImage(
    sharingLink: String,
    modifier: Modifier = Modifier,
) {
    val qrCode = remember(sharingLink) { ProfileSharingQrCode.encode(sharingLink) }
    val contentDescription = stringResource(R.string.online_profile_qr_code_content_description)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawQrCode(qrCode)
    }
}

private fun DrawScope.drawQrCode(qrCode: QrCodeMatrix) {
    val moduleSize = size.width / qrCode.width
    drawRect(Color.White)
    for (y in 0 until qrCode.height) {
        for (x in 0 until qrCode.width) {
            if (qrCode[x, y]) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(x * moduleSize, y * moduleSize),
                    size = Size(moduleSize, moduleSize),
                )
            }
        }
    }
}
