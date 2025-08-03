/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.review_enumeration

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.*
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentReviewEnumerationBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.fragments.perform_enumeration.PerformEnumerationAdapter
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import java.util.*

class ReviewEnumerationFragment : Fragment(),
    MapManager.MapManagerDelegate
{
    private lateinit var user: User
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var sharedNetworkViewModel : NetworkViewModel
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var performEnumerationAdapter: PerformEnumerationAdapter

    private var _binding: FragmentReviewEnumerationBinding? = null
    private val binding get() = _binding!!

    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: Point? = null

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

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
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

        if (enumArea.mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( enumArea.mbTilesPath )
        }

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
            this.mapView = mapView

            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                MapManager.instance().centerMap( enumArea, currentZoomLevel, mapView )
            }

            refreshMap()
        }

        if (ActivityCompat.checkSelfPermission( activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission( activity!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
            fusedLocationClient.requestLocationUpdates( locationRequest, locationCallback, Looper.getMainLooper())
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
        MapManager.instance().clearMap( mapView )

        var points = java.util.ArrayList<Point>()
        var pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40 )

        for (enumerationTeam in enumArea.enumerationTeams)
        {
            points = java.util.ArrayList<Point>()
            pointList = java.util.ArrayList<java.util.ArrayList<Point>>()

            enumerationTeam.polygon.map {
                points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
            }

            pointList.add( points )
            MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40, Color.RED, enumerationTeam.name )
        }

        for (location in enumArea.locations)
        {
            if (location.isLandmark)
            {
                MapManager.instance().createMarker( activity!!, mapView, location, R.drawable.location_blue, "" )
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

                MapManager.instance().createMarker( activity!!, mapView, location, resourceId, "" )
            }
        }
    }

    fun navigateToAddHouseholdFragment()
    {
        enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
            location.enumerationItems.find { it.uuid == sharedViewModel.currentEnumerationItemUuid }?.let { enumerationItem ->
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.value, false)
                bundle.putInt( Keys.kStartSubaddress.value, 0 )

                if (location.enumerationItems.size == 1)
                {
                    sharedViewModel.currentEnumerationItemUuid = location.enumerationItems[0].uuid
                    findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment, bundle)
                }
                else
                {
                    findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment, bundle)
                }
            }
        }
    }

    private fun didSelectLocation( location: Location )
    {
        sharedViewModel.currentLocationUuid = location.uuid

        if (location.isLandmark)
        {
            findNavController().navigate(R.id.action_navigate_to_AddLandmarkFragment)
        }
        else
        {
            navigateToAddHouseholdFragment()
        }
    }

    private var lastLocationUpdateTime: Long = 0

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
            val location = locationResult.locations.last()
            val accuracy = location.accuracy.toInt() // in meters
            val point = Point.fromLngLat( location.longitude, location.latitude )

            currentGPSLocation = point
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

            binding.locationTextView.text = String.format( "%.7f, %.7f", point.latitude(), point.longitude())

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_map_style_min, menu)
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

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, this ) { mapView ->
                    refreshMap()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onMarkerTapped( location: Location )
    {
        didSelectLocation( location )
    }

    override fun onZoomLevelChanged( zoomLevel: Double )
    {
        sharedViewModel.setCurrentZoomLevel( zoomLevel )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        fusedLocationClient.removeLocationUpdates( locationCallback )

        _binding = null
    }
}