package com.geoviksoft.turnia.interfaces

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
