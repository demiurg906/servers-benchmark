package ru.hse.spb.server

import ru.hse.spb.server.dummy.runDummyServer
import ru.hse.spb.server.nonblocking.runNonBlockingServer
import ru.hse.spb.server.smart.runSmartServer

private const val DUMMY_SERVER = "dummy"
private const val SMART_SERVER = "smart"
private const val NON_BLOCKING_SERVER = "non-blocking"
private const val ALL_SERVERS = "all"

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        DUMMY_SERVER -> runDummyServer()
        SMART_SERVER -> runSmartServer()
        NON_BLOCKING_SERVER -> runNonBlockingServer()
        ALL_SERVERS -> {
            Thread(::runDummyServer).start()
            Thread(::runSmartServer).start()
            runNonBlockingServer()
        }
        else -> {
            println("Usage: server <server-type>")
            println("server-types: [$DUMMY_SERVER | $SMART_SERVER | $NON_BLOCKING_SERVER | $ALL_SERVERS]")
        }
    }
}