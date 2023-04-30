package edu.gtri.gpssample.fragments.manage_enumeration_team
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentManageEnumerationTeamBinding
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.UDPBroadcaster
import edu.gtri.gpssample.network.models.*
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONObject
import java.net.DatagramPacket
import kotlin.collections.ArrayList

class ManageEnumerationTeamFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate, GPSSampleWifiManager.GPSSampleWifiManagerDelegate, InputDialog.InputDialogDelegate
{
    private lateinit var team: Team
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
//    private lateinit var studyAdapter: ManageEnumerationAreaAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var dataIsFresh = false
    private var _binding: FragmentManageEnumerationTeamBinding? = null
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
        setHasOptionsMenu( true )

        _binding = FragmentManageEnumerationTeamBinding.inflate(inflater, container, false)

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

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        binding.teamNameTextView.setText( team.name )

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

        binding.generateQrButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            gpsSamleWifiManager = GPSSampleWifiManager( this )
            gpsSamleWifiManager!!.startHotSpot()
        }

        binding.exportButton.setOnClickListener {
        }

        binding.performEnumerationButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationTeamFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun didStartHotspot( ssid: String, pass: String )
    {
        binding.progressBar.visibility = View.GONE

        binding.imageView.visibility = View.VISIBLE
        binding.usersOnlineTextView.visibility = View.VISIBLE

        val jsonObject = JSONObject()
        jsonObject.put( Keys.kSSID.toString(), ssid )
        jsonObject.put( Keys.kPass.toString(), pass )
        jsonObject.put( Keys.kTeam_id.toString(), team.id )
//        jsonObject.put( Keys.kStudy_uuid.toString(), study.uuid )
        jsonObject.put( Keys.kEnumArea_id.toString(), enumArea.id )
//        jsonObject.put( Keys.kConfig_uuid.toString(), study.config_uuid )

        val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
        qrgEncoder.setColorBlack(Color.WHITE);
        qrgEncoder.setColorWhite(Color.BLACK);

        val bitmap = qrgEncoder.bitmap
        binding.imageView.setImageBitmap(bitmap)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_manage_enumeration_team, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_team -> {
                InputDialog( activity!!, "Team Name", team.name, this )
            }

            R.id.action_delete_team -> {
                DAO.teamDAO.deleteTeam( team )
                findNavController().popBackStack()
                return true
            }
        }

        return false
    }

    override fun didEnterText( name: String )
    {
        team.name = name
        DAO.teamDAO.updateTeam( team )
        binding.teamNameTextView.setText( team.name )
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

//                    activity!!.runOnUiThread{
//                        studyAdapter.updateUsers( users )
//                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        gpsSamleWifiManager?.let {
            it.stopHotSpot()
        }

        _binding = null
    }
}