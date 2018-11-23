package ru.hse.spb.common

import ru.hse.spb.message.ProtoBuf
import java.net.InetSocketAddress
import java.nio.ByteBuffer

object ServerAddresses {
    val dummyServerAddress = InetSocketAddress("localhost", 8283)
    val smarterServerAddress = InetSocketAddress("localhost", 8284)
    val smartestServerAddress = InetSocketAddress("localhost", 8285)
}

fun List<Int>.generateMessage(nextRequest: Boolean? = null): ProtoBuf.Message = ProtoBuf.Message.newBuilder().apply {
    n = size
    addAllArray(this@generateMessage)
    if (nextRequest != null) {
        hasNextRequest = nextRequest
    }
}.build()

fun Int.toByteArray(): ByteArray {
    val bb = ByteBuffer.allocate(4)
    bb.putInt(this)
    return bb.array()
}