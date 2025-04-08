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
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.util.Property
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.GeoJson
import com.mapbox.maps.*
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.switchCase
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAt
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
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
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.File
import java.util.*

class CreateEnumerationAreaFragment : Fragment(),
    OnMapClickListener,
    View.OnTouchListener,
    OnCameraChangeListener,
    InputDialog.InputDialogDelegate,
    MapboxManager.MapTileCacheDelegate,
    CheckboxDialog.CheckboxDialogDelegate,
    DropdownDialog.DropdownDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate,
    SelectMapTilesDialog.SelectMapTilesDialogDelegate
{
    private lateinit var config: Config
    private lateinit var mapboxManager: MapboxManager
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var editMode = false
    private var addHousehold = false
    private var features = JSONArray()
    private var createMapTileCache = false
    private val binding get() = _binding!!
    private var showCurrentLocation = false
    private var createEnumAreaLocation = false
    private var createEnumAreaBoundary = false
    private var mapStyle = Style.MAPBOX_STREETS
    private var inputDialog: InputDialog? = null
    private val polygonHashMap = HashMap<Long,Any>()
    private var checkboxDialog: CheckboxDialog? = null
    private var point: com.mapbox.geojson.Point? = null
    private val pointHashMap = HashMap<Long,Location>()
    private var propertySelections = ArrayList<String>()
    private val unsavedEnumAreas = ArrayList<EnumArea>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var allPointAnnotations = ArrayList<PointAnnotation>()
    private val polyLinePoints = ArrayList<com.mapbox.geojson.Point>()
    private var _binding: FragmentCreateEnumerationAreaBinding? = null
    private var allPolygonAnnotations = ArrayList<PolygonAnnotation>()
    private var droppedPointAnnotations = ArrayList<PointAnnotation?>()
    private var allPolylineAnnotations = ArrayList<PolylineAnnotation>()
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var polylineAnnotation: PolylineAnnotation? = null

    private val kEnumAreaNameTag: Int = 0
    private val kEnumAreaLengthTag: Int = 1

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (sharedViewModel.currentZoomLevel?.value == null)
        {
            sharedViewModel.setCurrentZoomLevel( 16.0 )
        }

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = this.viewModel

            // Assign the fragment
            createEnumerationAreaFragment = this@CreateEnumerationAreaFragment
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

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value

        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 14.0 )
        }

        binding.mapView.gestures.addOnMapClickListener(this )

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        sharedPreferences.getString( Keys.kMBTilesPath.value, null)?.let { mbTilesPath ->
            if (TileServer.started)
            {
                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    initLocationComponent()
                    refreshMap()
                }
            }
            else
            {
                TileServer.startServer( activity!!, mbTilesPath, binding.mapView.getMapboxMap()) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        } ?: run {
            // no tiles have been loaded, no need to start the server, just load the default map style
            TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                initLocationComponent()
                refreshMap()
            }
        }

        mapboxManager = MapboxManager.instance( activity!! )

        if (config.enumAreas.isEmpty())
        {
            showCurrentLocation = true
            val locationComponentPlugin = binding.mapView.location
            locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            binding.mapView.gestures.addOnMoveListener(onMoveListener)
            binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }

        binding.centerOnLocationButton.setOnClickListener {
            showCurrentLocation = !showCurrentLocation

            if (showCurrentLocation)
            {
                val locationComponentPlugin = binding.mapView.location
                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                binding.mapView.gestures.addOnMoveListener(onMoveListener)
                binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else
            {
                binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                binding.mapView.gestures.removeOnMoveListener(onMoveListener)
                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
            }
        }

        binding.importButton.setOnClickListener {
            if (createEnumAreaBoundary || createEnumAreaLocation || addHousehold || createMapTileCache)
            {
                return@setOnClickListener
            }

            binding.addHouseholdButton.setBackgroundTintList(defaultColorList);

            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_enumeration)), 1023)
        }

        binding.createEnumAreaButton.setOnClickListener {
            if (addHousehold || createMapTileCache)
            {
                return@setOnClickListener
            }

            if (createEnumAreaBoundary)
            {
                createEnumAreaBoundary = false

                if (droppedPointAnnotations.size > 2)
                {
                    if (MapboxManager.isSelfIntersectingPolygon2( droppedPointAnnotations ))
                    {
                        droppedPointAnnotations.map { pointAnnotation ->
                            pointAnnotation?.let{ pointAnnotation ->
                                pointAnnotationManager?.delete( pointAnnotation )
                            }
                        }

                        droppedPointAnnotations.clear()
                        binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                        binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);

                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.polygon_is_self_intersecting), Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaNameTag, this, false )
                        binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                        binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
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
                    binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                    binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
                }
            }
            else
            {
                ConfirmationDialog( activity,
                    resources.getString(R.string.creation_options), "",
                    resources.getString(R.string.set_boundary),
                    resources.getString(R.string.set_location),
                    null,this@CreateEnumerationAreaFragment, true)
            }
        }

        binding.overlayView.setOnTouchListener(this)

