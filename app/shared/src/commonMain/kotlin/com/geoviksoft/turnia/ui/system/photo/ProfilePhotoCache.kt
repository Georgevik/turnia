package com.geoviksoft.turnia.ui.system.photo

import coil3.disk.DiskCache
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import okio.FileSystem
import okio.Path

/**
 * Every profile photo this device has seen, on disk, keyed by [ProfilePhoto.key]. A photo's file
 * never changes — a new photo is a new id — so an entry never goes stale: the only download is the
 * first one, and the user's own new photo costs none at all, since [prime] stores it as it goes up.
 */
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

    class Loaded(val bytes: ByteArray, val downloaded: Boolean)

    /** The photo's bytes, from disk if this device has them; null when it cannot be had. */
    suspend fun load(photo: ProfilePhoto): Loaded? {
        read(photo)?.let { return Loaded(it, downloaded = false) }
        val bytes = download(photo) ?: return null
        write(photo, bytes)
        return Loaded(bytes, downloaded = true)
    }

    fun prime(photo: ProfilePhoto, bytes: ByteArray) = write(photo, bytes)

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
