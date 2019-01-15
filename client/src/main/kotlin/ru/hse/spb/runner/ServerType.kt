package ru.hse.spb.runner

import ru.hse.spb.common.ServerAddresses
import java.net.InetSocketAddress

enum class ServerType(val hardMode: Boolean, private val addressGetter: () -> InetSocketAddress) {
    DUMMY(false, ServerAddresses::dummyServerAddress),
    SMART(false, ServerAddresses::smartServerAddress),
    NON_BLOCKING(true, ServerAddresses::nonBlockingServerAddress);

    companion object {
        fun fromString(string: String?): ServerType = when (string) {
            "dummy" -> DUMMY
            "smart" -> SMART
            "nonBlocking" -> NON_BLOCKING
            else -> throw IllegalArgumentException("Unknown server type $string")
        }
    }

    val address: InetSocketAddress
        get() = addressGetter.invoke()
}