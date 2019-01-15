package ru.hse.spb.server.dummy

import ru.hse.spb.common.ServerAddresses.dummyServerAddress
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.server.Server
import ru.hse.spb.server.common.sortReceivedList
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class DummyServer : Server {
    override fun runServer(address: InetSocketAddress) {
        val serverSocket = ServerSocket(address.port)
        val threadPool = Executors.newCachedThreadPool()
        while (true) {
            val socket = serverSocket.accept()
            threadPool.submit(Worker(socket))
        }
    }
}

class Worker(private val socket: Socket): Runnable {
    override fun run() {
        while (true) {
            val receivedMessage = ProtoBuf.Message.parseDelimitedFrom(socket.inputStream)

            // handle request
            val startTime = System.currentTimeMillis()
            val (sortingTime, sortedList) = sortReceivedList(receivedMessage)
            val message = sortedList.generateMessage()
            val endTime = System.currentTimeMillis()

            // send response
            message.writeDelimitedTo(socket.outputStream)

            // send metrics
            val metrics = ProtoBuf.Metrics.newBuilder()
                .setSortingTime(sortingTime)
                .setRequestTime(endTime - startTime)
                .build()
            metrics.writeDelimitedTo(socket.outputStream)
            if (!receivedMessage.hasNextRequest) {
                break
            }
        }
    }
}

fun main(args: Array<String>) {
    val server = DummyServer()
    server.runServer(dummyServerAddress)
}