package ru.hse.spb.server.nonblocking

import ru.hse.spb.common.ServerAddresses.smartestServerAddress
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.server.Server
import ru.hse.spb.server.common.Config
import ru.hse.spb.server.common.sortReceivedList
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

class NonblockingServer : Server {
    companion object {
        const val TIMEOUT = 50L
    }

    private val incomingSelector = Selector.open()
    private val outgoingSelector = Selector.open()
    private val threadPool =
        Executors.newFixedThreadPool(Config.threadPoolSize)

    override fun runServer(address: InetSocketAddress) {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.socket().bind(address)
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.register(incomingSelector, SelectionKey.OP_ACCEPT)

        Thread(this::processOutgoing).start()

        while (true) {
            incomingSelector.select()
            val selectedKeys = incomingSelector.selectedKeys()
            val iterator = selectedKeys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                when {
                    key.isAcceptable -> registerClient(serverSocketChannel)
                    key.isReadable -> receiveClient(key)
                }
                iterator.remove()
            }
        }
    }

    private fun registerClient(serverSocketChannel: ServerSocketChannel) {
        try {
            val client = serverSocketChannel.accept()
            client.configureBlocking(false)
            val clientInfo = ClientInfo()
            client.register(incomingSelector, SelectionKey.OP_READ, clientInfo)
            println("$clientInfo connected")
        } catch (e: IOException) {
            println("Can not accept client: ${e.message}")
        }
    }

    private fun receiveClient(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        val clientInfo = key.attachment() as ClientInfo

        try {
            if (clientInfo.size == null) {
                val buffer = clientInfo.sizeBuffer
                val readBytes = channel.read(buffer)
                if (readBytes == -1) {
                    println("We lost $clientInfo")
                    key.cancel()
                    return
                }

                if (buffer.isFull()) {
                    buffer.flip()
                    val size = buffer.int
                    clientInfo.size = size
                    buffer.clear()
                    clientInfo.messageBuffer = ByteBuffer.allocate(size)
                }
            } else {
                val buffer = clientInfo.messageBuffer
                requireNotNull(buffer)
                val readBytes = channel.read(buffer)
                if (readBytes == -1) {
                    println("We lost $clientInfo")
                    key.cancel()
                    return
                }
                if (buffer.isFull()) {
                    val bytes = ByteArray(buffer.capacity())
                    buffer.flip()
                    buffer.get(bytes)
                    clientInfo.size = null
                    threadPool.execute(Worker(bytes, System.currentTimeMillis(), key))
                }
            }
        } catch (e: IOException) {
            println("We lost $clientInfo: ${e.message}")
            key.cancel()
        }
        println("Message from $clientInfo received")
    }

    private fun processOutgoing() {
        while (true) {
            outgoingSelector.select(TIMEOUT)
            val selectedKeys = outgoingSelector.selectedKeys()
            val iterator = selectedKeys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                iterator.remove()
                val info = key.attachment() as OutgoingClientInfo
                val channel = key.channel() as SocketChannel
                try {
                    when (info) {
                        is OutgoingClientInfoWithMetrics -> {
                            if (info.inStartPosition()) {
                                info.startTime = System.currentTimeMillis() - info.startTime
                            }
                            channel.write(info.buffer)
                            println("Answer to ${info.clientInfo} send")
                            if (info.buffer.isFull()) {
                                val metrics = ProtoBuf.Metrics.newBuilder().apply {
                                    sortingTime = info.sortingTime
                                    requestTime = info.startTime
                                }.build()
                                key.attach(
                                    OutgoingClientInfoWithoutMetrics(
                                        metrics,
                                        info.clientInfo
                                    )
                                )
                            }
                        }
                        is OutgoingClientInfoWithoutMetrics -> {
                            channel.write(info.buffer)
                            println("Metrics to ${info.clientInfo} send")
                            if (info.buffer.isFull()) {
                                key.cancel()
                            }
                        }

                    }
                } catch (e: IOException) {
                    key.cancel()
                    println("We lost ${info.clientInfo}: ${e.message}")
                }
            }
        }
    }

    private inner class Worker(val bytes: ByteArray, val startTime: Long, val key: SelectionKey) : Runnable {
        override fun run() {
            val message = ProtoBuf.Message.parseFrom(bytes)
            if (!message.hasNextRequest) {
                key.cancel()
            }
            val clientInfo = key.attachment() as ClientInfo
            val channel = key.channel() as SocketChannel
            val (sortingTime, sortedList) = sortReceivedList(message)
            val outgoingClientInfo = OutgoingClientInfoWithMetrics(
                sortedList.generateMessage(),
                clientInfo,
                startTime,
                sortingTime
            )
            channel.register(outgoingSelector, SelectionKey.OP_WRITE, outgoingClientInfo)
            outgoingSelector.wakeup()
        }
    }
}

fun main() {
    NonblockingServer().runServer(smartestServerAddress)
}