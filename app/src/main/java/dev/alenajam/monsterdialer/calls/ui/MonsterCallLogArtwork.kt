package dev.alenajam.monsterdialer.calls.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import java.io.File

data class MonsterCallLogArtwork(
    val file: File? = null,
    val builtInResource: Int? = null,
) {
    companion object {
        fun plumguard() = MonsterCallLogArtwork(
            builtInResource = BuiltInCharacters.monster.character.contactArtwork.resource,
        )

        fun anonymous() = MonsterCallLogArtwork(
            builtInResource = BuiltInCharacters.anonymousMonster.enemyArtwork.resource,
        )
    }
}

@Composable
internal fun MonsterCallLogAvatar(artwork: MonsterCallLogArtwork) {
    Box(
        modifier = Modifier.size(42.dp),
        contentAlignment = Alignment.Center,
    ) {
        artwork.builtInResource?.let { resource ->
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        artwork.file?.let { file ->
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
