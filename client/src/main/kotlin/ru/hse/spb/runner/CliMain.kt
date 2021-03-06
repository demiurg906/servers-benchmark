package ru.hse.spb.runner

import java.util.concurrent.Executors

// size of array
const val N = 10_000
// number of clients
const val M = 5
// clients wait time (milliseconds)
const val DELTA = 1
// number of requests
const val X = 5

fun main(args: Array<String>) {
    val server = ServerType.fromString(args.firstOrNull())
    val config = MConfig(N, IntProgression.fromClosedRange(1, 10, 2), DELTA, X)
    val clientsThreadPool = Executors.newCachedThreadPool()
    val summaryStatistic = collectStatistic(config, server, clientsThreadPool)
    println(summaryStatistic)
}