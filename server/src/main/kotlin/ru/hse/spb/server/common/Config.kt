package ru.hse.spb.server.common

import java.io.File

object Config {
    val threadPoolSize: Int = File("config.ini").readLines().first().toInt()
}