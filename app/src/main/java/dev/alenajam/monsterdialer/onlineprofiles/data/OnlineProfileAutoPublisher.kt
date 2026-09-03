package dev.alenajam.monsterdialer.onlineprofiles.data

import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Keeps an enabled Online Profile aligned with changes to the active player characters. */
@Singleton
@OptIn(FlowPreview::class)
class OnlineProfileAutoPublisher @Inject constructor(
    private val assignments: CharacterAssignmentRepository,
    private val characters: CharactersRepository,
    private val publisher: OnlineProfilePublisher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        combine(
            assignments.assignmentVersion,
            characters.observeCharactersAssignableTo(CharacterAssignmentTarget.Player, CharacterType.Trainer),
            characters.observeCharactersAssignableTo(CharacterAssignmentTarget.Player, CharacterType.Monster),
        ) { _, _, _ -> Unit }
            .debounce(DebounceMillis)
            .onEach {
                runCatching {
                    publisher.publishIfChanged()?.let {
                        publisher.confirmRetention()
                    }
                }
            }
            .launchIn(scope)
    }

    private companion object {
        const val DebounceMillis = 1_000L
    }
}
