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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
//import edu.gtri.gpssample.database.DAO.Companion.collectionDataDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformCollectionBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.ExportDialog
import edu.gtri.gpssample.dialogs.LaunchSurveyDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.HashMap

class PerformCollectionFragment : Fragment(),
    OnCameraChangeListener,
    ExportDialog.ExportDialogDelegate,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate,
    LaunchSurveyDialog.LaunchSurveyDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var sampleArea: SampleArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performCollectionAdapter: PerformCollectionAdapter

    private var userId = 0
    private var enumAreaId = 0
    private val binding get() = _binding!!
    private val pointHashMap = java.util.HashMap<Long, Location>()
    private var _binding: FragmentPerformCollectionBinding? = null
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()

    private val kExportTag = 2
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this

        val samplingVm : SamplingViewModel by activityViewModels()
        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

        samplingVm.currentSampleArea?.value?.let { sampleArea ->
            this.sampleArea = sampleArea
        }
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

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value
        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 14.0 )
        }

        performCollectionAdapter = PerformCollectionAdapter( ArrayList<Location>() )
        performCollectionAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performCollectionAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.titleTextView.text =  "Configuration " + enumArea.name + " (" + team.name + " team)"

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    refreshMap()
                }
            }
        )

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager )

        binding.exportButton.setOnClickListener {
            sharedViewModel.currentConfiguration?.value?.let { config ->

                ConfirmationDialog( activity, resources.getString(R.string.enum_saved_doc),
                    resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code),
                    resources.getString(R.string.file_system), kExportTag, this)

                //ExportDialog( activity, "${config.name}-${team.name}", "${enumArea.name}-${team.name}", this )
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName

//        var locations = ArrayList<Location>()
//
////        for (location in sampleArea.locations)
////        {
////            if (!location.isLandmark && location.items.isNotEmpty())
////            {
////                // assuming only 1 enumerationItem per location, for now...
////                val sampledItem = location.items[0] as? SampledItem
////                sampledItem?.let{sampledItem ->
////                    if (sampledItem.samplingState == SamplingState.Sampled)
////                    {
////                        locations.add( location )
////                    }
////                }
////            }
////        }
//
//        performCollectionAdapter.updateLocations( locations )
    }

    fun refreshMap()
    {
        for (polygonAnnotation in allPolygonAnnotations)
        {
            polygonAnnotationManager.delete( polygonAnnotation )
        }

        allPolygonAnnotations.clear()

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager.delete( pointAnnotation )
        }

        allPointAnnotations.clear()

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            val polygonAnnotation = mapboxManager.addPolygon(pointList,"#000000")
            polygonAnnotation?.let {
                allPolygonAnnotations.add( it )
            }

            val currentZoomLevel = sharedViewModel.currentZoomLevel?.value
            currentZoomLevel?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
            }

            for (location in sampleArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    // assuming only 1 enumeration item per location, for now...
                    val sampledItem = location.enumerationItems[0]

                    if (sampledItem.samplingState == SamplingState.Sampled)
                    {
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        val pointAnnotation = mapboxManager.addMarker( point, R.drawable.home_black )

                        pointAnnotation?.let { pointAnnotation ->
                            allPointAnnotations.add( pointAnnotation )
                            pointHashMap[pointAnnotation.id] = location
                        }

                        pointAnnotationManager.apply {
                            addClickListener(
                                OnPointAnnotationClickListener { pointAnnotation ->
                                    pointHashMap[pointAnnotation.id]?.let { location ->
                                        sharedViewModel.locationViewModel.setCurrentLocation(location)
                                        LaunchSurveyDialog( activity, this@PerformCollectionFragment)
                                    }
                                    true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun onMapReadyXXX(googleMap: GoogleMap)
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

//        for (location in sampleArea.locations)
//        {
//            if (location.isLandmark)
//            {
//                val icon = BitmapDescriptorFactory.fromResource(R.drawable.location_blue)
//                map.addMarker( MarkerOptions()
//                    .position( LatLng( location.latitude, location.longitude ))
//                    .icon( icon )
//                )
//            }
//            else if (location.items.isNotEmpty())
//            {
//                // assuming only 1 enumerationItem per location, for now...
//
//                val sampledItem = location.items[0] as? SampledItem
//                sampledItem?.let{sampledItem ->
//                    if (sampledItem.samplingState == SamplingState.Sampled)
//                    {
////                        val collectionItem = DAO.collectionItemDAO.getCollectionItem( sampledItem.enumItem!!.collectionItemId )
////
//                        var icon = BitmapDescriptorFactory.fromResource(R.drawable.home_black)
////
////                        collectionItem?.let { collectionItem ->
////
////                            if (collectionItem.state == CollectionState.Incomplete)
////                            {
////                                icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)
////                            }
////                            else if (collectionItem.state == CollectionState.Complete)
////                            {
////                                icon = BitmapDescriptorFactory.fromResource(R.drawable.home_green)
////                            }
////                        }
//
//                        val marker = map.addMarker( MarkerOptions()
//                            .position( LatLng( location.latitude, location.longitude ))
//                            .icon( icon )
//                        )
//
////                        marker?.let {marker ->
////                            marker.tag = location
////
////                            map.setOnMarkerClickListener { marker ->
////                                marker.tag?.let { tag ->
////
////                                    val location = tag as Location
////                                    sharedViewModel.locationViewModel.setCurrentLocation(location)
////
////                                    LaunchSurveyDialog( activity, this)
////                                }
////
////                                false
////                            }
////                        }
//                    }
//                }
//            }
//        }
    }

    private fun didSelectLocation( location: Location )
    {
        sharedViewModel.locationViewModel.setCurrentLocation(location)
        LaunchSurveyDialog( activity, this)
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

            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved_doc), Toast.LENGTH_SHORT).show()
        }
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
                        sharedNetworkViewModel.networkHotspotModel.currentTeamId = sharedViewModel.teamViewModel.currentTeam?.value?.id
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
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHotspot(view : View)
    {
        sharedNetworkViewModel.createHotspot(view)
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

                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.location_not_found), Toast.LENGTH_SHORT).show()
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

//                enumArea.locations = DAO.locationDAO.getLocations( enumArea )

                val packedEnumArea = enumArea.pack()
                Log.d( "xxx", packedEnumArea )

                val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                val file = File(root, "$fileName.${Date().time}.json")
                val writer = FileWriter(file)
                writer.append(packedEnumArea)
                writer.flush()
                writer.close()

                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved_doc), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun launchSurveyButtonPressed()
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
            (this.activity!!.application as? MainApplication)?.currentLocationUUID = location.uuid
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "vnd.android.cursor.dir/vnd.odk.form"
            odk_result.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val odk_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        AdditionalInfoDialog( activity, "", "", this)
    }

    override fun markAsIncompleteButtonPressed()
    {
        AdditionalInfoDialog( activity, "", "", this)
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

//            val sampledItem = location.items[0] as? SampledItem
//            sampledItem?.let{sampledItem ->
//
//                refreshMap()
//
////                var collectionItem = DAO.collectionItemDAO.getCollectionItem(sampledItem.enumItem!!.collectionItemId)
////
////                var state = CollectionState.Complete
////
////                if (incompleteReason.isNotEmpty())
////                {
////                    state = CollectionState.Incomplete
////                }
////
////                if (collectionItem == null)
////                {
////                    val collectionItem = DAO.collectionItemDAO.createOrUpdateCollectionItem(
////                        CollectionItem( sampledItem.enumItem!!.id!!, state, incompleteReason, notes )
////                    )
////
////                    collectionItem?.id?.let {
////                        sampledItem.enumItem!!.collectionItemId = it
////                    }
////
////                    DAO.enumerationItemDAO.updateEnumerationItem( sampledItem.enumItem!! )
////                }
////                else
////                {
////                    collectionItem.state = state
////                    collectionItem.incompleteReason = incompleteReason
////                    collectionItem.notes = notes
////                    DAO.collectionItemDAO.createOrUpdateCollectionItem( collectionItem )
////                }
////
////                onMapReady(map)
//            }
        }
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}