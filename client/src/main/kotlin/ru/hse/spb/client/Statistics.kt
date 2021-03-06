package ru.hse.spb.client

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import ru.hse.spb.runner.Config
import ru.hse.spb.runner.RangeType
import java.io.File

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

data class SummaryStatistic(
    val statistics: Map<Int, AveragedStatistics>,
    val config: Config,
    val rangeType: RangeType
) {
    override fun toString(): String = StringBuilder().apply {
        appendln(config)
        for (key in statistics.keys.sorted()) {
            appendln("${rangeType.symbol} = $key")
            appendln(statistics[key])
            appendln()
        }
    }.toString()

    fun saveToCsv(file: File) {
        val header = config.label

        CSVPrinter(
            file.bufferedWriter(),
            CSVFormat.DEFAULT.withHeader(
                header,
                "Sorting time (s)",
                "Server response time (s)",
                "Client response time (s)"
            )
        ).use { printer ->
            for (key in statistics.keys.sorted()) {
                val (sortingTime, serverResponseTime, clientResponseTime) = statistics[key]
                    ?: throw IllegalStateException()
                printer.printRecord(key, sortingTime, serverResponseTime, clientResponseTime)
            }
            printer.flush()
        }
    }
}