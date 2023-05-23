package edu.gtri.gpssample.network.models
import android.util.Log
import java.math.BigInteger
import java.security.MessageDigest

import java.nio.ByteBuffer

data class TCPHeader(val command : Int, val payloadSize : Int)
{
    companion object
    {
        const val key : String = "PUT HASH HERE"
        const val size = 4 + key.length + 4

        fun fromByteArray(byteArray : ByteArray) : TCPHeader? {
            val byteBuffer = ByteBuffer.wrap(byteArray)
            byteBuffer.position(0)
            val command = byteBuffer.int
            byteBuffer.position(Int.SIZE_BYTES)
            val keyBytes: ByteArray = ByteArray(TCPHeader.key.length)
            byteBuffer.get(keyBytes)
            val key = String(keyBytes)
            byteBuffer.position(Int.SIZE_BYTES + TCPHeader.key.length)
            val payloadSize = byteBuffer.int
            if(key == TCPHeader.key) {
                return TCPHeader(command, payloadSize)
            }
            return null;
        }
    }
}


data class TCPMessage(val command : Int, val payload : String?) {

    constructor(header : TCPHeader, payload : String) : this(header.command, payload)

    var header : TCPHeader = TCPHeader(command, payload?.length ?: 0)
    fun toByteArray(): ByteArray? {
        val size: Int = 4 + 4 + TCPHeader.key.length + (payload?.length ?: 0)
        val adjusted  = size - (payload?.length ?: 0)
        Log.d("XXXXXXXX ", "SIZE ${size} and adjusted ${adjusted}")
        val byteBuffer = ByteBuffer.allocate(size)
            .putInt(header.command)
            .put(TCPHeader.key.toByteArray())
            .putInt(header.payloadSize)
            .put(payload?.toByteArray())

        return byteBuffer.array()
    }
    companion object
    {
        fun createMD5(input:String): String {
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
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
                return TCPMessage(command, payloadBytes.toString())
            }
            return null
        }
    }

}