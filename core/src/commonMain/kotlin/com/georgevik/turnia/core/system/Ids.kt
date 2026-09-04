package com.georgevik.turnia.core.system

import kotlin.uuid.Uuid

/** Ids are minted on the device: Firestore never has to hand one out before a write. */
fun createId(): String = Uuid.random().toString()

/** Short and shoutable: an invitation code is read out loud or typed by hand. */
fun createInvitationCode(): String = Uuid.random().toString().take(6).uppercase()