//        binding.mapTileRegionButton.setOnClickListener {
//            if (createEnumAreaBoundary || createEnumAreaLocation|| addHousehold)
//            {
//                return@setOnClickListener
//            }
//
//            if (createMapTileCache)
//            {
//                createMapTileCache = false
//                binding.overlayView.visibility = View.GONE
//                binding.mapTileRegionButton.setBackgroundTintList(defaultColorList);
//            }
//            else
//            {
//                createMapTileCache = true
//                polyLinePoints.clear()
//                polylineAnnotation?.let { it ->
//                    it.points = polyLinePoints
//                    polylineAnnotationManager?.update(it)
//                }
//                binding.overlayView.visibility = View.VISIBLE
//                binding.mapTileRegionButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
//            }
//        }

        binding.mapTileCacheButton.setOnClickListener {
            if (createEnumAreaBoundary || createEnumAreaLocation || addHousehold || createMapTileCache)
            {
                return@setOnClickListener
            }

            if (getAllMapTileRegions().isNotEmpty())
            {
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                MapboxManager.loadStylePack( activity!!, this )
            }
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.addHouseholdButton.setOnClickListener {
            if (createEnumAreaBoundary || createEnumAreaLocation || createMapTileCache)
            {
                return@setOnClickListener
            }

            if (addHousehold)
            {
                addHousehold = false
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
            }
            else
            {
                addHousehold = true
                removeAllPolygonOnClickListeners()
                binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
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
            findNavController().popBackStack()
        }
    }

    fun createAnnotationManagers()
    {
        pointAnnotationManager = mapboxManager.createPointAnnotationManager( pointAnnotationManager, binding.mapView )
        polygonAnnotationManager = mapboxManager.createPolygonAnnotationManager( polygonAnnotationManager, binding.mapView )
        polylineAnnotationManager = mapboxManager.createPolylineAnnotationManager( polylineAnnotationManager, binding.mapView )
    }

    fun loadStyle( geoJson: String, completion: (style: Style) -> Unit )
    {
        binding.mapView.getMapboxMap().loadStyle(style(mapStyle) {

            TileServer.rasterSource?.let { rasterSource ->
                +rasterSource
            }

            TileServer.rasterLayer?.let { rasterLayer ->
                +rasterLayer
            }

            +image("home_black") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_black))
            }

            +image("home_blue") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_blue))
            }

            +image("home_green") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_green))
            }

            +image("home_light_blue") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_light_blue))
            }

            +image("home_orange") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_orange))
            }

            +image("home_purple") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_purple))
            }

            +image("home_red") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.home_red))
            }

            +image("multi_home_black") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_black))
            }

            +image("multi_home_blue") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_blue))
            }

            +image("multi_home_green") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_green))
            }

            +image("multi_home_light_blue") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_light_blue))
            }

            +image("multi_home_orange") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_orange))
            }

            +image("multi_home_purple") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_purple))
            }

            +image("multi_home_red") {
                bitmap(BitmapFactory.decodeResource(this@CreateEnumerationAreaFragment.context!!.resources, R.drawable.multi_home_red))
            }

            +symbolLayer("LAYER_ID", "SOURCE_ID") {
                sourceLayer("SOURCE_LAYER_ID")
                iconImage(
                    switchCase {
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_black")
                        }
                        literal("home_black")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_blue")
                        }
                        literal("home_blue")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_green")
                        }
                        literal("home_green")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_light_blue")
                        }
                        literal("home_light_blue")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_orange")
                        }
                        literal("home_orange")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_purple")
                        }
                        literal("home_purple")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("home_red")
                        }
                        literal("home_red")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_black")
                        }
                        literal("multi_home_black")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_blue")
                        }
                        literal("multi_home_blue")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_green")
                        }
                        literal("multi_home_green")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_light_blue")
                        }
                        literal("multi_home_light_blue")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_orange")
                        }
                        literal("multi_home_orange")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_purple")
                        }
                        literal("multi_home_purple")
                        eq {
                            get {
                                literal("POI")
                            }
                            literal("multi_home_red")
                        }
                        literal("multi_home_red")

                        // Default case is to return an empty string so no icon will be loaded
                        literal("home_green")
                    }
                )

                iconAllowOverlap(true)
                iconAnchor(IconAnchor.CENTER)
            }

            +geoJsonSource( "SOURCE_ID" ) {
                this.data(geoJson.toString())
                this.build()
            }
        }) {
            completion( it )
        }
    }

    fun createFeature( lat: Double, lon: Double, icon: String ) : JSONObject
    {
        val geometry = JSONObject()
        geometry.put( "type", "Point" )

        val coordinates = JSONArray()
        coordinates.put( lon )
        coordinates.put( lat )

        geometry.put( "coordinates", coordinates )

        val properties = JSONObject()
        properties.put( "name", icon )
        properties.put("POI", icon )

        val feature = JSONObject()
        feature.put( "type", "Feature" )
        feature.put( "geometry", geometry )
        feature.put( "properties", properties )

        return feature
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
            if (createEnumAreaBoundary)
            {
                droppedPointAnnotations.add( mapboxManager.addMarker( pointAnnotationManager, point, R.drawable.location_blue ))
                return true
            }
            else if (createEnumAreaLocation)
            {
                this.point = point
                binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
                val inputDialog = InputDialog( activity!!, false, resources.getString(R.string.map_tile_boundary), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaLengthTag, this@CreateEnumerationAreaFragment )
                inputDialog.editText?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                return true
            }
            else if (addHousehold)
            {
                addHousehold = false
                createLocation( point.latitude(), point.longitude(), point.altitude())
                refreshMap()
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
                return true
            }
            else
            {
                binding.mapView.getMapboxMap().queryRenderedFeatures(
                    RenderedQueryGeometry(binding.mapView.getMapboxMap().pixelForCoordinate(point)!!),
                    RenderedQueryOptions(listOf("LAYER_ID"), null)
                ) {
                    if (!it.isValue || it.value!!.isEmpty()) return@queryRenderedFeatures
                    Log.d( "xxx", "selected HH" )
                    Log.d( "xxx", it.value.toString())
                    val selectedFeature = it.value!![0].feature
                    // ... do things with selected feature
                }
                return true
            }
        }

        return false
    }

    private fun refreshMap()
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

        removeAllPolygonOnClickListeners()

        if (editMode)
        {
            polygonAnnotationManager?.apply {
                addClickListener(
                    OnPolygonAnnotationClickListener { polygonAnnotation ->
                        val obj = polygonHashMap[polygonAnnotation.id]

                        if (obj is EnumArea)
                        {
                            ConfirmationDialog( activity, resources.getString(R.string.select_task),
                                "${resources.getString(R.string.rename_or_delete)} ${obj.name}?",
                                resources.getString(R.string.rename), resources.getString(R.string.delete), obj, this@CreateEnumerationAreaFragment)
                        }
                        else if (obj is MapTileRegion)
                        {
                            ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                                resources.getString(R.string.delete_map_tile_region),
                                resources.getString(R.string.no), resources.getString(R.string.yes), obj, this@CreateEnumerationAreaFragment)
                        }

                        true
                    }
                )
            }
        }

        val allEnumAreas = getAllEnumAreas()

        val geoJson = JSONObject()
        geoJson.put( "type", "FeatureCollection" )

        features = JSONArray()

        for (enumArea in allEnumAreas)
        {
            for (location in enumArea.locations)
            {
                var resourceName = "home_black"

                if (location.isLandmark)
                {
                    resourceName = "location_blue"
                }
                else if (location.enumerationItems.size == 1)
                {
                    if (location.enumerationItems.isNotEmpty())
                    {
                        val enumerationItem = location.enumerationItems[0]

                        if (enumerationItem.samplingState == SamplingState.Sampled)
                        {
                            when( enumerationItem.collectionState )
                            {
                                CollectionState.Undefined -> resourceName = "home_light_blue"
                                CollectionState.Incomplete -> resourceName = "home_orange"
                                CollectionState.Complete -> resourceName = "home_purple"
                            }
                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Undefined)
                        {
                            resourceName = "home_black"
                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                        {
                            resourceName = "home_red"
                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                        {
                            resourceName = "home_green"
                        }
                    }
                }
                else
                {
                    for (enumerationItem in location.enumerationItems)
                    {
                        if (enumerationItem.samplingState == SamplingState.Sampled && enumerationItem.collectionState == CollectionState.Undefined)
                        {
                            resourceName = "home_light_blue"
                            break
                        }
                    }

                    if (resourceName == "home_black")
                    {
                        for (enumerationItem in location.enumerationItems)
                        {
                            if (enumerationItem.samplingState == SamplingState.Sampled)
                            {
                                if (enumerationItem.collectionState == CollectionState.Incomplete)
                                {
                                    resourceName = "multi_home_orange"
                                    break
                                }
                                else if (enumerationItem.collectionState == CollectionState.Complete)
                                {
                                    resourceName = "multi_home_purple"
                                }
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Undefined)
                            {
                                resourceName = "multi_home_black"
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceName = "multi_home_red"
                                break
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                resourceName = "multi_home_green"
                            }
                        }
                    }
                }

                val feature = createFeature( location.latitude, location.longitude, resourceName )
                features.put(feature)
            }
        }

        geoJson.put( "features", features )

        loadStyle( geoJson.toString() ) { style ->

            style.addLayer(SymbolLayer("SOURCE_LAYER_ID", "SOURCE_ID"))

            createAnnotationManagers()

            for (enumArea in allEnumAreas) {
                enumArea.mapTileRegion?.let {
                    addPolygon(it)
                }

                addPolygon(enumArea)
            }

            MapboxManager.centerMap( activity!!, binding.mapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
        }
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
            val location = Location( timeZone, LocationType.Enumeration, -1, latitude, longitude, altitude, false, "", "")
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

    fun removeAllPolygonOnClickListeners()
    {
        polygonAnnotationManager?.apply {
            polygonAnnotationManager?.clickListeners?.removeAll {
                true
            }
        }
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

        val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
        val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
        mapboxManager.addViewAnnotationToPoint( binding.mapView.viewAnnotationManager, point, enumArea.name, "#80FFFFFF" )
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->
            if (p1.action == MotionEvent.ACTION_UP)
            {
//                if (polyLinePoints.size  < 2)
//                {
//                    createMapTileCache = false
//                    binding.overlayView.visibility = View.GONE
//                    binding.mapTileRegionButton.setBackgroundTintList(defaultColorList);
//
//                    return false
//                }
//
//                val points = ArrayList<Coordinate>()
//
//                // close the polygon
//                polyLinePoints.add( polyLinePoints[0])
//
//                // convert ArrayList<Point> to ArrayList<Coordinate>
//                polyLinePoints.map {
//                    points.add( Coordinate( it.longitude(), it.latitude()))
//                }
//
//                polylineAnnotation?.let {
//                    it.points = polyLinePoints
//                    polylineAnnotationManager?.update(it)
//                }
//
//                createMapTileCache = false
//                binding.overlayView.visibility = View.GONE
//                binding.mapTileRegionButton.setBackgroundTintList(defaultColorList);
//
//                val vertices = ArrayList<LatLon>()
//
//                val points1 = ArrayList<Coordinate>()
//
//                var creationDate = Date().time
//
//                polylineAnnotation?.points?.map { point ->
//                    vertices.add( LatLon( creationDate++, point.latitude(), point.longitude()))
//                    points1.add( Coordinate( point.longitude(), point.latitude()))
//                }
//
//                val latLngBounds = GeoUtils.findGeobounds(vertices)
//                val northEast = LatLon( 0, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
//                val southWest = LatLon( 0, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )
//
//                val mapTileRegion = MapTileRegion( northEast, southWest )
//
//                val geometryFactory = GeometryFactory()
//                val geometry1 = geometryFactory.createPolygon(points1.toTypedArray())
//
//                val deleteList = ArrayList<MapTileRegion>()
//
//                val allMapTileRegions = getAllMapTileRegions()
//
//                for (mtr in allMapTileRegions)
//                {
//                    val points2 = ArrayList<Coordinate>()
//                    points2.add( Coordinate( mtr.southWest.longitude, mtr.southWest.latitude ))
//                    points2.add( Coordinate( mtr.northEast.longitude, mtr.southWest.latitude ))
//                    points2.add( Coordinate( mtr.northEast.longitude, mtr.northEast.latitude ))
//                    points2.add( Coordinate( mtr.southWest.longitude, mtr.northEast.latitude ))
//                    points2.add( Coordinate( mtr.southWest.longitude, mtr.southWest.latitude ))
//                    val geometry2 = geometryFactory.createPolygon(points2.toTypedArray())
//                    if (geometry1.contains( geometry2 ))
//                    {
//                        deleteList.add( mtr )
//                    }
//                }
//
//                for (mtr in deleteList)
//                {
//                    if (config.mapTileRegions.contains( mtr ))
//                    {
//                        config.mapTileRegions.remove( mtr )
//                    }
//                    else if (unsavedMapTileRegions.contains( mtr ))
//                    {
//                        unsavedMapTileRegions.remove( mtr )
//                    }
//                }
//
//                unsavedMapTileRegions.add( mapTileRegion )

                polyLinePoints.clear()

                polylineAnnotation?.let {
                    it.points = polyLinePoints
                    polylineAnnotationManager?.update(it)
                }

                refreshMap()
            }
            else
            {
                val point = binding.mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(p1.x.toDouble(),p1.y.toDouble()))
                polyLinePoints.add( point )

                if (polylineAnnotation == null)
                {
                    val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                        .withPoints(polyLinePoints)
                        .withLineColor("#ee4e8b")
                        .withLineWidth(5.0)

                    polylineAnnotation = polylineAnnotationManager?.create(polylineAnnotationOptions)
                }
                else
                {
                    polylineAnnotation!!.points = polyLinePoints
                    polylineAnnotationManager?.update(polylineAnnotation!!)
                }
            }
        }

        return true
    }

    override fun didCancelText( tag: Any? )
    {
        droppedPointAnnotations.map { pointAnnotation ->
            pointAnnotation?.let{ pointAnnotation ->
                pointAnnotationManager?.delete( pointAnnotation )
            }
        }
        droppedPointAnnotations.clear()
    }

    override fun didPressQrButton()
    {
        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
        getResult.launch(intent)
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

    override fun didEnterText( name: String, tag: Any? )
    {
        if (tag is EnumArea)
        {
            tag.name = name
        }
        else if (tag is Int)
        {
            if (tag == kEnumAreaLengthTag)
            {
                name.toDoubleOrNull()?.let {
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

                        inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaNameTag, this, false )
                    }
                }
            }
            if (tag == kEnumAreaNameTag)
            {
                createEnumAreaBoundary = false
                createEnumAreaLocation = false

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

                val mapTileRegion = MapTileRegion( northEast, southWest )

                var mbTilesPath = ""
                var mbTilesSize: Long = 0

                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                sharedPreferences.getString( Keys.kMBTilesPath.value, "" )?.let {
                    val mbTilesFile = File( it )
                    if (mbTilesFile.exists() && mbTilesFile.length() > 0)
                    {
                        mbTilesPath = it
                        mbTilesSize = mbTilesFile.length()
                    }
                }

                if (name.isEmpty())
                {
                    val enumArea = EnumArea( config.uuid, "${resources.getString(R.string.enumeration_area)} ${unsavedEnumAreas.size + 1}", mbTilesPath, mbTilesSize, vertices, mapTileRegion )
                    unsavedEnumAreas.add( enumArea )
                }
                else
                {
                    val enumArea = EnumArea( config.uuid, name, mbTilesPath, mbTilesSize, vertices, mapTileRegion )
                    unsavedEnumAreas.add( enumArea )
                }

                refreshMap()
            }
        }
    }

    override fun didSelectFirstButton(tag: Any?)
    {
        if (tag == null)
        {
            createEnumAreaBoundary = true
            droppedPointAnnotations.clear()
            binding.createEnumAreaButton.setBackgroundResource( R.drawable.save_blue )
            binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
        }
        else if (tag is EnumArea)
        {
            inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), tag.name, resources.getString(R.string.cancel), resources.getString(R.string.save), tag, this, false )
        }
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        if (tag == null)
        {
            createEnumAreaLocation = true
            droppedPointAnnotations.clear()
            binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            refreshMap()
        }
        else if (tag is MapTileRegion)
        {
//            config.mapTileRegions.remove( tag )
//            unsavedMapTileRegions.remove( tag )
//            DAO.mapTileRegionDAO.delete( tag )
        }
        else if (tag is EnumArea)
        {
            unsavedEnumAreas.remove( tag )
            config.enumAreas.remove( tag )
            DAO.enumAreaDAO.delete( tag )
            refreshMap()
        }
        else if (tag is PointAnnotation)
        {
            val pointAnnotation = tag as PointAnnotation
            pointHashMap[pointAnnotation.id]?.let { location ->
                val allEnumAreas = getAllEnumAreas()

                val enumArea = findEnumAreaOfLocation( allEnumAreas, LatLng( location.latitude, location.longitude ))

                enumArea?.let {
                    enumArea.locations.remove(location)
                }

                DAO.locationDAO.delete(location)
            }

            refreshMap()
        }
        else if (tag is Uri)
        {
            TileServer.stopServer()

            TileServer.startServer( activity!!, tag, binding.mapView.getMapboxMap()) {
                refreshMap()
            }
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
                MapboxManager.loadTilePacks( activity!!, getAllMapTileRegions(), this )
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
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
        {
            data?.data?.let { uri ->
                activity!!.getContentResolver().openInputStream(uri)?.let {
                    val text = it.bufferedReader().readText()

                    val featureCollection = FeatureCollection.fromJson( text )

                    if (featureCollection.features.isNotEmpty())
                    {
                        val feature = featureCollection.features[0]

                        feature.geometry?.let { geometry ->
                            val keys = ArrayList(feature.properties.keys)
                            when (geometry) {
                                is MultiPolygon -> {
                                    DropdownDialog( activity!!, resources.getString(R.string.select_the_property_identifier), keys, text, this )
                                }
                                is Point -> {
                                    checkboxDialog = CheckboxDialog( activity!!, resources.getString(R.string.select_the_hh_identifiers), keys, text, feature, this )
                                }
                                else -> {}
                            }
                        }
                    }
                    else
                    {
                        dropdownDidSelectSaveButton( text, "" )
                    }
                }
            }
        }
    }

    override fun checkboxDialogDidSelectSaveButton( json: String, selections: ArrayList<String> )
    {
        propertySelections = selections
        dropdownDidSelectSaveButton( json, "" )
    }

    override fun checkboxDialogDidSelectCancelButton()
    {
    }

    override fun dropdownDidSelectSaveButton( json: String, response: String )
    {
        activity!!.runOnUiThread {
            busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.importing_locations), this, false )
        }

        Thread {
            try
            {
                parseGeoJson( json, response )

                if (showCurrentLocation && unsavedEnumAreas.isNotEmpty())
                {
                    activity!!.runOnUiThread {
                        showCurrentLocation = false
                        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
                        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
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

            busyIndicatorDialog?.let {
                activity!!.runOnUiThread {
                    it.alertDialog.cancel()
                }
            }
        }.start()
    }

    override fun dropdownDidSelectCancelButton( json: String )
    {
        dropdownDidSelectSaveButton( json, "" )
    }

    data class PointWithProperty( var point: Point, var property: String )
    {
    }

    fun parseGeoJson( text: String, nameKey: String )
    {
        val points = ArrayList<PointWithProperty>()
        val featureCollection = FeatureCollection.fromJson( text )

        featureCollection.forEach { feature ->

            var name = "${resources.getString(R.string.enumeration_area)} ${unsavedEnumAreas.size + 1}"

            feature.getStringProperty(nameKey)?.let {
                name = it
            }

            feature.geometry?.let { geometry ->
                when( geometry ) {
                    is MultiPolygon -> {
                        val multiPolygon = geometry as MultiPolygon

                        var creationDate = Date().time

                        val vertices = ArrayList<LatLon>()

                        multiPolygon.coordinates[0][0].forEach { position ->
                            vertices.add( LatLon( creationDate++, position.latitude, position.longitude ))
                        }

                        val latLngBounds = GeoUtils.findGeobounds(vertices)
                        val northEast = LatLon( 0, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
                        val southWest = LatLon( 0, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

                        val mapTileRegion = MapTileRegion( northEast, southWest )

                        var mbTilesPath = ""
                        var mbTilesSize: Long = 0

                        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                        sharedPreferences.getString( Keys.kMBTilesPath.value, "" )?.let {
                            val mbTilesFile = File( it )
                            if (mbTilesFile.exists() && mbTilesFile.length() > 0)
                            {
                                mbTilesPath = it
                                mbTilesSize = mbTilesFile.length()
                            }
                        }

                        val enumArea = EnumArea(config.uuid, name, mbTilesPath, mbTilesSize, vertices, mapTileRegion )

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

                for (enumArea in allEnumAreas)
                {
                    val enumAreaPoints = ArrayList<Coordinate>()

                    enumArea.vertices.map {
                        enumAreaPoints.add( Coordinate( it.toLatLng().longitude, it.toLatLng().latitude ))
                    }

                    val geometryFactory = GeometryFactory()
                    val geometry: Geometry = geometryFactory.createPolygon(enumAreaPoints.toTypedArray())

                    val coordinate = Coordinate( point.point.coordinates.longitude, point.point.coordinates.latitude )
                    val geometry1 = geometryFactory.createPoint( coordinate )
                    if (geometry.contains( geometry1 ))
                    {
                        var altitude: Double = 0.0
                        point.point.coordinates.altitude?.let {
                            altitude = it
                        }

                        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
                        val location = Location( timeZone, LocationType.Enumeration, -1, point.point.coordinates.latitude, point.point.coordinates.longitude, altitude, false, "", point.property )

                        enumArea.locations.add( location )
                        break // found! assuming that it can only exist in a single EA, for now!
                    }
                }
            }
        }

        lifecycleScope.launch {
            refreshMap()
        }
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
//        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(it)
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
        val locationComponentPlugin = binding.mapView.location
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

    private fun onCameraTrackingDismissed() {
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onCameraChanged(eventData: CameraChangedEventData)
    {
        sharedViewModel.setCurrentZoomLevel( binding.mapView.getMapboxMap().cameraState.zoom )
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
                mapStyle = Style.MAPBOX_STREETS
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, mapStyle )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    refreshMap()
                }
            }

            R.id.satellite_streets ->
            {
                mapStyle = Style.SATELLITE_STREETS
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMapStyle.value, mapStyle )
                editor.commit()

                TileServer.loadMapboxStyle( activity!!, binding.mapView.getMapboxMap()) {
                    refreshMap()
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                SelectMapTilesDialog( activity!!, TileServer.getCachedFiles( activity!! ), this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            if (TileServer.fileExists( activity!!, uri ))
            {
                ConfirmationDialog( activity, resources.getString(R.string.oops),
                    resources.getString(R.string.re_import),
                    resources.getString(R.string.no),
                    resources.getString(R.string.yes), uri, this)
            }
            else
            {
                TileServer.stopServer()

                TileServer.startServer( activity!!, uri, binding.mapView.getMapboxMap()) {
                    refreshMap()
                }
            }
        }
    }

    override fun selectMapTilesDialogDidSelectSaveButton( selection: String )
    {
        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        val editor = sharedPreferences.edit()
        editor.putString( Keys.kMBTilesPath.value, mbTilesPath )
        editor.commit()

        TileServer.stopServer()

        TileServer.startServer( activity!!, mbTilesPath, binding.mapView.getMapboxMap()) {
            refreshMap()
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)

        _binding = null
    }
}