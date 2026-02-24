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
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.*
import com.mapbox.maps.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapManager
import edu.gtri.gpssample.managers.TileServer
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.File
import java.util.*

class CreateOsmEnumerationAreaFragment : Fragment(),
    View.OnTouchListener,
    MapManager.MapManagerDelegate,
    MapManager.MapTileCacheDelegate,
    CheckboxDialog.CheckboxDialogDelegate,
    SelectionDialog.SelectionDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate,
    MultiConfirmationDialog.MulitConfirmationDialogDelegate
{
    private lateinit var mapView: View
    private lateinit var config: Config
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var editMode = false
    private var radius: Double = 0.0
    private val binding get() = _binding!!
    private var mapStyle = Style.MAPBOX_STREETS
    private var inputDialog: InputDialog? = null
    private var selectedEnumArea: EnumArea? = null
    private var checkboxDialog: CheckboxDialog? = null
    private var centerPoint: com.mapbox.geojson.Point? = null
    private var propertySelections = ArrayList<String>()
    private val unsavedEnumAreas = ArrayList<EnumArea>()
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var droppedPoints = ArrayList<com.mapbox.geojson.Point>()
    private var _binding: FragmentCreateEnumerationAreaBinding? = null

    private val kEnumAreaNameTag: Int = 0
    private val kEnumAreaLengthTag: Int = 1

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (sharedViewModel.currentZoomLevel?.value == null)
        {
            sharedViewModel.setCurrentZoomLevel( 16.0 )
        }

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = this.viewModel
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

        if (config.enumAreas.isNotEmpty() && config.enumAreas[0].mbTilesPath.isNotEmpty())
        {
            TileServer.startServer( config.enumAreas[0].mbTilesPath )
        }
        else
        {
            TileServer.stopServer()
        }

        binding.addHouseholdButton.backgroundTintList?.let {
            defaultColorList = it
        }

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, 0.0,this ) { mapView ->
            this.mapView = mapView

            MapManager.instance().enableLocationUpdates( activity!!, mapView )
            binding.osmLabel.visibility = if (mapView is org.osmdroid.views.MapView) View.VISIBLE else View.GONE

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

        binding.centerOnLocationButton.setOnClickListener {
            if (binding.centerOnLocationButton.backgroundTintList == defaultColorList)
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

        binding.importButton.setOnClickListener {
            if (currentTapType != TapType.None)
            {
                return@setOnClickListener
            }

            ConfirmationDialog( activity,
                resources.getString(R.string.select_file_type), "",
                resources.getString(R.string.import_geojson),
                resources.getString(R.string.import_mbtiles),
                null, true ) { buttonPressed, tag ->
                when( buttonPressed )
                {
                    ConfirmationDialog.ButtonPress.Left -> {
                        val intent = Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_GET_CONTENT)

                        startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_enumeration)), 1023)
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                    }
                    ConfirmationDialog.ButtonPress.None -> {
                    }
                }
            }

        }

        binding.createEnumAreaButton.setOnClickListener {
            if (currentTapType == TapType.None)
            {
                droppedPoints.clear()
                ConfirmationDialog( activity, resources.getString(R.string.creation_options), "", resources.getString(R.string.set_boundary), resources.getString(R.string.set_location), null, true ) { buttonPressed, tag ->
                    when( buttonPressed )
                    {
                        ConfirmationDialog.ButtonPress.Left -> {
                            currentTapType = TapType.CreateEnumAreaBoundary
                            binding.mapOverlayView.visibility = View.VISIBLE
                            binding.createEnumAreaButton.setBackgroundResource( R.drawable.save_blue )
                            binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                            Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.define_boundary), Toast.LENGTH_SHORT).show()
                        }
                        ConfirmationDialog.ButtonPress.Right -> {
                            currentTapType = TapType.CreateEnumAreaLocation
                            droppedPoints.clear()
                            binding.mapOverlayView.visibility = View.VISIBLE
                            binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
                            Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.define_center), Toast.LENGTH_SHORT).show()
                        }
                        ConfirmationDialog.ButtonPress.None -> {
                        }
                    }
                }
            }
            else if (currentTapType == TapType.CreateEnumAreaLocation)
            {
                currentTapType = TapType.None

                binding.mapOverlayView.visibility = View.GONE
                binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
            }
            else if (currentTapType == TapType.CreateEnumAreaBoundary)
            {
                currentTapType = TapType.None

                binding.mapOverlayView.visibility = View.GONE
                binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);

                MapManager.instance().clearMap( mapView )

                if (droppedPoints.size > 2)
                {
                    if (GeoUtils.isSelfIntersectingPolygon1( droppedPoints ))
                    {
                        Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.polygon_is_self_intersecting), Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaNameTag ) { action, text, tag ->
                            when (action) {
                                InputDialog.Action.DidCancel -> {
                                    binding.createEnumAreaButton.backgroundTintList = defaultColorList
                                }
                                InputDialog.Action.DidEnterText -> {
                                    createEnumArea( TapType.CreateEnumAreaBoundary, text )
                                }
                                InputDialog.Action.DidPressQRButton -> {
                                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                    getResult.launch(intent)
                                }
                            }
                        }
                    }
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
                MapManager.instance().cacheMapTiles(activity!!, mapView, mapTileRegions, this )
            }
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
                binding.mapOverlayView.visibility = View.VISIBLE
                binding.addHouseholdButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
            else if (currentTapType == TapType.AddHousehold)
            {
                currentTapType = TapType.None
                binding.mapOverlayView.visibility = View.GONE
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
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateOsmEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun refreshMap()
    {
        MapManager.instance().clearMap( mapView )

        val allEnumAreas = getAllEnumAreas()

        for (enumArea in allEnumAreas)
        {
            enumArea.mapTileRegion?.let {
                addPolygon(it)
            }

            addPolygon(enumArea)

            if (!editMode && enumArea.breadcrumbs.isNotEmpty())
            {
                for (breadcrumb in enumArea.breadcrumbs)
                {
                    MapManager.instance().createMarker( activity!!, mapView, com.mapbox.geojson.Point.fromLngLat(breadcrumb.longitude, breadcrumb.latitude), R.drawable.breadcrumb, "")
                }

                val breadcrumbs = ArrayList<Breadcrumb>()
                var groupId: String = ""

                for (breadcrumb in enumArea.breadcrumbs)
                {
                    if (breadcrumbs.isEmpty())
                    {
                        groupId = breadcrumb.groupId
                    }

                    if (breadcrumb.groupId == groupId)
                    {
                        breadcrumbs.add( breadcrumb )
                    }
                    else
                    {
                        MapManager.instance().createPolyline( mapView, breadcrumbs )
                        groupId = breadcrumb.groupId
                        breadcrumbs.clear()
                        breadcrumbs.add( breadcrumb )
                    }
                }

                if (breadcrumbs.isNotEmpty())
                {
                    MapManager.instance().createPolyline( mapView, breadcrumbs )
                }
            }

            val markerProperties = ArrayList<MapManager.MarkerProperty>()

            for (location in enumArea.locations)
            {
                var resourceId = R.drawable.home_black

                if (location.isLandmark)
                {
                    resourceId = R.drawable.location_blue
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
                                CollectionState.Undefined -> resourceId = R.drawable.home_light_blue
                                CollectionState.Incomplete -> resourceId = R.drawable.home_orange
                                CollectionState.Complete -> resourceId = R.drawable.home_purple
                            }
                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Undefined)
                        {
                            resourceId = R.drawable.home_black
                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                        {
                            resourceId = R.drawable.home_red
                        }
                        else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                        {
                            resourceId = R.drawable.home_green
                        }
                    }
                }
                else
                {
                    for (enumerationItem in location.enumerationItems)
                    {
                        if (enumerationItem.samplingState == SamplingState.Sampled && enumerationItem.collectionState == CollectionState.Undefined)
                        {
                            resourceId = R.drawable.multi_home_light_blue
                            break
                        }
                    }

                    if (resourceId == R.drawable.home_black)
                    {
                        for (enumerationItem in location.enumerationItems)
                        {
                            if (enumerationItem.samplingState == SamplingState.Sampled)
                            {
                                if (enumerationItem.collectionState == CollectionState.Incomplete)
                                {
                                    resourceId = R.drawable.multi_home_orange
                                    break
                                }
                                else if (enumerationItem.collectionState == CollectionState.Complete)
                                {
                                    resourceId = R.drawable.multi_home_purple
                                }
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Undefined)
                            {
                                resourceId = R.drawable.multi_home_black
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Incomplete)
                            {
                                resourceId = R.drawable.multi_home_red
                                break
                            }
                            else if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                resourceId = R.drawable.home_green
                            }
                        }
                    }
                }

                var title = ""
                if (location.enumerationItems.isNotEmpty())
                {
                    title = location.enumerationItems[0].subAddress
                }

                markerProperties.add( MapManager.MarkerProperty( location, resourceId, title ))
            }

            if (markerProperties.isNotEmpty())
            {
                MapManager.instance().loadMarkers( activity!!, binding.osmMapView, markerProperties )
            }
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

        MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0, Color.BLACK )
    }

    fun addPolygon( enumArea: EnumArea )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        enumArea.vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        MapManager.instance().createPolygon( mapView, pointList, Color.BLACK, 0x40, Color.RED, enumArea.name )
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->
            if (p1.action == MotionEvent.ACTION_UP) {
                val point = MapManager.instance().getLocationFromPixelPoint(mapView, p1)

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

                            MultiConfirmationDialog(
                                activity, resources.getString(R.string.select_task),
                                "", items, enumArea, this
                            )
                        }
                    }
                    TapType.AddHousehold -> {
                        currentTapType = TapType.None
                        binding.mapOverlayView.visibility = View.GONE
                        binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
                        createLocation(point.latitude(), point.longitude(), point.altitude())
                        refreshMap()
                    }
                    TapType.CreateEnumAreaBoundary -> {
                        droppedPoints.add( point )
                        MapManager.instance().createMarker( activity!!, mapView, point, R.drawable.location_blue, "" )
                    }
                    TapType.CreateEnumAreaLocation -> {
                        centerPoint = MapManager.instance().getLocationFromPixelPoint(mapView, p1)
                        binding.mapOverlayView.visibility = View.GONE
                        binding.createEnumAreaButton.backgroundTintList = defaultColorList
                        currentTapType = TapType.None

                        val inputDialog = InputDialog(activity!!, false, resources.getString(R.string.map_tile_boundary), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaLengthTag ) { action, text, tag ->
                            when (action) {
                                InputDialog.Action.DidCancel -> {}
                                InputDialog.Action.DidEnterText -> {
                                    text.toDoubleOrNull()?.let {
                                        radius = it * 1000
                                        inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kEnumAreaNameTag, false ) { action, text, tag ->
                                            when (action) {
                                                InputDialog.Action.DidCancel -> {
                                                    binding.createEnumAreaButton.backgroundTintList = defaultColorList
                                                }
                                                InputDialog.Action.DidEnterText -> {
                                                    createEnumArea( TapType.CreateEnumAreaLocation, text )
                                                }
                                                InputDialog.Action.DidPressQRButton -> {
                                                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                                                    getResult.launch(intent)
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
                inputDialog?.editText?.let { editText ->
                    editText.setText( payload.toString())
                }
            }
        }

    fun createEnumArea( tapType: TapType, name: String )
    {
        if (tapType == TapType.CreateEnumAreaBoundary)
        {
            val vertices = ArrayList<LatLon>()

            var creationDate = Date().time

            for (point in droppedPoints)
            {
                vertices.add( LatLon( creationDate++, point.latitude(), point.longitude()))
            }

            val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()
            pointList.add( droppedPoints )

            val latLngBounds = GeoUtils.findGeobounds(vertices)
            val northEast = LatLon( creationDate++, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
            val southWest = LatLon( creationDate++, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

            val mapTileRegion = MapTileRegion( northEast, southWest )

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

            refreshMap()
        }
        else if (tapType == TapType.CreateEnumAreaLocation)
        {
            centerPoint?.let { centerPoint ->
                val r_earth = 6378000.0
                var creationDate = Date().time

                var latitude  = centerPoint.latitude()  + (radius / r_earth) * (180.0 / Math.PI)
                var longitude = centerPoint.longitude() + (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                var northEast = LatLon( creationDate++, latitude, longitude )

                latitude  = centerPoint.latitude()  - (radius / r_earth) * (180.0 / Math.PI)
                longitude = centerPoint.longitude() - (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                var southWest = LatLon( creationDate++, latitude, longitude )

                val vertices = ArrayList<LatLon>()

                vertices.add( LatLon( creationDate++, southWest.latitude, southWest.longitude ))
                vertices.add( LatLon( creationDate++, northEast.latitude, southWest.longitude ))
                vertices.add( LatLon( creationDate++, northEast.latitude, northEast.longitude ))
                vertices.add( LatLon( creationDate++, southWest.latitude, northEast.longitude ))

                val points = ArrayList<com.mapbox.geojson.Point>()
                val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

                vertices.map {
                    points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
                }

                pointList.add( points )

                val latLngBounds = GeoUtils.findGeobounds(vertices)
                northEast = LatLon( creationDate++, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
                southWest = LatLon( creationDate++, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

                val mapTileRegion = MapTileRegion( northEast, southWest )

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

                refreshMap()
            }
        }

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
        ConfirmationDialog( activity, "",
            resources.getString(R.string.attach_mbtiles_question),
            resources.getString(R.string.no),
            resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
            when( buttonPressed )
            {
                ConfirmationDialog.ButtonPress.Left -> {
                }
                ConfirmationDialog.ButtonPress.Right -> {
                    if (TileServer.getCachedFiles( activity!! ).isNotEmpty())
                    {
                        SelectionDialog( activity!!, TileServer.getCachedFiles( activity!! ),this)
                    }
                    else
                    {
                        filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                    }
                }
                ConfirmationDialog.ButtonPress.None -> {
                }
            }
        }
    }

    override fun didSelectMultiButton( selection: String, tag: Any? )
    {
        val enumArea = tag as EnumArea

        when (selection) {
            resources.getString(R.string.rename) -> {
                inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), enumArea.name, resources.getString(R.string.cancel), resources.getString(R.string.save), tag, false ) { action, text, tag ->
                    when (action) {
                        InputDialog.Action.DidCancel -> {}
                        InputDialog.Action.DidEnterText -> {
                            (tag as EnumArea).name = text
                            refreshMap()
                        }
                        InputDialog.Action.DidPressQRButton -> {}
                    }
                }

            }
            resources.getString(R.string.delete) -> {
                MapManager.instance().clearMap( mapView )
                unsavedEnumAreas.remove( tag )
                config.enumAreas.remove( tag )
                DAO.enumAreaDAO.delete( tag )
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
                                        checkboxDialog = CheckboxDialog( activity!!, resources.getString(R.string.select_the_hh_identifiers), items, json, feature, this )
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

    override fun checkboxDialogDidSelectCancelButton()
    {
    }

    override fun checkboxDialogDidSelectSaveButton( json: String, selections: ArrayList<String> )
    {
        propertySelections = selections
        processGeoJson( json, "" )
    }

    fun processGeoJson( json: String, nameKey: String, strataKey: String = "" )
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
                    parseGeoJson( json, nameKey, strataKey )
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

    fun parseGeoJson( text: String, nameKey: String, strataKey: String )
    {
        val points = ArrayList<edu.gtri.gpssample.fragments.create_enumeration_area.CreateEnumerationAreaFragment.PointWithProperty>()
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

                        val enumArea = EnumArea(config.uuid, "", name, "", 0, vertices, mapTileRegion )

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

                        activity!!.runOnUiThread {
                            sharedViewModel.currentZoomLevel?.value?.let { currentZoomLevel ->
                                MapManager.instance().stopCenteringOnLocation( mapView )
                                binding.centerOnLocationButton.setBackgroundTintList(defaultColorList);
                                MapManager.instance().centerMap( enumArea, currentZoomLevel, mapView )
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
                        points.add(
                            edu.gtri.gpssample.fragments.create_enumeration_area.CreateEnumerationAreaFragment.PointWithProperty(
                                point,
                                jsonString
                            )
                        )
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
                val location = Location( timeZone, LocationType.Enumeration, -1, point.point.coordinates.latitude, point.point.coordinates.longitude, altitude, false, "", point.property )

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

    data class PointWithProperty( var point: Point, var property: String )
    {
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

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, 0.0,this ) { mapView ->
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

                MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, 0.0,this ) { mapView ->
                    refreshMap()
                }
            }

            R.id.import_map_tiles ->
            {
                filePickerLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
            }

            R.id.select_map_tiles ->
            {
                SelectionDialog( activity!!, TileServer.getCachedFiles( activity!! ),this)
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
//                    refreshMap()
                    TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
                }
            }
            else
            {
                TileServer.startServer( activity!!, uri, "", binding.mapboxMapView.getMapboxMap()) {
//                    refreshMap()
                    TileServer.centerMap( binding.mapboxMapView.getMapboxMap(), sharedViewModel.currentZoomLevel?.value )
                }
            }

            selectedEnumArea?.let {
                it.mbTilesPath = filePath
                it.mbTilesSize = fileSize
                selectedEnumArea = null
            }
        }
    }

    override fun didMakeSelection( selection: String, tag: Int )
    {
        val mbTilesPath = activity!!.cacheDir.toString() + "/" + selection

        selectedEnumArea?.let {
            it.mbTilesPath = mbTilesPath
            val mbtilesFile = File( mbTilesPath )
            it.mbTilesSize = mbtilesFile.length()
            selectedEnumArea = null
        }

        TileServer.startServer( mbTilesPath )

        MapManager.instance().selectMap( activity!!, config, binding.osmMapView, binding.mapboxMapView, binding.northUpImageView, null, 0.0,this ) { mapView ->
            refreshMap()
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

        _binding = null
    }
}