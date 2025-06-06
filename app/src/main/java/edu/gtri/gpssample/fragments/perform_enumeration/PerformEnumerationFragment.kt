/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_enumeration

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
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
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.*
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentPerformEnumerationBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.utils.TestUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationFragment : Fragment(),
    OnMapClickListener,
    OnCameraChangeListener,
    InfoDialog.InfoDialogDelegate,
    InputDialog.InputDialogDelegate,
    MapboxManager.MapTileCacheDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private lateinit var user: User
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var enumerationTeam: EnumerationTeam
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private var _binding: FragmentPerformEnumerationBinding? = null
    private val binding get() = _binding!!

    private var dropMode = false
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private val pointHashMap = HashMap<Long,Location>()
    private val polygonHashMap = HashMap<Long,EnumArea>()
    private val enumerationTeamLocations = ArrayList<Location>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()
    private var allPolylineAnnotations = java.util.ArrayList<PolylineAnnotation>()

    private val kExportTag = 1
    private val kAddHouseholdTag = 2
    private val kSelectHouseholdTag = 3
    private val kFileLocationTag = 4

    private var maxSubaddress = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPerformEnumerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value

        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 16.0 )
        }

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
            sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
            sharedNetworkViewModel.networkClientModel.encryptionPassword = config.encryptionPassword
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "currentConfiguration was not initialized.", Toast.LENGTH_LONG).show()
            return
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        sharedViewModel.teamViewModel.currentEnumerationTeam?.value?.let {
            enumerationTeam = it
        }

        enumerationTeamLocations.clear()

        for (teamLocationUuid in enumerationTeam.locationUuids)
        {
            enumArea.locations.find { location -> location.uuid == teamLocationUuid  }?.let { location ->
                enumerationTeamLocations.add( location )
            }
        }

        for (location in enumArea.locations)
        {
            if (location.isLandmark)
            {
                enumerationTeamLocations.add( location )
            }
        }

        (activity!!.application as? MainApplication)?.user?.let {
            user = it
        }

        performEnumerationAdapter = PerformEnumerationAdapter( ArrayList<Location>(), enumArea.name )
        performEnumerationAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performEnumerationAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        binding.titleTextView.text =  enumArea.name + " (" + enumerationTeam.name + " " +  resources.getString(R.string.team) + ")"

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        var style = Style.MAPBOX_STREETS
        sharedPreferences.getString( Keys.kMapStyle.value, null)?.let {
            style = it
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            style,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        )

        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager, polylineAnnotationManager )

        pointAnnotationManager.apply {
            addClickListener(
                OnPointAnnotationClickListener { pointAnnotation ->

                    pointHashMap[pointAnnotation.id]?.let { location ->
                        sharedViewModel.locationViewModel.setCurrentLocation(location)

                        if (location.isLandmark)
                        {
                            findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
                        }
                        else
                        {
                            navigateToAddHouseholdFragment()
                        }
                    }

                    true
                }
            )
        }

        binding.mapView.gestures.addOnMapClickListener(this )

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.legendImageView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.mapTileCacheButton.setOnClickListener {
            enumArea.mapTileRegion?.let {
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                MapboxManager.loadStylePack( activity!!, this )
            }
        }

        val centerOnCurrentLocation = sharedViewModel.centerOnCurrentLocation?.value
        if (centerOnCurrentLocation == null)
        {
            sharedViewModel.setCenterOnCurrentLocation( false )
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
            if (centerOnCurrentLocation)
            {
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.helpButton.setOnClickListener {
            PerformEnumerationHelpDialog( activity!! )
        }

        binding.centerOnLocationButton.setOnClickListener {
            sharedViewModel.centerOnCurrentLocation?.value?.let { centerOnCurrentLocation ->
                if (centerOnCurrentLocation)
                {
                    sharedViewModel.setCenterOnCurrentLocation( false )
                    binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    sharedViewModel.setCenterOnCurrentLocation( true )
                    binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
                refreshMap()
            }
        }

        binding.addHouseholdButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            if (gpsAccuracyIsGood())
            {
                if (config.allowManualLocationEntry)
                {
                    ConfirmationDialog( activity, resources.getString(R.string.select_location),
                        "", resources.getString(R.string.current_location), resources.getString(R.string.new_location), kAddHouseholdTag, this, true)
                }
                else
                {
                    didSelectFirstButton( kAddHouseholdTag )
                }
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_LONG).show()
            }
        }

        binding.addLandmarkButton.setOnClickListener {
            if (dropMode)
            {
                dropMode = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }

            if (gpsAccuracyIsGood())
            {
                currentGPSAccuracy?.let { accuracy ->
                    currentGPSLocation?.let { point ->
                        val altitude = if (point.altitude().isNaN()) 0.0 else point.altitude()
                        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                        val location = Location( timeZone, LocationType.Enumeration, accuracy, point.latitude(), point.longitude(), altitude, true, "", "")

                        DAO.locationDAO.createOrUpdateLocation( location, enumArea )
                        enumArea.locations.add(location)
                        sharedViewModel.locationViewModel.setCurrentLocation(location)
                        findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
                    } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.current_location_not_set), Toast.LENGTH_LONG).show()
                }
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_accuracy_error), Toast.LENGTH_LONG).show()
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
                    ConfirmationDialog( activity, resources.getString(R.string.export_enum_data),
                        resources.getString(R.string.select_export_message),
                        resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
                }
            }
        }

        var sampledCount = 0
        var surveyedCount = 0
        var enumerationCount = 0

        for (location in enumerationTeamLocations)
        {
            for (enumItem in location.enumerationItems)
            {
                if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                {
                    enumerationCount += 1
                }
                if (enumItem.samplingState == SamplingState.Sampled)
                {
                    sampledCount += 1
                }
                if (enumItem.collectionState == CollectionState.Complete)
                {
                    surveyedCount += 1
                }
            }
        }

        for (location in enumArea.locations)
        {
            for (enumItem in location.enumerationItems)
            {
                if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                {
                    enumItem.subAddress.toIntOrNull()?.let {
                        if (it > maxSubaddress)
                        {
                            maxSubaddress = it
                        }
                    }
                }
            }
        }

        if (enumerationCount == 0)
        {
            InputDialog( activity!!, false, resources.getString(R.string.subaddress_start), "1", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this, false, true, true )
        }

        binding.listItemEnumArea.titleLayout.visibility = View.GONE
        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.PerformEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun gpsAccuracyIsGood(): Boolean
    {
        currentGPSAccuracy?.let {
            return (it <= config.minGpsPrecision)
        }

        return false
    }

    private fun gpsLocationIsGood( location: Location ) : Boolean
    {
        var editMode = false

        if (gpsAccuracyIsGood())
        {
            currentGPSLocation?.let { point ->
                val distance = GeoUtils.distanceBetween( LatLng( location.latitude, location.longitude ), LatLng( point.latitude(), point.longitude()))
                editMode = distance <= config.minGpsPrecision
            }
        }

        return editMode
    }

    private fun refreshMap()
    {
        binding.mapView.getMapboxMap().removeOnCameraChangeListener( this )

        performEnumerationAdapter.updateLocations( enumerationTeamLocations )

        for (polygonAnnotation in allPolygonAnnotations)
        {
            polygonAnnotationManager.delete( polygonAnnotation )
        }

        allPolygonAnnotations.clear()

        for (polylineAnnotation in allPolylineAnnotations)
        {
            polylineAnnotationManager.delete( polylineAnnotation )
        }

        allPolylineAnnotations.clear()

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager.delete( pointAnnotation )
        }

        allPointAnnotations.clear()

        pointHashMap.clear()

        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumerationTeam.polygon.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty())
        {
            val polygonAnnotation = mapboxManager.addPolygon( pointList, "#000000", 0.25 )

            polygonAnnotation?.let { polygonAnnotation ->
                polygonHashMap[polygonAnnotation.id] = enumArea
                allPolygonAnnotations.add( polygonAnnotation)
            }

            val polylineAnnotation = mapboxManager.addPolyline( pointList[0], "#ff0000" )

            polylineAnnotation?.let { polylineAnnotation ->
                allPolylineAnnotations.add( polylineAnnotation)
            }

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
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
                    val pointAnnotation = mapboxManager.addMarker( point, R.drawable.location_blue )

                    pointAnnotation?.let {
                        pointHashMap[pointAnnotation.id] = location
                        allPointAnnotations.add( pointAnnotation )
                    }
                }
            }

            for (location in enumerationTeamLocations)
            {
                if (!location.isLandmark)
                {
                    var resourceId = if (location.enumerationItems.size > 1) R.drawable.multi_home_black else R.drawable.home_black

                    var numComplete = 0

                    for (item in location.enumerationItems)
                    {
                        val enumerationItem = item as EnumerationItem?
                        if(enumerationItem != null)
                        {
                            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceId = if (location.enumerationItems.size > 1) R.drawable.multi_home_red else R.drawable.home_red
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
                        resourceId = if (location.enumerationItems.size > 1) R.drawable.multi_home_green else R.drawable.home_green
                    }

                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                    pointAnnotation?.let {
                        pointHashMap[pointAnnotation.id] = location
                        allPointAnnotations.add( pointAnnotation )
                    }

                    if (location.enumerationItems.isNotEmpty())
                    {
                        mapboxManager.addViewAnnotationToPoint( binding.mapView.viewAnnotationManager, point, location.enumerationItems[0].subAddress, "#00FFFFFF")
                    }
                }
            }
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )
    }

    fun navigateToAddHouseholdFragment()
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

            val bundle = Bundle()
            bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
            bundle.putInt( Keys.kStartSubaddress.value, maxSubaddress)

            if (location.enumerationItems.isEmpty())
            {
                if (gpsLocationIsGood( location ))
                {
                    val enumerationItem = EnumerationItem()

                    if (config.autoIncrementSubaddress)
                    {
                        enumerationItem.subAddress = "${maxSubaddress + 1}"
                    }

                    sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )

                    ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.is_multi_family), resources.getString(R.string.no), resources.getString(R.string.yes), kSelectHouseholdTag, this)
                }
                else
                {
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_location_error), Toast.LENGTH_LONG).show()
                }
            }
            else if (location.enumerationItems.size == 1)
            {
                sharedViewModel.locationViewModel.setCurrentEnumerationItem( location.enumerationItems[0])
                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
            }
            else
            {
                findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment, bundle)
            }
        }
    }

    override fun onMapClick(point: com.mapbox.geojson.Point): Boolean
    {
        if (dropMode)
        {
            dropMode = false
            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

            if (config.proximityWarningIsEnabled)
            {
                enumArea.locations.map{
                    if (!it.isLandmark)
                    {
                        val haversineCheck = GeoUtils.isCloseTo( LatLng( it.latitude, it.longitude), LatLng(point.latitude(),point.longitude()), config.proximityWarningValue )
                        if (haversineCheck.withinBounds)
                        {
                            val distance = String.format( "%.1f", haversineCheck.distance)
                            val message = "${resources.getString(R.string.duplicate_warning)} (${distance}m)"
                            ConfirmationDialog( activity, resources.getString(R.string.warning), message, resources.getString(R.string.no), resources.getString(R.string.yes), point, this)
                            return true
                        }
                    }
                }
            }

            var accuracy = -1

            currentGPSAccuracy?.let {
                accuracy = it
            }

            val altitude = if (point.altitude().isNaN()) 0.0 else point.altitude()
            val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
            val location = Location( timeZone, LocationType.Enumeration, accuracy, point.latitude(), point.longitude(), altitude, false, "", "")

            if (gpsLocationIsGood( location ))
            {
                DAO.locationDAO.createOrUpdateLocation( location, enumArea )
                enumArea.locations.add(location)

                sharedViewModel.locationViewModel.setCurrentLocation(location)

                enumerationTeamLocations.add(location)
                enumerationTeam.locationUuids.add(location.uuid)
                DAO.enumerationTeamDAO.updateConnectorTable( enumerationTeam )
                navigateToAddHouseholdFragment()
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.gps_location_error), Toast.LENGTH_LONG).show()
            }

            return true
        }

        return false
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

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
        }
        else
        {
            navigateToAddHouseholdFragment()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectFirstButton(tag: Any?)
    {
        if (tag is Point) // HH is too close to an existing HH, don't add it
        {
            return
        }

        if (tag == kSelectHouseholdTag) // HH is not multifamily
        {
            sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
            }

            return
        }

        if (tag == kFileLocationTag)
        {
            exportToDefaultLocation()
            return
        }

        if (tag == kAddHouseholdTag)  // use current location
        {
            currentGPSLocation?.let { point ->

                if (config.proximityWarningIsEnabled)
                {
                    enumArea.locations.map{
                        if (!it.isLandmark)
                        {
                            val haversineCheck = GeoUtils.isCloseTo( LatLng( it.latitude, it.longitude), LatLng(point.latitude(),point.longitude()), config.proximityWarningValue)
                            if (haversineCheck.withinBounds)
                            {
                                val distance = String.format( "%.1f", haversineCheck.distance)
                                val message = "${resources.getString(R.string.duplicate_warning)} (${distance}m)"
                                ConfirmationDialog( activity, resources.getString(R.string.warning), message, resources.getString(R.string.no), resources.getString(R.string.yes), point, this)
                                return
                            }
                        }
                    }
                }

                var accuracy = -1

                currentGPSAccuracy?.let {
                    accuracy = it
                }

                val altitude = if (point.altitude().isNaN()) 0.0 else point.altitude()
                val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                val location = Location( timeZone, LocationType.Enumeration, accuracy, point.latitude(), point.longitude(), altitude, false, "", "")

                DAO.locationDAO.createOrUpdateLocation( location, enumArea )
                enumArea.locations.add(location)

                sharedViewModel.locationViewModel.setCurrentLocation(location)

                enumerationTeamLocations.add(location)
                enumerationTeam.locationUuids.add(location.uuid)
                DAO.enumerationTeamDAO.updateConnectorTable( enumerationTeam )
                navigateToAddHouseholdFragment()
            } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.current_location_not_set), Toast.LENGTH_LONG).show()

            return
        }

        // Launch connection screen
        view?.let{ view ->
            sharedNetworkViewModel.setCurrentConfig(config)

            when(user.role)
            {
                Role.Admin.toString(),
                Role.Supervisor.toString() ->
                {
                    sharedNetworkViewModel.networkHotspotModel.setTitle(resources.getString(R.string.export_configuration))
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Export)
                    sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
                    startHotspot(view)
                }

                Role.Enumerator.toString() ->
                {
                    sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.EnumerationTeam)
                    sharedNetworkViewModel.networkClientModel.currentConfig = config
                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                    getResult.launch(intent)
                }
            }
        }
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        if (tag is Point)
        {
            var accuracy = -1

            currentGPSAccuracy?.let {
                accuracy = it
            }

            val altitude = if (tag.altitude().isNaN()) 0.0 else tag.altitude()
            val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
            val location = Location( timeZone, LocationType.Enumeration, accuracy, tag.latitude(), tag.longitude(), altitude, false, "", "")

            DAO.locationDAO.createOrUpdateLocation( location, enumArea )
            enumArea.locations.add(location)

            sharedViewModel.locationViewModel.setCurrentLocation(location)

            enumerationTeamLocations.add(location)
            enumerationTeam.locationUuids.add(location.uuid)
            DAO.enumerationTeamDAO.updateConnectorTable( enumerationTeam )
            navigateToAddHouseholdFragment()
        }
        else
        {
            when(tag)
            {
                kSelectHouseholdTag ->
                {
                    sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                        val bundle = Bundle()
                        bundle.putBoolean( Keys.kEditMode.value, gpsLocationIsGood( location ))
                        bundle.putInt( Keys.kStartSubaddress.value, maxSubaddress)
                        findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment,bundle)
                    }
                }

                kAddHouseholdTag -> {
                    dropMode = true
                    binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }

                kExportTag -> {
                    ConfirmationDialog( activity, resources.getString(R.string.select_file_location), "", resources.getString(R.string.default_location), resources.getString(R.string.let_me_choose), kFileLocationTag, this, true)
                }

                kFileLocationTag -> {
                    exportToDevice()
                }
            }
        }
    }

    fun exportToDefaultLocation()
    {
        try
        {
            val user = (activity!!.application as MainApplication).user

            var userName = user!!.name.replace(" ", "" ).uppercase()

            if (userName.length > 3)
            {
                userName = userName.substring(0,3)
            }

            val role = user.role.toString().substring(0,1).uppercase()

            val formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
            val dateTime = LocalDateTime.now().format(formatter)

            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample/Enumerated")
            root.mkdirs()

            var version = ""
            val versionName = BuildConfig.VERSION_NAME.split( "#" )
            if (versionName.size == 2)
            {
                version = versionName[1]
            }

            val clusterName = enumArea.name.replace(" ", "" ).uppercase()

            when(user.role)
            {
                Role.Admin.toString(),
                Role.Supervisor.toString() ->
                {
                    val packedConfig = config.packMinimal()
                    val fileName = "${role}-${userName}-${clusterName}-EN-${dateTime!!}-${version}.json"
                    val file = File(root, fileName)
                    val writer = FileWriter(file)
                    writer.append(packedConfig)
                    writer.flush()
                    writer.close()

                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
                }

                Role.Enumerator.toString() ->
                {
                    val packedConfig = config.pack()
                    val fileName = "${role}-${userName}-${clusterName}-${dateTime!!}-${version}.json"
                    val file = File(root, fileName)
                    val writer = FileWriter(file)
                    writer.append(packedConfig)
                    writer.flush()
                    writer.close()

                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
                }
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity!!.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
        }
    }

    fun exportToDevice()
    {
        var userName = user.name.replace(" ", "" ).uppercase()

        if (userName.length > 3)
        {
            userName = userName.substring(0,3)
        }

        val role = user.role.toString().substring(0,1).uppercase()

        val formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
        val dateTime = LocalDateTime.now().format(formatter)

        var version = ""
        val versionName = BuildConfig.VERSION_NAME.split( "#" )
        if (versionName.size == 2)
        {
            version = versionName[1]
        }

        val clusterName = enumArea.name.replace(" ", "" ).uppercase()
        var fileName = ""

        when(user.role)
        {
            Role.Admin.toString(),
            Role.Supervisor.toString() ->
            {
                fileName = "${role}-${userName}-${clusterName}-EN-${dateTime!!}-${version}.json"
            }

            Role.Enumerator.toString() ->
            {
                fileName = "${role}-${userName}-${clusterName}-${dateTime!!}-${version}.json"
            }
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult( intent, REQUEST_CODE_PICK_DIR )
    }

    val REQUEST_CODE_PICK_DIR = 1001

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        try
        {
            if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == Activity.RESULT_OK)
            {
                data?.data?.let { uri ->
                    var packedConfig = ""

                    when( user.role )
                    {
                        Role.Admin.toString(),
                        Role.Supervisor.toString() ->
                        {
                            packedConfig = config.packMinimal()
                        }

                        Role.Enumerator.toString() ->
                        {
                            packedConfig = config.pack()
                        }
                    }

                    activity!!.applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use {
                            it.write(packedConfig.toByteArray())
                            it.close()
                            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        catch (ex: java.lang.Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity!!.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
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
                val payload = it.data!!.getStringExtra(Keys.kPayload.value)

                val jsonObject = JSONObject(payload);

                Log.d("xxx", jsonObject.toString(2))

                val ssid = jsonObject.getString(Keys.kSSID.value)
                val pass = jsonObject.getString(Keys.kPass.value)
                val serverIp = jsonObject.getString(Keys.kIpAddress.value)

                Log.d("xxxx", "the ssid, pass, serverIP ${ssid}, ${pass}, ${serverIp}")

                sharedNetworkViewModel.connectHotspot(ssid, pass, serverIp)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHotspot(view : View)
    {
        sharedNetworkViewModel.createHotspot(view)
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        sharedViewModel.centerOnCurrentLocation?.value?.let {
            if (it)
            {
                binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
                binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(point)
            }
        }
    }

    private var lastLocationUpdateTime: Long = 0

    private val locationConsumer = object : LocationConsumer
    {
        override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) {
        }

        override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?)
        {
            if (location.size > 0)
            {
                val point = location.last()
                binding.locationTextView.text = String.format( "%.7f, %.7f", point.latitude(), point.longitude())
                currentGPSLocation = point

                if (Date().time - lastLocationUpdateTime > 3000)
                {
                    lastLocationUpdateTime = Date().time

                    for (loc in enumerationTeamLocations)
                    {
                        val currentLatLng = LatLng( point.latitude(), point.longitude())
                        val itemLatLng = LatLng( loc.latitude, loc.longitude )
                        val distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )
                        if (distance < 400) // display in meters or feet
                        {
                            if (config.distanceFormat == DistanceFormat.Meters)
                            {
                                loc.distance = distance
                                loc.distanceUnits = resources.getString( R.string.meters )
                            }
                            else
                            {
                                loc.distance = distance * 3.28084
                                loc.distanceUnits = resources.getString( R.string.feet )
                            }
                        }
                        else // display in kilometers or miles
                        {
                            if (config.distanceFormat == DistanceFormat.Meters)
                            {
                                loc.distance = distance / 1000.0
                                loc.distanceUnits = resources.getString( R.string.kilometers )
                            }
                            else
                            {
                                loc.distance = distance / 1609.34
                                loc.distanceUnits = resources.getString( R.string.miles )
                            }
                        }
                    }

                    performEnumerationAdapter.updateLocations( enumerationTeamLocations )
                }
            }
        }

        override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {
        }

        override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {
        }
    }

    private val onIndicatorAccurracyRadiusChangedListener = OnIndicatorAccuracyRadiusChangedListener {
        val accuracy = it.toInt()
        currentGPSAccuracy = accuracy

        if (accuracy <= config.minGpsPrecision)
        {
            binding.accuracyLabelTextView.text = " " + resources.getString(R.string.good)
            binding.accuracyLabelTextView.setTextColor( Color.parseColor("#0000ff"))
        }
        else
        {
            binding.accuracyLabelTextView.text = " " + resources.getString(R.string.poor)
            binding.accuracyLabelTextView.setTextColor( Color.parseColor("#ff0000") )
        }

        binding.accuracyValueTextView.text = " : ${accuracy.toString()}m"
    }

    private fun initLocationComponent()
    {
        val locationComponentPlugin = binding.mapView.location

        locationComponentPlugin.enabled = true
        locationComponentPlugin.getLocationProvider()?.registerLocationConsumer( locationConsumer )
        locationComponentPlugin.addOnIndicatorPositionChangedListener( onIndicatorPositionChangedListener )

        val locationComponentPlugin2 = binding.mapView.location2
        locationComponentPlugin2.enabled = true
        locationComponentPlugin2.addOnIndicatorAccuracyRadiusChangedListener( onIndicatorAccurracyRadiusChangedListener )

        locationComponentPlugin2.updateSettings2 {
            this.showAccuracyRing = true
        }

        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    activity!!,
                    R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    activity!!,
                    R.drawable.mapbox_user_icon_shadow,
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

    override fun stylePackLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.style_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                enumArea.mapTileRegion?.let {
                    val mapTileRegions = ArrayList<MapTileRegion>()
                    mapTileRegions.add( it )
                    MapboxManager.loadTilePacks( activity!!, mapTileRegions, this )
                }
            }
        }
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress(resources.getString(R.string.downloading_map_tiles) + " ${numLoaded}/${numNeeded}")
            }
        }
    }

    override fun tilePacksLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                }
            }
        }
    }

    override fun didPressCancelButton()
    {
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    override fun didCancelText( tag: Any? )
    {
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        name.toIntOrNull()?.let {
            maxSubaddress = it - 1
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_set_subaddress, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.set_subaddress ->
            {
                InputDialog( activity!!, false, resources.getString(R.string.subaddress_start), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this, false, true )
            }

            R.id.mapbox_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                binding.mapView.getMapboxMap().loadStyleUri(
                    Style.MAPBOX_STREETS,
                    object : Style.OnStyleLoaded {
                        override fun onStyleLoaded(style: Style) {
                            refreshMap()
                        }
                    }
                )
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                binding.mapView.getMapboxMap().loadStyleUri(
                    Style.SATELLITE_STREETS,
                    object : Style.OnStyleLoaded {
                        override fun onStyleLoaded(style: Style) {
                            refreshMap()
                        }
                    }
                )
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.getLocationProvider()?.unRegisterLocationConsumer( locationConsumer )
        binding.mapView.location.removeOnIndicatorPositionChangedListener( onIndicatorPositionChangedListener )

        _binding = null
    }
}