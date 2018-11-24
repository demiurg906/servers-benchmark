package ru.hse.spb.server

import com.google.protobuf.GeneratedMessageLite
import ru.hse.spb.common.ServerAddresses.smartestServerAddress
import ru.hse.spb.common.generateMessage
import ru.hse.spb.message.ProtoBuf
import ru.hse.spb.server.common.Config
import ru.hse.spb.server.common.sortReceivedList
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

class ClientInfo {
    val sizeBuffer: ByteBuffer = ByteBuffer.allocate(4)
    var messageBuffer: ByteBuffer? = null

    var size: Int? = null
}

sealed class OutgoingClientInfo(message: GeneratedMessageLite) {
    val buffer: ByteBuffer = ByteBuffer.allocate(4 + message.serializedSize)

    init {
        val stream = ByteArrayOutputStream()
        message.writeDelimitedTo(stream)
        buffer.put(stream.toByteArray())
        buffer.flip()
    }
}

class OutgoingClientInfoWithMetrics(
    message: GeneratedMessageLite,
    var startTime: Long,
    val sortingTime: Long
) : OutgoingClientInfo(message) {
    fun inStartPosition(): Boolean = buffer.inStartPosition()
}

class OutgoingClientInfoWithoutMetrics(message: GeneratedMessageLite) : OutgoingClientInfo(message)

class SmartestServer : Server {
    companion object {
        const val TIMEOUT = 50L
    }

    private val readSelector = Selector.open()
    private val writeSelector = Selector.open()
    private val threadPool = Executors.newFixedThreadPool(Config.threadPoolSize)

    override fun runServer(address: InetSocketAddress) {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.socket().bind(address)

        Thread(this::processIncoming).start()
        Thread(this::processOutgoing).start()

        while (true) {
            val socketChannel = serverSocketChannel.accept()
            socketChannel.configureBlocking(false)
            println("accepted")
            socketChannel.register(readSelector, SelectionKey.OP_READ, ClientInfo())
        }
    }

    private fun processIncoming() {
        while (true) {
            readSelector.select(TIMEOUT)
            val selectedKeys = readSelector.selectedKeys()
            val iterator = selectedKeys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                iterator.remove()
                val channel = key.channel() as SocketChannel
                val clientInfo = key.attachment() as ClientInfo
                if (clientInfo.size == null) {
                    val buffer = clientInfo.sizeBuffer
                    channel.read(buffer)
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
                    channel.read(buffer)
                    if (buffer.isFull()) {
                        val bytes = ByteArray(buffer.capacity())
                        buffer.flip()
                        buffer.get(bytes)
                        clientInfo.size = null
                        threadPool.execute(Worker(bytes, System.currentTimeMillis(), key))
                    }
                }
            }
        }
    }

    private fun processOutgoing() {
        while (true) {
            writeSelector.select(TIMEOUT)
            val selectedKeys = writeSelector.selectedKeys()
            val iterator = selectedKeys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                iterator.remove()
                val info = key.attachment()
                val channel = key.channel() as SocketChannel
                try {
                    when (info) {
                        is OutgoingClientInfoWithMetrics -> {
                            if (info.inStartPosition()) {
                                info.startTime = System.currentTimeMillis() - info.startTime
                            }
                            channel.write(info.buffer)
                            if (info.buffer.isFull()) {
                                val metrics = ProtoBuf.Metrics.newBuilder().apply {
                                    sortingTime = info.sortingTime
                                    requestTime = info.startTime
                                }.build()
                                key.attach(OutgoingClientInfoWithoutMetrics(metrics))
                            }
                        }
                        is OutgoingClientInfoWithoutMetrics -> {
                            channel.write(info.buffer)
                            if (info.buffer.isFull()) {
                                key.cancel()
                            }
                        }

                    }
                } catch (e: IOException) {
                    println("we lost client. ${e.message}")
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
            val channel = key.channel() as SocketChannel
            val (sortingTime, sortedList) = sortReceivedList(message)
            val outgoingClientInfo = OutgoingClientInfoWithMetrics(
                sortedList.generateMessage(),
                startTime,
                sortingTime
            )
            channel.register(writeSelector, SelectionKey.OP_WRITE, outgoingClientInfo)
        }
    }
}

private fun ByteBuffer.isFull(): Boolean = position() == limit()
private fun ByteBuffer.inStartPosition(): Boolean = position() == 0

fun main() {
    SmartestServer().runServer(smartestServerAddress)
}