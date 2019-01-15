package ru.hse.spb.server.smart

import ru.hse.spb.common.ServerAddresses.smarterServerAddress
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.server.Server
import ru.hse.spb.server.common.Config
import ru.hse.spb.server.common.sortReceivedList
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class SmartServer : Server {
    private val clientsCounter = AtomicInteger(0)
    private val aliveClients = ConcurrentHashMap<Int, Unit>()
    private val workersThreadPool = Executors.newFixedThreadPool(Config.threadPoolSize)
    private val receiversThreadPool = Executors.newCachedThreadPool()

    override fun runServer(address: InetSocketAddress) {
        val serverSocket = ServerSocket(address.port)

        while (true) {
            try {
                val socket = serverSocket.accept()
                val clientId = clientsCounter.getAndIncrement()
                aliveClients[clientId] = Unit
                receiversThreadPool.submit(Worker(socket, clientId))
            } catch (e: IOException) {
                println("Can not accept client: ${e.message}")
            }
        }
    }

    private inner class Worker(private val socket: Socket, private val id: Int) : Runnable {
        private val replierThreadPool = Executors.newSingleThreadExecutor()

        override fun run() {
            while (true) {
                val message = try {
                    ProtoBuf.Message.parseDelimitedFrom(socket.inputStream)
                } catch (e: IOException) {
                    e.log()
                    return
                }
                println("Message from client $id received")
                val startTime = System.currentTimeMillis()
                workersThreadPool.submit {
                    val (sortingTime, sortedList) = sortReceivedList(message)
                    val answer = sortedList.generateMessage()
                    replierThreadPool.submit {
                        val endTime = System.currentTimeMillis()
                        try {
                            answer.writeDelimitedTo(socket.outputStream)
                            println("Answer to client $id send")

                            val metrics = ProtoBuf.Metrics.newBuilder()
                                .setSortingTime(sortingTime)
                                .setRequestTime(endTime - startTime)
                                .build()
                            metrics.writeDelimitedTo(socket.outputStream)
                            println("Metrics to client $id send")

                            if (!aliveClients.containsKey(id)) {
                                replierThreadPool.shutdown()
                            }
                        } catch (e: IOException) {
                            e.log()
                        }
                    }
                }

                if (!message.hasNextRequest) {
                    aliveClients.remove(id)
                    break
                }
            }
        }

        private fun IOException.log() {
            println("We lost client $id: $message")
        }
    }
}

fun main(args: Array<String>) {
    SmartServer().runServer(smarterServerAddress)
}