package dev.alenajam.monsterdialer.battle.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.battle.data.BattleEncounter
import dev.alenajam.monsterdialer.battle.data.BattleMonster
import dev.alenajam.monsterdialer.battle.data.BattlePanel
import dev.alenajam.monsterdialer.battle.data.BattleTiming
import dev.alenajam.monsterdialer.battle.data.BattleSequenceCoordinator
import dev.alenajam.monsterdialer.battle.data.BattlePhase
import dev.alenajam.monsterdialer.battle.data.BattleUiState
import dev.alenajam.monsterdialer.battle.data.BattleVisualAsset
import dev.alenajam.monsterdialer.battle.data.EncounterType
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
    val resources = LocalResources.current
    val coordinator = remember(scope, timing, resources) {
        BattleSequenceCoordinator(scope, timing, string = { resource, arguments ->
            resources.getString(resource, *arguments)
        })
    }
    val state by coordinator.state.collectAsState()

    LaunchedEffect(encounter.id, encounter.type) { coordinator.start(encounter) }
    DisposableEffect(coordinator) { onDispose(coordinator::stop) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(36.dp),
        tonalElevation = 8.dp
    ) {
        BattleScene(
            state = state,
            timing = timing,
            onAnimationCompleted = coordinator::animationCompleted,
            onDialogueCompleted = coordinator::dialogueCompleted,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(30.dp))
        )
    }
}

