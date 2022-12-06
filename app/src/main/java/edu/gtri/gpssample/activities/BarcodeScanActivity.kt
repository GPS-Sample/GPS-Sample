package edu.gtri.gpssample.activities

import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import edu.gtri.gpssample.R
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivityBarcodeScanBinding
import edu.gtri.gpssample.network.HeartBeatTransmitter
import edu.gtri.gpssample.network.UDPBroadcastTransmitter
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.InetAddress

class BarcodeScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var role: Role

    private val message = "Hello, World."
    private lateinit var myInetAddress: InetAddress
    private lateinit var serverInetAddress: InetAddress
    private val heartBeatTransmitter = HeartBeatTransmitter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        role = Role.valueOf(intent.getStringExtra("role")!!)

        when (role) {
            Role.Admin -> binding.titleTextView.text = resources.getString( R.string.admin )
            Role.Supervisor -> binding.titleTextView.text = resources.getString( R.string.supervisor )
            Role.Enumerator -> binding.titleTextView.text = resources.getString( R.string.enumerator )
        }

        binding.scanButton.setOnClickListener {
            val intent = Intent(this, CameraXLivePreviewActivity::class.java)
            startActivityForResult( intent, 0 )
            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.continueButton.setOnClickListener {
            startNextActivityForRole( role )
        }

        binding.signOutButton.setOnClickListener {

            heartBeatTransmitter.enabled = false

            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
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

            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)

            if (connectivityManager is ConnectivityManager) {
                connectivityManager.requestNetwork( networkRequest, networkCallback )
            }

//            startNextActivityForRole( role )
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
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

            lifecycleScope.launchWhenStarted {
                whenStarted {
                    heartBeatTransmitter.transmit( myInetAddress, serverInetAddress, message.toByteArray())
                }
            }

            binding.payloadTextView.text = binding.payloadTextView.text.toString() + "\n\nserver addr: " + serverAddress + "\nmy addr: " + myAddress
        }
    }

    private fun startNextActivityForRole( role: Role )
    {
        finish()

        lateinit var intent: Intent

        when (role) {
            Role.Supervisor -> intent = Intent(this, SupervisorSelectRoleActivity::class.java)
            Role.Enumerator -> intent = Intent(this, EnumeratorActivity::class.java)
            else -> {}
        }

        intent.putExtra( "role", role.toString() )

        startActivity( intent )

        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}