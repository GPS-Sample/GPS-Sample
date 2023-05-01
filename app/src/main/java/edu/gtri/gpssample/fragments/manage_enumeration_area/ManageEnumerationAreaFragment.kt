package edu.gtri.gpssample.fragments.ManageEnumerationArea

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import edu.gtri.gpssample.fragments.manage_enumeration_area.UsersAdapter
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.UDPBroadcaster
import edu.gtri.gpssample.network.models.*
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.net.DatagramPacket
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
class ManageEnumerationAreaFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate, GPSSampleWifiManager.GPSSampleWifiManagerDelegate, InputDialog.InputDialogDelegate
{
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var teamsAdapter: TeamsAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var gpsSampleWifiManager: GPSSampleWifiManager

    private var dataIsFresh = false
    private var _binding: FragmentManageEnumerationAreaBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private var users = ArrayList<User>()
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
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
            teamsAdapter = TeamsAdapter( teams )
        }

        teamsAdapter.didSelectTeam = this::didSelectTeam

        binding.teamRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.teamRecyclerView.adapter = teamsAdapter
        binding.teamRecyclerView.layoutManager = LinearLayoutManager(activity)

        binding.titleTextView.text = enumArea.name + " Teams"

        binding.addButton.setOnClickListener {
            InputDialog( activity!!, "Enter Team Name", null, this )
        }

        usersAdapter = UsersAdapter(users)

        binding.usersRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.usersRecyclerView.adapter = usersAdapter
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(activity!!)

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
                    usersAdapter.updateUsers( users )
                }
            },{throwable->
                Log.d( "xxx", throwable.stackTraceToString())
            })
            .addTo( compositeDisposable )

        binding.generateQrButton.setOnClickListener {
            if (!this::gpsSampleWifiManager.isInitialized)
            {
                binding.progressBar.visibility = View.VISIBLE
                gpsSampleWifiManager = GPSSampleWifiManager( this )
                gpsSampleWifiManager.startHotSpot()
            }
        }

        binding.exportButton.setOnClickListener {
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

        binding.qrImageView.visibility = View.VISIBLE
        binding.usersOnlineTextView.visibility = View.VISIBLE

        val jsonObject = JSONObject()
        jsonObject.put( Keys.kSSID.toString(), ssid )
        jsonObject.put( Keys.kPass.toString(), pass )
        jsonObject.put( Keys.kEnumArea_id.toString(), enumArea.id )

        val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.qrImageView.width )
        qrgEncoder.setColorBlack(Color.WHITE);
        qrgEncoder.setColorWhite(Color.BLACK);

        val bitmap = qrgEncoder.bitmap
        binding.qrImageView.setImageBitmap(bitmap)
    }

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

                    activity!!.runOnUiThread{
                        usersAdapter.updateUsers( users )
                    }
                }
            }
        }
    }

    override fun didEnterText( name: String )
    {
        enumArea.id?.let { id ->
            val team = Team( id, name )
            DAO.teamDAO.createOrUpdateTeam( team )
            teamsAdapter.updateTeams( DAO.teamDAO.getTeams( id ))
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        if (this::gpsSampleWifiManager.isInitialized)
        {
            gpsSampleWifiManager.stopHotSpot()
        }

        _binding = null
    }
}