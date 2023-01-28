package edu.gtri.gpssample.fragments.ManageStudy

import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentManageStudyBinding
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.network.TCPClient
import edu.gtri.gpssample.network.TCPServer
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.NetworkUser
import edu.gtri.gpssample.network.UDPBroadcastReceiver
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ManageStudyFragment : Fragment(), UDPBroadcastReceiver.UDPBroadcastReceiverDelegate, TCPServer.TCPServerDelegate
{
    private var study: Study? = null
    private var serverInetAddress: InetAddress? = null
    private var _binding: FragmentManageStudyBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private lateinit var studyAdapter: ManageStudyAdapter
    private val tcpServer: TCPServer = TCPServer()
    private val udpBroadcastReceiver: UDPBroadcastReceiver = UDPBroadcastReceiver()
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private lateinit var viewModel: ManageStudyViewModel
    private var networkUsers = ArrayList<NetworkUser>()
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageStudyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentManageStudyBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        val studyId = arguments!!.getInt( Key.kStudyId.toString(), -1);

        if (studyId < 0)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        study = DAO.studyDAO.getStudy( studyId )

        if (study == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id: $studyId not found.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.studyNameTextView.setText( "Study " + study!!.name )

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        studyAdapter = ManageStudyAdapter(networkUsers)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = studyAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity!!)

        val oldWifiAdresses = getWifiApIpAddresses()

        Log.d( "xxx", "searching for old WiFi addresses..." )
        for (address in oldWifiAdresses)
        {
            Log.d( "xxx", address.hostAddress )
        }

        binding.generateBarcodeButton.setOnClickListener {
            try
            {
                binding.generateBarcodeButton.isEnabled = false

                val wifiManager = activity!!.applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback()
                {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                    {
                        super.onStarted(reservation)

                        binding.generateBarcodeButton.visibility = View.GONE
                        binding.imageView.visibility = View.VISIBLE
                        binding.usersOnlineTextView.visibility = View.VISIBLE

                        localOnlyHotspotReservation = reservation

                        val wifiConfiguration = reservation.wifiConfiguration

                        val ssid = wifiConfiguration!!.SSID //reservation.softApConfiguration.ssid
                        val pass = wifiConfiguration!!.preSharedKey //reservation.softApConfiguration.passphrase
                        Toast.makeText(activity!!.applicationContext, "ssid = " + ssid, Toast.LENGTH_SHORT).show()

                        Log.d( "xxx", "ssid = " + ssid );
                        Log.d( "xxx", "pass = " + pass );

                        val newWifiAddresses = getWifiApIpAddresses()

                        if (oldWifiAdresses.isEmpty())
                        {
                            if (!newWifiAddresses.isEmpty())
                            {
                                serverInetAddress = newWifiAddresses[0]
                            }
                        }
                        else
                        {
                            for (oldAddr in oldWifiAdresses)
                            {
                                for (newAddr in newWifiAddresses)
                                {
                                    if (newAddr.hostAddress.equals( oldAddr.hostAddress ))
                                    {
                                        continue
                                    }
                                    else
                                    {
                                        serverInetAddress = newAddr
                                        break;
                                    }
                                }
                            }
                        }

                        if (serverInetAddress != null)
                        {
                            lifecycleScope.launch {
                                tcpServer.beginListening( serverInetAddress!!, this@ManageStudyFragment )
                            }

                            lifecycleScope.launch {
                                udpBroadcastReceiver.beginListening( serverInetAddress!!, this@ManageStudyFragment )
                            }
                        }

                        val jsonObject = JSONObject()
                        jsonObject.put( Key.kSSID.toString(), ssid )
                        jsonObject.put( Key.kPass.toString(), pass )

                        val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
                        qrgEncoder.setColorBlack(Color.WHITE);
                        qrgEncoder.setColorWhite(Color.BLACK);

                        val bitmap = qrgEncoder.bitmap
                        binding.imageView.setImageBitmap(bitmap)
                        (activity!!.application as MainApplication).barcodeBitmap = bitmap
                    }
                }, Handler())
            } catch(e: Exception) {
                Log.d( "xxx", e.printStackTrace().toString())
            }
        }

        Observable
            .interval(2000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( {
                if (dataIsFresh)
                {
                    dataIsFresh = false;
                }
                else
                {
                    networkUsers.clear()
                    studyAdapter.updateUsers( networkUsers )
                }
            },{throwable->
                Log.d( "xxx", throwable.stackTraceToString())
            })
            .addTo( compositeDisposable )
    }

    private var dataIsFresh = false

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    {
        dataIsFresh = true

        val message = String( datagramPacket.data, 0, datagramPacket.length )

        val networkCommand = Json.decodeFromString<NetworkCommand>( message )

        when( networkCommand.command )
        {
            NetworkCommand.NetworkUserCommand -> {
                val networkUser = Json.decodeFromString<NetworkUser>( networkCommand.message )
                val user = networkUsers.find { it.name == networkUser.name }
                if (user == null)
                {
                    networkUsers.add( networkUser )

                    activity!!.runOnUiThread{
                        studyAdapter.updateUsers( networkUsers )
                    }
                }
            }
            NetworkCommand.NetworkRequestConfigCommand -> {
                Log.d( "xxx", "received: NetworkRequestConfigCommand" )
            }
        }
    }

    fun getWifiApIpAddresses(): ArrayList<InetAddress> {
        val list = ArrayList<InetAddress>()
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                if (intf.getName().contains("wlan")) {
                    val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress: InetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress()) {
                            val inetAddr = inetAddress.hostAddress!!
                            if (!inetAddr.contains(":")) {
                                list.add( inetAddress)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("xxx", ex.toString())
        }
        return list
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_study, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_study -> {

                val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestConfigCommand, "" )
                val networkCommandMessage = Json.encodeToString( networkCommand )
                lifecycleScope.launch {
                    TCPClient().write( serverInetAddress!!.hostAddress, networkCommandMessage )
                }
                return true
            }

            R.id.action_delete_study -> {
                DAO.studyDAO.deleteStudy( study!! )
                findNavController().popBackStack()
                return true
            }
        }

        return false
    }

    override fun didReceiveMessage(message: String )
    {
        Log.d( "xxx", "didReceiveMessage: $message" )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        tcpServer.stopReceiving()
        udpBroadcastReceiver.stopReceiving()

        localOnlyHotspotReservation?.close()

        _binding = null
    }
}