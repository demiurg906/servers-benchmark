package ru.hse.spb.runner

import ru.hse.spb.client.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

fun singleRun(n: Int, m: Int, delta: Int, x: Int, serverType: ServerType, threadPool: ExecutorService): AveragedStatistics {
    val futures = mutableListOf<Future<ClientStatistic>>()
    for (i in 1..m) {
        futures += threadPool.submit<ClientStatistic> {
            Client(serverType, n, n, delta, x).serve()
        }
    }

    val statistics = futures.map {
        try {
        it.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: ClientExecutionException("Unknown error")
        }
    }

    return statistics.calculateAverage()
}

fun collectStatistic(config: Config, serverType: ServerType, threadPool: ExecutorService): SummaryStatistic {
    return when (config) {
        is NConfig -> {
            val statistics = mutableMapOf<Int, AveragedStatistics>()
            for (n in config.nRange) {
                statistics[n] = singleRun(n, config.m, config.delta, config.x, serverType, threadPool)
            }
            SummaryStatistic(statistics, config, RangeType.N_RANGE)
        }

        is MConfig -> {
            val statistics = mutableMapOf<Int, AveragedStatistics>()
            for (m in config.mRange) {
                statistics[m] = singleRun(config.n, m, config.delta, config.x, serverType, threadPool)
            }
            SummaryStatistic(statistics, config, RangeType.M_RANGE)
        }

        is DeltaConfig -> {
            val statistics = mutableMapOf<Int, AveragedStatistics>()
            for (delta in config.deltaRange) {
                statistics[delta] = singleRun(config.n, config.m, delta, config.x, serverType, threadPool)
            }
            SummaryStatistic(statistics, config, RangeType.DELTA_RANGE)
        }
    }
}

