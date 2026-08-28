package dev.alenajam.monsterdialer.data.characters

import kotlinx.coroutines.flow.Flow
import java.io.File

data class MonsterPack(
    val id: String,
    val name: String,
    val version: String,
    val creator: String?,
    val enabled: Boolean
)

interface PacksRepository {
    fun getPacks(): Flow<List<MonsterPack>>
    suspend fun importPack(file: File): Result<Unit>
    suspend fun togglePack(packId: String, enabled: Boolean)
    suspend fun deletePack(packId: String)
}
