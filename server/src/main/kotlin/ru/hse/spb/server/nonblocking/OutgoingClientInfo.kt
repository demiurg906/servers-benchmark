package ru.hse.spb.server.nonblocking

import com.google.protobuf.GeneratedMessageLite
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

sealed class OutgoingClientInfo(message: GeneratedMessageLite, val clientInfo: ClientInfo) {
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
    clientInfo: ClientInfo,
    var startTime: Long,
    val sortingTime: Long
) : OutgoingClientInfo(message, clientInfo) {
    fun inStartPosition(): Boolean = buffer.inStartPosition()
}

class OutgoingClientInfoWithoutMetrics(message: GeneratedMessageLite, clientInfo: ClientInfo) :
    OutgoingClientInfo(message, clientInfo)