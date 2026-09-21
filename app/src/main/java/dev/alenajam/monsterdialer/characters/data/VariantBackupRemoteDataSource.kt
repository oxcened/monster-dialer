package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference

/** Remote boundary for private character-variant backups. */
interface VariantBackupRemoteDataSource {
    suspend fun restore(): Set<CharacterReference>
    suspend fun backup(references: Set<CharacterReference>)
    fun isSignedIn(): Boolean
}
