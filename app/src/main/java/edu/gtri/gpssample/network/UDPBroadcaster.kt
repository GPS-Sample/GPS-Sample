package edu.gtri.gpssample.network

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.network.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPBroadcaster
{
    private var port = 61234
    private var datagramSocket: DatagramSocket? = null
    private lateinit var delegate: UDPBroadcasterDelegate

    constructor()
    {
        datagramSocket = DatagramSocket(port)
        datagramSocket!!.broadcast = true
        datagramSocket!!.reuseAddress = true
    }

    //--------------------------------------------------------------------------
    interface UDPBroadcasterDelegate
    {
        fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    }

    //--------------------------------------------------------------------------
    suspend fun transmit( myInetAddress: InetAddress, broadcastInetAddress: InetAddress, message: String )
    {
        val backgroundResult = withContext(Dispatchers.Default)
        {
            Log.d( "xxx", "transmitting command on $myInetAddress:$port to ${broadcastInetAddress}:$port" )

            datagramSocket!!.send( DatagramPacket( message.toByteArray(), message.length, broadcastInetAddress, port ))
        }
    }

    //--------------------------------------------------------------------------
    suspend fun beginReceiving( myInetAddress: InetAddress, broadcastInetAddress: InetAddress, delegate: UDPBroadcasterDelegate )
    {
        this.delegate = delegate

        val backgroundResult = withContext(Dispatchers.Default)
        {
            Log.d( "xxx", "waiting for UDP messages on $myInetAddress:$port..." )

            var enabled = true

            while (enabled)
            {
                try {
                    val buf = ByteArray(4096)
                    val datagramPacket = DatagramPacket(buf, buf.size)

                    datagramSocket!!.receive(datagramPacket)

                    val fromAddress = datagramPacket.address

                    if (fromAddress != myInetAddress)
                    {
                        didReceiveDatagramPacket( datagramPacket, myInetAddress, broadcastInetAddress )
                    }
                }
                catch (ex: Exception)
                {
                    Log.d( "xxx", ex.stackTraceToString())
                    enabled = false
                    closeSocket()
                }
            }

            Log.d( "xxx", "stopped waiting for data" )
        }
    }

    //--------------------------------------------------------------------------
    fun didReceiveDatagramPacket( datagramPacket: DatagramPacket, myInetAddress: InetAddress, broadcastInetAddress: InetAddress )
    {
        val fragment = delegate as Fragment

        val networkCommand = NetworkCommand.unpack( datagramPacket.data, datagramPacket.length )

        if (networkCommand.command != NetworkCommand.NetworkUserRequest)
        {
            Log.d( "xxx", "Received network command: " + networkCommand.command )
        }

        when( networkCommand.command )
        {
            NetworkCommand.NetworkConfigRequest -> {

                fragment.lifecycleScope.launch {
                    DAO.configDAO.getConfig( networkCommand.parm1 )?.let {
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkConfigResponse, networkCommand.uuid, "", "", it.pack())
                        transmit( myInetAddress, broadcastInetAddress, networkResponse.pack())
                    } ?: Toast.makeText( fragment.activity!!.applicationContext, "config<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                }
            }

            NetworkCommand.NetworkStudyRequest -> {
                fragment.lifecycleScope.launch {
                    DAO.studyDAO.getStudy( networkCommand.parm1 )?.let {
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkStudyResponse, networkCommand.uuid, "", "", it.pack())
                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    } ?: Toast.makeText( fragment.activity!!.applicationContext, "study<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                }
            }

            NetworkCommand.NetworkFieldsRequest -> {
                fragment.lifecycleScope.launch {
                  //  val fields = DAO.fieldDAO.getFields( networkCommand.parm1 )
//                    if (fields.isEmpty())
//                    {
//                        Toast.makeText( fragment.activity!!.applicationContext, "fields<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
//                    }
//                    else
//                    {
//                        val networkFields = NetworkFields( fields )
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkFieldsResponse, networkCommand.uuid, "", "", networkFields.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    }
                }
            }

            NetworkCommand.NetworkRulesRequest -> {
                fragment.lifecycleScope.launch {
                   // val rules = DAO.ruleDAO.getRules( networkCommand.parm1 )
//                    if (rules.isEmpty())
//                    {
//                        Toast.makeText( fragment.activity!!.applicationContext, "rules<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
//                    }
//                    else
//                    {
//                        val networkRules = NetworkRules( rules )
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRulesResponse, networkCommand.uuid, "", "", networkRules.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    }
                }
            }

            NetworkCommand.NetworkFiltersRequest -> {
//                fragment.lifecycleScope.launch {
//                    val filters = DAO.filterDAO.getFilters( networkCommand.parm1 )
//                    if (filters.isEmpty())
//                    {
//                        Toast.makeText( fragment.activity!!.applicationContext, "filters<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
//                    }
//                    else
//                    {
//                        val networkFilters = NetworkFilters( filters )
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkFiltersResponse, networkCommand.uuid, "", "", networkFilters.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    }
//                }
            }

            NetworkCommand.NetworkFilterRulesRequest -> {
                fragment.lifecycleScope.launch {
//                    val filterRules = DAO.filterRuleDAO.getFilterRules( networkCommand.parm1 )
//                    if (filterRules.isEmpty())
//                    {
//                        Toast.makeText( fragment.activity!!.applicationContext, "study<${networkCommand.parm1} does not contain any FilterRules.>", Toast.LENGTH_SHORT).show()
//                    }
//                    else
//                    {
//                        val networkFilterRules = NetworkFilterRules( filterRules )
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkFilterRulesResponse, networkCommand.uuid, "", "", networkFilterRules.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    }
                }
            }

            NetworkCommand.NetworkEnumAreaRequest -> {
//                fragment.lifecycleScope.launch {
//                    DAO.enumAreaDAO.getEnumArea( networkCommand.parm1 )?.let {
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkEnumAreaResponse, networkCommand.uuid, "", "", it.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    } ?: Toast.makeText( fragment.activity!!.applicationContext, "enum_area<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
//                }
            }

            NetworkCommand.NetworkRectangleRequest -> {
//                fragment.lifecycleScope.launch {
//                    DAO.rectangleDAO.getRectangle( networkCommand.parm1 )?.let {
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRectangleResponse, networkCommand.uuid, "", "", it.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    } ?: Toast.makeText( fragment.activity!!.applicationContext, "rectangle<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
//                }
            }

            NetworkCommand.NetworkTeamRequest -> {
//                fragment.lifecycleScope.launch {
//                    DAO.teamDAO.getTeam( networkCommand.parm1 )?.let {
//                        val networkResponse = NetworkCommand( NetworkCommand.NetworkTeamResponse, networkCommand.uuid, "", "", it.pack())
//                        transmit( myInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
//                    } ?: Toast.makeText( fragment.activity!!.applicationContext, "team<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
//                }
            }
        }

        delegate.didReceiveDatagramPacket( datagramPacket )
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

            var enabled = true

            while( enabled )
            {
                try {
                    datagramSocket!!.send( datagramPacket )
                    delay(1000)
                }
                catch( ex: Exception )
                {
                    Log.d( "xxx", ex.stackTraceToString())
                    enabled = false
                    closeSocket()
                }
            }

            Log.d( "xxx", "finished transmitting data" )
        }
    }

    //--------------------------------------------------------------------------
    fun closeSocket()
    {
        if (datagramSocket != null)
        {
            datagramSocket!!.close()
            datagramSocket = null
        }
    }
}
