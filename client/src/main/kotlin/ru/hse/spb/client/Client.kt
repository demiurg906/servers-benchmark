package ru.hse.spb.client

import ru.hse.spb.common.generateMessage
import ru.hse.spb.common.toByteArray
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.runner.ServerType
import java.io.IOException
import java.net.Socket
import java.util.stream.Stream
import kotlin.streams.toList

class Client(
    private val serverType: ServerType,
    private val maxElement: Int,
    private val arraySize: Int,
    private val delta: Int,
    private val totalRequests: Int
) {
    private fun generateData(): List<Int> = Stream.generate((0..maxElement)::random).limit(arraySize.toLong()).toList()

    fun serve(): ClientStatistic {
        val requestsStatistics = mutableListOf<RequestStatistic>()
        val servingStartTime = System.currentTimeMillis()
        try {
            Socket(serverType.address.hostName, serverType.address.port).use { socket ->
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()
                for (requestNumber in 1..totalRequests) {
                    println("request number $requestNumber")
                    try {
                        // send message
                        val sendMessage = generateData().generateMessage(requestNumber < totalRequests)
                        val startTime = System.currentTimeMillis()
                        if (serverType.hardMode) {
                            outputStream.write(sendMessage.serializedSize.toByteArray())
                            sendMessage.writeTo(outputStream)
                        } else {
                            sendMessage.writeDelimitedTo(outputStream)
                        }
                        outputStream.flush()

                        // receive message (that message is useless)
                        ProtoBuf.Message.parseDelimitedFrom(inputStream)
                        val endTime = System.currentTimeMillis()
                        // process metrics
                        val metrics = ProtoBuf.Metrics.parseDelimitedFrom(inputStream)
                        requestsStatistics += RequestStatistic(
                            startTime,
                            endTime,
                            endTime - startTime,
                            metrics.requestTime,
                            metrics.sortingTime
                        )
                    } catch (e: IOException) {
                        // TODO log it
                        println("Feels bad, man. Cause ${e.message}")
                    }
                    try {
                        Thread.sleep(delta.toLong() * 1000)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        } catch (e: IOException) {
            // TODO log it
            println("Feels bad, man. Cause ${e.message}")
        }
        val servingEndTime = System.currentTimeMillis()
        return ClientStatistic(servingStartTime, servingEndTime, requestsStatistics)
    }
}