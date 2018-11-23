package ru.hse.spb.client

import ru.hse.spb.common.dummyServerAddress
import ru.hse.spb.common.smarterServerAddress
import ru.hse.spb.common.smartestServerAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.Future

// size of array
const val N = 10_000
// number of clients
const val M = 5
// clients wait time (seconds)
const val DELTA = 1
// number of requests
const val X = 5

fun singleRun(n: Int, m: Int, delta: Int, x: Int, serverType: ServerType) {
    val threadPool = Executors.newCachedThreadPool()
    val futures = mutableListOf<Future<ClientStatistic>>()
    for (i in 1..M) {
        futures += threadPool.submit<ClientStatistic> {
            Client(serverType, n, n, delta, x).serve()
        }
    }

    val statistics = futures.map { it.get() }
    threadPool.shutdown()

    val averagedStatistics = statistics.calculateAverage()

    println()
    println("Array size: $n")
    println("Number of clients: $m")
    println("Delta: $delta")
    println()

    println("Client response time is ${averagedStatistics.clientResponseTime} seconds")
    println("Server response time is ${averagedStatistics.serverResponseTime} seconds")
    println("Sorting time is ${averagedStatistics.sortingTime} seconds")
}

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


fun main(args: Array<String>) {
    val server = ServerType.fromString(args.firstOrNull())
    singleRun(N, M, DELTA, X, server)
}