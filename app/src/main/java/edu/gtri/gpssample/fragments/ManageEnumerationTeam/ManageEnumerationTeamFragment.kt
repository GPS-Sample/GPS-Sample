package edu.gtri.gpssample.fragments.ManageEnumerationTeam

import edu.gtri.gpssample.fragments.ManageEnumerationArea.ManageEnumerationAreaAdapter
import edu.gtri.gpssample.fragments.ManageEnumerationArea.ManageEnumerationAreaViewModel

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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentManageEnumerationAreaBinding
import edu.gtri.gpssample.databinding.FragmentManageEnumerationTeamBinding
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.UDPBroadcaster
import edu.gtri.gpssample.network.models.*
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

class ManageEnumerationTeamFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate, GPSSampleWifiManager.GPSSampleWifiManagerDelegate
{
    private lateinit var team: Team
    private lateinit var study: Study
    private lateinit var enumArea: EnumArea
    private lateinit var gpsSamleWifiManager: GPSSampleWifiManager
    private lateinit var studyAdapter: ManageEnumerationAreaAdapter
    private lateinit var viewModel: ManageEnumerationAreaViewModel

    private var dataIsFresh = false
    private var _binding: FragmentManageEnumerationTeamBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private var users = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageEnumerationAreaViewModel::class.java)
        (activity!!.application as? MainApplication)?.currentFragment = this.javaClass.simpleName
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

        binding.progressBar.visibility = View.VISIBLE

        // required: studyId
        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.",Toast.LENGTH_SHORT).show()
            return
        }

        val study_uuid = arguments!!.getString(Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.studyDAO.getStudy( study_uuid )?.let { study ->
            this.study = study
        }

        if (!this::study.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id ${study_uuid} not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // required enumArea_uuid
        val enumArea_uuid = arguments!!.getString(Keys.kEnumArea_uuid.toString(), "");

        if (enumArea_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: enumArea_uuid.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.enumAreaDAO.getEnumArea( enumArea_uuid )?.let {
            this.enumArea = it
        }

        if (!this::enumArea.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! EnumArea with id ${enumArea_uuid} not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // required team_uuid
        val team_uuid = arguments!!.getString(Keys.kTeam_uuid.toString(), "");

        if (team_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: team_uuid.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.teamDAO.getTeam( team_uuid )?.let {
            this.team = it
        }

        if (!this::team.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Team with id ${team_uuid} not found.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.studyNameTextView.setText( team.name )

        studyAdapter = ManageEnumerationAreaAdapter(users)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = studyAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity!!)

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

        // START THE HOTSPOT !!!

        gpsSamleWifiManager = GPSSampleWifiManager( this )
        gpsSamleWifiManager.startHotSpot()
    }

    override fun didStartHotspot( ssid: String, pass: String )
    {
        binding.progressBar.visibility = View.GONE

        binding.imageView.visibility = View.VISIBLE
        binding.usersOnlineTextView.visibility = View.VISIBLE

        val jsonObject = JSONObject()
        jsonObject.put( Keys.kSSID.toString(), ssid )
        jsonObject.put( Keys.kPass.toString(), pass )
        jsonObject.put( Keys.kTeam_uuid.toString(), team.uuid )
        jsonObject.put( Keys.kStudy_uuid.toString(), study.uuid )
        jsonObject.put( Keys.kEnumArea_uuid.toString(), enumArea.uuid )
        jsonObject.put( Keys.kConfig_uuid.toString(), study.config_uuid )

        val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
        qrgEncoder.setColorBlack(Color.WHITE);
        qrgEncoder.setColorWhite(Color.BLACK);

        val bitmap = qrgEncoder.bitmap
        binding.imageView.setImageBitmap(bitmap)
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

        when( networkCommand.command )
        {
            NetworkCommand.NetworkUserRequest -> {
                val user = User.unpack( networkCommand.message )

                if (users.find { it.name == user.name } == null)
                {
                    users.add( user )

                    activity!!.runOnUiThread{
                        studyAdapter.updateUsers( users )
                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        compositeDisposable.clear()

        gpsSamleWifiManager.stopHotSpot()

        _binding = null
    }
}