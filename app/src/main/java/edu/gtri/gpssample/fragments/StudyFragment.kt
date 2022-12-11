package edu.gtri.gpssample.fragments

import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.adapters.OnlineStatusAdapter
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentStudyBinding
import edu.gtri.gpssample.models.UserModel
import edu.gtri.gpssample.network.UDPBroadcastReceiver
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit

class StudyFragment : Fragment(), UDPBroadcastReceiver.UDPBroadcastReceiverDelegate
{
    private var _binding: FragmentStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var onlineStatusAdapter: OnlineStatusAdapter
    private val udpBroadcastReceiver: UDPBroadcastReceiver = UDPBroadcastReceiver()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentStudyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if ((activity!!.application as MainApplication).users.isEmpty())
        {
            var user1 = UserModel()
            user1.name = "Russell"
            (activity!!.application as MainApplication).users.add( user1 )

            var user2 = UserModel()
            user2.name = "Brian"
            (activity!!.application as MainApplication).users.add( user2 )

            var user3 = UserModel()
            user3.name = "Megan"
            (activity!!.application as MainApplication).users.add( user3 )
        }

        onlineStatusAdapter = OnlineStatusAdapter((activity!!.application as MainApplication).users)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = onlineStatusAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity!!)

        val oldWifiAdresses = getWifiApIpAddresses()

        binding.generateBarcodeButton.setOnClickListener {
            try
            {
                val wifiManager = activity!!.applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback()
                {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                    {
                        super.onStarted(reservation)

                        val ssid = reservation.softApConfiguration.ssid
                        val pass = reservation.softApConfiguration.passphrase
                        Toast.makeText(activity!!.applicationContext, "ssid = " + ssid, Toast.LENGTH_SHORT).show()

                        Log.d( "xxx", "ssid = " + ssid );
                        Log.d( "xxx", "pass = " + pass );

                        val newWifiAddresses = getWifiApIpAddresses()

                        var inetAddress: InetAddress? = null

                        if (oldWifiAdresses.isEmpty())
                        {
                            if (!newWifiAddresses.isEmpty())
                            {
                                inetAddress = newWifiAddresses[0]
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
                                        inetAddress = newAddr
                                        break;
                                    }
                                }
                            }
                        }

                        if (inetAddress != null)
                        {
                            Log.d( "xxx", "inetAddress = " + inetAddress!!.hostAddress )

                            lifecycleScope.launchWhenStarted {
                                whenStarted {
                                    udpBroadcastReceiver.beginListening( inetAddress!!, this@StudyFragment )
                                }
                            }
                        }

                        val jsonObject = JSONObject()
                        jsonObject.put( "ssid", ssid )
                        jsonObject.put( "pass", pass )

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

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        Observable
            .interval(2000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (dataIsFresh)
                {
                    dataIsFresh = false;
                }
                else if ((activity!!.application as MainApplication).users[0].isOnline)
                {
                    (activity!!.application as MainApplication).users[0].isOnline = false
                    onlineStatusAdapter.updateUsers( (activity!!.application as MainApplication).users )
                }
            }
    }

    private var dataIsFresh = false

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket)
    {
        dataIsFresh = true

        (activity!!.application as MainApplication).users[0].isOnline = true

        activity!!.runOnUiThread{
            onlineStatusAdapter.updateUsers( (activity!!.application as MainApplication).users )
        }

        Log.d( "xxx", "received : " + datagramPacket.length )
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
                                Log.d("xxx", inetAddress.getHostAddress())
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
}