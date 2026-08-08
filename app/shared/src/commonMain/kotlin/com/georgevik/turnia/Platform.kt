package com.georgevik.turnia

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform