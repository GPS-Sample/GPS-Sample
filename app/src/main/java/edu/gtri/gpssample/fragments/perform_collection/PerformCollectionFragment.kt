package edu.gtri.gpssample.fragments.perform_collection

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
import edu.gtri.gpssample.database.DAO.Companion.collectionDataDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformCollectionBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.ExportDialog
import edu.gtri.gpssample.dialogs.LaunchSurveyDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.HashMap

class PerformCollectionFragment : Fragment(),
    OnMapReadyCallback,
    ExportDialog.ExportDialogDelegate,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate,
    LaunchSurveyDialog.LaunchSurveyDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var performCollectionAdapter: PerformCollectionAdapter

    private var userId = 0
    private var enumAreaId = 0
    private var _binding: FragmentPerformCollectionBinding? = null
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
        _binding = FragmentPerformCollectionBinding.inflate(inflater, container, false)
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

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        val user = (activity!!.application as? MainApplication)?.user

        user?.id?.let {
            userId = it
        }

        val enumDataList = DAO.enumDataDAO.getEnumData(enumArea, team)

        performCollectionAdapter = PerformCollectionAdapter( enumDataList )
        performCollectionAdapter.didSelectEnumData = this::didSelectEnumData

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performCollectionAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.titleTextView.text =  "Configuration " + enumArea.name + " (" + team.name + " team)"

        binding.toolbarLayout.visibility = View.GONE
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.exportButton.setOnClickListener {
            sharedViewModel.currentConfiguration?.value?.let { config ->

                ConfirmationDialog( activity, "Export Enumeration Data", "Select an export method", "QR Code", "File System", kExportTag, this)

                //ExportDialog( activity, "${config.name}-${team.name}", "${enumArea.name}-${team.name}", this )
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    var once = true

    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap

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

        val enumDataList = DAO.enumDataDAO.getEnumData(enumArea, team)

        for (enumData in enumDataList)
        {
            var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

            val marker = map.addMarker( MarkerOptions()
                .position( LatLng( enumData.latitude, enumData.longitude ))
                .icon( icon )
            )

            marker?.let {marker ->
                marker.tag = enumData

                val collectionData = DAO.collectionDataDAO.getCollectionData( enumData.collectionDataId )

                collectionData?.let { collectionData ->

                    var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)

                    if (collectionData.incomplete)
                    {
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)
                    }
                    else if (collectionData.valid)
                    {
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
                    }

                    marker.setIcon( icon )
                }
            }

            map.setOnMarkerClickListener { marker ->
                marker.tag?.let {tag ->
                    val enum_data = tag as EnumData
                    sharedViewModel.enumDataViewModel.setCurrentEnumData(enum_data)

                    LaunchSurveyDialog( activity, this)
                }

                false
            }
        }
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

    private fun didSelectEnumData( enumData: EnumData )
    {
        sharedViewModel.enumDataViewModel.setCurrentEnumData(enumData)

        LaunchSurveyDialog( activity, this)
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
                    DAO.configDAO.updateAllLists( config )

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

                enumArea.enumDataList = DAO.enumDataDAO.getEnumData(enumArea,team)
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
                        enumArea.enumDataList = DAO.enumDataDAO.getEnumData(enumArea,team)

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
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHotspot(view : View)
    {
        sharedNetworkViewModel.createHotspot(view)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun launchSurveyButtonPressed()
    {
        sharedViewModel.enumDataViewModel.currentEnumData?.value?.let { enumData ->

            (this.activity!!.application as? MainApplication)?.currentEnumDataUUID = enumData.uuid
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "vnd.android.cursor.dir/vnd.odk.form"
            odk_result.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val odk_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        AdditionalInfoDialog( activity, false, "", "", this)
    }

    override fun markAsIncompleteButtonPressed()
    {
        AdditionalInfoDialog( activity, true, "", "", this)
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incomplete: Boolean, incompleteReason: String, notes: String )
    {
        sharedViewModel.enumDataViewModel.currentEnumData?.value?.let { enumData ->
            enumData.id?.let { enumDataId ->
                val valid = !incomplete
                val collectionData = collectionDataDAO.createOrUpdateCollectionData( CollectionData( enumDataId, valid, incomplete, incompleteReason, notes ))
                collectionData?.id?.let {
                    enumData.collectionDataId = it
                    DAO.enumDataDAO.updateEnumData( enumData )
                    onMapReady(map)
                }
            }
        }
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

            enumArea.enumDataList = DAO.enumDataDAO.getEnumData(enumArea,team)
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