@Composable
fun BattleScene(
    state: BattleUiState,
    modifier: Modifier = Modifier,
    timing: BattleTiming = BattleTiming(),
    onAnimationCompleted: (Long, BattlePhase) -> Unit = { _, _ -> },
    onDialogueCompleted: (Long, Long) -> Unit = { _, _ -> }
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
    val sceneDescription = stringResource(R.string.battle_scene_description)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF2))
            .clipToBounds()
            .semantics { contentDescription = sceneDescription }
    ) {
        val availableScale = minOf(1f, maxWidth / 360.dp, maxHeight / 320.dp)
        val scale = if (availableScale < 1f) availableScale * 0.95f else 1f
        val dialogueHeight = 104.dp
        BattlePanelView(
            monster = encounter.enemy,
            panel = state.enemyPanel,
            isEnemy = true,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp, start = 12.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        )
        BattleSprite(
            sprite = enemySprite(state),
            description = enemyDescription(state),
            colorFilter = if (state.phase <= BattlePhase.TrainersColorizing) grayscale else null,
            radiant = state.showEnemyRadiance,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
                .offset { IntOffset(enemyOffset.value.roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(1f, 0f)
                }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp * scale),
            ) {
                BattleSprite(
                    sprite = playerSprite(state),
                    description = stringResource(R.string.your_monster, encounter.player.name),
                    colorFilter = if (state.phase <= BattlePhase.TrainersColorizing) grayscale else null,
                    radiant = state.showPlayerRadiance,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 28.dp)
                        .offset { IntOffset(playerOffset.value.roundToInt(), 0) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 1f)
                        }
                )
                BattlePanelView(
                    monster = encounter.player,
                    panel = state.playerPanel,
                    isEnemy = false,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(1f, 1f)
                        }
                )
            }
            BattleDialogue(
                message = state.message,
                dialogueId = state.dialogueId,
                isTyping = state.isTyping,
                timing = timing,
                onCompleted = { onDialogueCompleted(state.runId, state.dialogueId) },
                height = dialogueHeight * scale,
                textScale = scale,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun BattleSprite(
    sprite: BattleVisualAsset,
    description: String,
    colorFilter: ColorFilter?,
    radiant: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(116.dp), contentAlignment = Alignment.Center) {
        when (sprite) {
            is BattleVisualAsset.VectorDrawable -> Image(
                painter = painterResource(sprite.resource),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
            else -> Image(
                bitmap = pixelBitmapResource(sprite),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (radiant) Radiance(Modifier.fillMaxSize())
    }
}

@Composable
private fun Radiance(modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    val description = stringResource(R.string.radiant_sparkle)
    LaunchedEffect(Unit) {
        repeat(48) {
            frame = it
            delay(10)
        }
    }
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val unit = size.minDimension / 58f
        val center = Offset(size.width / 2f, size.height / 2f)
        val colors = listOf(Color(0xFFFFF03D), Color(0xFFFFC928), Color(0xFFFFF9B0))
        repeat(6) { index ->
            val progress = ((frame + index * 8) % 48) / 47f
            val angle = index * 1.0472f + progress * 1.8f
            val distance = unit * (4f + progress * 24f)
            val sparkleCenter = Offset(
                x = center.x + kotlin.math.cos(angle) * distance,
                y = center.y + kotlin.math.sin(angle) * distance
            )
            val alpha = if (progress < 0.18f) progress / 0.18f else (1f - progress) / 0.82f
            val sparkleUnit = unit * (0.65f + (1f - progress) * 1.15f)
            drawCrystalSparkle(sparkleCenter, sparkleUnit, colors[index % colors.size].copy(alpha = alpha))
        }
    }
}

private fun DrawScope.drawCrystalSparkle(center: Offset, unit: Float, color: Color) {
    drawRect(color, topLeft = Offset(center.x - unit, center.y - unit * 3f), size = androidx.compose.ui.geometry.Size(unit * 2f, unit * 6f))
    drawRect(color, topLeft = Offset(center.x - unit * 3f, center.y - unit), size = androidx.compose.ui.geometry.Size(unit * 6f, unit * 2f))
    drawRect(color.copy(alpha = color.alpha * 0.7f), topLeft = Offset(center.x - unit * 2f, center.y - unit * 2f), size = androidx.compose.ui.geometry.Size(unit * 4f, unit * 4f))
}

@Composable
internal fun BattlePanelView(
    monster: BattleMonster?,
    panel: BattlePanel,
    isEnemy: Boolean,
    modifier: Modifier = Modifier
) {
    if (panel == BattlePanel.Hidden || monster == null) return
    if (panel == BattlePanel.Roster) {
        val resourceName = if (isEnemy) "battle_enemy_roster" else "battle_player_roster"
        Image(
            bitmap = pixelBitmapResource(
                BattleVisualAsset.AppDrawable(
                    if (isEnemy) R.drawable.battle_enemy_roster else R.drawable.battle_player_roster,
                    resourceName
                )
            ),
            contentDescription = stringResource(R.string.available_monsters),
            contentScale = ContentScale.FillWidth,
            filterQuality = FilterQuality.None,
            modifier = modifier.width(152.dp)
        )
        return
    }

    val image = if (isEnemy) R.drawable.battle_enemy_life_bar else R.drawable.battle_player_life_bar
    val imageName = if (isEnemy) "battle_enemy_life_bar" else "battle_player_life_bar"
    val font = battleFontFamily()
    Box(modifier.width(160.dp).height(if (isEnemy) 72.dp else 89.dp)) {
        Image(
            bitmap = pixelBitmapResource(BattleVisualAsset.AppDrawable(image, imageName)),
            contentDescription = stringResource(R.string.monster_status, monster.name),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(160.dp)
                .height(if (isEnemy) 62.dp else 79.dp)
        )
        androidx.compose.material3.Text(
            text = monster.name.uppercase(),
            style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
            modifier = Modifier.align(if (isEnemy) Alignment.TopStart else Alignment.TopEnd)
        )
        androidx.compose.material3.Text(
            text = monster.level.toString(),
            style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
            modifier = Modifier.offset(x = if (isEnemy) 105.dp else 100.dp, y = if (isEnemy) 18.dp else 16.dp)
        )
        if (!isEnemy) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .offset(x = 31.dp, y = 50.dp)
                    .width(97.dp)
            ) {
                androidx.compose.material3.Text(
                    text = monster.hp.toString(),
                    style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
                )
                androidx.compose.material3.Text(
                    text = monster.maxHp.toString(),
                    style = TextStyle(fontFamily = font, fontSize = 16.sp, color = Color.Black),
                )
            }
        }
    }
}

