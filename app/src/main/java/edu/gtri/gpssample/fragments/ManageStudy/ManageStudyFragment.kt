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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.databinding.FragmentManageStudyBinding
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.network.UDPBroadcaster
import edu.gtri.gpssample.network.models.*
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

class ManageStudyFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate
{
    private var dataIsFresh = false
    private lateinit var study: Study
    private var serverInetAddress: InetAddress? = null
    private var broadcastInetAddress: InetAddress? = null
    private var _binding: FragmentManageStudyBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private lateinit var studyAdapter: ManageStudyAdapter
    private val udpBroadcaster: UDPBroadcaster = UDPBroadcaster()
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private lateinit var viewModel: ManageStudyViewModel
    private var users = ArrayList<User>()

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

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        binding.progressBar.visibility = View.VISIBLE

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        val study_uuid = arguments!!.getString( Key.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.studyDAO.getStudy( study_uuid )?.let { study ->
            this.study = study
        }

        if (!this::study.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $study_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.studyNameTextView.setText( "Study " + study.name )

        studyAdapter = ManageStudyAdapter(users)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = studyAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity!!)

        generateBarcode()

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
                    users.clear()
                    studyAdapter.updateUsers( users )
                }
            },{throwable->
                Log.d( "xxx", throwable.stackTraceToString())
            })
            .addTo( compositeDisposable )
    }

    fun generateBarcode()
    {
        try
        {
            val oldWifiAdresses = getWifiApIpAddresses()

            val wifiManager = activity!!.applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback()
            {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                {
                    super.onStarted(reservation)

                    binding.progressBar.visibility = View.GONE

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
                        val components = serverInetAddress!!.hostAddress.split(".")
                        val broadcast_address = components[0] + "." + components[1] + "." + components[2] + ".255"
                        broadcastInetAddress = InetAddress.getByName( broadcast_address )

                        Log.d( "xxx", broadcast_address )

                        lifecycleScope.launch {
                            udpBroadcaster.beginReceiving( serverInetAddress!!, this@ManageStudyFragment )
                        }
                    }

                    val jsonObject = JSONObject()
                    jsonObject.put( Key.kSSID.toString(), ssid )
                    jsonObject.put( Key.kPass.toString(), pass )
                    jsonObject.put( Key.kStudy_uuid.toString(), study.uuid )
                    jsonObject.put( Key.kConfig_uuid.toString(), study.config_uuid )

                    val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
                    qrgEncoder.setColorBlack(Color.WHITE);
                    qrgEncoder.setColorWhite(Color.BLACK);

                    val bitmap = qrgEncoder.bitmap
                    binding.imageView.setImageBitmap(bitmap)
                    (activity!!.application as MainApplication).barcodeBitmap = bitmap
                }
            }, Handler())
        } catch(e: Exception) {
            Log.d( "xxx", e.stackTraceToString())
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
            }

            R.id.action_delete_study -> {
                DAO.studyDAO.deleteStudy( study )
                findNavController().popBackStack()
                return true
            }
        }

        return false
    }

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    {
        dataIsFresh = true

        val networkCommand = NetworkCommand.unpack( datagramPacket.data, datagramPacket.length )

        if (networkCommand.command != NetworkCommand.NetworkUserCommand)
        {
            Log.d( "xxx", "Received network command: " + networkCommand.command )
        }

        when( networkCommand.command )
        {
            NetworkCommand.NetworkUserCommand -> {
                val user = User.unpack( networkCommand.message )

                if (users.find { it.name == user.name } == null)
                {
                    users.add( user )

                    activity!!.runOnUiThread{
                        studyAdapter.updateUsers( users )
                    }
                }
            }

            NetworkCommand.NetworkRequestConfigCommand -> {
                lifecycleScope.launch {
                    DAO.configDAO.getConfig( networkCommand.parm1 )?.let {
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRequestConfigResponse, networkCommand.uuid, "", "", it.pack())
                        udpBroadcaster.transmit( serverInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    } ?: Toast.makeText( activity!!.applicationContext, "config<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                }
            }

            NetworkCommand.NetworkRequestStudyCommand -> {
                lifecycleScope.launch {
                    DAO.studyDAO.getStudy( networkCommand.parm1 )?.let {
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRequestStudyResponse, networkCommand.uuid, "", "", it.pack())
                        udpBroadcaster.transmit( serverInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    } ?: Toast.makeText( activity!!.applicationContext, "study<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                }
            }

            NetworkCommand.NetworkRequestFieldsCommand -> {
                lifecycleScope.launch {
                    val fields = DAO.fieldDAO.getFields( networkCommand.parm1 )
                    if (fields.isEmpty())
                    {
                        Toast.makeText( activity!!.applicationContext, "fields<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        val networkFields = NetworkFields( fields )
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRequestFieldsResponse, networkCommand.uuid, "", "", networkFields.pack())
                        udpBroadcaster.transmit( serverInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    }
                }
            }

            NetworkCommand.NetworkRequestRulesCommand -> {
                lifecycleScope.launch {
                    val rules = DAO.ruleDAO.getRules( networkCommand.parm1 )
                    if (rules.isEmpty())
                    {
                        Toast.makeText( activity!!.applicationContext, "rules<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        val networkRules = NetworkRules( rules )
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRequestRulesResponse, networkCommand.uuid, "", "", networkRules.pack())
                        udpBroadcaster.transmit( serverInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    }
                }
            }

            NetworkCommand.NetworkRequestFiltersCommand -> {
                lifecycleScope.launch {
                    val filters = DAO.filterDAO.getFilters( networkCommand.parm1 )
                    if (filters.isEmpty())
                    {
                        Toast.makeText( activity!!.applicationContext, "filters<${networkCommand.parm1} not found.>", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        val networkFilters = NetworkFilters( filters )
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRequestFiltersResponse, networkCommand.uuid, "", "", networkFilters.pack())
                        udpBroadcaster.transmit( serverInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    }
                }
            }

            NetworkCommand.NetworkRequestFilterRulesCommand -> {
                lifecycleScope.launch {
                    val filterRules = DAO.filterRuleDAO.getFilterRules( networkCommand.parm1 )
                    if (filterRules.isEmpty())
                    {
                        Toast.makeText( activity!!.applicationContext, "study<${networkCommand.parm1} does not contain any FilterRules.>", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        val networkFilterRules = NetworkFilterRules( filterRules )
                        val networkResponse = NetworkCommand( NetworkCommand.NetworkRequestFilterRulesResponse, networkCommand.uuid, "", "", networkFilterRules.pack())
                        udpBroadcaster.transmit( serverInetAddress!!, broadcastInetAddress!!, networkResponse.pack())
                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        udpBroadcaster.stopReceiving()

        localOnlyHotspotReservation?.close()

        _binding = null
    }
}