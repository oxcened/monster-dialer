package dev.alenajam.monsterdialer.packs.data

import android.app.Application
import android.net.Uri
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import dev.alenajam.monsterdialer.packs.data.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.data.CharacterPackInstaller
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Singleton
class PacksRepositoryImpl @Inject constructor(
    private val app: Application,
    private val catalog: CharacterPackCatalog,
    @CharacterPacksDir private val storageRoot: File,
    private val assignmentRepository: CharacterAssignmentRepository
) : PacksRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val installer = CharacterPackInstaller(storageRoot, catalog = catalog)
    private val _packs = MutableStateFlow<List<MonsterPack>>(emptyList())
    
    init {
        scope.launch(Dispatchers.IO) {
            installBundledPacksIfNeeded()
            refreshPacks()
        }
        scope.launch {
            catalog.packs.collectLatest {
                refreshPacks()
            }
        }
    }

    private suspend fun installBundledPacksIfNeeded() {
        val bundledManifest = readBundledPackManifest()
        val installedRecord = catalog.list().firstOrNull { it.id == bundledManifest.id }
        val activeManifest = File(storageRoot, "${bundledManifest.id}/active/${CharacterPackValidator.ManifestPath}")
        if (installedRecord?.version == bundledManifest.version && activeManifest.isFile) return

        app.assets.open(BundledOddBunchAssetPath).use { installAndClearAssignments(it) }
    }

    private fun readBundledPackManifest(): CharacterPackManifest {
        val archive = File.createTempFile("bundled-pack-", ".monsterpack", app.cacheDir)
        return try {
            app.assets.open(BundledOddBunchAssetPath).use { input ->
                archive.outputStream().use { output -> input.copyTo(output) }
            }
            CharacterPackArchiveReader().read(archive).manifest
        } finally {
            archive.delete()
        }
    }

    private fun refreshPacks() {
        _packs.value = catalog.list().map { record ->
            MonsterPack(
                id = record.id,
                name = record.name,
                version = record.version,
                creator = record.creator,
                license = record.license,
                enabled = record.enabled,
                characterCount = record.characterCount
            )
        }
    }

    override fun getPacks(): Flow<List<MonsterPack>> = _packs.asStateFlow()

    override suspend fun importPack(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            file.inputStream().use { installAndClearAssignments(it) }
            refreshPacks()
        }
    }

    suspend fun importPackFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            app.contentResolver.openInputStream(uri)?.use { installAndClearAssignments(it) }
                ?: throw Exception("Unable to open stream")
            refreshPacks()
        }
    }

    suspend fun previewPackFromUri(uri: Uri): CharacterPackPreview = withContext(Dispatchers.IO) {
        val archive = File.createTempFile("pack-preview-", ".zip", app.cacheDir)
        try {
            app.contentResolver.openInputStream(uri)?.use { input ->
                archive.outputStream().use { output -> input.copyWithLimit(output) }
            } ?: throw Exception("Unable to open stream")
            val pack = CharacterPackArchiveReader().read(archive)
            val previewImagePath = pack.manifest.characters
                .asSequence()
                .flatMap { character -> character.visualVariants.asSequence() }
                .mapNotNull { variant -> variant.frontImage ?: variant.backImage }
                .firstOrNull()
            val previewImage = previewImagePath?.let { path ->
                ZipFile(archive).use { zip ->
                    zip.getInputStream(requireNotNull(zip.getEntry(path))).use { it.readBytes() }
                }
            }
            CharacterPackPreview(pack.manifest, previewImage)
        } finally {
            archive.delete()
        }
    }

    override suspend fun togglePack(packId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        if (!enabled) {
            assignmentRepository.clearAssignmentsForPack(packId)
        }
        catalog.setEnabled(packId, enabled)
        refreshPacks()
    }

    override suspend fun deletePack(packId: String) = withContext(Dispatchers.IO) {
        assignmentRepository.clearAssignmentsForPack(packId)
        catalog.remove(packId)
        File(storageRoot, packId).deleteRecursively()
        refreshPacks()
    }

    private fun java.io.InputStream.copyWithLimit(output: java.io.OutputStream) {
        val buffer = ByteArray(BufferSize)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > MaxArchiveBytes) throw CharacterPackValidationException("Pack archive is too large")
            output.write(buffer, 0, read)
        }
    }

    private fun CharacterPackManifest.references(): Set<CharacterReference> = characters.flatMapTo(mutableSetOf()) { character ->
        character.visualVariants.map { variant -> CharacterReference(id, character.id, variant.id) }
    }

    private suspend fun installAndClearAssignments(source: InputStream): InstalledCharacterPack {
        val installed = installer.install(source)
        assignmentRepository.clearAssignmentsMissingFromPack(
            installed.manifest.id,
            installed.manifest.references(),
        )
        return installed
    }

    private companion object {
        const val BundledOddBunchAssetPath = "builtin-packs/the-odd-bunch.monsterpack"
        const val BufferSize = 8 * 1024
        const val MaxArchiveBytes = 24L * 1024 * 1024
    }
}
