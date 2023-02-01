package edu.gtri.gpssample.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPBroadcaster
{
    private var port = 61234
    private var receiverEnabled = false
    private var transmitterEnabled = false
    private var datagramSocket: DatagramSocket? = null
    private lateinit var delegate: UDPBroadcasterDelegate

    //--------------------------------------------------------------------------
    interface UDPBroadcasterDelegate
    {
        fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    }

    //--------------------------------------------------------------------------
    fun transmitterIsEnabled() : Boolean
    {
        return transmitterEnabled
    }

    //--------------------------------------------------------------------------
    fun receiverIsEnabled() : Boolean
    {
        return receiverEnabled
    }

    //--------------------------------------------------------------------------
    suspend fun transmit( myInetAddress: InetAddress, broadcastInetAddress: InetAddress, message: String )
    {
        val backgroundResult = withContext(Dispatchers.Default)
        {
            if (datagramSocket == null)
            {
                datagramSocket = DatagramSocket(port,myInetAddress)
                datagramSocket!!.broadcast = true
                datagramSocket!!.reuseAddress = true
            }

            Log.d( "xxx", "transmitting command on $myInetAddress:$port to ${broadcastInetAddress}:$port" )

            datagramSocket!!.send( DatagramPacket( message.toByteArray(), message.length, broadcastInetAddress, port ))
        }
    }

    //--------------------------------------------------------------------------
    suspend fun beginReceiving( inetAddress: InetAddress, delegate: UDPBroadcasterDelegate )
    {
        this.delegate = delegate

        val backgroundResult = withContext(Dispatchers.Default)
        {
            if (datagramSocket == null)
            {
                datagramSocket = DatagramSocket(port)
                datagramSocket!!.broadcast = true
                datagramSocket!!.reuseAddress = true
            }

            Log.d( "xxx", "waiting for UDP messages on $inetAddress:$port..." )

            receiverEnabled = true

            while (receiverEnabled)
            {
                try {
                    val buf = ByteArray(4096)
                    val datagramPacket = DatagramPacket(buf, buf.size)

                    datagramSocket!!.receive(datagramPacket)

                    val fromAddress = datagramPacket.address

                    if (fromAddress != inetAddress)
                    {
                        delegate.didReceiveDatagramPacket( datagramPacket )
                    }
                }
                catch (ex: Exception)
                {
                    Log.d( "xxx", ex.stackTraceToString())
                    stopReceiving()
                }
            }

            Log.d( "xxx", "stopped waiting for data" )
        }
    }

    //--------------------------------------------------------------------------
    suspend fun beginTransmitting( myInetAddress: InetAddress, broadcastInetAddress: InetAddress, message: String )
    {
        Log.d( "xxx", "begin transmitting on $myInetAddress:$port to ${broadcastInetAddress}:$port" )

        val backgroundResult = withContext(Dispatchers.IO)
        {
            if (datagramSocket == null)
            {
                datagramSocket = DatagramSocket(port,myInetAddress)
                datagramSocket!!.broadcast = true
                datagramSocket!!.reuseAddress = true
            }

            val datagramPacket = DatagramPacket( message.toByteArray(), message.length, broadcastInetAddress, port)

            delay(1000)

            transmitterEnabled = true

            while( transmitterEnabled )
            {
                try {
                    datagramSocket!!.send( datagramPacket )
                    delay(1000)
                }
                catch( ex: Exception )
                {
                    Log.d( "xxx", ex.stackTraceToString())
                    stopTransmitting()
                }
            }

            Log.d( "xxx", "finished transmitting data" )
        }
    }

    //--------------------------------------------------------------------------
    fun stopTransmitting()
    {
        transmitterEnabled = false
        if (datagramSocket != null && !transmitterEnabled && !receiverEnabled)
        {
            datagramSocket!!.close()
        }
    }

    //--------------------------------------------------------------------------
    fun stopReceiving()
    {
        receiverEnabled = false
        if (datagramSocket != null && !transmitterEnabled && !receiverEnabled)
        {
            datagramSocket!!.close()
        }
    }
}