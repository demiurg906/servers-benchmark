package ru.hse.spb.server.nonblocking

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

class ClientInfo {
    companion object {
        private val idCounter = AtomicInteger()
    }

    private val id = idCounter.getAndIncrement()

    val sizeBuffer: ByteBuffer = ByteBuffer.allocate(4)
    var messageBuffer: ByteBuffer? = null

    var size: Int? = null

    override fun toString(): String {
        return "client $id"
    }
}