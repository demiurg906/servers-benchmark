package ru.hse.spb.server

import java.net.InetSocketAddress

interface Server {
    fun runServer(address: InetSocketAddress)
}