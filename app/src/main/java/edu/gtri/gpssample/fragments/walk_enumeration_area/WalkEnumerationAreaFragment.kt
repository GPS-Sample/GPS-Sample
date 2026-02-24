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
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class WalkEnumerationAreaFragment : Fragment(),
    View.OnTouchListener,
    MapManager.MapManagerDelegate
{
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var defaultColorList : ColorStateList
    private lateinit var fusedLocationClient : FusedLocationProviderClient

    private var inputDialog: InputDialog? = null
    private var selectedEnumArea: EnumArea? = null

    private var isRecording = false
    private val binding get() = _binding!!
    private var showCurrentLocation = true
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

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        binding.centerOnLocationButton.backgroundTintList?.let {
            defaultColorList = it
            binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }

        if (config.enumAreas.isNotEmpty() && config.enumAreas[0].mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( config.enumAreas[0].mbTilesPath )
        }

        binding.mapOverlayView.visibility = View.GONE

        val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, zoom,this ) { mapView ->
            this.mapView = mapView
            MapManager.instance().enableLocationUpdates( activity!!, mapView )
//            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

            if (config.enumAreas.isNotEmpty())
            {
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                    MapManager.instance().centerMap( config.enumAreas[0], currentZoomLevel, mapView )
                }
            }
            else
            {
                MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                    MapManager.instance().setZoomLevel( mapView, currentZoomLevel )
                }
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

        if (config.enumAreas.isNotEmpty())
        {
            binding.saveButton.isEnabled = false
            binding.walkButton.isEnabled = false
            binding.addPointButton.isEnabled = false
            binding.deletePointButton.isEnabled = false
        }

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
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
                        MapManager.instance().createMarker( activity!!, mapView, point, R.drawable.location_blue, "" )
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
                                Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.polygon_is_self_intersecting), Toast.LENGTH_LONG).show()
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
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.define_center), Toast.LENGTH_SHORT).show()
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
                ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_point), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                    when( buttonPressed )
                    {
                        ConfirmationDialog.ButtonPress.Left -> {
                        }
                        ConfirmationDialog.ButtonPress.Right -> {
                            polyLinePoints.removeAt(polyLinePoints.lastIndex)

                            MapManager.instance().clearMap( mapView )

                            if (polyLinePoints.isNotEmpty())
                            {
                                startPoint?.let {
                                    MapManager.instance().createMarker( activity!!, mapView, it, R.drawable.location_blue, "" )
                                }
                            }

                            if (polyLinePoints.size > 1)
                            {
                                MapManager.instance().createPolyline( mapView, polyLinePoints, Color.RED )
                            }
                        }
                        ConfirmationDialog.ButtonPress.None -> {
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
                MapManager.instance().startCenteringOnLocation( activity!!, mapView )
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                MapManager.instance().stopCenteringOnLocation( mapView )
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.helpButton.setOnClickListener {
            WalkEnumerationHelpHelpDialog( activity!! )
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

                inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaName, false )  { action, text, tag ->
                    when (action) {
                        InputDialog.Action.DidCancel -> {}
                        InputDialog.Action.DidEnterText -> {createEnumArea( text )}
                        InputDialog.Action.DidPressQRButton -> {
                            val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                            getResult.launch(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.WalkEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun clearMap()
    {
        ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.clear_map), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
            when( buttonPressed )
            {
                ConfirmationDialog.ButtonPress.Left -> {
                }
                ConfirmationDialog.ButtonPress.Right -> {
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

                    DAO.configDAO.createOrUpdateConfig( config )
                }
                ConfirmationDialog.ButtonPress.None -> {
                }
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
                inputDialog?.editText?.let { editText ->
                    editText.setText( payload.toString())
                }
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

            val mapTileRegion = MapTileRegion( northEast, southWest )

            selectedEnumArea = EnumArea( config.uuid,"", name2, "", 0, vertices, mapTileRegion )
            config.enumAreas.add( selectedEnumArea!! )

            DAO.configDAO.createOrUpdateConfig( config )?.let { config ->
                config.enumAreas[0].let { enumArea ->
                    DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( EnumerationTeam( enumArea.uuid, "Auto Gen", enumArea.vertices, ArrayList<String>()))?.let { enumerationTeam ->
                        enumArea.enumerationTeams.add( enumerationTeam )
                    }
                }
            }

            binding.saveButton.isEnabled = false
            binding.walkButton.isEnabled = false
            binding.addPointButton.isEnabled = false
            binding.deletePointButton.isEnabled = false

            if (config.studies.isNotEmpty() && config.studies.first().samplingMethod == SamplingMethod.Strata)
            {
                selectedEnumArea?.let { selectedEnumArea ->
                    presentStrataSelectionDialog( selectedEnumArea )
                }
            }

            refreshMap()
        }
    }

    fun presentStrataSelectionDialog( enumArea: EnumArea )
    {
        val study = config.studies.first()

        DropdownDialog(requireActivity(), resources.getString(R.string.select_strata), study.stratas, null ) { strata ->
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

                DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )

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

                val inputDialog = InputDialog( activity!!, false, resources.getString(R.string.map_tile_boundary), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null )  { action, text, tag ->
                    when (action) {
                        InputDialog.Action.DidCancel -> {}
                        InputDialog.Action.DidEnterText -> {
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

                                    inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaName, false )  { action, text, tag ->
                                        when (action) {
                                            InputDialog.Action.DidCancel -> {}
                                            InputDialog.Action.DidEnterText -> {createEnumArea(text)}
                                            InputDialog.Action.DidPressQRButton -> {
                                                val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                                getResult.launch(intent)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        InputDialog.Action.DidPressQRButton -> {}
                    }
                }
                inputDialog.editText?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
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
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
                editor.commit()

                val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, zoom,this ) { mapView ->
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, Style.SATELLITE_STREETS )
                editor.commit()

                val zoom = sharedViewModel.currentZoomLevel?.value ?: 0.0

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, zoom,this ) { mapView ->
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

    override fun onMarkerTapped( location: Location )
    {
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