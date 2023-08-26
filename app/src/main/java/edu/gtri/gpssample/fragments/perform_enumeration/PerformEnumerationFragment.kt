package edu.gtri.gpssample.fragments.perform_enumeration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
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
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
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
    private lateinit var team: Team
    private lateinit var map: GoogleMap
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter

    private var userId = 0
    private var dropMode = false
    private var currentLocation: LatLng? = null

    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!

    private val pointHashMap = HashMap<Long,Location>()
    private val polygonHashMap = HashMap<Long,EnumArea>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()

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

        val currentZoomLevel = sharedViewModel.performEnumerationModel.currentZoomLevel?.value

        if (currentZoomLevel == null)
        {
            sharedViewModel.performEnumerationModel.setCurrentZoomLevel( 14.0 )
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {enum_area ->
            enumArea = enum_area
        }

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

        binding.mapView.gestures.addOnMapClickListener(this )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.addHouseholdButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                map.setOnMapClickListener(null)
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            ConfirmationDialog( activity, resources.getString(R.string.select_location),
                "", resources.getString(R.string.current_location), resources.getString(R.string.new_location), kSelectLocationTag, this)
        }

        binding.addLandmarkButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                map.setOnMapClickListener(null)
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.exportButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                map.setOnMapClickListener(null)
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            sharedViewModel.currentConfiguration?.value?.let { config ->

                val user = (activity!!.application as MainApplication).user

                user?.let { user ->
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
            val polygonAnnotation = mapboxManager.addPolygon( pointList )

            polygonAnnotation?.let { polygonAnnotation ->
                polygonHashMap[polygonAnnotation.id] = enumArea
                allPolygonAnnotations.add( polygonAnnotation)
            }

            val currentZoomLevel = sharedViewModel.performEnumerationModel.currentZoomLevel?.value

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

                    for (item in location.items)
                    {
                        val enumerationItem = item as EnumerationItem?
                        if(enumerationItem != null)
                        {
                            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceId = R.drawable.home_red
                                break
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                numComplete++
                            }
                        }
                    }

                    if (numComplete > 0 && numComplete == location.items.size)
                    {
                        resourceId = R.drawable.home_green
                    }

                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                    pointAnnotation?.let {
                        pointHashMap[pointAnnotation.id] = location
                        allPointAnnotations.add( pointAnnotation )
                    }

                    pointAnnotationManager?.apply {
                        addClickListener(
                            OnPointAnnotationClickListener { pointAnnotation ->
                                pointHashMap[pointAnnotation.id]?.let { location ->
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
            createLocation( LatLng( point.latitude(), point.longitude()))
            refreshMap()
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            return true
        }

        return false
    }

    fun createLocation( latLng: LatLng )
    {
        enumArea.locations.map{
            val haversineCheck = GeoUtils.isCloseTo( LatLng( it.latitude, it.longitude), latLng )
            if (haversineCheck.withinBounds)
            {
                val message = "Distance: ${haversineCheck.distance}\n\n " +
                        "coord1 ${haversineCheck.start.latitude}, ${haversineCheck.start.longitude} \n"+
                        "coord2 ${haversineCheck.end.latitude}, ${haversineCheck.end.longitude} \n"+
                        " ${resources.getString(R.string.duplicate_warning)}"
                ConfirmationDialog( activity, resources.getString(R.string.warning), message, resources.getString(R.string.no), resources.getString(R.string.yes), latLng, this)
                return
            }
        }

        val location = Location( LocationType.Enumeration, latLng.latitude, latLng.longitude, false)
        DAO.locationDAO.createOrUpdateLocation( location, enumArea )
        enumArea.locations.add(location)
        refreshMap()
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.performEnumerationModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    private fun addMapObjectsXXX()
    {
        performEnumerationAdapter.updateLocations( enumArea.locations )

        map.clear()

        val points = ArrayList<LatLng>()

        team.polygon.map {
            val pt = it.toLatLng()
            points.add( pt)
        }

        if (points.isNotEmpty())
        {

            val polygon = PolygonOptions()
                .clickable(false)
                .addAll( points )

            map.addPolygon(polygon)
        }

        val latLngBounds = GeoUtils.findGeobounds(team.polygon)
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0 ))

//        sharedViewModel.performEnumerationModel.currentZoomLevel?.value?.let { zoomLevel ->
//            map.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
//            currentLocation?.let { latLng ->
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
//            }
//        }

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

                for (item in location.items)
                {
                    val enumerationItem = item as? EnumerationItem
                    enumerationItem?.let{enumerationItem ->
                        if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                        {
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.home_red)

                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                        {
                            numComplete++
                        }
                    }

                }

                if (numComplete > 0 && numComplete == location.items.size)
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

    fun onMapReadyXXX(googleMap: GoogleMap)
    {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            map.isMyLocationEnabled = true
        }

        refreshMap()

        requestNewLocationData()

//        map.setOnCameraMoveListener {
//            sharedViewModel.performEnumerationModel.setCurrentZoomLevel(map.cameraPosition.zoom)
//        }
    }

    private fun didSelectLocation( location: Location )
    {
        if (dropMode)
        {
            dropMode = false
            map.setOnMapClickListener(null)
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
        }

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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        if (tag is LatLng)
        {
            return
        }

        if (tag == kSelectLocationTag)
        {
            currentLocation?.let {
//                addHousehold( it )
            }

            return
        }

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
                        // TODO
                        // teamId should be passed to setHotspotMode() instead of setting a global var
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
                        sharedNetworkViewModel.networkClientModel.currentEnumArea = enumArea
                        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                        getResult.launch(intent)
                    }
                }
            }
        }
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag is LatLng)
        {
            val location = Location( LocationType.Enumeration, tag.latitude, tag.longitude, false)
            DAO.locationDAO.createOrUpdateLocation( location, enumArea )
            enumArea.locations.add(location)
            refreshMap()
        }
        else
        {
            when(tag)
            {
                kSelectLocationTag -> {
                    InfoDialog( activity, resources.getString(R.string.new_location), resources.getString(R.string.tap_the_map), resources.getString(R.string.ok), null, this)
                }

                kExportTag -> {
                    sharedViewModel.currentConfiguration?.value?.let { config ->

                        val user = (activity!!.application as MainApplication).user

                        user?.let { user ->
                            when(user.role)
                            {
                                Role.Supervisor.toString(), Role.Admin.toString() ->
                                {
                                    team.id?.let {
                                        config.teamId = it
                                    }

                                    val packedConfig = config.pack()
                                    Log.d( "xxx", packedConfig )

                                    config.teamId = 0

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
        }
    }

    override fun didSelectOkButton(tag: Any?)
    {
        binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

        dropMode = true

//        map.setOnMapClickListener { latLng ->
//            dropMode = false
//            addHousehold( latLng )
//            map.setOnMapClickListener(null)
//            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
//        }
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

    private fun requestNewLocationData()
    {
        if (ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
            locationResult.lastLocation?.let{ location ->
                val location = LatLng(location.latitude, location.longitude)
                sharedViewModel.performEnumerationModel.currentZoomLevel?.value?.let { zoomLevel ->
//                    if (currentLocation == null)
//                    {
//                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))
//                    }
//                    else
//                    {
//                        map.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
//                    }

                    currentLocation = location
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        fusedLocationClient.removeLocationUpdates( locationCallback )

        _binding = null
    }
}