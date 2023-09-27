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
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationFragment : Fragment(),
    OnMapClickListener,
    OnCameraChangeListener,
    InfoDialog.InfoDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var user: User
    private lateinit var team: Team
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter

    private var gpsLocation: Point? = null

    private var dropMode = false
    private var showCurrentLocation = true

    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!

    private val pointHashMap = HashMap<Long,Location>()
    private val polygonHashMap = HashMap<Long,EnumArea>()
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()

    private val kExportTag = 2
    private val kSelectLocationTag = 3

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

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value

        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 14.0 )
        }

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        sharedViewModel.teamViewModel.currentTeam?.value?.let {
            team = it
        }

        (activity!!.application as? MainApplication)?.user?.let {
            user = it
        }

        performEnumerationAdapter = PerformEnumerationAdapter( ArrayList<Location>() )
        performEnumerationAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performEnumerationAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.titleTextView.text =  "Configuration " + enumArea.name + " (" + team.name + " team)"

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        )

        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.addOnMoveListener(onMoveListener)
        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager )

        binding.mapView.gestures.addOnMapClickListener(this )

        binding.centerOnLocationButton.setOnClickListener {
            showCurrentLocation = !showCurrentLocation
            if (showCurrentLocation)
            {
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.addHouseholdButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            ConfirmationDialog( activity, resources.getString(R.string.select_location),
                "", resources.getString(R.string.current_location), resources.getString(R.string.new_location), kSelectLocationTag, this)
        }

        binding.addLandmarkButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.exportButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            when(user.role)
            {
                Role.Supervisor.toString(), Role.Admin.toString() ->
                {
                    ConfirmationDialog( activity, resources.getString(R.string.export_configuration) ,
                        resources.getString(R.string.select_export_message),
                        resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
                }
                Role.Enumerator.toString() ->
                {
                    ConfirmationDialog( activity, resources.getString(R.string.enum_saved_doc),
                        resources.getString(R.string.select_export_message),
                        resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun refreshMap()
    {
        binding.mapView.getMapboxMap().removeOnCameraChangeListener( this )

        performEnumerationAdapter.updateLocations( enumArea.locations )

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

        team.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty())
        {
            val polygonAnnotation = mapboxManager.addPolygon( pointList, "#000000" )

            polygonAnnotation?.let { polygonAnnotation ->
                polygonHashMap[polygonAnnotation.id] = enumArea
                allPolygonAnnotations.add( polygonAnnotation)
            }

            val currentZoomLevel = sharedViewModel.currentZoomLevel?.value

            currentZoomLevel?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(team.polygon)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
            }

            for (location in enumArea.locations)
            {
                if (location.isLandmark)
                {
                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    mapboxManager.addMarker( point, R.drawable.location_blue )
                }
                else
                {
                    var resourceId = R.drawable.home_black

                    var numComplete = 0

                    for (item in location.enumerationItems)
                    {
                        val enumerationItem = item as EnumerationItem?
                        if(enumerationItem != null)
                        {
                            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceId = R.drawable.home_orange
                                break
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                numComplete++
                            }
                        }
                    }

                    if (numComplete > 0 && numComplete == location.enumerationItems.size)
                    {
                        resourceId = R.drawable.home_purple
                    }

                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                    pointAnnotation?.let {
                        pointHashMap[pointAnnotation.id] = location
                        allPointAnnotations.add( pointAnnotation )
                    }

                    pointAnnotationManager.apply {
                        addClickListener(
                            OnPointAnnotationClickListener { pointAnnotation ->
                                pointHashMap[pointAnnotation.id]?.let { location ->
                                    sharedViewModel.locationViewModel.setCurrentLocation(location)
                                    sharedViewModel.locationViewModel.setIsLocationUpdateTimeValid(false)
                                    if (location.isLandmark)
                                    {
                                        findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
                                    }
                                    else
                                    {
                                        findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
                                    }
                                }
                                true
                            }
                        )
                    }
                }
            }
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )
    }

    override fun onMapClick(point: com.mapbox.geojson.Point): Boolean
    {
        if (dropMode)
        {
            dropMode = false
            createLocation( point, false )
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            return true
        }

        return false
    }

    fun createLocation( point: Point, isLocationUpdateTimeValid: Boolean )
    {
        enumArea.locations.map{
            val haversineCheck = GeoUtils.isCloseTo( LatLng( it.latitude, it.longitude), LatLng(point.latitude(),point.longitude()))
            if (haversineCheck.withinBounds)
            {
                val message = "${resources.getString(R.string.duplicate_warning)} (${haversineCheck.distance}m)"
                ConfirmationDialog( activity, resources.getString(R.string.warning), message, resources.getString(R.string.no), resources.getString(R.string.yes), point, this)
                return
            }
        }

        val location = Location( LocationType.Enumeration, point.latitude(), point.longitude(), false)
        DAO.locationDAO.createOrUpdateLocation( location, enumArea )
        enumArea.locations.add(location)

        sharedViewModel.locationViewModel.setCurrentLocation(location)
        sharedViewModel.locationViewModel.setIsLocationUpdateTimeValid(isLocationUpdateTimeValid)

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
        }
        else
        {
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
        }
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    private fun didSelectLocation( location: Location )
    {
        if (dropMode)
        {
            dropMode = false
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
        }

        sharedViewModel.locationViewModel.setCurrentLocation(location)
        sharedViewModel.locationViewModel.setIsLocationUpdateTimeValid(false)

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
        }
        else
        {
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        if (tag is Point)
        {
            return
        }

        if (tag == kSelectLocationTag)
        {
            gpsLocation?.let {
                createLocation( it, true )
            } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.current_location_not_set), Toast.LENGTH_LONG).show()

            return
        }

        // Launch connection screen
        view?.let{ view ->
            sharedNetworkViewModel.setCurrentConfig(config)

            when(user.role)
            {
                Role.Admin.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Admin)
                    startHotspot(view)
                }

                Role.Supervisor.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Supervisor)

                    startHotspot(view)
                }

                Role.Enumerator.toString() ->
                {
                    sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.EnumerationTeam)
                    sharedNetworkViewModel.networkClientModel.currentEnumArea = enumArea
                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                    getResult.launch(intent)
                }
            }
        }
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag is Point)
        {
            val location = Location( LocationType.Enumeration, tag.latitude(), tag.longitude(), false)
            DAO.locationDAO.createOrUpdateLocation( location, enumArea )
            enumArea.locations.add(location)

            sharedViewModel.locationViewModel.setCurrentLocation(location)
            sharedViewModel.locationViewModel.setIsLocationUpdateTimeValid(false)

            if (location.isLandmark)
            {
                findNavController().navigate(R.id.action_navigate_to_AddLocationFragment)
            }
            else
            {
                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
            }
        }
        else
        {
            when(tag)
            {
                kSelectLocationTag -> {
                    dropMode = true
                    binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }

                kExportTag -> {
                    when(user.role)
                    {
                        Role.Supervisor.toString(), Role.Admin.toString() ->
                        {
                            val packedConfig = config.pack()
                            Log.d( "xxx", packedConfig )

                            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                            val file = File(root, "Configuration.${Date().time}.json")
                            val writer = FileWriter(file)
                            writer.append(packedConfig)
                            writer.flush()
                            writer.close()

                            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved_doc), Toast.LENGTH_SHORT).show()
                        }

                        Role.Enumerator.toString() ->
                        {
                            val packedEnumArea = enumArea.pack()
                            Log.d( "xxx", packedEnumArea )

                            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
                            val file = File(root, "EnumArea.${Date().time}.json")
                            val writer = FileWriter(file)
                            writer.append(packedEnumArea)
                            writer.flush()
                            writer.close()

                            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enum_saved_doc), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun didSelectOkButton(tag: Any?)
    {
        dropMode = true
        binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
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

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
//        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        sharedViewModel.locationViewModel.setCurrentLocationUpdateTime(Date())
        gpsLocation = point
        if (showCurrentLocation)
        {
            binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
            binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(point)
        }
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private fun initLocationComponent()
    {
        val locationComponentPlugin = binding.mapView.location

        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
    }

    private fun onCameraTrackingDismissed()
    {
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)

        _binding = null
    }
}