package ru.hse.spb.server.common

import ru.hse.spb.message.ProtoBuf

data class SortingInfo(val sortingTime: Long, val sortedList: List<Int>)

fun sortReceivedList(message: ProtoBuf.Message): SortingInfo {
    val startTime = System.currentTimeMillis()
    val list = bubbleSort(message.arrayList)
    val endTime = System.currentTimeMillis()
    return SortingInfo(endTime - startTime, list)
}

private fun bubbleSort(list: List<Int>): List<Int> {
    val res = list.toMutableList()

    val n = res.size
    for (i in 0 until n - 1) {
        for (j in 0 until n - i - 1) {
            if (res[j] > res[j + 1]) {
                val temp = res[j]
                res[j] = res[j + 1]
                res[j + 1] = temp
            }
        }
    }
    return res
}