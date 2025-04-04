/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network.models

import java.nio.ByteBuffer

data class TCPHeader(val command : Int, val payloadSize : Long)
{
    companion object
    {
        const val KEY : String = "PUT HASH HERE"
        const val SIZE = Int.SIZE_BYTES + KEY.length + Long.SIZE_BYTES

        fun fromByteArray(byteArray : ByteArray) : TCPHeader?
        {
            val byteBuffer = ByteBuffer.wrap(byteArray)
            byteBuffer.position(0)
            val command = byteBuffer.int
            byteBuffer.position(Int.SIZE_BYTES)
            val keyBytes = ByteArray(TCPHeader.KEY.length)
            byteBuffer.get(keyBytes)
            val key = String(keyBytes)
            byteBuffer.position(Int.SIZE_BYTES + KEY.length)
            val payloadSize = byteBuffer.long
            if (key == KEY) {
                return TCPHeader(command, payloadSize)
            }
            return null;
        }
    }
}

data class TCPMessage(val command : Int, val payload : ByteArray)
{
    constructor( header : TCPHeader, payload : ByteArray) : this(header.command, payload)

    val header = TCPHeader(command, payload.size.toLong())

    fun toByteArray(): ByteArray?
    {
        val keyBuffer = TCPHeader.KEY.toByteArray()

        val byteBuffer = ByteBuffer.allocate(TCPHeader.SIZE + payload.size )
            .putInt(header.command)
            .put(keyBuffer)
            .putLong(header.payloadSize)
            .put(payload)

        return byteBuffer.array()
    }

    fun toHeaderByteArray( payloadSize: Long ): ByteArray
    {
        val header = TCPHeader(command, payloadSize)
        val keyBuffer = TCPHeader.KEY.toByteArray()

        val byteBuffer = ByteBuffer.allocate( TCPHeader.SIZE )
            .putInt(header.command)
            .put(keyBuffer)
            .putLong(payloadSize)

        return byteBuffer.array()
    }
}