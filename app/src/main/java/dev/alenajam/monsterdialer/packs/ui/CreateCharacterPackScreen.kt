package dev.alenajam.monsterdialer.packs.ui

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideButton
import dev.alenajam.monsterdialer.characters.ui.GuideContent
import dev.alenajam.monsterdialer.packs.data.CharacterPackArchive
import dev.alenajam.monsterdialer.packs.data.CharacterPackExportRequest
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterPackScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateCharacterPackViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val sharedFile by viewModel.sharedFile.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    val defaultVersion = stringResource(R.string.default_pack_version)
    var version by remember(defaultVersion) { mutableStateOf(defaultVersion) }
    var creator by remember { mutableStateOf("") }
    var advancedExpanded by remember { mutableStateOf(false) }
    val defaultLicense = stringResource(R.string.default_license)
    var license by remember(defaultLicense) { mutableStateOf(defaultLicense) }
    val request = remember(selectedIds, name, version, creator, license) {
        CharacterPackExportRequest(selectedIds, viewModel.packIdFor(name), name, version, creator, license)
    }
    val canExport = selectedIds.isNotEmpty() && name.isNotBlank() && version.isNotBlank() && license.isNotBlank()
    val fileName = stringResource(R.string.character_pack_file_name, name.ifBlank { stringResource(R.string.character_pack_default_name) })
    val shareTitle = stringResource(R.string.share_character_pack)
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CharacterPackArchive.MimeType)) { uri ->
        if (uri != null) viewModel.export(request, uri, context)
    }

    LaunchedEffect(sharedFile) {
        sharedFile?.let { file ->
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = CharacterPackArchive.MimeType
                putExtra(Intent.EXTRA_STREAM, file)
                clipData = ClipData.newRawUri(fileName, file)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, shareTitle))
            viewModel.consumeSharedFile()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.create_character_pack)) }, navigationIcon = {
        IconButton(onClick = onNavigateBack) { AppIcon(LocalAppIcons.current.arrowLeft, null) }
    }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(stringResource(R.string.select_characters_for_pack))
                    ContextualGuideButton(
                        contents = listOf(GuideContent(R.string.characters_help_packs_title, R.string.characters_help_packs_message)),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
            items(viewModel.characters, key = { it.character.id }) { exported ->
                Card(onClick = { viewModel.toggle(exported.character.id) }, colors = CardDefaults.cardColors()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(exported.frontImageFile ?: exported.backImageFile, null, Modifier.size(56.dp))
                        Text(exported.character.name, Modifier.weight(1f))
                        Checkbox(exported.character.id in selectedIds, onCheckedChange = { viewModel.toggle(exported.character.id) })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.pack_details))
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.pack_name_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(creator, { creator = it }, label = { Text(stringResource(R.string.creator_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { advancedExpanded = !advancedExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.advanced_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        AppIcon(
                            icon = if (advancedExpanded) LocalAppIcons.current.arrowUp else LocalAppIcons.current.arrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (advancedExpanded) {
                        OutlinedTextField(version, { version = it }, label = { Text(stringResource(R.string.pack_version_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(license, { license = it }, label = { Text(stringResource(R.string.license_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    Button({ viewModel.exportForSharing(request, context, fileName) }, Modifier.fillMaxWidth(), enabled = canExport) { Text(stringResource(R.string.share)) }
                    Button({ save.launch(fileName) }, Modifier.fillMaxWidth(), enabled = canExport) { Text(stringResource(R.string.save_to_device)) }
                }
            }
        }
    }
}
