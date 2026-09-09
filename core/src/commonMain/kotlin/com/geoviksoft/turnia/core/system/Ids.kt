package com.geoviksoft.turnia.core.system

import kotlin.uuid.Uuid

/** Ids are minted on the device: Firestore never has to hand one out before a write. */
fun createId(): String = Uuid.random().toString()
