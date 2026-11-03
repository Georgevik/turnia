package com.geoviksoft.turnia.core.data.datasource.storage

import dev.gitlive.firebase.storage.Data

/** GitLive's [Data] wraps a `ByteArray` on Android and an `NSData` on iOS. */
internal expect fun ByteArray.toStorageData(): Data

internal expect fun Data.toByteArray(): ByteArray
