package com.dravenmiller.overseersterminal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform