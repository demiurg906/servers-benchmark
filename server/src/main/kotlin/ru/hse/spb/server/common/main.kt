package ru.hse.spb.server.common

import java.nio.ByteBuffer

fun main() {
    val buffer = ByteBuffer.allocate(4)
    val size = 29849
    buffer.putInt(size)
    println(buffer.array())
}