package edu.gtri.gpssample.fragments.perform_enumeration

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.ExportDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationFragment : Fragment(),
    OnMapReadyCallback,
    ExportDialog.ExportDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter

    private var userId = 0
    private var enumAreaId = 0
    private var dropMode = false
    private var addLocation: LatLng? = null
    private var _binding: FragmentPerformEnumerationBinding? = null
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private val binding get() = _binding!!

    private val kExportTag = 2
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformEnumerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enum_area ->
            enumArea = enum_area
            enumArea.id?.let {id ->
                enumAreaId = id
            }
        }

        Log.d( "xxx", enumArea.toString())

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        val user = (activity!!.application as? MainApplication)?.user

        user?.id?.let {
            userId = it
        }

        performEnumerationAdapter = PerformEnumerationAdapter( ArrayList<Location>() )
        performEnumerationAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performEnumerationAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.titleTextView.text =  "Configuration " + enumArea.name + " (" + team.name + " team)"

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        val defaultColorList = binding.dropPinButton.backgroundTintList

        binding.dropPinButton.setOnClickListener {

            if (!dropMode)
            {
                dropMode = true
                addLocation = null
                addMapObjects()
                binding.dropPinButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

                map.setOnMapClickListener {
                    if (dropMode)
                    {
                        addLocation = it
                        map.addMarker(MarkerOptions().position(it))
                        dropMode = false
                        binding.dropPinButton.setBackgroundTintList(defaultColorList);
                    }
                }
            }
            else
            {
                dropMode = false
                binding.dropPinButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.addHouseholdButton.setOnClickListener {

            addLocation?.let { location ->
                val location = Location(enumAreaId, location.latitude, location.longitude, false)
                location.enumerationTeamId = team.id!!
                sharedViewModel.locationViewModel.setCurrentLocation(location)

                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
            } ?: kotlin.run {
                Toast.makeText(activity!!.applicationContext, "Location not found", Toast.LENGTH_SHORT).show()
            }

            addLocation = null
        }

        binding.addLandmarkButton.setOnClickListener {

//            location?.let { location ->
//                var enumData = EnumData(userId, enumAreaId, false, false, "", "", location.latitude, location.longitude)
//                enumData.isLocation = true
//                sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)
//
//                findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
//            } ?: kotlin.run {
//                Toast.makeText(activity!!.applicationContext, "Location not found", Toast.LENGTH_SHORT).show()
//            }

            addLocation = null
        }

        binding.exportButton.setOnClickListener {
            sharedViewModel.currentConfiguration?.value?.let { config ->

                val user = (activity!!.application as MainApplication).user

                user?.let { user ->
                    when(user.role)
                    {
                        Role.Supervisor.toString(), Role.Admin.toString() ->
                        {
                            ConfirmationDialog( activity, "Export Configuration", "Select an export method", "QR Code", "File System", kExportTag, this)
                        }
                        Role.Enumerator.toString() ->
                        {
                            ConfirmationDialog( activity, "Export Enumeration Data", "Select an export method", "QR Code", "File System", kExportTag, this)
                        }
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName

        enumArea.locations = DAO.locationDAO.getLocations(enumArea, team)
        performEnumerationAdapter.updateLocations( enumArea.locations )

        if (this::map.isInitialized)
        {
            addMapObjects()
        }
    }

    var once = true

    fun addMapObjects()
    {
        map.clear()

        val points = ArrayList<LatLng>()

        team.polygon.map {
            points.add( it.toLatLng())
        }

        if (points.isNotEmpty())
        {
            val polygon = PolygonOptions()
                .clickable(false)
                .addAll( points )

            map.addPolygon(polygon)
        }

        if (once)
        {
            once = false
            val latLng = getCenter()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom( latLng, 14.0f))
        }

        for (location in enumArea.locations)
        {
            var marker: Marker? = null
            var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

            if (location.isLandmark)
            {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.location_blue)
            }
            else
            {
                var numComplete = 0

                for (enumerationItem in location.enumerationItems)
                {
                    if (enumerationItem.state == EnumerationState.Incomplete)
                    {
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)
                        break
                    }
                    else if (enumerationItem.state == EnumerationState.Complete)
                    {
                        numComplete++
                    }
                }

                if (numComplete > 0 && numComplete == location.enumerationItems.size)
                {
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                }
            }

            marker = map.addMarker( MarkerOptions()
                .position( LatLng( location.latitude, location.longitude ))
                .icon( icon )
            )

            marker?.let {marker ->
                marker.tag = location

                map.setOnMarkerClickListener { marker ->
                    marker.tag?.let { tag ->
                        val location = tag as Location
                        sharedViewModel.locationViewModel.setCurrentLocation(location)

                        if (location.isLandmark)
                        {
                            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
                        }
                        else
                        {
                            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
                        }
                    }

                    false
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        addMapObjects()
    }

    fun getCenter() : LatLng
    {
        var sumLat: Double = 0.0
        var sumLon: Double = 0.0

        for (latLon in enumArea.vertices)
        {
            sumLat += latLon.latitude
            sumLon += latLon.longitude
        }

        return LatLng( sumLat/enumArea.vertices.size, sumLon/enumArea.vertices.size )
    }

    private fun didSelectLocation( location: Location )
    {
        sharedViewModel.locationViewModel.setCurrentLocation(location)

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
        }
        else
        {
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
        }
    }

    override fun shouldExport( fileName: String, configuration: Boolean, qrCode: Boolean )
    {
        if (qrCode)
        {
            if (configuration)
            {
            }
            else
            {
            }
        }
        else
        {
            if (configuration)
            {
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    // this is a hack
//                    DAO.configDAO.updateAllLists( config )

                    team.id?.let {
                        config.teamId = it
                    }

                    val packedConfig = config.pack()
                    Log.d( "xxx", packedConfig )

                    config.teamId = 0

                    val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                    val file = File(root, "$fileName.${Date().time}.json")
                    val writer = FileWriter(file)
                    writer.append(packedConfig)
                    writer.flush()
                    writer.close()

                    Toast.makeText(activity!!.applicationContext, "The configuration has been saved to the Documents directory.", Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                // Sync the enumeration data back to the admin.
                // For this scenario, we're trying to export the enumeration data only,
                // not the entire configuration.
                // On the admin side, the app will need to be able to add
                // the enumeration data only, not the entire EnumArea container.
                // EnumData ids on the admin side will need to be autogenerated
                // during the import to accommodate uploads from multiple enumerators.
                // We'll also need to handle duplicate updates from the same enumerator.

//                enumArea.locations = DAO.locationDAO.getLocations(enumArea,team)

                val packedEnumArea = enumArea.pack()
                Log.d( "xxx", packedEnumArea )

                val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                val file = File(root, "$fileName.${Date().time}.json")
                val writer = FileWriter(file)
                writer.append(packedEnumArea)
                writer.flush()
                writer.close()

                Toast.makeText(activity!!.applicationContext, "Enumeration data has been saved to the Documents directory.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        // Launch connection screen
        view?.let{view ->
            val user = (activity!!.application as MainApplication).user
            user?.let { user ->

                sharedViewModel?.currentConfiguration?.value?.let{
                    sharedNetworkViewModel.setCurrentConfig(it)
                }
                //TODO: fix this! compare should be the enum
                when(user.role)
                {
                    Role.Supervisor.toString() ->
                    {
                        sharedNetworkViewModel.networkHotspotModel.currentTeamId = sharedViewModel.teamViewModel.currentTeam?.value?.id
                        sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Supervisor)

                        startHotspot(view)
                    }

                    Role.Admin.toString() ->
                    {
                        sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Admin)
                        startHotspot(view)
                    }

                    Role.Enumerator.toString() ->
                    {
                        // start camera
                        // set what client mode we are
                        sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.EnumerationTeam)

                        // I don't know why this should be necessary.
//                        enumArea.locations = DAO.locationDAO.getLocations(enumArea,team)

                        sharedNetworkViewModel.networkClientModel.currentEnumArea = enumArea
                        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                        getResult.launch(intent)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.toString())

                val jsonObject = JSONObject(payload);

                Log.d("xxx", jsonObject.toString(2))

                val ssid = jsonObject.getString(Keys.kSSID.toString())
                val pass = jsonObject.getString(Keys.kPass.toString())
                val serverIp = jsonObject.getString(Keys.kIpAddress.toString())

                Log.d("xxxx", "the ssid, pass, serverIP ${ssid}, ${pass}, ${serverIp}")

                sharedNetworkViewModel.connectHotspot(ssid, pass, serverIp)

//                findNavController().navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
                // need to pass this into the network view model
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHotspot(view : View)
    {
        sharedNetworkViewModel.createHotspot(view)
    }

    override fun didSelectRightButton(tag: Any?)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->

            // Sync the enumeration data back to the admin.
            // For this scenario, we're trying to export the enumeration data only,
            // not the entire configuration.
            // On the admin side, the app will need to be able to add
            // the enumeration data only, not the entire EnumArea container.
            // EnumData ids on the admin side will need to be autogenerated
            // during the import to accommodate uploads from multiple enumerators.
            // We'll also need to handle duplicate updates from the same enumerator.

//            enumArea.locations = DAO.locationDAO.getLocations(enumArea,team)

            val packedEnumArea = enumArea.pack()
            Log.d( "xxx", packedEnumArea )

            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
            val file = File(root, "EnumArea.${Date().time}.json")
            val writer = FileWriter(file)
            writer.append(packedEnumArea)
            writer.flush()
            writer.close()

            Toast.makeText(activity!!.applicationContext, "Enumeration data has been saved to the Documents directory.", Toast.LENGTH_SHORT).show()
        }
    }
}