package com.geoviksoft.turnia.core.data.datasource.storage

import dev.gitlive.firebase.storage.Data

internal actual fun ByteArray.toStorageData(): Data = Data(this)

internal actual fun Data.toByteArray(): ByteArray = data
