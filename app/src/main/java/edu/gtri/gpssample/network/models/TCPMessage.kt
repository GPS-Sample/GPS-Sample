/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network.models

import java.math.BigInteger
import java.security.MessageDigest
import java.nio.ByteBuffer

data class TCPHeader(val command : Int, val payloadSize : Int)
{
    companion object
    {
        const val key : String = "PUT HASH HERE"
        const val size = 4 + key.length + 4

        fun fromByteArray(byteArray : ByteArray) : TCPHeader?
        {
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

data class TCPMessage(val command : Int, val payload : ByteArray)
{
    constructor( header : TCPHeader, payload : ByteArray) : this(header.command, payload)

    val header = TCPHeader(command, payload.size)

    fun toByteArray(): ByteArray?
    {
        val keyBuffer = TCPHeader.key.toByteArray()

        val size: Int = 4 + 4 + TCPHeader.key.length + payload.size

        val byteBuffer = ByteBuffer.allocate(size)
            .putInt(header.command)
            .put(keyBuffer)
            .putInt(header.payloadSize)
            .put(payload)

        return byteBuffer.array()
    }

    fun toHeaderByteArray( payloadSize: Int ): ByteArray
    {
        val header = TCPHeader(command, payloadSize)
        val keyBuffer = TCPHeader.key.toByteArray()

        val size: Int = 4 + 4 + TCPHeader.key.length

        val byteBuffer = ByteBuffer.allocate(size)
            .putInt(header.command)
            .put(keyBuffer)
            .putInt(payloadSize)

        return byteBuffer.array()
    }
}