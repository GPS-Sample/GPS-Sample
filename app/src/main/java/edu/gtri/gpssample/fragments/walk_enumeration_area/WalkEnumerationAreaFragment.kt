/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.walk_enumeration_area

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentWalkEnumerationAreaBinding
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableDropdownDialogHost
import edu.gtri.gpssample.ui.compose.ComposableInputDialogHost
import edu.gtri.gpssample.ui.compose.ComposableMapLegendDialogHost
import edu.gtri.gpssample.ui.compose.ComposableWalkEnumerationAreaHelpDialogHost
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class WalkEnumerationAreaFragment : Fragment(), View.OnTouchListener
{
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var defaultColorList : ColorStateList
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var composableInputDialogHost: ComposableInputDialogHost
    private lateinit var composableDropdownDialogHost: ComposableDropdownDialogHost
    private lateinit var composableMapLegendDialogHost: ComposableMapLegendDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost
    private lateinit var composableWalkEnumerationAreaHelpDialogHost: ComposableWalkEnumerationAreaHelpDialogHost
    private var isRecording = false
    private val binding get() = _binding!!
    private var showCurrentLocation = false
    private var currentGPSAccuracy: Int? = null
    private var startPoint: com.mapbox.geojson.Point? = null
    private var currentGPSLocation: com.mapbox.geojson.Point? = null
    private var _binding: FragmentWalkEnumerationAreaBinding? = null
    private val polyLinePoints = ArrayList<com.mapbox.geojson.Point>()
    private val kEnumAreaName = 3

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentWalkEnumerationAreaBinding.inflate( inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = this.viewModel

            // Assign the fragment
            walkEnumerationAreaFragment = this@WalkEnumerationAreaFragment
        }

        composableInputDialogHost = ComposableInputDialogHost()
        composableDropdownDialogHost = ComposableDropdownDialogHost()
        composableMapLegendDialogHost = ComposableMapLegendDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()
        composableWalkEnumerationAreaHelpDialogHost = ComposableWalkEnumerationAreaHelpDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableInputDialogHost.Content()
            composableDropdownDialogHost.Content()
            composableMapLegendDialogHost.Content()
            composableConfirmationDialogHost.Content()
            composableWalkEnumerationAreaHelpDialogHost.Content()
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(requireActivity().applicationContext, resources.getString(R.string.config_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        binding.legendTextView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.legendImageView.setOnClickListener {
            composableMapLegendDialogHost.show()
        }

        binding.centerOnLocationButton.backgroundTintList?.let {
            defaultColorList = it
        }

        if (config.enumAreas.isNotEmpty() && config.enumAreas[0].mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( config.enumAreas[0].mbTilesPath )
        }

        binding.mapOverlayView.visibility = View.GONE

        MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null ) { mapView ->
            this.mapView = mapView

            MapManager.instance().enableLocationUpdates( requireActivity(), mapView )
            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            if (config.enumAreas.isNotEmpty())
            {
//                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                MapManager.instance().centerMap( config.enumAreas[0], mapView )
            }
            else
            {
                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null)
                        {
                            val point = com.mapbox.geojson.Point.fromLngLat( location.longitude, location.latitude )
                            MapManager.instance().centerMap( point, mapView )
                        }
                    }
                }
//                MapManager.instance().startCenteringOnLocation( activity!!, mapView )
//                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
//                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
//                    MapManager.instance().setZoomLevel( mapView, currentZoomLevel )
//                }
            }

            refreshMap()
        }

        if (ActivityCompat.checkSelfPermission( requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission( requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.requestLocationUpdates( locationRequest, locationCallback, Looper.getMainLooper())
        }

        if (config.enumAreas.isNotEmpty())
        {
            binding.saveButton.isEnabled = false
            binding.walkButton.isEnabled = false
            binding.addPointButton.isEnabled = false
            binding.deletePointButton.isEnabled = false
        }

        binding.mapOverlayView.setOnTouchListener(this)

        binding.walkButton.setOnClickListener {

            if (binding.walkButton.backgroundTintList == defaultColorList)
            {
                binding.addPointButton.setBackgroundTintList(defaultColorList);
                binding.walkButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                if (polyLinePoints.isNotEmpty())
                {
                    clearMap()
                }
                else
                {
                    binding.walkButton.setBackgroundTintList(defaultColorList);
                }
            }
        }

        binding.addPointButton.setOnClickListener {
            if (binding.walkButton.backgroundTintList != defaultColorList) // walking...
            {
                currentGPSLocation?.let { point ->
                    polyLinePoints.add( point )

                    if (polyLinePoints.size == 1)
                    {
                        startPoint = point
                        MapManager.instance().createMarker( requireActivity(), mapView, point, R.drawable.location_blue, "" )
                    }
                    else if (polyLinePoints.size > 1)
                    {
                        val points = ArrayList<Point>()
                        points.add( polyLinePoints[polyLinePoints.size-2])
                        points.add( polyLinePoints[polyLinePoints.size-1])
                        MapManager.instance().createPolyline( mapView, points, Color.RED )
                        if (polyLinePoints.size > 2)
                        {
                            val testPoints = ArrayList<com.mapbox.geojson.Point>()
                            testPoints.addAll( polyLinePoints )
                            testPoints.add( polyLinePoints[0])
                            if (GeoUtils.isSelfIntersectingPolygon1( testPoints ))
                            {
                                Toast.makeText(requireActivity().applicationContext,  resources.getString(R.string.polygon_is_self_intersecting), Toast.LENGTH_LONG).show()
                            } else {}
                        } else {}
                    } else {}
                }
            }
            else
            {
                if (binding.mapOverlayView.visibility == View.VISIBLE)
                {
                    binding.mapOverlayView.visibility = View.GONE
                    binding.addPointButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    Toast.makeText(requireActivity().applicationContext,  resources.getString(R.string.define_center), Toast.LENGTH_SHORT).show()
                    binding.mapOverlayView.visibility = View.VISIBLE
                    binding.addPointButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                }
            }
        }

        binding.deletePointButton.setOnClickListener {
            binding.mapOverlayView.visibility = View.GONE
            binding.addPointButton.setBackgroundTintList(defaultColorList);

            if (polyLinePoints.size > 0)
            {
                composableConfirmationDialogHost.show(
                    title = resources.getString(R.string.please_confirm),
                    message = resources.getString(R.string.delete_point),
                    leftButtonText = resources.getString(R.string.no),
                    rightButtonText = resources.getString(R.string.yes),
                ) { selection ->
                    if (selection == resources.getString(R.string.yes)) {
                        polyLinePoints.removeAt(polyLinePoints.lastIndex)

                        MapManager.instance().clearMap( mapView )

                        if (polyLinePoints.isNotEmpty())
                        {
                            startPoint?.let {
                                MapManager.instance().createMarker( requireActivity(), mapView, it, R.drawable.location_blue, "" )
                            }
                        }

                        if (polyLinePoints.size > 1)
                        {
                            MapManager.instance().createPolyline( mapView, polyLinePoints, Color.RED )
                        }
                    }
                }
            }
        }

        binding.deleteEverythingButton.setOnClickListener {
            binding.mapOverlayView.visibility = View.GONE
            binding.addPointButton.setBackgroundTintList(defaultColorList)
            clearMap()
        }

        binding.centerOnLocationButton.setOnClickListener {
            showCurrentLocation = !showCurrentLocation

            if (showCurrentLocation)
            {
                MapManager.instance().startCenteringOnLocation( requireActivity(), mapView )
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                MapManager.instance().stopCenteringOnLocation( mapView )
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.helpButton.setOnClickListener {
            composableWalkEnumerationAreaHelpDialogHost.show()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            if (polyLinePoints.size > 2)
            {
                isRecording = false

                // close the polygon
                polyLinePoints.add( polyLinePoints[0] )

                composableInputDialogHost.show(
                    title = null,
                    description = resources.getString(R.string.enter_enum_area_name),
                    text = "",
                    cancelable = true,
                    onQrClick = {
                        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                        getResult.launch(intent)
                    },
                    onResult = { text ->
                        if (text.isNotEmpty())
                        {
                            createEnumArea( text )
                        }
                    }
                )
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (requireActivity().application as? MainApplication)?.currentFragment = FragmentNumber.WalkEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun clearMap()
    {
        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.please_confirm),
            message = resources.getString(R.string.clear_map),
            leftButtonText = resources.getString(R.string.no),
            rightButtonText = resources.getString(R.string.yes),
        ) { selection ->
            if (selection == resources.getString(R.string.yes)) {
                isRecording = false

                MapManager.instance().clearMap( mapView )

                binding.walkButton.isEnabled = true
                binding.saveButton.isEnabled = true
                binding.addPointButton.isEnabled = true
                binding.deletePointButton.isEnabled = true
                binding.walkButton.setBackgroundTintList(defaultColorList);

                for (enumArea in config.enumAreas)
                {
                    DAO.enumAreaDAO.delete( enumArea )
                }

                config.enumAreas.clear()
                config.selectedEnumAreaUuid = ""

                DAO.configDAO.createOrUpdateConfig( config, UUID.randomUUID().toString())
            }
        }
    }

    private fun refreshMap()
    {
        MapManager.instance().clearMap( mapView )

        for (enumArea in config.enumAreas)
        {
            addPolygon(enumArea)
            enumArea.mapTileRegion?.let {
                addPolygon( it )
            }
        }
    }

    fun addPolygon( enumArea: EnumArea )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40 )
    }

    fun addPolygon( mapTileRegion: MapTileRegion )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        val vertices = ArrayList<LatLon>()

        vertices.add( LatLon( 0, mapTileRegion.southWest.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( 0, mapTileRegion.northEast.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( 0, mapTileRegion.northEast.latitude, mapTileRegion.northEast.longitude ))
        vertices.add( LatLon( 0, mapTileRegion.southWest.latitude, mapTileRegion.northEast.longitude ))

        vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0, Color.BLACK )
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.value)
                composableInputDialogHost.updateQrText(payload.toString())
            }
        }

    fun createEnumArea( name: String )
    {
        val vertices = ArrayList<LatLon>()

        var creationDate = Date().time

        for (point in polyLinePoints)
        {
            vertices.add( LatLon( creationDate++, point.latitude(), point.longitude()))
        }

        if (vertices.size > 2)
        {
            var name2 = name

            if (name2.isEmpty())
            {
                name2 = "${resources.getString(R.string.enumeration_area)} 1"
            }

            val latLngBounds = GeoUtils.findGeobounds(vertices)
            val northEast = LatLon( 0, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
            val southWest = LatLon( 0, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )
            val mapTileRegion = MapTileRegion( northEast, southWest, "" )
            val enumArea = EnumArea( config.uuid,"", name2, "", 0, vertices, mapTileRegion )

            mapTileRegion.enumAreaUuid = enumArea.uuid

            val enumerationTeam = EnumerationTeam( enumArea.uuid, "Auto Gen", enumArea.vertices, ArrayList<String>())

            config.enumAreas.add( enumArea )
            config.selectedEnumAreaUuid = enumArea.uuid

            enumArea.enumerationTeams.add( enumerationTeam )
            enumArea.selectedEnumerationTeamUuid = enumerationTeam.uuid

            DAO.configDAO.createOrUpdateConfig( config, UUID.randomUUID().toString())

            binding.saveButton.isEnabled = false
            binding.walkButton.isEnabled = false
            binding.addPointButton.isEnabled = false
            binding.deletePointButton.isEnabled = false

            if (config.studies.isNotEmpty() && config.studies.first().samplingMethod == SamplingMethod.Strata)
            {
                presentStrataSelectionDialog( enumArea )
            }

            refreshMap()
        }
    }

    fun presentStrataSelectionDialog( enumArea: EnumArea )
    {
        val study = config.studies.first()

        composableDropdownDialogHost.showStrata(title = resources.getString(R.string.select_strata), strataList = study.stratas) { strata ->
            strata?.let { strata ->
                enumArea.strataUuid = strata.uuid
                if (enumArea.name.contains("[") && enumArea.name.contains("]"))
                {
                    enumArea.name = enumArea.name.replace(Regex("\\[.*?]"), "[" + strata.name + "]")
                }
                else
                {
                    enumArea.name = enumArea.name + "-[" + strata.name + "]"
                }

                DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, UUID.randomUUID().toString())

                refreshMap()
            }
        }
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->
            if (p1.action == MotionEvent.ACTION_DOWN)
            {
                binding.mapOverlayView.visibility = View.GONE
                binding.addPointButton.setBackgroundTintList(defaultColorList)

                startPoint = MapManager.instance().getLocationFromPixelPoint( mapView, p1 )

                composableInputDialogHost.show(
                    title = null,
                    description = resources.getString(R.string.map_tile_boundary),
                    text = "",
                    inputTypeNumber = true,
                    cancelable = true,
                    onResult = { text ->
                        if (text.isNotEmpty())
                        {
                            text.toDoubleOrNull()?.let {
                                val radius = it * 1000
                                val r_earth = 6378000.0

                                startPoint?.let { point ->
                                    var latitude  = point.latitude()  + (radius / r_earth) * (180.0 / Math.PI)
                                    var longitude = point.longitude() + (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                                    val northEast = LatLon( 0, latitude, longitude )

                                    latitude  = point.latitude()  - (radius / r_earth) * (180.0 / Math.PI)
                                    longitude = point.longitude() - (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                                    val southWest = LatLon( 0, latitude, longitude )

                                    polyLinePoints.clear()
                                    val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

                                    polyLinePoints.add( com.mapbox.geojson.Point.fromLngLat( northEast.longitude, northEast.latitude ))
                                    polyLinePoints.add( com.mapbox.geojson.Point.fromLngLat( northEast.longitude, southWest.latitude ))
                                    polyLinePoints.add( com.mapbox.geojson.Point.fromLngLat( southWest.longitude, southWest.latitude ))
                                    polyLinePoints.add( com.mapbox.geojson.Point.fromLngLat( southWest.longitude, northEast.latitude ))
                                    polyLinePoints.add( com.mapbox.geojson.Point.fromLngLat( northEast.longitude, northEast.latitude ))

                                    pointList.add( polyLinePoints )

                                    MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40 )

                                    composableInputDialogHost.show(
                                        title = null,
                                        description = resources.getString(R.string.enter_enum_area_name),
                                        text = "",
                                        cancelable = true,
                                        onQrClick = {
                                            val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                            getResult.launch(intent)
                                        },
                                        onResult = { text ->
                                            if (text.isNotEmpty())
                                            {
                                                createEnumArea(text )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        return true
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
                val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                MapManager.instance().selectMap( requireActivity(), config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null ) { mapView ->
                    refreshMap()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

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
        }
    }

    override fun onDestroyView()
    {
        fusedLocationClient.removeLocationUpdates( locationCallback )

        _binding = null

        super.onDestroyView()
    }
}