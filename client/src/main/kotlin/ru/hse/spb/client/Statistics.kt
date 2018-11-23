package ru.hse.spb.client

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
)

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
