package dev.alenajam.monsterdialer.onlineprofiles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

/** Shows and removes the Online Profile associated with the selected contact. */
@Composable
fun ColumnScope.LinkedOnlineProfileContent(
    viewModel: ContactCharacterSettingsViewModel,
) {
    val profileId by viewModel.linkedOnlineProfileId.collectAsStateWithLifecycle()
    val navigator = LocalSettingsSubpageNavigator.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (profileId == null) {
            Text(
                text = stringResource(R.string.linked_online_profile_none),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.linked_online_profile_link_hint),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.linked_online_profile_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = requireNotNull(profileId),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = viewModel::unlinkOnlineProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.unlink_online_profile))
            }
        }
        Button(
            onClick = { navigator?.navigateBack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.close))
        }
    }
}
