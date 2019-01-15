package ru.hse.spb.server.dummy

import ru.hse.spb.common.ServerAddresses.dummyServerAddress
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.server.Server
import ru.hse.spb.server.common.sortReceivedList
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DummyServer : Server {
    override fun runServer(address: InetSocketAddress) {
        val serverSocket = ServerSocket(address.port)
        val threadPool = Executors.newCachedThreadPool()
        while (true) {
            try {
                val socket = serverSocket.accept()
                threadPool.submit(Worker(socket))
            } catch (e: IOException) {
                println("Can not accept client: ${e.message}")
            }
        }
    }
}

class Worker(private val socket: Socket) : Runnable {
    companion object {
        val idCounter = AtomicInteger()
    }

    private val id = idCounter.getAndIncrement()

    override fun run() {
        try {
            while (true) {
                val receivedMessage = ProtoBuf.Message.parseDelimitedFrom(socket.inputStream)
                println("Message from client $id received")

                // handle request
                val startTime = System.currentTimeMillis()
                val (sortingTime, sortedList) = sortReceivedList(receivedMessage)
                val message = sortedList.generateMessage()
                val endTime = System.currentTimeMillis()

                // send response
                message.writeDelimitedTo(socket.outputStream)
                println("Answer to client $id send")

                // send metrics
                val metrics = ProtoBuf.Metrics.newBuilder()
                    .setSortingTime(sortingTime)
                    .setRequestTime(endTime - startTime)
                    .build()
                metrics.writeDelimitedTo(socket.outputStream)
                println("Metrics to client $id send")
                if (!receivedMessage.hasNextRequest) {
                    break
                }
            }
        } catch (e: IOException) {
            println("We lost client $id: ${e.message}")
        }
    }
}

fun main(args: Array<String>) {
    val server = DummyServer()
    server.runServer(dummyServerAddress)
}