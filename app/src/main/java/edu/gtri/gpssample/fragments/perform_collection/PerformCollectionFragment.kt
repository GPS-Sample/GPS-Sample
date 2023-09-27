package edu.gtri.gpssample.fragments.perform_collection

import android.content.Intent
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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
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
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformCollectionBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
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

class PerformCollectionFragment : Fragment(),
    OnCameraChangeListener,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate,
    LaunchSurveyDialog.LaunchSurveyDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var user: User
    private lateinit var team: Team
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var sampleArea: SampleArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performCollectionAdapter: PerformCollectionAdapter

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

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enum_area ->
            enumArea = enum_area
            enumArea.id?.let {id ->
                enumAreaId = id
            }
        }

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        val _user = (activity!!.application as? MainApplication)?.user

        _user?.let { user ->
            this.user = user
        }

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value
        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 14.0 )
        }

        val sampledLocations = ArrayList<Location>()

        sampleArea.locations.map {
            if (it.enumerationItems[0].samplingState == SamplingState.Sampled)
            {
                sampledLocations.add( it )
            }
        }

        performCollectionAdapter = PerformCollectionAdapter( sampledLocations )
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

            when(user.role)
            {
                Role.Supervisor.toString(), Role.Admin.toString() ->
                {
                    ConfirmationDialog( activity, resources.getString(R.string.export_configuration) , resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
                }
                Role.DataCollector.toString() ->
                {
                    ConfirmationDialog( activity, resources.getString(R.string.export_collection_data), resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
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
                        var color = R.drawable.home_black

                        when(sampledItem.collectionState)
                        {
                            CollectionState.Undefined -> color = R.drawable.home_black
                            CollectionState.Complete -> color = R.drawable.home_green
                            CollectionState.Incomplete -> color = R.drawable.home_red
                        }

                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        val pointAnnotation = mapboxManager.addMarker( point, color )

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

    private fun didSelectLocation( location: Location )
    {
        sharedViewModel.locationViewModel.setCurrentLocation(location)
        LaunchSurveyDialog( activity, this)
    }

    override fun didSelectRightButton(tag: Any?)
    {
        var payload: String = ""
        var name: String = ""

        when(user.role) {
            Role.Supervisor.toString() ->
            {
                name = "Config"
                payload = config.pack()
            }

            Role.Enumerator.toString() ->
            {
                name = "EnumArea"
                payload = enumArea.pack()
            }

            Role.DataCollector.toString() ->
            {
                name = "SampleArea"
                payload = sampleArea.pack()
            }
        }

        Log.d( "xxx", payload )

        val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
        val file = File(root, "${name}.${Date().time}.json")
        val writer = FileWriter(file)
        writer.append(payload)
        writer.flush()
        writer.close()

        Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved_doc), Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        // Launch connection screen
        view?.let{ view ->
            sharedNetworkViewModel.setCurrentConfig(config)

            when(user.role)
            {
                Role.Supervisor.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Supervisor)
                    startHotspot(view)
                }

                Role.Admin.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Admin)
                    startHotspot(view)
                }

                Role.DataCollector.toString() ->
                {
                    sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.CollectionTeam)
                    sharedNetworkViewModel.networkClientModel.currentSampleArea = sampleArea
                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                    getResult.launch(intent)
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

            val sampledItem = location.enumerationItems[0]

            sampledItem.collectionState = CollectionState.Complete
            sampledItem.notes = notes

            if (incompleteReason.isNotEmpty())
            {
                sampledItem.collectionState = CollectionState.Incomplete
            }

            DAO.enumerationItemDAO.updateEnumerationItem( sampledItem, location )

            refreshMap()
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