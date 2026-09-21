package dev.alenajam.monsterdialer.packs.data

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Downloads a catalog entry into private temporary storage before using the normal pack importer. */
@Singleton
class CatalogPackInstaller @Inject constructor(
    private val app: Application,
    private val client: RemotePackCatalogClient,
    private val packsRepository: PacksRepository,
) {
    suspend fun install(pack: RemotePackCatalogPack): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val archive = File.createTempFile("catalog-pack-", ".monsterpack", app.cacheDir)
            try {
                client.download(pack, archive)
                packsRepository.importPack(archive).getOrThrow()
            } finally {
                archive.delete()
            }
        }
    }
}
