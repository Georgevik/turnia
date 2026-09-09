package com.geoviksoft.turnia.ui.system

import kotlin.uuid.Uuid

fun createUuid(): String = Uuid.random().toString()
