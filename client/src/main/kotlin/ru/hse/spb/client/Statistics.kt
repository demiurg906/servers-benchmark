package ru.hse.spb.client

import ru.hse.spb.runner.Config
import ru.hse.spb.runner.RangeType

data class RequestStatistic(
    val startTime: Long,
    val endTime: Long,
    val clientResponseTime: Long,
    val serverResponseTime: Long,
    val sortingTime: Long
)

data class AveragedStatistics(
    val sortingTime: Double,
    val serverResponseTime: Double,
    val clientResponseTime: Double
) {
    override fun toString(): String = "Client response time is %.3f seconds\n".format(clientResponseTime) +
        "Server response time is %.3f seconds\n".format(serverResponseTime) +
        "Sorting time is %.3f seconds".format(sortingTime)
}

data class ClientStatistic(val startTime: Long, val endTime: Long, val requests: List<RequestStatistic>)

fun List<ClientStatistic>.calculateAverage(): AveragedStatistics {
    val lastStartTime = minBy { it.startTime }?.startTime ?: 0
    val firstEndTime = maxBy { it.endTime }?.endTime ?: 0
    
    val requestStatistics = flatMap { it.requests }
        .filter { it.startTime >= lastStartTime && it.endTime <= firstEndTime }

    fun Int.transform() = this.toDouble() / requestStatistics.size / 1000
    
    return AveragedStatistics(
        requestStatistics.sumBy { it.sortingTime.toInt() }.transform(),
        requestStatistics.sumBy { it.serverResponseTime.toInt() }.transform(),
        requestStatistics.sumBy { it.clientResponseTime.toInt() }.transform()
    )
}

data class SummaryStatistic(val statistics: Map<Int, AveragedStatistics>, val config: Config,  val rangeType: RangeType) {
    override fun toString(): String = StringBuilder().apply {
        appendln(config)
        for (key in statistics.keys.sorted()) {
            appendln("${rangeType.symbol} = $key")
            appendln(statistics[key])
            appendln()
        }
    }.toString()
}