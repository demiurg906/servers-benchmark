package ru.hse.spb.runner

import ru.hse.spb.common.dummyServerAddress
import ru.hse.spb.common.smarterServerAddress
import ru.hse.spb.common.smartestServerAddress
import java.net.InetSocketAddress

enum class ServerType(val hardMode: Boolean, val address: InetSocketAddress) {
    DUMMY(false, dummyServerAddress),
    SMARTER(false, smarterServerAddress),
    SMARTEST(true, smartestServerAddress);

    companion object {
        fun fromString(string: String?): ServerType = when (string) {
            "dummy" -> DUMMY
            "smarter" -> SMARTER
            "smartest" -> SMARTEST
            else -> throw IllegalArgumentException("Unknown server type $string")
        }
    }
}