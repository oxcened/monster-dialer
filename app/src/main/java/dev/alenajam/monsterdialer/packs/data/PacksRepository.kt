package dev.alenajam.monsterdialer.packs.data

import kotlinx.coroutines.flow.Flow
import java.io.File

interface PacksRepository {
    fun getPacks(): Flow<List<MonsterPack>>
    suspend fun importPack(file: File): Result<Unit>
    suspend fun togglePack(packId: String, enabled: Boolean)
    suspend fun deletePack(packId: String)
}
