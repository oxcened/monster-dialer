package dev.alenajam.monsterdialer.ui.battle

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alenajam.monsterdialer.R
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BattleScreen(
    encounter: BattleEncounter,
    modifier: Modifier = Modifier,
    timing: BattleTiming = BattleTiming()
) {
    val scope = rememberCoroutineScope()
    val coordinator = remember(scope, timing) { BattleSequenceCoordinator(scope, timing) }
    val state by coordinator.state.collectAsState()

    LaunchedEffect(encounter.id, encounter.type) { coordinator.start(encounter) }
    DisposableEffect(coordinator) { onDispose(coordinator::stop) }

    BattleScene(
        state = state,
        timing = timing,
        onAnimationCompleted = coordinator::animationCompleted,
        modifier = modifier
    )
}

@Composable
fun BattleScene(
    state: BattleUiState,
    modifier: Modifier = Modifier,
    timing: BattleTiming = BattleTiming(),
    onAnimationCompleted: (Long, BattlePhase) -> Unit = { _, _ -> }
) {
    val encounter = state.encounter ?: return
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val entranceDistancePx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val playerOffset = remember(state.runId, entranceDistancePx) { Animatable(entranceDistancePx) }
    val enemyOffset = remember(state.runId, entranceDistancePx) { Animatable(-entranceDistancePx) }
    val saturation = remember(state.runId) { Animatable(0f) }

    LaunchedEffect(state.runId, state.phase) {
        when (state.phase) {
            BattlePhase.TrainersEntering -> {
                coroutineScope {
                    val player = async {
                        playerOffset.snapTo(entranceDistancePx)
                        playerOffset.animateTo(0f, tween(timing.trainerEnterMillis, easing = LinearEasing))
                    }
                    val enemy = async {
                        enemyOffset.snapTo(-entranceDistancePx)
                        enemyOffset.animateTo(0f, tween(timing.trainerEnterMillis, easing = LinearEasing))
                    }
                    player.await()
                    enemy.await()
                }
                onAnimationCompleted(state.runId, state.phase)
            }
            BattlePhase.TrainersColorizing -> {
                saturation.animateTo(1f, tween(timing.colorizeMillis, easing = LinearEasing))
                onAnimationCompleted(state.runId, state.phase)
            }
            BattlePhase.EnemyTrainerLeaving -> {
                enemyOffset.animateTo(entranceDistancePx, tween(timing.trainerExitMillis, easing = LinearEasing))
                onAnimationCompleted(state.runId, state.phase)
            }
            BattlePhase.PlayerTrainerLeaving -> {
                playerOffset.animateTo(-entranceDistancePx, tween(timing.trainerExitMillis, easing = LinearEasing))
                onAnimationCompleted(state.runId, state.phase)
            }
            BattlePhase.EnemyRevealing -> enemyOffset.snapTo(0f)
            BattlePhase.PlayerRevealing -> playerOffset.snapTo(0f)
            else -> Unit
        }
    }

    val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation.value) })
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .semantics { contentDescription = "Pokémon battle scene" }
    ) {
        BattlePanelView(
            pokemon = encounter.enemy,
            panel = state.enemyPanel,
            isEnemy = true,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 28.dp, start = 12.dp)
        )
        BattleSprite(
            sprite = enemySprite(state),
            description = enemyDescription(state),
            colorFilter = if (state.phase <= BattlePhase.TrainersColorizing) grayscale else null,
            sparkles = state.showEnemySparkles,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
                .offset { IntOffset(enemyOffset.value.roundToInt(), 0) }
        )
        BattleSprite(
            sprite = playerSprite(state),
            description = "Your ${encounter.player.name}",
            colorFilter = if (state.phase <= BattlePhase.TrainersColorizing) grayscale else null,
            sparkles = state.showPlayerSparkles,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp, top = 28.dp)
                .offset { IntOffset(playerOffset.value.roundToInt(), 0) }
        )
        BattlePanelView(
            pokemon = encounter.player,
            panel = state.playerPanel,
            isEnemy = false,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 52.dp)
        )
        BattleDialogue(
            message = state.message,
            isTyping = state.isTyping,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 124.dp)
        )
    }
}

@Composable
private fun BattleSprite(
    sprite: BattleVisualAsset,
    description: String,
    colorFilter: ColorFilter?,
    sparkles: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Image(
            bitmap = pixelBitmapResource(sprite),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
        if (sparkles) Sparkles(Modifier.fillMaxSize())
    }
}

@Composable
private fun Sparkles(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(48) {
            frame = it
            delay(10)
        }
    }
    val resource = remember(frame) {
        context.resources.getIdentifier("battle_shiny_sparkles_$frame", "drawable", context.packageName)
    }
    if (resource != 0) {
        Image(
            bitmap = pixelBitmapResource(
                BattleVisualAsset.AppDrawable(resource, "battle_shiny_sparkles_$frame")
            ),
            contentDescription = "Shiny sparkle",
            filterQuality = FilterQuality.None,
            modifier = modifier
        )
    }
}

