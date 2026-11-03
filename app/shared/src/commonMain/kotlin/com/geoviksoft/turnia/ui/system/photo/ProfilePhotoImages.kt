package com.geoviksoft.turnia.ui.system.photo

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import okio.Buffer
import okio.FileSystem

/**
 * The image loader every avatar draws through. Coil keeps the decoded photos in memory; the bytes
 * on disk are [ProfilePhotoCache]'s, which is what a photo is fetched through.
 */
fun profilePhotoImageLoader(context: PlatformContext, cache: ProfilePhotoCache): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(ProfilePhotoKeyer)
            add(ProfilePhotoFetcher.Factory(cache))
        }
        // The disk is ProfilePhotoCache's, already keyed by photo; a second copy would only cost space.
        .diskCache(null)
        .build()

private object ProfilePhotoKeyer : Keyer<ProfilePhoto> {
    override fun key(data: ProfilePhoto, options: Options): String = data.key
}

private class ProfilePhotoFetcher(
    private val photo: ProfilePhoto,
    private val cache: ProfilePhotoCache,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // Failing is what makes the avatar keep showing the animal.
        val loaded = cache.load(photo) ?: error("Profile photo ${photo.key} is unavailable")
        return SourceFetchResult(
            source = ImageSource(Buffer().write(loaded.bytes), FileSystem.SYSTEM),
            mimeType = "image/webp",
            dataSource = if (loaded.downloaded) DataSource.NETWORK else DataSource.DISK,
        )
    }

    class Factory(private val cache: ProfilePhotoCache) : Fetcher.Factory<ProfilePhoto> {
        override fun create(data: ProfilePhoto, options: Options, imageLoader: ImageLoader): Fetcher =
            ProfilePhotoFetcher(data, cache)
    }
}
