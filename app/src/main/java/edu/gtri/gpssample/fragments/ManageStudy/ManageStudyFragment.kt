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
import edu.gtri.gpssample.models.Study
import edu.gtri.gpssample.network.UDPBroadcastReceiver
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit

class ManageStudyFragment : Fragment(), UDPBroadcastReceiver.UDPBroadcastReceiverDelegate
{
    private var study: Study? = null
    private var _binding: FragmentManageStudyBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private lateinit var studyAdapter: ManageStudyAdapter
    private val udpBroadcastReceiver: UDPBroadcastReceiver = UDPBroadcastReceiver()
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private lateinit var viewModel: ManageStudyViewModel

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
                Toast.makeText(activity!!.applicationContext, "StudyFragment", Toast.LENGTH_SHORT).show()
            }
        }

        val users = DAO.userDAO.getUsers()

        studyAdapter = ManageStudyAdapter(users)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = studyAdapter
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

                        localOnlyHotspotReservation = reservation

                        val wifiConfiguration = reservation.wifiConfiguration

                        val ssid = wifiConfiguration!!.SSID //reservation.softApConfiguration.ssid
                        val pass = wifiConfiguration!!.preSharedKey //reservation.softApConfiguration.passphrase
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
                                    udpBroadcastReceiver.beginListening( inetAddress!!, this@ManageStudyFragment )
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

        Observable
            .interval(2000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( {
                if (dataIsFresh)
                {
                    dataIsFresh = false;
                }
//                else if ((activity!!.application as MainApplication).users[0].isOnline)
//                {
//                    (activity!!.application as MainApplication).users[0].isOnline = false
//                    studyAdapter.updateUsers( (activity!!.application as MainApplication).users )
//                }
            },{throwable->
                Log.d( "xxx", throwable.stackTraceToString())
            })
            .addTo( compositeDisposable )
    }

    private var dataIsFresh = false

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket)
    {
        dataIsFresh = true

//        (activity!!.application as MainApplication).users[0].isOnline = true
//
//        activity!!.runOnUiThread{
//            studyAdapter.updateUsers( (activity!!.application as MainApplication).users )
//        }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_study, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_study -> {
                val bundle = Bundle()

                bundle.putInt( Key.kStudyId.toString(), study!!.id )
                bundle.putInt( Key.kConfigId.toString(), study!!.configId )

                findNavController().navigate( R.id.action_navigate_to_CreateStudyFragment, bundle )
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

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        udpBroadcastReceiver.stopReceiving()

        localOnlyHotspotReservation?.close()

        _binding = null
    }
}