package ru.hse.spb.runner

import ru.hse.spb.common.ServerAddresses
import java.net.InetSocketAddress

enum class ServerType(val hardMode: Boolean, private val addressGetter: () -> InetSocketAddress) {
    DUMMY(false, ServerAddresses::dummyServerAddress),
    SMARTER(false, ServerAddresses::smarterServerAddress),
    SMARTEST(true, ServerAddresses::smartestServerAddress);

    companion object {
        fun fromString(string: String?): ServerType = when (string) {
            "dummy" -> DUMMY
            "smarter" -> SMARTER
            "smartest" -> SMARTEST
            else -> throw IllegalArgumentException("Unknown server type $string")
        }
    }

    val address: InetSocketAddress
        get() = addressGetter.invoke()
}