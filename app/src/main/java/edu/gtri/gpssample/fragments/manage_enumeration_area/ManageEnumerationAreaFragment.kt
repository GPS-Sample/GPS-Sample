package edu.gtri.gpssample.fragments.ManageEnumerationArea

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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentManageEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.fragments.manage_enumeration_teams.ManageEnumerationTeamsAdapter
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.UDPBroadcaster
import edu.gtri.gpssample.network.models.*
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
class ManageEnumerationAreaFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate, GPSSampleWifiManager.GPSSampleWifiManagerDelegate, InputDialog.InputDialogDelegate
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var manageEnumerationAreaAdapter: ManageEnumerationAreaAdapter

    private var dataIsFresh = false
    private var _binding: FragmentManageEnumerationAreaBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private var users = ArrayList<User>()
    private var gpsSamleWifiManager: GPSSampleWifiManager? = null
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
//        setHasOptionsMenu( true )

        _binding = FragmentManageEnumerationAreaBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        enumArea.id?.let {
            val teams = DAO.teamDAO.getTeams( it )
            manageEnumerationAreaAdapter = ManageEnumerationAreaAdapter( teams )
        }

        manageEnumerationAreaAdapter.didSelectTeam = this::didSelectTeam

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageEnumerationAreaAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.titleTextView.text = enumArea.name + " Teams"

        binding.addButton.setOnClickListener {
            InputDialog( activity!!, "Enter Team Name", null, this )
        }

//        binding.studyNameTextView.setText( enumArea.name )

//        studyAdapter = ManageEnumerationAreaAdapter(users)
//
//        binding.recyclerView.itemAnimator = DefaultItemAnimator()
//        binding.recyclerView.adapter = studyAdapter
//        binding.recyclerView.layoutManager = LinearLayoutManager(activity!!)

//        Observable
//            .interval(2000, TimeUnit.MILLISECONDS)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe( {
//                if (dataIsFresh)
//                {
//                    dataIsFresh = false;
//                }
//                else
//                {
//                    users.clear()
//                    studyAdapter.updateUsers( users )
//                }
//            },{throwable->
//                Log.d( "xxx", throwable.stackTraceToString())
//            })
//            .addTo( compositeDisposable )

        binding.generateBarcodeButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            gpsSamleWifiManager = GPSSampleWifiManager( this )
            gpsSamleWifiManager!!.startHotSpot()
        }

//        binding.superviseButton.setOnClickListener {
//            findNavController().navigate(R.id.action_navigate_to_ManageEnumerationTeamsFragment)
//        }

        binding.finishButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectTeam( team: Team)
    {
        sharedViewModel.teamViewModel.setCurrentTeam( team )

        findNavController().navigate( R.id.action_navigate_to_ManageEnumerationTeamFragment )
    }

    override fun didStartHotspot( ssid: String, pass: String )
    {
        binding.progressBar.visibility = View.GONE

//        binding.imageView.visibility = View.VISIBLE
//        binding.usersOnlineTextView.visibility = View.VISIBLE

        val jsonObject = JSONObject()
        jsonObject.put( Keys.kSSID.toString(), ssid )
        jsonObject.put( Keys.kPass.toString(), pass )
        jsonObject.put( Keys.kStudy_uuid.toString(), study.uuid )
        jsonObject.put( Keys.kEnumArea_id.toString(), enumArea.id )
        jsonObject.put( Keys.kConfig_uuid.toString(), study.config_uuid )

//        val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
//        qrgEncoder.setColorBlack(Color.WHITE);
//        qrgEncoder.setColorWhite(Color.BLACK);

//        val bitmap = qrgEncoder.bitmap
//        binding.imageView.setImageBitmap(bitmap)
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
//    {
//        super.onCreateOptionsMenu(menu, inflater)
//
//        inflater.inflate(R.menu.menu_manage_enumeration_area, menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//
//        when (item.itemId) {
//            R.id.action_supervise_area -> {
//                val bundle = Bundle()
//                bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
//                bundle.putInt( Keys.kEnumArea_id.toString(), enumArea.id!! )
//                findNavController().navigate(R.id.action_navigate_to_ManageEnumerationTeamsFragment, bundle)
//
//                return true
//            }
//
//        }
//
//        return false
//    }

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    {
        dataIsFresh = true

        val networkCommand = NetworkCommand.unpack( datagramPacket.data, datagramPacket.length )

        when( networkCommand.command )
        {
            NetworkCommand.NetworkUserRequest -> {
                val user = User.unpack( networkCommand.message )

                if (users.find { it.name == user.name } == null)
                {
                    users.add( user )

//                    activity!!.runOnUiThread{
//                        studyAdapter.updateUsers( users )
//                    }
                }
            }
        }
    }

    override fun didEnterText( name: String )
    {
        enumArea.id?.let { enum_area_id ->
            val team = Team( enum_area_id, name )
            DAO.teamDAO.createTeam( team )
            manageEnumerationAreaAdapter.updateTeams( DAO.teamDAO.getTeams( enum_area_id ))
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        if (gpsSamleWifiManager != null)
        {
            gpsSamleWifiManager!!.stopHotSpot()
        }

        _binding = null
    }
}