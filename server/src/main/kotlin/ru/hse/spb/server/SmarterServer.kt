package ru.hse.spb.server

import ru.hse.spb.common.ServerAddresses.smarterServerAddress
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.server.common.sortReceivedList
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

const val THRED_POOL_SIZE = 8

class SmarterServer : Server {
    private val clientsCounter = AtomicInteger(0)
    private val aliveClients = ConcurrentHashMap<Int, Unit>()

    override fun runServer(address: InetSocketAddress) {
        val serverSocket = ServerSocket(address.port)
        val receiversThreadPool = Executors.newCachedThreadPool()
        val workersThreadPool = Executors.newFixedThreadPool(THRED_POOL_SIZE)
        while (true) {
            val socket = serverSocket.accept()
            val clientId = clientsCounter.getAndIncrement()
            aliveClients[clientId] = Unit
            val replierThreadPool = Executors.newSingleThreadExecutor()
            receiversThreadPool.submit {
                while (true) {
                    val message = ProtoBuf.Message.parseDelimitedFrom(socket.inputStream)
                    val startTime = System.currentTimeMillis()
                    workersThreadPool.submit {
                        val (sortingTime, sortedList) = sortReceivedList(message)
                        val answer = sortedList.generateMessage()
                        replierThreadPool.submit {
                            val endTime = System.currentTimeMillis()
                            answer.writeDelimitedTo(socket.outputStream)

                            val metrics = ProtoBuf.Metrics.newBuilder()
                                .setSortingTime(sortingTime)
                                .setRequestTime(endTime - startTime)
                                .build()
                            metrics.writeDelimitedTo(socket.outputStream)
                            if (!aliveClients.containsKey(clientId)) {
                                replierThreadPool.shutdown()
                            }
                        }
                    }

                    if (!message.hasNextRequest) {
                        aliveClients.remove(clientId)
                        break
                    }
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    SmarterServer().runServer(smarterServerAddress)
}