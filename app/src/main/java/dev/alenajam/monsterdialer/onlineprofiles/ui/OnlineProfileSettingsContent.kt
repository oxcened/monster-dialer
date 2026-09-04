package dev.alenajam.monsterdialer.onlineprofiles.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideButton
import dev.alenajam.monsterdialer.characters.ui.GuideContent
import dev.alenajam.monsterdialer.onlineprofiles.data.ProfileSharingLink
import dev.alenajam.monsterdialer.onlineprofiles.data.ProfileSharingQrCode
import dev.alenajam.monsterdialer.onlineprofiles.data.QrCodeMatrix
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun OnlineProfileSection(viewModel: OnlineProfileSettingsViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val working by viewModel.isWorking.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showRetentionCheckIn by viewModel.showRetentionCheckIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val enablingDescription = stringResource(R.string.online_profile_enabling)
    val regeneratingDescription = stringResource(R.string.online_profile_regenerating)
    val deletingDescription = stringResource(R.string.online_profile_deleting)
    val keepingOnlineDescription = stringResource(R.string.online_profile_keeping_online)
    val signingInDescription = stringResource(R.string.online_profile_signing_in)
    val googleSignInNotConfigured = stringResource(R.string.online_profile_google_sign_in_not_configured)
    val googleServerClientId = remember(resources) {
        resources.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?.let(resources::getString)
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRegenerate by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(viewModel, googleServerClientId) {
        viewModel.signInRequests.collectLatest {
            val serverClientId = googleServerClientId
            if (serverClientId == null) {
                viewModel.failGoogleSignIn(googleSignInNotConfigured)
            } else {
                runCatching { GoogleProfileSignIn.idToken(context, serverClientId) }
                    .onSuccess(viewModel::completeGoogleSignIn)
                    .onFailure { exception -> viewModel.failGoogleSignIn(exception.message) }
            }
        }
    }
    if (profile == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        LocalAppIcons.current.person,
                        null,
                        Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.online_profile_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            ContextualGuideButton(
                                contents = listOf(
                                    GuideContent(R.string.online_profile_guide_title, R.string.online_profile_guide_message),
                                    GuideContent(R.string.online_profile_public_id_title, R.string.online_profile_public_id_message),
                                    GuideContent(R.string.online_profile_delete_guide_title, R.string.online_profile_delete_guide_message),
                                ),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Text(
                            stringResource(R.string.online_profile_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f),
                        )
                    }
                }
                Text(
                    stringResource(R.string.online_profile_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = viewModel::enable,
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (working) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).semantics {
                                    contentDescription = if (operation == OnlineProfileOperation.SignIn) signingInDescription else enablingDescription
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.online_profile_enable))
                        }
                    }
                    if (isSignedIn) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching { GoogleProfileSignIn.clearCredentialState(context) }
                                    viewModel.signOut()
                                }
                            },
                            enabled = !working,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.online_profile_sign_out_google))
                        }
                    }
                }
            }
        }
    } else {
        val currentProfile = requireNotNull(profile)
        val sharingLink = ProfileSharingLink.urlFor(currentProfile.publicProfileId)
        val shareText = stringResource(
            R.string.online_profile_share_text,
            sharingLink,
        )
        val shareTitle = stringResource(R.string.online_profile_share)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(LocalAppIcons.current.person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.online_profile_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            ContextualGuideButton(
                                contents = listOf(
                                    GuideContent(R.string.online_profile_guide_title, R.string.online_profile_guide_message),
                                    GuideContent(R.string.online_profile_public_id_title, R.string.online_profile_public_id_message),
                                    GuideContent(R.string.online_profile_delete_guide_title, R.string.online_profile_delete_guide_message),
                                ),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Text(stringResource(R.string.online_profile_enabled), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText)
                        }, shareTitle))
                    }, enabled = !working, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.online_profile_share)) }
                    OutlinedButton(
                        onClick = { showQrCode = true },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.online_profile_show_qr_code)) }
                    OutlinedButton(
                        onClick = { confirmRegenerate = true },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (operation == OnlineProfileOperation.Regenerate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).semantics { contentDescription = regeneratingDescription },
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.online_profile_regenerate))
                        }
                    }
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        if (operation == OnlineProfileOperation.Delete) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).semantics { contentDescription = deletingDescription },
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.online_profile_delete))
                        }
                    }
                }
            }
        }
        if (showQrCode) {
            AlertDialog(
                onDismissRequest = { showQrCode = false },
                title = { Text(stringResource(R.string.online_profile_qr_code_title)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProfileSharingQrCodeImage(sharingLink)
                        Text(stringResource(R.string.online_profile_qr_code_description))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQrCode = false }) { Text(stringResource(R.string.close)) }
                },
            )
        }
        if (showRetentionCheckIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.online_profile_check_in_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        stringResource(R.string.online_profile_check_in_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                    Button(
                        onClick = viewModel::keepOnline,
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (operation == OnlineProfileOperation.KeepOnline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).semantics { contentDescription = keepingOnlineDescription },
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.online_profile_keep_online))
                        }
                    }
                }
            }
        }
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
    if (confirmRegenerate) AlertDialog(
        onDismissRequest = { confirmRegenerate = false },
        title = { Text(stringResource(R.string.online_profile_regenerate)) },
        text = { Text(stringResource(R.string.online_profile_regenerate_message)) },
        confirmButton = { TextButton(onClick = { confirmRegenerate = false; viewModel.regenerate() }) { Text(stringResource(R.string.online_profile_regenerate)) } },
        dismissButton = { TextButton(onClick = { confirmRegenerate = false }) { Text(stringResource(R.string.cancel)) } },
    )
    error?.let { AlertDialog(onDismissRequest = viewModel::clearError, title = { Text(stringResource(R.string.online_profile_error_title)) }, text = { Text(it) }, confirmButton = { TextButton(onClick = viewModel::clearError) { Text(stringResource(R.string.close)) } }) }
}

@Composable
private fun ProfileSharingQrCodeImage(sharingLink: String) {
    val qrCode = remember(sharingLink) { ProfileSharingQrCode.encode(sharingLink) }
    val contentDescription = stringResource(R.string.online_profile_qr_code_content_description)
    Canvas(
        modifier = Modifier
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
