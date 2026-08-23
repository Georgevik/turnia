package com.georgevik.turnia.ui.system

import kotlin.uuid.Uuid

fun createUuid(): String = Uuid.random().toString()
