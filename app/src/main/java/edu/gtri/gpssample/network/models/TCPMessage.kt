package edu.gtri.gpssample.network.models

import java.nio.ByteBuffer

data class TCPHeader(val command : Int, val payloadSize : Int)
{
    companion object
    {
        val key : String = "PUT HASH HERE"
    }
}

data class TCPMessage(val header : TCPHeader, val payload : String) {

    fun toByteArray(): ByteArray? {
        val size: Int = 4 + 4 + TCPHeader.key.length + payload.length

        val byteBuffer = ByteBuffer.allocate(size)
            .putInt(header.command)
            .put(TCPHeader.key.toByteArray())
            .putInt(header.payloadSize)
            .put(payload.toByteArray())

        return byteBuffer.array()
    }
    fun fromByteArray(byteArray : ByteArray) : TCPMessage?
    {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.position(0)
        val command = byteBuffer.int
        byteBuffer.position(Int.SIZE_BYTES)
        val keyBytes : ByteArray = ByteArray(TCPHeader.key.length)
        byteBuffer.get(keyBytes)
        val key = String(keyBytes)
        byteBuffer.position(Int.SIZE_BYTES + TCPHeader.key.length)
        val payloadLength = byteBuffer.int
        val payloadBytes = ByteArray(payloadLength)
        byteBuffer.get(payloadBytes)
        // check
        if(key == TCPHeader.key) {
            val header = TCPHeader(command, payloadLength)
            return TCPMessage(header, payload)
        }
        return null
    }
}