@Composable
internal fun BattlePanelView(
    pokemon: BattlePokemon?,
    panel: BattlePanel,
    isEnemy: Boolean,
    modifier: Modifier = Modifier
) {
    if (panel == BattlePanel.Hidden || pokemon == null) return
    if (panel == BattlePanel.Pokeballs) {
        val resourceName = if (isEnemy) "battle_enemy_pokeballs" else "battle_player_pokeballs"
        Image(
            bitmap = pixelBitmapResource(
                BattleVisualAsset.AppDrawable(
                    if (isEnemy) R.drawable.battle_enemy_pokeballs else R.drawable.battle_player_pokeballs,
                    resourceName
                )
            ),
            contentDescription = "Available Poké Balls",
            contentScale = ContentScale.FillWidth,
            filterQuality = FilterQuality.None,
            modifier = modifier.width(152.dp)
        )
        return
    }

    val image = if (isEnemy) R.drawable.battle_enemy_life_bar else R.drawable.battle_player_life_bar
    val imageName = if (isEnemy) "battle_enemy_life_bar" else "battle_player_life_bar"
    val font = battleFontFamily()
    Box(modifier.width(160.dp).height(if (isEnemy) 62.dp else 79.dp)) {
        Image(
            bitmap = pixelBitmapResource(BattleVisualAsset.AppDrawable(image, imageName)),
            contentDescription = "${pokemon.name} status",
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(160.dp)
                .height(if (isEnemy) 62.dp else 79.dp)
        )
        androidx.compose.material3.Text(
            text = pokemon.name.uppercase(),
            style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
            modifier = Modifier.align(if (isEnemy) Alignment.TopStart else Alignment.TopEnd)
        )
        androidx.compose.material3.Text(
            text = pokemon.level.toString(),
            style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
            modifier = Modifier.offset(x = 105.dp, y = 14.dp)
        )
        if (!isEnemy) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .offset(x = 43.dp, y = 45.dp)
                    .width(85.dp)
            ) {
                androidx.compose.material3.Text(
                    text = pokemon.hp.toString(),
                    style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
                )
                androidx.compose.material3.Text(
                    text = pokemon.maxHp.toString(),
                    style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
                )
            }
        }
    }
}

@Composable
private fun BattleDialogue(message: String, isTyping: Boolean, modifier: Modifier = Modifier) {
    val font = battleFontFamily()
    Box(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .height(104.dp)
            .background(Color.Black)
            .padding(2.dp)
            .background(Color.White)
            .padding(3.dp)
            .background(Color.Black)
            .padding(4.dp)
            .background(Color.White)
            .semantics { if (!isTyping) liveRegion = LiveRegionMode.Polite }
    ) {
        androidx.compose.material3.Text(
            text = message,
            style = TextStyle(fontFamily = font, fontSize = 18.sp, lineHeight = 21.sp, color = Color.Black),
            modifier = Modifier.padding(5.dp)
        )
    }
}

private fun enemySprite(state: BattleUiState): BattleVisualAsset {
    val encounter = requireNotNull(state.encounter)
    if (encounter.type != EncounterType.Trainer) {
        val enemy = requireNotNull(encounter.enemy)
        return enemy.frontSprite
    }
    return when {
        state.enemyRevealFrame == 1 -> BattleVisualAsset.AppDrawable(R.drawable.battle_reveal_1, "battle_reveal_1")
        state.enemyRevealFrame == 2 -> BattleVisualAsset.AppDrawable(R.drawable.battle_reveal_2, "battle_reveal_2")
        state.enemyRevealFrame == 3 -> BattleVisualAsset.AppDrawable(R.drawable.battle_reveal_3, "battle_reveal_3")
        state.enemyRevealFrame >= 4 -> requireNotNull(encounter.enemy).frontSprite
        else -> encounter.enemyTrainerSprite
    }
}

private fun playerSprite(state: BattleUiState): BattleVisualAsset {
    val encounter = requireNotNull(state.encounter)
    return when (state.playerRevealFrame) {
        1 -> BattleVisualAsset.AppDrawable(R.drawable.battle_reveal_1, "battle_reveal_1")
        2 -> BattleVisualAsset.AppDrawable(R.drawable.battle_reveal_2, "battle_reveal_2")
        3 -> BattleVisualAsset.AppDrawable(R.drawable.battle_reveal_3, "battle_reveal_3")
        4 -> encounter.player.backSprite
        else -> encounter.playerTrainerSprite
    }
}

private fun enemyDescription(state: BattleUiState): String {
    val encounter = requireNotNull(state.encounter)
    return if (state.enemyRevealFrame > 0 || encounter.type != EncounterType.Trainer) {
        "Opponent ${encounter.enemy?.name.orEmpty()}"
    } else {
        "Opponent trainer ${encounter.enemyTrainerName.orEmpty()}"
    }
}

@Composable
private fun pixelBitmapResource(asset: BattleVisualAsset): ImageBitmap {
    val context = LocalContext.current
    return remember(context.resources, asset) {
        val bitmap = when (asset) {
            is BattleVisualAsset.AppDrawable -> {
                val resolvedResource = asset.resource.takeIf { it != 0 } ?: asset.fallbackName?.let { name ->
                    context.resources.getIdentifier(name, "drawable", context.packageName)
                } ?: 0
                BitmapFactory.decodeResource(context.resources, resolvedResource)
            }
            is BattleVisualAsset.LocalFile -> BitmapFactory.decodeFile(asset.path)
        }
        requireNotNull(bitmap) {
            "Unable to decode battle asset $asset"
        }.asImageBitmap()
    }
}

@Composable
private fun battleFontFamily(): FontFamily = if (LocalInspectionMode.current) {
    // Layoutlib can replace generated font IDs with 0 while rendering previews.
    FontFamily.Monospace
} else {
    FontFamily(Font(R.font.johto_mono))
}
