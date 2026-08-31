package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCharacterScreen(
    onNavigateBack: () -> Unit,
    characterType: CharacterType,
    characterId: String? = null,
    preferredAssignmentTarget: CharacterAssignmentTarget? = null,
    viewModel: AddCharacterViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val isRadiant by viewModel.isRadiant.collectAsStateWithLifecycle()
    val frontImageUri by viewModel.frontImageUri.collectAsStateWithLifecycle()
    val backImageUri by viewModel.backImageUri.collectAsStateWithLifecycle()
    val radiantFrontImageUri by viewModel.radiantFrontImageUri.collectAsStateWithLifecycle()
    val radiantBackImageUri by viewModel.radiantBackImageUri.collectAsStateWithLifecycle()
    val existingFrontImageFile by viewModel.existingFrontImageFile.collectAsStateWithLifecycle()
    val existingBackImageFile by viewModel.existingBackImageFile.collectAsStateWithLifecycle()
    val existingRadiantFrontImageFile by viewModel.existingRadiantFrontImageFile.collectAsStateWithLifecycle()
    val existingRadiantBackImageFile by viewModel.existingRadiantBackImageFile.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val maxHp by viewModel.maxHp.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isLimitReached by viewModel.isLimitReached.collectAsStateWithLifecycle()
    val isAssignedToPlayer by viewModel.isAssignedToPlayer.collectAsStateWithLifecycle()
    val isAssignedToContact by viewModel.isAssignedToContact.collectAsStateWithLifecycle()
    val creationResult by viewModel.creationResult.collectAsStateWithLifecycle()

    val frontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onFrontImageSelected(uri)
    }
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onBackImageSelected(uri)
    }
    val radiantFrontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onRadiantFrontImageSelected(uri)
    }
    val radiantBackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onRadiantBackImageSelected(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.setPreferredAssignmentTarget(preferredAssignmentTarget)
        if (characterId != null) {
            viewModel.loadCharacter(characterId)
        } else {
            viewModel.onTypeChanged(characterType)
        }
    }

    LaunchedEffect(creationResult) {
        if (creationResult != null) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(
                            if (characterId != null) {
                                if (characterType == CharacterType.Trainer) R.string.edit_trainer_title
                                else R.string.edit_monster_title
                            } else {
                                if (characterType == CharacterType.Trainer) R.string.create_trainer_title
                                else R.string.create_monster_title
                            }
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        AppIcon(
                            icon = LocalAppIcons.current.arrowLeft,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Character Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Sprites Section Header
                        Text(
                            text = stringResource(R.string.character_sprites_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val frontRequired = isAssignedToContact || 
                                preferredAssignmentTarget == CharacterAssignmentTarget.Contact
                            val backRequired = isAssignedToPlayer || 
                                preferredAssignmentTarget == CharacterAssignmentTarget.Player

                            // Front Sprite
                            SpritePicker(
                                label = stringResource(R.string.front_sprite_label),
                                isRequired = frontRequired,
                                description = stringResource(R.string.front_sprite_description),
                                image = frontImageUri ?: existingFrontImageFile,
                                onPick = { frontPicker.launch("image/*") },
                                onClear = viewModel::clearFrontImage,
                                modifier = Modifier.weight(1f),
                                enabled = !isLimitReached
                            )

                            // Back Sprite
                            SpritePicker(
                                label = stringResource(R.string.back_sprite_label),
                                isRequired = backRequired,
                                description = stringResource(R.string.back_sprite_description),
                                image = backImageUri ?: existingBackImageFile,
                                onPick = { backPicker.launch("image/*") },
                                onClear = viewModel::clearBackImage,
                                modifier = Modifier.weight(1f),
                                enabled = !isLimitReached
                            )
                        }

                        if (isRadiant) {
                            Text(
                                text = stringResource(R.string.radiant_sprites_section),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                SpritePicker(
                                    label = stringResource(R.string.radiant_front_sprite_label),
                                    isRequired = frontImageUri != null || existingFrontImageFile != null,
                                    description = stringResource(R.string.front_sprite_description),
                                    image = radiantFrontImageUri ?: existingRadiantFrontImageFile,
                                    onPick = { radiantFrontPicker.launch("image/*") },
                                    onClear = viewModel::clearRadiantFrontImage,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLimitReached,
                                )
                                SpritePicker(
                                    label = stringResource(R.string.radiant_back_sprite_label),
                                    isRequired = backImageUri != null || existingBackImageFile != null,
                                    description = stringResource(R.string.back_sprite_description),
                                    image = radiantBackImageUri ?: existingRadiantBackImageFile,
                                    onPick = { radiantBackPicker.launch("image/*") },
                                    onClear = viewModel::clearRadiantBackImage,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLimitReached,
                                )
                            }
                        }

                        // Divider between sprites and name
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )

                        // Name Input
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.character_name_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.required_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            OutlinedTextField(
                                value = name,
                                onValueChange = viewModel::onNameChanged,
                                placeholder = { Text(stringResource(R.string.character_name_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isLimitReached,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                            )
                        }

                        // Advanced Section (Only for monsters for now)
                        if (type == CharacterType.Monster) {
                            AdvancedSection(
                                type = type,
                                isRadiant = isRadiant,
                                onRadiantChange = viewModel::onRadiantChanged,
                                level = level,
                                onLevelChange = viewModel::onLevelChanged,
                                maxHp = maxHp,
                                onMaxHpChange = viewModel::onMaxHpChanged,
                                enabled = !isLimitReached
                            )
                        }
                    }
                }

                val hasFront = frontImageUri != null || existingFrontImageFile != null
                val hasBack = backImageUri != null || existingBackImageFile != null
                val frontRequiredByAssignment = isAssignedToContact
                val backRequiredByAssignment = isAssignedToPlayer
                val frontRequired = frontRequiredByAssignment || 
                    preferredAssignmentTarget == CharacterAssignmentTarget.Contact
                val backRequired = backRequiredByAssignment || 
                    preferredAssignmentTarget == CharacterAssignmentTarget.Player

                if ((frontRequiredByAssignment && !hasFront) || (backRequiredByAssignment && !hasBack)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (frontRequiredByAssignment && !hasFront) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AppIcon(
                                        icon = LocalAppIcons.current.block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.front_sprite_required_for_contact),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            if (backRequiredByAssignment && !hasBack) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AppIcon(
                                        icon = LocalAppIcons.current.block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.back_sprite_required_for_player),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLimitReached) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIcon(
                                icon = LocalAppIcons.current.block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.character_limit_reached_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Button(
                    onClick = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = run {
                        val hasRadiantFront = radiantFrontImageUri != null || existingRadiantFrontImageFile != null
                        val hasRadiantBack = radiantBackImageUri != null || existingRadiantBackImageFile != null
                        val hasCompleteRadiantVariant = !isRadiant ||
                            (!hasFront || hasRadiantFront) && (!hasBack || hasRadiantBack)
                        val isValid = (!frontRequired || hasFront) && 
                            (!backRequired || hasBack) &&
                            (hasFront || hasBack) &&
                            hasCompleteRadiantVariant

                        name.isNotBlank() && isValid && !isSaving && !isLimitReached
                    }
                ) {
                    Text(
                        stringResource(R.string.save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Extra spacing at the bottom to prevent cutoff by navigation bars
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun AdvancedSection(
    type: CharacterType,
    isRadiant: Boolean,
    onRadiantChange: (Boolean) -> Unit,
    level: String,
    onLevelChange: (String) -> Unit,
    maxHp: String,
    onMaxHpChange: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.advanced_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            AppIcon(
                icon = if (expanded) LocalAppIcons.current.arrowUp else LocalAppIcons.current.arrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Radiant Toggle (Only for monsters)
                if (type == CharacterType.Monster) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = enabled) { onRadiantChange(!isRadiant) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppIcon(
                            icon = LocalMonsterAppIcons.current.radiant,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isRadiant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.radiant_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.radiant_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRadiant,
                            onCheckedChange = onRadiantChange,
                            enabled = enabled
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Level Input
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.level_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = level,
                            onValueChange = onLevelChange,
                            placeholder = { Text("1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Max HP Input
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.max_hp_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = maxHp,
                            onValueChange = onMaxHpChange,
                            placeholder = { Text("100") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpritePicker(
    label: String,
    isRequired: Boolean,
    description: String,
    image: Any?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isRequired) {
                Text(
                    text = stringResource(R.string.required_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(enabled = enabled) { onPick() },
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Clear button
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clickable(enabled = enabled) { onClear() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppIcon(
                            icon = LocalAppIcons.current.close,
                            contentDescription = stringResource(R.string.remove),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.change),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIcon(
                        icon = LocalMonsterAppIcons.current.addCharacter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.select),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
            lineHeight = 14.sp
        )
    }
}
