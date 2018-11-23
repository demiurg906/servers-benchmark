package ru.hse.spb.runner

// size of array
const val N = 10_000
// number of clients
const val M = 5
// clients wait time (seconds)
const val DELTA = 1
// number of requests
const val X = 5

fun singleRunMain(args: Array<String>) {
    val server = ServerType.fromString(args.firstOrNull())
    val averagedStatistics = singleRun(N, M, DELTA, X, server)
    println()
    println("Array size: $N")
    println("Number of clients: $M")
    println("Delta: $DELTA")
    println()
}

fun main(args: Array<String>) {
    val server = ServerType.fromString(args.firstOrNull())
    val config = MConfig(N, IntProgression.fromClosedRange(1, 10, 2), DELTA, X)
    val summaryStatistic = collectStatistic(config, server)
    println(summaryStatistic)
}