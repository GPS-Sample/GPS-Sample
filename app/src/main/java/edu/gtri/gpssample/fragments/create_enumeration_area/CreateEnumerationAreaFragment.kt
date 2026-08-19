/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.create_enumeration_area

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.*
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.MapManager.Companion.GEORGIA_TECH
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.PreferencesManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.ui.compose.ComposableCheckboxDialogHost
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialogHost
import edu.gtri.gpssample.ui.compose.ComposableInputDialogHost
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialogHost
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateEnumerationAreaFragment : Fragment(),
    OnMapClickListener,
    View.OnTouchListener,
    OnCameraChangeListener,
    MapManager.MapTileCacheDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private lateinit var config: Config
    private lateinit var mapboxManager: MapboxManager
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel
    private var editMode = false
    private val binding get() = _binding!!
    private var showCurrentLocation = false
    private var selectedEnumArea: EnumArea? = null
    private val polygonHashMap = HashMap<String,Any>()
    private var point: com.mapbox.geojson.Point? = null
    private var propertySelections = ArrayList<String>()
    private val unsavedEnumAreas = ArrayList<EnumArea>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var allPointAnnotations = ArrayList<PointAnnotation>()
    private var _binding: FragmentCreateEnumerationAreaBinding? = null
    private var allPolygonAnnotations = ArrayList<PolygonAnnotation>()
    private var droppedPointAnnotations = ArrayList<PointAnnotation?>()
    private var allPolylineAnnotations = ArrayList<PolylineAnnotation>()
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private lateinit var composableInputDialogHost: ComposableInputDialogHost
    private lateinit var composableCheckboxDialogHost: ComposableCheckboxDialogHost
    private lateinit var composableSelectionDialogHost: ComposableSelectionDialogHost
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost

    enum class TapType {
        None,
        EditEnumArea,
        CreateEnumAreaLocation,
        AddHousehold,
        CreateEnumAreaBoundary
    }

    private var currentTapType = TapType.None

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateEnumerationAreaBinding.inflate( inflater, container, false)
        return binding.root
    }

    private var debugPressCount = 0
    private var shouldAutoEnumerateLocations = false
    private var timeOfLastPress : Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = this.viewModel
        }

        composableInputDialogHost = ComposableInputDialogHost()
        composableCheckboxDialogHost = ComposableCheckboxDialogHost()
        composableSelectionDialogHost = ComposableSelectionDialogHost()
        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableInputDialogHost.Content()
            composableCheckboxDialogHost.Content()
            composableSelectionDialogHost.Content()
            composableConfirmationDialogHost.Content()
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        arguments?.getBoolean(Keys.kEditMode.value)?.let { editMode ->
            this.editMode = editMode
        }

        if (!editMode)
        {
            binding.toolbarTitle.visibility = View.GONE
            binding.toolbarLayout.visibility = View.GONE
            binding.buttonLayout.visibility = View.GONE
        }

        binding.mapboxMapView.gestures.addOnMapClickListener(this )

        MapManager.instance().centerMap( GEORGIA_TECH, binding.mapboxMapView )

        if (config.enumAreas.isNotEmpty())
        {
            val enumArea = config.enumAreas[0]

            MapboxManager.centerMap( enumArea, MapManager.zoomLevel(), binding.mapboxMapView.getMapboxMap())

            if (enumArea.mbTilesPath.isNotEmpty())
            {
                TileServer.startServer( activity!!, null, enumArea.mbTilesPath, binding.mapboxMapView.getMapboxMap()) {
                    initLocationComponent()
                    refreshMap()
                }
            }
            else
            {
                TileServer.loadMapboxStyle( activity!!, binding.mapboxMapView.getMapboxMap()) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        }
        else
        {
            TileServer.loadMapboxStyle( activity!!, binding.mapboxMapView.getMapboxMap()) {
                initLocationComponent()
                refreshMap()
            }
        }

        mapboxManager = MapboxManager.instance( activity!! )

        if (config.enumAreas.isEmpty())
        {
            showCurrentLocation = true
            val locationComponentPlugin = binding.mapboxMapView.location
            locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            binding.mapboxMapView.gestures.addOnMoveListener(onMoveListener)
            binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }

        binding.centerOnLocationButton.setOnClickListener {
            showCurrentLocation = !showCurrentLocation

            if (showCurrentLocation)
            {
                val locationComponentPlugin = binding.mapboxMapView.location
                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                binding.mapboxMapView.gestures.addOnMoveListener(onMoveListener)
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                binding.mapboxMapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                binding.mapboxMapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                binding.mapboxMapView.gestures.removeOnMoveListener(onMoveListener)
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.deleteButton.setOnClickListener {
            composableConfirmationDialogHost.show(
                title = resources.getString(R.string.delete_all_enumeration_areas_message),
                message = "",
                leftButtonText = resources.getString(R.string.no),
                rightButtonText = resources.getString(R.string.yes),
                destructive = true
            ) { selection ->
                if (selection == resources.getString(R.string.yes)) {
                    unsavedEnumAreas.clear()
                    for (enumArea in config.enumAreas)
                    {
                        DAO.enumAreaDAO.delete( enumArea )
                    }
                    config.enumAreas.clear()
                    PreferencesManager.removeAllHashes(config.uuid )
                    refreshMap()
                }
            }
        }

        binding.importButton.setOnClickListener {
            if (currentTapType != TapType.None)
            {
                return@setOnClickListener
            }

            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

            composableSelectionDialogHost.show(
                title = resources.getString(R.string.select_file_type),
                message = null,
                items = listOf(resources.getString(R.string.import_geojson),resources.getString(R.string.import_mbtiles)),
            ) { selection ->
                if (selection == resources.getString(R.string.import_geojson))
                {
                    val intent = Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)

                    startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_enumeration)), 1023)
                }
                else if (selection == resources.getString(R.string.import_mbtiles))
                {
                    filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                }
            }
        }

        binding.createEnumAreaButton.setOnClickListener {
            if (currentTapType == TapType.None)
            {
                composableSelectionDialogHost.show(
                    title = resources.getString(R.string.creation_options),
                    message = null,
                    items = listOf(resources.getString(R.string.set_boundary),resources.getString(R.string.set_location)),
                ) { selection ->
                    if (selection == resources.getString(R.string.set_boundary))
                    {
                        currentTapType = TapType.CreateEnumAreaBoundary
                        droppedPointAnnotations.clear()
                        binding.createEnumAreaButton.setBackgroundResource( R.drawable.save_blue )
                        binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.define_boundary), Toast.LENGTH_SHORT).show()
                    }
                    else if (selection == resources.getString(R.string.set_location))
                    {
                        currentTapType = TapType.CreateEnumAreaLocation
                        droppedPointAnnotations.clear()
                        binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                        refreshMap()
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.define_center), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else if (currentTapType == TapType.CreateEnumAreaLocation)
            {
                currentTapType = TapType.None
                binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
            }
            else if (currentTapType == TapType.CreateEnumAreaBoundary)
            {
                currentTapType = TapType.None
                binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);

                if (droppedPointAnnotations.size > 2)
                {
                    if (GeoUtils.isSelfIntersectingPolygon2( droppedPointAnnotations ))
                    {
                        droppedPointAnnotations.map { pointAnnotation ->
                            pointAnnotation?.let{ pointAnnotation ->
                                pointAnnotationManager?.delete( pointAnnotation )
                            }
                        }

                        droppedPointAnnotations.clear()

                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.polygon_is_self_intersecting), Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        composableInputDialogHost.show(
                            title = resources.getString(R.string.enter_enum_area_name),
                            text = "",
                            onResult = { text ->
                                if (text.isEmpty())
                                {
                                    droppedPointAnnotations.map { pointAnnotation ->
                                        pointAnnotation?.let{ pointAnnotation ->
                                            pointAnnotationManager?.delete( pointAnnotation )
                                        }
                                    }
                                    droppedPointAnnotations.clear()
                                }
                                else
                                {
                                    createEnumArea( text )
                                }
                            }
                        )
                    }
                }
                else
                {
                    droppedPointAnnotations.map { pointAnnotation ->
                        pointAnnotation?.let{ pointAnnotation ->
                            pointAnnotationManager?.delete( pointAnnotation )
                        }
                    }

                    droppedPointAnnotations.clear()
                }
            }
        }

        binding.mapOverlayView.visibility = View.GONE
        binding.mapOverlayView.setOnTouchListener(this)

        binding.mapTileCacheButton.setOnClickListener {
            if (currentTapType != TapType.None)
            {
                return@setOnClickListener
            }

            val mapTileRegions = getAllMapTileRegions()

            if (mapTileRegions.isNotEmpty())
            {
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                MapManager.instance().cacheMapTiles(activity!!, binding.mapboxMapView, mapTileRegions, this )
            }
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.editLocationButton.setOnClickListener {
            if (currentTapType == TapType.None)
            {
                currentTapType = TapType.EditEnumArea
                binding.mapOverlayView.visibility = View.VISIBLE
                binding.editLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.select_ea), Toast.LENGTH_SHORT).show()
            }
            else if (currentTapType == TapType.EditEnumArea)
            {
                currentTapType = TapType.None
                binding.mapOverlayView.visibility = View.GONE
                binding.editLocationButton.setBackgroundTintList( defaultColorList );
            }
        }

        binding.addHouseholdButton.setOnClickListener {
            if (currentTapType == TapType.None)
            {
                currentTapType = TapType.AddHousehold
                binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else if (currentTapType == TapType.AddHousehold)
            {
                currentTapType = TapType.None
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.legendImageView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.helpButton.setOnClickListener {
            CreateEnumAreaHelpDialog( activity!! )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            config.enumAreas.addAll( unsavedEnumAreas )

            for (enumArea in config.enumAreas)
            {
                enumArea.version = UUID.randomUUID().toString()
            }

            findNavController().popBackStack()
        }

        binding.toolbarTitle.setOnClickListener {
            if (!shouldAutoEnumerateLocations)
            {
                val timeSpan = Date().time - timeOfLastPress

                if (timeSpan > 2000)
                {
                    debugPressCount = 0
                }
                else
                {
                    debugPressCount += 1
                    if (debugPressCount == 6)
                    {
                        shouldAutoEnumerateLocations = true
                        Toast.makeText(activity!!.applicationContext,  "Imported locations will be Auto Enumerated!", Toast.LENGTH_SHORT).show()
                    }
                }

                timeOfLastPress = Date().time
            }
        }
    }

    fun createAnnotationManagers()
    {
        pointAnnotationManager = mapboxManager.createPointAnnotationManager( pointAnnotationManager, binding.mapboxMapView )
        polygonAnnotationManager = mapboxManager.createPolygonAnnotationManager( polygonAnnotationManager, binding.mapboxMapView )
        polylineAnnotationManager = mapboxManager.createPolylineAnnotationManager( polylineAnnotationManager, binding.mapboxMapView )
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onMapClick(point: com.mapbox.geojson.Point): Boolean
    {
        if (editMode)
        {
            if (currentTapType == TapType.CreateEnumAreaBoundary)
            {
                droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, point, R.drawable.location_blue ))
                return true
            }
            else if (currentTapType == TapType.CreateEnumAreaLocation)
            {
                this.point = point
                currentTapType = TapType.None
                binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);

                composableInputDialogHost.show(
                    title = resources.getString(R.string.map_tile_boundary),
                    text = "",
                    inputTypeNumber = true,
                    onResult = { text ->
                        if (text.isEmpty())
                        {
                            droppedPointAnnotations.map { pointAnnotation ->
                                pointAnnotation?.let{ pointAnnotation ->
                                    pointAnnotationManager?.delete( pointAnnotation )
                                }
                            }
                            droppedPointAnnotations.clear()
                        }
                        else
                        {
                            text.toDoubleOrNull()?.let {
                                val radius = it * 1000
                                val r_earth = 6378000.0

                                point?.let { point ->
                                    var latitude  = point.latitude()  + (radius / r_earth) * (180.0 / Math.PI)
                                    var longitude = point.longitude() + (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                                    val northEast = LatLon( 0, latitude, longitude )

                                    latitude  = point.latitude()  - (radius / r_earth) * (180.0 / Math.PI)
                                    longitude = point.longitude() - (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                                    val southWest = LatLon( 0, latitude, longitude )

                                    var p = com.mapbox.geojson.Point.fromLngLat( northEast.longitude, northEast.latitude )
                                    droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, p, R.drawable.location_blue ))

                                    p =com.mapbox.geojson.Point.fromLngLat( northEast.longitude, southWest.latitude )
                                    droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, p, R.drawable.location_blue ))

                                    p = com.mapbox.geojson.Point.fromLngLat( southWest.longitude, southWest.latitude )
                                    droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, p, R.drawable.location_blue ))

                                    p = com.mapbox.geojson.Point.fromLngLat( southWest.longitude, northEast.latitude )
                                    droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, p, R.drawable.location_blue ))

                                    p = com.mapbox.geojson.Point.fromLngLat( northEast.longitude, northEast.latitude )
                                    droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, p, R.drawable.location_blue ))

                                    composableInputDialogHost.show(
                                        title = resources.getString(R.string.enter_enum_area_name),
                                        text = "",
                                        onQrClick = {
                                            val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                            getResult.launch(intent)
                                        },
                                        onResult = {text ->
                                            if (text.isNotEmpty())
                                            {
                                                createEnumArea( text )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                return true
            }
            else if (currentTapType == TapType.AddHousehold)
            {
                currentTapType = TapType.None
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
                createLocation( point.latitude(), point.longitude(), point.altitude())
                refreshMap()
                return true
            }
        }

        return false
    }

    private fun refreshMap()
    {
        binding.mapboxMapView.getMapboxMap().removeOnCameraChangeListener( this )

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

        val allEnumAreas = getAllEnumAreas()

        createAnnotationManagers()

        for (enumArea in allEnumAreas)
        {
            enumArea.mapTileRegion?.let {
                addPolygon(it)
            }

            addPolygon(enumArea)
        }

        MapManager.instance().loadMarkers( activity!!, binding.mapboxMapView, allEnumAreas )

        binding.mapboxMapView.getMapboxMap().addOnCameraChangeListener(this)
    }

    fun getAllEnumAreas() : ArrayList<EnumArea>
    {
        val allEnumAreas = ArrayList<EnumArea>()
        allEnumAreas.addAll( config.enumAreas )
        allEnumAreas.addAll( unsavedEnumAreas )
        return allEnumAreas
    }

    fun getAllMapTileRegions() : ArrayList<MapTileRegion>
    {
        val allMapTileRegions = ArrayList<MapTileRegion>()
        val allEnumAreas = getAllEnumAreas()

        for (enumArea in allEnumAreas)
        {
            enumArea.mapTileRegion?.let {
                allMapTileRegions.add( it )
            }
        }

        return allMapTileRegions
    }
    fun createLocation( latitude: Double, longitude: Double, altitude: Double )
    {
        var enumArea = findEnumAreaOfLocation( config.enumAreas, LatLng( latitude, longitude ))

        if (enumArea == null)
        {
            enumArea = findEnumAreaOfLocation( unsavedEnumAreas, LatLng( latitude, longitude ))
        }

        enumArea?.let{  enumArea ->
            val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
            val location = Location( timeZone, -1, latitude, longitude, altitude, false, "", "")
            enumArea.locations.add(location)
            refreshMap()
        }
    }

    fun findEnumAreaOfLocation( enumAreas: ArrayList<EnumArea>, latLng: LatLng ) : EnumArea?
    {
        for (enumArea in enumAreas)
        {
            val points = ArrayList<Coordinate>()

            enumArea.vertices.map {
                points.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
            }

            points.add( points[0])

            val geometryFactory = GeometryFactory()
            val geometry: Geometry = geometryFactory.createPolygon(points.toTypedArray())

            val coordinate = Coordinate( latLng.longitude, latLng.latitude )
            val geometry1 = geometryFactory.createPoint( coordinate )
            if (geometry.contains( geometry1 ))
            {
                return enumArea
            }
        }

        return null
    }

    fun addPolygon( mapTileRegion: MapTileRegion )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        val vertices = ArrayList<LatLon>()

        var creationDate = Date().time

        vertices.add( LatLon( creationDate++, mapTileRegion.southWest.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( creationDate++, mapTileRegion.northEast.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( creationDate++,mapTileRegion.northEast.latitude, mapTileRegion.northEast.longitude ))
        vertices.add( LatLon( creationDate++, mapTileRegion.southWest.latitude, mapTileRegion.northEast.longitude ))

        vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        val polygonAnnotation = mapboxManager.addPolygon( polygonAnnotationManager, pointList, "#000000", 0.0 )

        polygonAnnotation?.let { polygonAnnotation ->
            polygonHashMap[polygonAnnotation.id] = mapTileRegion
            allPolygonAnnotations.add( polygonAnnotation)
        }

        // create the polygon border
        val polylineAnnotation = mapboxManager.addPolyline( polylineAnnotationManager, pointList[0], "#000000" )

        polylineAnnotation?.let { polylineAnnotation ->
            allPolylineAnnotations.add( polylineAnnotation )
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

        val polygonAnnotation = mapboxManager.addPolygon( polygonAnnotationManager, pointList, "#000000", 0.25 )

        polygonAnnotation?.let { polygonAnnotation ->
            polygonHashMap[polygonAnnotation.id] = enumArea
            allPolygonAnnotations.add( polygonAnnotation)
        }

        // create the polygon border
        val polylineAnnotation = mapboxManager.addPolyline( polylineAnnotationManager, pointList[0], "#ff0000" )

        polylineAnnotation?.let { polylineAnnotation ->
            allPolylineAnnotations.add( polylineAnnotation )
        }

        // add the label
        val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
        val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
        mapboxManager.addLabelAtPoint( binding.mapboxMapView, point, enumArea.name, "#80FFFFFF" )
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->
            if (p1.action == MotionEvent.ACTION_UP) {
                val point = MapManager.instance().getLocationFromPixelPoint(binding.mapboxMapView, p1)

                when( currentTapType )
                {
                    TapType.EditEnumArea -> {
                        findEnumAreaOfLocation(
                            getAllEnumAreas(),
                            LatLng(point.latitude(), point.longitude())
                        )?.let { enumArea ->
                            currentTapType = TapType.None
                            binding.mapOverlayView.visibility = View.GONE
                            binding.editLocationButton.backgroundTintList = defaultColorList

                            val items = ArrayList<String>()
                            items.add(resources.getString(R.string.rename))
                            items.add(resources.getString(R.string.delete))
                            items.add(resources.getString(R.string.attach_mbtiles))
                            items.add(resources.getString(R.string.detach_mbtiles))

                            if (config.studies.isNotEmpty() && config.studies.first().samplingMethod == SamplingMethod.Strata)
                            {
                                items.add(resources.getString(R.string.select_strata))
                            }

                            composableSelectionDialogHost.show(
                                title = resources.getString(R.string.select_task),
                                message = null,
                                items = items,
                            ) { selection ->
                                when (selection) {
                                    resources.getString(R.string.rename) -> {
                                        composableInputDialogHost.show(
                                            title = resources.getString(R.string.enter_enum_area_name),
                                            text = enumArea.name,
                                            onQrClick = {
                                                val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                                getResult.launch(intent)
                                            },
                                            onResult = { text ->
                                                if (text.isNotEmpty())
                                                {
                                                    mapboxManager.removeViewAnnotation( binding.mapboxMapView.viewAnnotationManager, enumArea.name )
                                                    enumArea.name = text // handles re-name
                                                    refreshMap()
                                                }
                                            }
                                        )
                                    }
                                    resources.getString(R.string.delete) -> {
                                        mapboxManager.removeViewAnnotation( binding.mapboxMapView.viewAnnotationManager, enumArea.name, )
                                        unsavedEnumAreas.remove( enumArea )
                                        config.enumAreas.remove( enumArea )
                                        DAO.enumAreaDAO.delete( enumArea )

                                        refreshMap()
                                    }
                                    resources.getString(R.string.attach_mbtiles) -> {
                                        selectedEnumArea = enumArea
                                        filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                                    }
                                    resources.getString(R.string.detach_mbtiles) -> {
                                        enumArea.mbTilesSize = 0
                                        enumArea.mbTilesPath = ""
                                    }
                                    resources.getString(R.string.select_strata) -> {
                                        presentStrataSelectionDialog( enumArea )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        return true
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
        currentTapType = TapType.None

        val vertices = ArrayList<LatLon>()

        var creationDate = Date().time

        droppedPointAnnotations.map { pointAnnotation ->
            pointAnnotation?.let{ pointAnnotation ->
                vertices.add( LatLon( creationDate++, pointAnnotation.point.latitude(), pointAnnotation.point.longitude()))
                pointAnnotationManager?.delete( pointAnnotation )
            }
        }

        val latLngBounds = GeoUtils.findGeobounds(vertices)
        val northEast = LatLon( 0, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
        val southWest = LatLon( 0, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

        val mapTileRegion = MapTileRegion( northEast, southWest, "" )

        if (name.isEmpty())
        {
            selectedEnumArea = EnumArea( config.uuid, "", "${resources.getString(R.string.enumeration_area)} ${unsavedEnumAreas.size + 1}", "", 0, vertices, mapTileRegion )
            unsavedEnumAreas.add( selectedEnumArea!! )
        }
        else
        {
            selectedEnumArea = EnumArea( config.uuid, "", name, "", 0, vertices, mapTileRegion )
            unsavedEnumAreas.add( selectedEnumArea!! )
        }

        mapTileRegion.enumAreaUuid = selectedEnumArea!!.uuid

        refreshMap()

        if (config.studies.isNotEmpty() && config.studies.first().samplingMethod == SamplingMethod.Strata)
        {
            selectedEnumArea?.let { selectedEnumArea ->
                presentStrataSelectionDialog( selectedEnumArea )
            }
        }
        else
        {
            presentMBTilesDialog()
        }
    }

    fun presentMBTilesDialog()
    {
        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.attach_mbtiles_question),
            message = "",
            leftButtonText = resources.getString(R.string.no),
            rightButtonText = resources.getString(R.string.yes),
            destructive = false
        ) { selection ->
            if (selection == resources.getString(R.string.yes)) {
                val cachedFiles = TileServer.getCachedFiles( activity!! )
                if (cachedFiles.isNotEmpty())
                {
                    composableSelectionDialogHost.show(
                        title = resources.getString(R.string.select_map_tiles),
                        message = null,
                        items = cachedFiles,
                    ) { selection ->
                        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection
                        TileServer.startServer( activity!!, null, mbTilesPath, binding.mapboxMapView.getMapboxMap()) {
                            refreshMap()
                            TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), MapManager.zoomLevel() )
                        }
                    }
                }
                else
                {
                    filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                }
            }
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
                refreshMap()
                presentMBTilesDialog()
            }
        }
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress("${numLoaded}/${numNeeded}")
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
        MapManager.instance().cancelTilePackDownload()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        try {
            if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
            {
                data?.data?.let { uri ->
                    activity!!.getContentResolver().openInputStream(uri)?.let {
                        val json = it.bufferedReader().readText()

                        val featureCollection = FeatureCollection.fromJson( json )

                        if (featureCollection.features.isNotEmpty())
                        {
                            val feature = featureCollection.features[0]

                            feature.geometry?.let { geometry ->
                                val items = ArrayList(feature.properties.keys)
                                when (geometry) {
                                    is Polygon,
                                    is MultiPolygon -> {
                                        DropdownDialog( activity!!, resources.getString(R.string.select_the_property_identifier), items ) { propertySelection ->
                                            if (config.studies.isNotEmpty() && config.studies.first().samplingMethod == SamplingMethod.Strata)
                                            {
                                                DropdownDialog(requireActivity(), resources.getString(R.string.select_the_strata_identifier), items ) { strataSelection ->
                                                    if (strataSelection.isNotEmpty())
                                                    {
                                                        processGeoJson( json, propertySelection, strataSelection )
                                                    }
                                                    else
                                                    {
                                                        processGeoJson( json, propertySelection )
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                processGeoJson( json, propertySelection )
                                            }
                                        }
                                    }
                                    is Point -> {
                                        composableCheckboxDialogHost.show(
                                            title = resources.getString(R.string.select_the_hh_identifiers),
                                            items = items,
                                            isChecked = emptyList()
                                        ) { selections ->
                                            propertySelections = selections
                                            processGeoJson( json, "" )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                        else
                        {
                            processGeoJson( json, "" )
                        }
                    }
                }
            }
        } catch( ex: Exception )
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
            Log.d( "xxx", ex.stackTraceToString())
        }
    }

    fun processGeoJson( json: String, nameKey: String, strataKey: String = "" )
    {
        val hash = PreferencesManager.computeHash( json )

        if (PreferencesManager.isHashImported(config.uuid, hash ))
        {
            composableConfirmationDialogHost.show(
                title = resources.getString(R.string.oops),
                message = resources.getString(R.string.duplicate_import),
                leftButtonText = resources.getString(R.string.no),
                rightButtonText = resources.getString(R.string.yes),
                destructive = false
            ) { selection ->
                if (selection == resources.getString(R.string.yes)) {
                    finishProcessGeoJson( json, nameKey, strataKey )
                }
            }
        }
        else
        {
            PreferencesManager.saveHash(config.uuid, hash )
            finishProcessGeoJson( json, nameKey, strataKey )
        }
    }

    fun finishProcessGeoJson( json: String, nameKey: String, strataKey: String = "" )
    {
        if (json.isEmpty())
        {
        }
        else
        {
            activity!!.runOnUiThread {
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.importing_locations), this, false )
            }

            Thread {
                try
                {
                    DAO.instance().writableDatabase.beginTransaction()
                    parseGeoJson( json, nameKey, strataKey )
                    DAO.instance().writableDatabase.setTransactionSuccessful()
                    DAO.instance().writableDatabase.endTransaction()

                    if (showCurrentLocation && unsavedEnumAreas.isNotEmpty())
                    {
                        activity!!.runOnUiThread {
                            showCurrentLocation = false
                            binding.mapboxMapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                            binding.mapboxMapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                            binding.mapboxMapView.gestures.removeOnMoveListener(onMoveListener)
                            binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                        }
                    }
                }
                catch( ex: Exception)
                {
                    activity!!.runOnUiThread {
                        Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                finally
                {
                    busyIndicatorDialog?.let { busyIndicatorDialog ->
                        activity!!.runOnUiThread {
                            busyIndicatorDialog.alertDialog.cancel()
                        }
                    }
                }
            }.start()
        }
    }

    data class PointWithProperty( var point: Point, var property: String )
    {
    }

    fun parseGeoJson( text: String, nameKey: String, strataKey: String )
    {
        var hasBeenCentered = false
        var creationDate = Date().time
        val points = ArrayList<PointWithProperty>()
        val featureCollection = FeatureCollection.fromJson( text )

        featureCollection.forEach { feature ->

            var name = "${resources.getString(R.string.enumeration_area)} ${unsavedEnumAreas.size + 1}"

            feature.getStringProperty(nameKey)?.let {
                name = it
            }

            var strataName = ""

            feature.getStringProperty(strataKey)?.let {
                strataName = it
            }

            feature.geometry?.let { geometry ->
                when( geometry ) {
                    is Polygon,
                    is MultiPolygon -> {
                        val vertices = ArrayList<LatLon>()

                        if (geometry is Polygon)
                        {
                            geometry.coordinates[0].forEach { position ->
                                vertices.add( LatLon( creationDate++, position.latitude, position.longitude ))
                            }
                        }
                        else if (geometry is MultiPolygon)
                        {
                            val multiPolygon = geometry as MultiPolygon

                            multiPolygon.coordinates[0][0].forEach { position ->
                                vertices.add( LatLon( creationDate++, position.latitude, position.longitude ))
                            }
                        }

                        val latLngBounds = GeoUtils.findGeobounds(vertices)
                        val northEast = LatLon( 0, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
                        val southWest = LatLon( 0, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

                        val mapTileRegion = MapTileRegion( northEast, southWest, "" )

                        val enumArea = EnumArea(creationDate++,config.uuid, "", name, "", 0, vertices, mapTileRegion )

                        mapTileRegion.enumAreaUuid = enumArea.uuid

                        var strata: Strata? = null

                        if (strataName.isNotEmpty())
                        {
                            val study = config.studies.first()

                            for (aStrata in study.stratas)
                            {
                                if (aStrata.name.lowercase() == strataName.lowercase())
                                {
                                    strata = aStrata
                                }
                            }
                        }

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
                        }

                        if (!hasBeenCentered)
                        {
                            hasBeenCentered = true
                            activity!!.runOnUiThread {
                                MapManager.instance().stopCenteringOnLocation( binding.mapboxMapView )
                                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                                MapManager.instance().centerMap( enumArea, binding.mapboxMapView )
                                refreshMap()
                            }
                        }

                        unsavedEnumAreas.add(enumArea)
                    }
                    is Point -> {
                        val jsonArray = ArrayList<JSONObject>()

                        var jsonString = ""

                        for (selection in propertySelections)
                        {
                            if (selection.isNotEmpty())
                            {
                                val value = feature.getStringProperty( selection )

                                if (jsonString.isEmpty())
                                {
                                    jsonString = "{"
                                }
                                else
                                {
                                    jsonString += ", "
                                }

                                jsonString += "\"${selection}\" : \"${value}\""
                                val jsonObject = JSONObject()
                                jsonObject.put( selection, value )
                                jsonArray.add( jsonObject )
                            }
                        }

                        if (jsonString.isNotEmpty())
                        {
                            jsonString += "}"
                        }

                        val point = geometry as Point
                        points.add( PointWithProperty( point, jsonString ))
                    }
                    else -> {}
                }
            }
        }

        // figure out which enumArea contains each point

        if (points.isNotEmpty())
        {
            var count = 0

            for (point in points)
            {
                busyIndicatorDialog?.let {
                    activity!!.runOnUiThread {
                        it.updateProgress("${count}/${points.size}")
                    }
                }

                count += 1

                val allEnumAreas = ArrayList<EnumArea>()

                if (config.enumAreas.isNotEmpty())
                {
                    allEnumAreas.addAll( config.enumAreas)
                }

                if (unsavedEnumAreas.isNotEmpty())
                {
                    allEnumAreas.addAll( unsavedEnumAreas )
                }

                var altitude = 0.0
                point.point.coordinates.altitude?.let {
                    altitude = it
                }

                val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                val location = Location( timeZone, -1, point.point.coordinates.latitude, point.point.coordinates.longitude, altitude, false, "", point.property )

                if (shouldAutoEnumerateLocations)
                {
                    autoEnumerate( location )
                }

                val enumArea = findEnumAreaOfLocation( allEnumAreas, LatLng( point.point.coordinates.latitude, point.point.coordinates.longitude ))?.let { enumArea ->
                    enumArea.locations.add( location )
                }

                if (enumArea == null && allEnumAreas.size == 1)
                {
                    allEnumAreas.first().locations.add( location )
                }
            }
        }

        lifecycleScope.launch {
            refreshMap()
        }
    }

    var subAddress = 0

    fun autoEnumerate( location: Location )
    {
        subAddress+= 1

        val enumerationItem = EnumerationItem()

        enumerationItem.enumerationIncompleteReason = ""
        enumerationItem.enumerationState = EnumerationState.Enumerated
        enumerationItem.enumerationNotes = ""
        enumerationItem.enumerationDate = Date().time
        enumerationItem.subAddress = subAddress.toString()
        enumerationItem.locationUuid = location.uuid

        if (config.studies.isEmpty())
        {
            config.studies.add( Study( "Study", SamplingMethod.Cluster, 10000, SampleType.NumberHouseholds ))
        }

        if (config.studies[0].fields.isEmpty())
        {
            val study = config.studies[0]
            val noteField = Field( null, 1, "Note", FieldType.Note, false, false, false, false, false, false, null, null,study.uuid)
            val textField = Field( null, 2, "Text", FieldType.Text, false, false, false, false, false, false, null, null,study.uuid)
            val numberField = Field( null, 3, "Number", FieldType.Number, false, false, true, false, false, false, null, null,study.uuid)
            val dateField = Field( null, 4, "Date", FieldType.Date, false, false, false, false, true, false, null, null,study.uuid)
            val checkBoxField = Field( null, 5, "Checkbox", FieldType.Checkbox, false, false, false, false, false, false, null, null,study.uuid)
            val dropDownField = Field( null, 6, "Dropdown", FieldType.Dropdown, false, false, false, false, false, false, null, null, study.uuid )

            checkBoxField.fieldOptions.add( FieldOption("CB 1" ))
            checkBoxField.fieldOptions.add( FieldOption("CB 2" ))
            checkBoxField.fieldOptions.add( FieldOption("CB 3" ))

            dropDownField.fieldOptions.add( FieldOption("DD 1" ))
            dropDownField.fieldOptions.add( FieldOption("DD 2" ))
            dropDownField.fieldOptions.add( FieldOption("DD 3" ))

            config.studies[0].fields.add( noteField )
            config.studies[0].fields.add( textField )
            config.studies[0].fields.add( numberField )
            config.studies[0].fields.add( dateField )
            config.studies[0].fields.add( checkBoxField )
            config.studies[0].fields.add( dropDownField )
        }

        var creationDate = Date().time

        for (field in config.studies[0].fields)
        {
            val fieldData = FieldData(creationDate++, field.uuid, enumerationItem.uuid )

            if (field.type == FieldType.Note)
            {
                fieldData.textValue = "Some Note"
            }

            if (field.type == FieldType.Text)
            {
                fieldData.textValue = "Some Text"
            }

            if (field.type == FieldType.Number)
            {
                fieldData.numberValue= 999.0
            }

            if (field.type == FieldType.Date)
            {
                fieldData.dateValue = Date().time
            }

            if (field.type == FieldType.Checkbox)
            {
                fieldData.fieldDataOptions.add( FieldDataOption( "CB 1", true ))
                fieldData.fieldDataOptions.add( FieldDataOption( "CB 2", false ))
                fieldData.fieldDataOptions.add( FieldDataOption( "CB 3", true ))
            }

            if (field.type == FieldType.Dropdown)
            {
                fieldData.dropdownIndex = 1
                fieldData.fieldDataOptions.add( FieldDataOption( "DD 1", false ))
                fieldData.fieldDataOptions.add( FieldDataOption( "DD 2", false ))
                fieldData.fieldDataOptions.add( FieldDataOption( "DD 3", false ))
            }

            enumerationItem.fieldDataList.add( fieldData )
        }

        DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, enumerationItem.version )
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapboxMapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        binding.mapboxMapView.gestures.focalPoint = binding.mapboxMapView.getMapboxMap().pixelForCoordinate(it)
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

    private fun initLocationComponent() {
        binding.mapboxMapView.location.apply {
            locationPuck = createDefault2DPuck(withBearing = true)
            puckBearingEnabled = true
            enabled = true
        }
    }

    private fun onCameraTrackingDismissed() {
        binding.mapboxMapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapboxMapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapboxMapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        MapManager.setZoomLevel( binding.mapboxMapView.getMapboxMap().cameraState.zoom )
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
                val mapStyle = Style.MAPBOX_STREETS
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, mapStyle )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapboxMapView.getMapboxMap()) {
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                val mapStyle = Style.SATELLITE_STREETS
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, mapStyle )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapboxMapView.getMapboxMap()) {
                    refreshMap()
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                val cachedFiles = TileServer.getCachedFiles( activity!! )
                if (cachedFiles.isNotEmpty())
                {
                    composableSelectionDialogHost.show(
                        title = resources.getString(R.string.select_map_tiles),
                        message = null,
                        items = cachedFiles,
                    ) { selection ->
                        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection
                        TileServer.startServer( activity!!, null, mbTilesPath, binding.mapboxMapView.getMapboxMap()) {
                            refreshMap()
                            TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), MapManager.zoomLevel() )
                        }
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val (filePath, fileSize) = TileServer.filePathSize(activity!!, uri)

            if (TileServer.fileExists( activity!!, uri ))
            {
                TileServer.startServer( activity!!, null, filePath, binding.mapboxMapView.getMapboxMap()) {
                    refreshMap()
                    TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), MapManager.zoomLevel() )
                }
            }
            else
            {
                TileServer.startServer( activity!!, uri, "", binding.mapboxMapView.getMapboxMap()) {
                    refreshMap()
                    TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), MapManager.zoomLevel() )
                }
            }

            selectedEnumArea?.let {
                it.mbTilesPath = filePath
                it.mbTilesSize = fileSize
                selectedEnumArea = null
            }
        }
    }

    override fun onDestroyView()
    {
        binding.mapboxMapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapboxMapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapboxMapView.gestures.removeOnMoveListener(onMoveListener)

        _binding = null

        super.onDestroyView()
    }
}