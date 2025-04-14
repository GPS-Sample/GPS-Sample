/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.review_collection

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.model.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentReviewCollectionBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.fragments.perform_collection.PerformCollectionAdapter
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import java.util.*

class ReviewCollectionFragment : Fragment(), OnCameraChangeListener, SelectionDialog.SelectionDialogDelegate
{
    private lateinit var user: User
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var samplingViewModel: SamplingViewModel
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var sharedNetworkViewModel: NetworkViewModel
    private lateinit var performCollectionAdapter: PerformCollectionAdapter

    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null

    private val binding get() = _binding!!
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private var landmarkLocations = ArrayList<Location>()
    private var _binding: FragmentReviewCollectionBinding? = null
    private val locationHashMap = java.util.HashMap<Long, Location>()
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()
    private var allPolylineAnnotations = java.util.ArrayList<PolylineAnnotation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm: ConfigurationViewModel by activityViewModels()
        val networkVm: NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this

        val samplingVm: SamplingViewModel by activityViewModels()
        samplingViewModel = samplingVm
        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentReviewCollectionBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.currentConfiguration?.value?.let { config ->
            sharedNetworkViewModel.networkHotspotModel.encryptionPassword = config.encryptionPassword
            sharedNetworkViewModel.networkClientModel.encryptionPassword = config.encryptionPassword
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        val _user = (activity!!.application as? MainApplication)?.user

        _user?.let { user ->
            this.user = user
        }

        if (sharedViewModel.currentZoomLevel?.value == null)
        {
            sharedViewModel.setCurrentZoomLevel( 16.0 )
        }

        val enumerationItems = ArrayList<EnumerationItem>()

        for (location in enumArea.locations)
        {
            for (enumurationItem in location.enumerationItems)
            {
                if (enumurationItem.samplingState == SamplingState.Sampled)
                {
                    enumerationItems.add( enumurationItem )
                }
            }
        }

        landmarkLocations.clear()

        for (location in enumArea.locations)
        {
            if (location.isLandmark)
            {
                landmarkLocations.add( location )
            }
        }

        performCollectionAdapter = PerformCollectionAdapter( ArrayList<EnumerationItem>(), ArrayList<Location>(), enumArea.name )
        performCollectionAdapter.updateItems( enumerationItems, landmarkLocations )

        performCollectionAdapter.didSelectItem = this::didSelectItem

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performCollectionAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( activity!!, null, enumArea.mbTilesPath, binding.mapView.getMapboxMap()) {
                initLocationComponent()
                createAnnotationManagers()
                refreshMap()
            }
        }
        else
        {
            TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                initLocationComponent()
                createAnnotationManagers()
                refreshMap()
            }
        }

        mapboxManager = MapboxManager.instance( activity!! )

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.legendImageView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        val centerOnCurrentLocation = sharedViewModel.centerOnCurrentLocation?.value
        if (centerOnCurrentLocation == null)
        {
            sharedViewModel.setCenterOnCurrentLocation( false )
        }

        updateSummaryInfo()
    }

    fun createAnnotationManagers() {
        pointAnnotationManager = mapboxManager.createPointAnnotationManager(pointAnnotationManager, binding.mapView)
        polygonAnnotationManager = mapboxManager.createPolygonAnnotationManager(polygonAnnotationManager, binding.mapView)
        polylineAnnotationManager = mapboxManager.createPolylineAnnotationManager(polylineAnnotationManager, binding.mapView)

        pointAnnotationManager?.apply {
            addClickListener(
                OnPointAnnotationClickListener { pointAnnotation ->
                    locationHashMap[pointAnnotation.id]?.let { location ->
                        sharedViewModel.locationViewModel.setCurrentLocation(location)

                        if (location.isLandmark)
                        {
                            findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
                        }
                        else
                        {
                            if (location.enumerationItems.size > 1)
                            {
                                val bundle = Bundle()
                                bundle.putBoolean( Keys.kEditMode.value, false)
                                bundle.putBoolean( Keys.kGpsAccuracyIsGood.value, gpsAccuracyIsGood())
                                bundle.putBoolean( Keys.kGpsLocationIsGood.value, gpsLocationIsGood( location ))

                                findNavController().navigate(R.id.action_navigate_to_PerformMultiCollectionFragment, bundle)
                            }
                            else
                            {
                                sharedViewModel.locationViewModel.setCurrentEnumerationItem( location.enumerationItems[0] )
                                sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
                                    (this@ReviewCollectionFragment.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = location.enumerationItems[0].uuid
                                    (this@ReviewCollectionFragment.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
                                    (this@ReviewCollectionFragment.activity!!.application as? MainApplication)?.currentSubAddress = location.enumerationItems[0].subAddress

                                    val bundle = Bundle()
                                    bundle.putBoolean( Keys.kEditMode.value, false)
                                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
                                }
                            }
                        }
                    }
                    true
                }
            )
        }
    }

    fun updateSummaryInfo()
    {
        var sampledCount = 0
        var enumerationCount = 0
        var surveyedCount = 0

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->
            for (location in enumArea.locations)
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
        }

        binding.listItemEnumArea.titleLayout.visibility = View.GONE
        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ReviewCollectionFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun refreshMap()
    {
        binding.mapView.getMapboxMap().removeOnCameraChangeListener( this )

        for (polygonAnnotation in allPolygonAnnotations)
        {
            polygonAnnotationManager?.delete( polygonAnnotation )
        }

        allPolygonAnnotations.clear()

        for (polylineAnnotation in allPolylineAnnotations)
        {
            polylineAnnotationManager?.delete( polylineAnnotation )
        }

        allPolylineAnnotations.clear()

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager?.delete( pointAnnotation )
        }

        allPointAnnotations.clear()
        locationHashMap.clear()

        sharedViewModel.currentConfiguration?.value?.let { config ->
            for (enumArea in config.enumAreas)
            {
                for (location in enumArea.locations)
                {
                    if (location.isLandmark)
                    {
                        val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                        val pointAnnotation = mapboxManager.addMarker( pointAnnotationManager, point, R.drawable.location_blue )

                        pointAnnotation?.let {
                            locationHashMap[pointAnnotation.id] = location
                            allPointAnnotations.add( pointAnnotation )
                        }
                    }
                }
            }
        }

        addPolygon( enumArea.vertices, "" )
        MapboxManager.centerMap( enumArea, sharedViewModel.currentZoomLevel?.value, binding.mapView.getMapboxMap())

        for (collectionTeam in enumArea.collectionTeams)
        {
            addPolygon( collectionTeam.polygon, collectionTeam.name )
        }

        for (location in enumArea.locations)
        {
            if (!location.isLandmark && location.enumerationItems.isNotEmpty())
            {
                var resourceId = 0

                if (location.enumerationItems.size == 1)
                {
                    val sampledItem = location.enumerationItems[0]

                    if (sampledItem.samplingState == SamplingState.Sampled)
                    {
                        when( sampledItem.collectionState )
                        {
                            CollectionState.Undefined -> resourceId = R.drawable.home_light_blue
                            CollectionState.Incomplete -> resourceId = R.drawable.home_orange
                            CollectionState.Complete -> resourceId = R.drawable.home_purple
                        }
                    }
                }
                else
                {
                    for (sampledItem in location.enumerationItems)
                    {
                        if (sampledItem.samplingState == SamplingState.Sampled && sampledItem.collectionState == CollectionState.Undefined)
                        {
                            resourceId = R.drawable.multi_home_light_blue
                            break
                        }
                    }

                    if (resourceId == 0)
                    {
                        for (sampledItem in location.enumerationItems)
                        {
                            if (sampledItem.samplingState == SamplingState.Sampled)
                            {
                                if (sampledItem.collectionState == CollectionState.Incomplete)
                                {
                                    resourceId = R.drawable.multi_home_orange
                                    break
                                }
                                else if (sampledItem.collectionState == CollectionState.Complete)
                                {
                                    resourceId = R.drawable.multi_home_purple
                                }
                            }
                        }
                    }
                }

                if (resourceId > 0)
                {
                    // one point per location!
                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    val pointAnnotation = mapboxManager.addMarker( pointAnnotationManager, point, resourceId )

                    pointAnnotation?.let { pointAnnotation ->
                        allPointAnnotations.add( pointAnnotation )
                        locationHashMap[pointAnnotation.id] = location
                    }
                }
            }
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )
    }

    fun addPolygon( vertices: ArrayList<LatLon>, label: String )
    {
        val points = java.util.ArrayList<Point>()
        val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        if (pointList.isNotEmpty() && pointList[0].isNotEmpty()) {
            val polygonAnnotation = mapboxManager.addPolygon( polygonAnnotationManager, pointList, "#000000", 0.25)

            polygonAnnotation?.let { polygonAnnotation ->
                allPolygonAnnotations.add(polygonAnnotation)
            }

            val polylineAnnotation = mapboxManager.addPolyline( polylineAnnotationManager, pointList[0], "#ff0000")

            polylineAnnotation?.let { polylineAnnotation ->
                allPolylineAnnotations.add(polylineAnnotation)
            }

            val latLngBounds = GeoUtils.findGeobounds(vertices)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            mapboxManager.addViewAnnotationToPoint( binding.mapView.viewAnnotationManager, point, label, "#80FFFFFF" )
        }
    }

    private fun didSelectItem( item: Any )
    {
        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let { enumArea ->

            if (item is Location)
            {
                sharedViewModel.locationViewModel.setCurrentLocation(item)
                findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
            }
            else if (item is EnumerationItem)
            {
                DAO.locationDAO.getLocation( item.locationUuid )?.let { location ->
                    sharedViewModel.locationViewModel.setCurrentLocation(location)
                    sharedViewModel.locationViewModel.setCurrentEnumerationItem(item)
                    (this.activity!!.application as? MainApplication)?.currentEnumerationItemUUID = item.uuid
                    (this.activity!!.application as? MainApplication)?.currentEnumerationAreaName = enumArea.name
                    (this.activity!!.application as? MainApplication)?.currentSubAddress = item.subAddress

                    val bundle = Bundle()
                    bundle.putBoolean( Keys.kEditMode.value, false )
                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
                }
            }
        }
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
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

        override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?) {
            if (location.size > 0)
            {
                val point = location.last()
                binding.locationTextView.text = String.format( "%.7f, %.7f", point.latitude(), point.longitude())
                currentGPSLocation = point

                if (Date().time - lastLocationUpdateTime > 3000)
                {
                    lastLocationUpdateTime = Date().time

                    sharedViewModel.currentConfiguration?.value?.let { config ->

                        for (item in performCollectionAdapter.items)
                        {
                            var loc: Location? = null
                            val currentLatLng = LatLng( point.latitude(), point.longitude())

                            if (item is Location)
                            {
                                loc = item
                            }
                            else if (item is EnumerationItem)
                            {
                                enumArea.locations.find { l -> l.uuid == item.locationUuid  }?.let {
                                    loc = it
                                }
                            }

                            var distanceUnits = ""
                            val itemLatLng = LatLng( loc!!.latitude, loc!!.longitude )
                            var distance = GeoUtils.distanceBetween( currentLatLng, itemLatLng )

                            if (distance < 400) // display in meters or feet
                            {
                                if (config.distanceFormat == DistanceFormat.Meters)
                                {
                                    distanceUnits = resources.getString( R.string.meters )
                                }
                                else
                                {
                                    distance = distance * 3.28084
                                    distanceUnits = resources.getString( R.string.feet )
                                }
                            }
                            else // display in kilometers or miles
                            {
                                if (config.distanceFormat == DistanceFormat.Meters)
                                {
                                    distance = distance / 1000.0
                                    distanceUnits = resources.getString( R.string.kilometers )
                                }
                                else
                                {
                                    distance = distance / 1609.34
                                    distanceUnits = resources.getString( R.string.miles )
                                }
                            }

                            if (item is Location)
                            {
                                item.distance = distance
                                item.distanceUnits = distanceUnits
                            }
                            else if (item is EnumerationItem)
                            {
                                item.distance = distance
                                item.distanceUnits = distanceUnits
                            }
                        }

                        performCollectionAdapter.updateItems( performCollectionAdapter.enumerationItems, performCollectionAdapter.locations )
                    }
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

        sharedViewModel.currentConfiguration?.value?.let { config ->
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
        }

        binding.accuracyValueTextView.text = " : ${accuracy.toString()}m"
    }

    private fun gpsAccuracyIsGood(): Boolean
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            currentGPSAccuracy?.let {
                return (it <= config.minGpsPrecision)
            }
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
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    editMode = distance <= config.minGpsPrecision
                }
            }
        }

        return editMode
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_map_style, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.mapbox_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    createAnnotationManagers()
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    createAnnotationManagers()
                    refreshMap()
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                SelectionDialog( activity!!, TileServer.getCachedFiles( activity!! ), this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            TileServer.startServer( activity!!, uri, "", binding.mapView.getMapboxMap()) {
                createAnnotationManagers()
                refreshMap()
                TileServer.centerMap( binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
            }
        }
    }

    override fun didMakeSelection( selection: String, tag: Int )
    {
        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection

        TileServer.startServer( activity!!, null, mbTilesPath, binding.mapView.getMapboxMap()) {
            createAnnotationManagers()
            refreshMap()
            TileServer.centerMap( binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.getLocationProvider()?.unRegisterLocationConsumer( locationConsumer )
        binding.mapView.location.removeOnIndicatorPositionChangedListener( onIndicatorPositionChangedListener )
        _binding = null
    }
}