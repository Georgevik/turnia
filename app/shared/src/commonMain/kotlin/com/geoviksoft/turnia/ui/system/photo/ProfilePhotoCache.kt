package com.geoviksoft.turnia.ui.system.photo

import coil3.disk.DiskCache
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import okio.FileSystem
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import okio.Path

/**
 * Every profile photo this device has seen, on disk, keyed by [ProfilePhoto.key]. A photo's file
 * never changes — a new photo is a new id — so an entry never goes stale: the only download is the
 * first one, and the user's own new photo costs none at all, since [prime] stores it as it goes up.
 */
@OptIn(ExperimentalAtomicApi::class)
class ProfilePhotoCache(
    directory: Path,
    private val download: suspend (ProfilePhoto) -> ByteArray?,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val disk = DiskCache.Builder()
        .fileSystem(fileSystem)
        .directory(directory)
        .maxSizeBytes(MAX_BYTES)
        .build()

    // Photos that could not be downloaded this session — a cached profile can name one since
    // replaced and deleted — so a row drawn again does not ask Storage again. Coil fetches on
    // several threads at once, hence the atomic swap of an immutable set.
    private val unavailable = AtomicReference(emptySet<String>())

    class Loaded(val bytes: ByteArray, val downloaded: Boolean)

    /** The photo's bytes, from disk if this device has them; null when it cannot be had. */
    suspend fun load(photo: ProfilePhoto): Loaded? {
        read(photo)?.let { return Loaded(it, downloaded = false) }
        if (photo.key in unavailable.load()) return null

        val bytes = download(photo)
        if (bytes == null) {
            markUnavailable(photo)
            return null
        }
        write(photo, bytes)
        return Loaded(bytes, downloaded = true)
    }

    fun prime(photo: ProfilePhoto, bytes: ByteArray) = write(photo, bytes)

    private fun markUnavailable(photo: ProfilePhoto) {
        while (true) {
            val current = unavailable.load()
            if (unavailable.compareAndSet(current, current + photo.key)) return
        }
    }

    private fun read(photo: ProfilePhoto): ByteArray? =
        disk.openSnapshot(photo.key)?.use { snapshot -> fileSystem.read(snapshot.data) { readByteArray() } }

    private fun write(photo: ProfilePhoto, bytes: ByteArray) {
        val editor = disk.openEditor(photo.key) ?: return
        try {
            fileSystem.write(editor.data) { write(bytes) }
            editor.commit()
        } catch (failure: Exception) {
            editor.abort()
        }
    }

    private companion object {
        // About a thousand photos at 30 KB: far more people than anyone shares a group with.
        const val MAX_BYTES = 32L * 1024 * 1024
    }
}
