package edu.gtri.gpssample.network

import java.net.Socket

object NetworkUtils {
    fun readToEnd(fullSize : Int, actualRead : Int, socket : Socket, payloadArray : ByteArray )
    {
        if(actualRead < fullSize)
        {
            val left = fullSize - actualRead
            val leftover = ByteArray(left)
            // maybe read one byte at a time?
            val read = socket.inputStream.read(leftover, 0,left)
            if(read > 0)
            {
                for ( i in (fullSize - left) until fullSize)
                {
                    payloadArray[i] = leftover[i - (fullSize - left)]
                }
            }else
            {
                // throw read error, give up
            }
        }
    }
}