package edu.gtri.gpssample.fragments

import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.activities.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.databinding.FragmentBarcodeScanBinding
import edu.gtri.gpssample.network.HeartBeatTransmitter
import org.json.JSONObject
import java.net.InetAddress

class BarcodeScanFragment : Fragment()
{
    private var _binding: FragmentBarcodeScanBinding? = null
    private val binding get() = _binding!!
    private val message = "Hello, World."
    private lateinit var myInetAddress: InetAddress
    private lateinit var serverInetAddress: InetAddress
    private val heartBeatTransmitter = HeartBeatTransmitter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentBarcodeScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scanButton.setOnClickListener {
            val intent = Intent(activity!!, CameraXLivePreviewActivity::class.java)
            startActivityForResult( intent, 0 )
        }

        binding.signOutButton.setOnClickListener {

            heartBeatTransmitter.stopTransmitting()

            val connectivityManager = activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback( networkCallback )

            findNavController().navigate(R.id.action_navigate_to_SignInSignUpFragment)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == ResultCode.BarcodeScanned.value)
        {
            val payload = data!!.getStringExtra( "value" )

            val jsonObject = JSONObject( payload );

            Log.d( "xxx", jsonObject.toString(2))
            binding.payloadTextView.text = jsonObject.toString(2)

            val builder = WifiNetworkSpecifier.Builder()
            builder.setSsid( jsonObject.getString("ssid" ));
            builder.setWpa2Passphrase(jsonObject.getString("pass" ))
            val wifiNetworkSpecifier = builder.build()

            val networkRequestBuilder = NetworkRequest.Builder()
            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

            val networkRequest = networkRequestBuilder.build()

            val connectivityManager = activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork( networkRequest, networkCallback )
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
            super.onCapabilitiesChanged(network, networkCapabilities)

            if (networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED) && !heartBeatTransmitter.isEnabled())
            {
                lifecycleScope.launchWhenStarted {
                    whenStarted {
                        heartBeatTransmitter.beginTransmitting( myInetAddress, serverInetAddress, message.toByteArray())
                    }
                }
            }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
            super.onLinkPropertiesChanged(network, linkProperties)

            var serverAddress = linkProperties.dhcpServerAddress.toString().substring(1)
            // linkProperties.linkAddresses[0] is the IPV6 address
            val myAddress = linkProperties.linkAddresses[1].toString().substring(0, linkProperties.linkAddresses[1].toString().length-3)

            Log.d( "xxx", "server addr: " + serverAddress)
            Log.d( "xxx", "my addr: " + myAddress )

            val components = serverAddress.split(".")
            serverAddress = components[0] + "." + components[1] + "." + components[2] + ".255"

            myInetAddress = InetAddress.getByName( myAddress )
            serverInetAddress = InetAddress.getByName( serverAddress )

            binding.payloadTextView.text = binding.payloadTextView.text.toString() + "\n\nserver addr: " + serverAddress + "\nmy addr: " + myAddress
        }
    }
}