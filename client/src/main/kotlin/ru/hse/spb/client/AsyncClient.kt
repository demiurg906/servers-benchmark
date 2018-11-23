package ru.hse.spb.client

import com.google.protobuf.GeneratedMessageLite
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.readFully
import kotlinx.coroutines.io.writeFully
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import java.io.IOException
import java.net.SocketAddress
import java.util.stream.Stream
import kotlin.streams.toList

@KtorExperimentalAPI
class AsyncClient(
    private val address: SocketAddress,
    private val maxElement: Int,
    private val arraySize: Int,
    private val delta: Int,
    private val totalRequests: Int,
    private val id: Int
) {
    private fun generateData(): List<Int> = Stream.generate((0..maxElement)::random).limit(arraySize.toLong()).toList()

    suspend fun serve(): ClientStatistic {
        val requestsStatistics = mutableListOf<RequestStatistic>()
        val servingStartTime = System.currentTimeMillis()
        try {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(address)
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)

            for (requestNumber in 1..totalRequests) {
                println("client $id request $requestNumber")
                try {
                    // send message
                    val sendMessage = generateData().generateMessage(requestNumber < totalRequests)
                    val startTime = System.currentTimeMillis()
                    println(sendMessage.serializedSize)
                    output.writeInt(sendMessage.serializedSize)

                    output.writeFully(sendMessage.toByteArray())


                    // receive message (that message is useless)
                    receiveMessage(input){ bytes -> ProtoBuf.Message.parseFrom(bytes) }

                    val endTime = System.currentTimeMillis()
                    // process metrics
                    val metrics = receiveMessage(input){ bytes -> ProtoBuf.Metrics.parseFrom(bytes) }
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
                delay(delta.toLong())
            }

        } catch (e: IOException) {
            // TODO log it
            println("Feels bad, man. Cause ${e.message}")
        }
        val servingEndTime = System.currentTimeMillis()
        return ClientStatistic(servingStartTime, servingEndTime, requestsStatistics)
    }
}

suspend inline fun <reified T : GeneratedMessageLite>receiveMessage(input: ByteReadChannel, parser: (ByteArray) -> T): T {
    val size = input.readInt()
    val byteArray =  ByteArray(size)
    input.readFully(byteArray)
    return parser(byteArray)
}