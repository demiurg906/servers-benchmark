package ru.hse.spb.server.nonblocking

import java.nio.ByteBuffer

internal fun ByteBuffer.isFull(): Boolean = position() == limit()
internal fun ByteBuffer.inStartPosition(): Boolean = position() == 0