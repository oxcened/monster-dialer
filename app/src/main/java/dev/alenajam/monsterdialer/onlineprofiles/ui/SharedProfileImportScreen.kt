package dev.alenajam.monsterdialer.onlineprofiles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideButton
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.feature.contacts.ContactPickerScreen
import kotlinx.coroutines.launch
import android.widget.Toast

/** Selects the local contact that should use a profile opened from a shared link. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedProfileImportScreen(
    viewModel: ContactCharacterSettingsViewModel,
    onNavigateBack: () -> Unit,
    onProfileLinked: () -> Unit,
) {
    var isChoosingContact by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val linkFailedMessage = stringResource(R.string.shared_profile_import_link_failed)

    if (isChoosingContact) {
        ContactPickerScreen(
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.shared_profile_import_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            AppIcon(
                                LocalAppIcons.current.arrowLeft,
                                contentDescription = stringResource(R.string.navigate_back),
                            )
                        }
                    },
                    actions = {
                        ContextualGuideButton(
                            contents = sharedOnlineProfileGuideContents(),
                            contentDescription = R.string.open_shared_online_profile_guide,
                        )
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppIcon(
                    LocalAppIcons.current.person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.shared_profile_import_heading),
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.shared_profile_import_description),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { isChoosingContact = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                ) {
                    Text(stringResource(R.string.shared_profile_import_choose_contact))
                }
            }
        }
    }
}
