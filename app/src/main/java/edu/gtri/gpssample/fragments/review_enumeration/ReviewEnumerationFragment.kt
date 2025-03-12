/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.review_enumeration

import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.TextView
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
import com.google.android.gms.location.zza
import com.google.android.gms.maps.model.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.zoom
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.*
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentReviewEnumerationBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.utils.TestUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.fragments.perform_enumeration.PerformEnumerationAdapter
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class ReviewEnumerationFragment : Fragment(), OnCameraChangeListener
{
    private lateinit var user: User
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var mapboxManager: MapboxManager
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private var _binding: FragmentReviewEnumerationBinding? = null
    private val binding get() = _binding!!

    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null
    private val pointHashMap = HashMap<Long,Location>()
    private val polygonHashMap = HashMap<Long,EnumArea>()
    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()
    private var allPolygonAnnotations = java.util.ArrayList<PolygonAnnotation>()
    private var allPolylineAnnotations = java.util.ArrayList<PolylineAnnotation>()

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
        _binding = FragmentReviewEnumerationBinding.inflate(inflater, container, false)
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

        (activity!!.application as? MainApplication)?.user?.let {
            user = it
        }

        performEnumerationAdapter = PerformEnumerationAdapter( enumArea.locations, enumArea.name )
        performEnumerationAdapter.didSelectLocation = this::didSelectLocation

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = performEnumerationAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

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

        var sampledCount = 0
        var surveyedCount = 0
        var enumerationCount = 0

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

        binding.listItemEnumArea.titleLayout.visibility = View.GONE
        binding.listItemEnumArea.numberEnumeratedTextView.text = "$enumerationCount"
        binding.listItemEnumArea.numberSampledTextView.text = "$sampledCount"
        binding.listItemEnumArea.numberSurveyedTextView.text = "$surveyedCount"
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ReviewEnumerationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun gpsAccuracyIsGood(): Boolean
    {
        currentGPSAccuracy?.let {
            return (it <= config.minGpsPrecision)
        }

        return false
    }

    private fun refreshMap()
    {
        binding.mapView.getMapboxMap().removeOnCameraChangeListener( this )

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

        addPolygon( enumArea.vertices, "" )

        for (enumerationTeam in enumArea.enumerationTeams)
        {
            addPolygon( enumerationTeam.polygon, enumerationTeam.name )
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

        for (location in enumArea.locations)
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
            val polygonAnnotation = mapboxManager.addPolygon(pointList, "#000000", 0.25)

            polygonAnnotation?.let { polygonAnnotation ->
                polygonHashMap[polygonAnnotation.id] = enumArea
                allPolygonAnnotations.add(polygonAnnotation)
            }

            val polylineAnnotation = mapboxManager.addPolyline(pointList[0], "#ff0000")

            polylineAnnotation?.let { polylineAnnotation ->
                allPolylineAnnotations.add(polylineAnnotation)
            }

            val latLngBounds = GeoUtils.findGeobounds(vertices)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            mapboxManager.addViewAnnotationToPoint( binding.mapView.viewAnnotationManager, point, label, "#80FFFFFF" )
        }
    }

    fun navigateToAddHouseholdFragment()
    {
        sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->

            val bundle = Bundle()
            bundle.putBoolean( Keys.kEditMode.value, false)
            bundle.putInt( Keys.kStartSubaddress.value, 0 )

            if (location.enumerationItems.size == 1)
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

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
    }

    private fun didSelectLocation( location: Location )
    {
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

                    for (loc in enumArea.locations)
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

                    performEnumerationAdapter.updateLocations( enumArea.locations )
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