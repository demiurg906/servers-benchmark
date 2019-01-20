package ru.hse.spb.runner

import ru.hse.spb.common.ServerAddresses
import java.net.InetSocketAddress

enum class ServerType(val prettyName: String, val hardMode: Boolean, private val addressGetter: () -> InetSocketAddress) {
    DUMMY("Dummy", false, ServerAddresses::dummyServerAddress),
    SMART("Smart", false, ServerAddresses::smartServerAddress),
    NON_BLOCKING("Non-blocking",true, ServerAddresses::nonBlockingServerAddress);

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