@Composable
private fun BattleDialogue(
    message: String,
    dialogueId: Long,
    isTyping: Boolean,
    timing: BattleTiming,
    onCompleted: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 104.dp,
    textScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val font = battleFontFamily()
    val style = TextStyle(
        fontFamily = font,
        fontSize = 18.sp * textScale,
        lineHeight = 21.sp * textScale,
        color = Color.Black,
    )
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth(0.95f)
            .height(height)
            .background(Color.Black)
            .padding(2.dp)
            .background(Color.White)
            .padding(3.dp)
            .background(Color.Black)
            .padding(4.dp)
            .background(Color.White)
            .semantics { if (!isTyping) liveRegion = LiveRegionMode.Polite }
    ) {
        val textWidth = with(LocalDensity.current) { (maxWidth - 10.dp).roundToPx() }
        val pages = remember(message, textWidth) {
            measuredDialoguePages(message, textMeasurer, style, textWidth)
        }
        var displayedMessage by remember(dialogueId) { mutableStateOf("") }
        LaunchedEffect(dialogueId, pages) {
            if (message.isBlank()) {
                displayedMessage = ""
                return@LaunchedEffect
            }
            pages.forEachIndexed { pageIndex, page ->
                page.indices.forEach { index ->
                    displayedMessage = page.take(index + 1)
                    delay(timing.characterMillis)
                }
                if (pageIndex < pages.lastIndex) delay(timing.dialoguePageHoldMillis)
            }
            onCompleted()
        }
        androidx.compose.material3.Text(
            text = displayedMessage,
            style = style,
            maxLines = 3,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(5.dp)
        )
    }
}

private fun measuredDialoguePages(
    text: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle,
    maxWidth: Int
): List<String> {
    if (text.isBlank()) return emptyList()
    fun fits(page: String) = !textMeasurer.measure(
        text = AnnotatedString(page),
        style = style,
        overflow = TextOverflow.Clip,
        maxLines = 3,
        constraints = Constraints(maxWidth = maxWidth)
    ).hasVisualOverflow

    val pages = mutableListOf<String>()
    var remaining = text.trim()
    while (remaining.isNotEmpty()) {
        if (fits(remaining)) {
            pages += remaining
            break
        }
        var end = remaining.length
        while (end > 1 && !fits(remaining.substring(0, end))) end--
        val wordBoundary = remaining.lastIndexOfAny(charArrayOf(' ', '\n', '\t'), end - 1)
        val pageEnd = if (wordBoundary > 0) wordBoundary else end
        pages += remaining.substring(0, pageEnd).trimEnd()
        remaining = remaining.substring(if (wordBoundary > 0) wordBoundary + 1 else pageEnd).trimStart()
    }
    return pages
}

private fun enemySprite(state: BattleUiState): BattleVisualAsset {
    val encounter = requireNotNull(state.encounter)
    if (encounter.type != EncounterType.Trainer) {
        return encounter.enemy?.frontSprite ?: encounter.enemyTrainerSprite
    }
    return when {
        state.enemyRevealFrame == 1 -> BattleVisualAsset.VectorDrawable(R.drawable.battle_reveal_rift_1)
        state.enemyRevealFrame == 2 -> BattleVisualAsset.VectorDrawable(R.drawable.battle_reveal_rift_2)
        state.enemyRevealFrame == 3 -> BattleVisualAsset.VectorDrawable(R.drawable.battle_reveal_rift_3)
        state.enemyRevealFrame >= 4 -> encounter.enemy?.frontSprite ?: encounter.enemyTrainerSprite
        else -> encounter.enemyTrainerSprite
    }
}

private fun playerSprite(state: BattleUiState): BattleVisualAsset {
    val encounter = requireNotNull(state.encounter)
    return when (state.playerRevealFrame) {
        1 -> BattleVisualAsset.VectorDrawable(R.drawable.battle_reveal_rift_1)
        2 -> BattleVisualAsset.VectorDrawable(R.drawable.battle_reveal_rift_2)
        3 -> BattleVisualAsset.VectorDrawable(R.drawable.battle_reveal_rift_3)
        4 -> encounter.player.backSprite ?: encounter.playerTrainerSprite
        else -> encounter.playerTrainerSprite
    }
}

@Composable
private fun enemyDescription(state: BattleUiState): String {
    val encounter = requireNotNull(state.encounter)
    return if (state.enemyRevealFrame > 0 || encounter.type != EncounterType.Trainer) {
        stringResource(R.string.opponent_monster, encounter.enemy?.name.orEmpty())
    } else {
        stringResource(R.string.opponent_trainer, encounter.enemyTrainerName.orEmpty())
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
            is BattleVisualAsset.VectorDrawable -> error("Vector drawables must be rendered with painterResource.")
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
    FontFamily(Font(R.font.ui_pixel_font))
}
