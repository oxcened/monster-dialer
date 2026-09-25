package dev.alenajam.monsterdialer.backup.ui

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.backup.data.LocalBackupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

@HiltViewModel
class LocalBackupViewModel @Inject constructor(private val repository: LocalBackupRepository) : ViewModel() {
    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()
    fun export(uri: Uri) = viewModelScope.launch { _message.value = if (repository.export(uri).isSuccess) R.string.local_backup_exported else R.string.local_backup_failed }
    fun import(uri: Uri, onRestored: () -> Unit) = viewModelScope.launch {
        if (repository.import(uri).isSuccess) {
            onRestored()
        } else {
            _message.value = R.string.local_backup_failed
        }
    }
    fun dismissMessage() { _message.value = null }
}

@Composable
fun LocalBackupScreen(
    viewModel: LocalBackupViewModel = hiltViewModel(),
) {
    val navigator = LocalSettingsSubpageNavigator.current
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    val backupFileName = stringResource(R.string.local_backup_file_name)
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(LocalBackupMimeType)) { uri -> uri?.let(viewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissMessage()
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.local_backup_description))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFF202020))
                Column {
                    RetroSelectableRow(selected = false, onClick = { exportLauncher.launch(backupFileName) }) {
                        Text(stringResource(R.string.local_backup_export).uppercase(Locale.ROOT), fontFamily = FontFamily(Font(R.font.ui_pixel_font)), fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp))
                    }
                    RetroSelectableRow(selected = false, onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Text(stringResource(R.string.local_backup_import).uppercase(Locale.ROOT), fontFamily = FontFamily(Font(R.font.ui_pixel_font)), fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp))
                    }
                }
            }
            RetroFooter(
                onBack = { navigator?.navigateBack() },
                backKey = stringResource(R.string.retro_key_b),
                backLabel = stringResource(R.string.back),
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 2.dp),
            )
        }
        pendingImport?.let { uri ->
            RetroConfirmationDialog(
                title = stringResource(R.string.local_backup_import_confirmation_title),
                message = stringResource(R.string.local_backup_import_confirmation_message),
                noLabel = stringResource(R.string.cancel), yesLabel = stringResource(R.string.local_backup_import),
                fontFamily = FontFamily(Font(R.font.ui_pixel_font)), onDismissRequest = { pendingImport = null },
                onConfirm = {
                    viewModel.import(uri) {
                        Toast.makeText(context, R.string.local_backup_restored, Toast.LENGTH_SHORT).show()
                        (context as? Activity)?.recreate()
                    }
                    pendingImport = null
                },
            )
        }
    }
}

private const val LocalBackupMimeType = "application/vnd.monsterdialer.backup"
