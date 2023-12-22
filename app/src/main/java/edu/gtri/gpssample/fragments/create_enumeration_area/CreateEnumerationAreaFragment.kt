package edu.gtri.gpssample.fragments.create_enumeration_area

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class CreateEnumerationAreaFragment : Fragment(),
    OnMapClickListener,
    View.OnTouchListener,
    OnCameraChangeListener,
    InputDialog.InputDialogDelegate,
    MapboxManager.MapTileCacheDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private lateinit var config: Config
    private lateinit var mapboxManager: MapboxManager
    private lateinit var polylineAnnotation: PolylineAnnotation
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private var editMode = false
    private var addHousehold = false
    private var createEnumArea = false
    private var createMapTileCache = false
    private val binding get() = _binding!!
    private var showCurrentLocation = false
    private val polygonHashMap = HashMap<Long,Any>()
    private val pointHashMap = HashMap<Long,Location>()
    private val unsavedEnumAreas = ArrayList<EnumArea>()
    private lateinit var defaultColorList : ColorStateList
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private val unsavedMapTileRegions = ArrayList<MapTileRegion>()
    private var allPointAnnotations = ArrayList<PointAnnotation>()
    private val polyLinePoints = ArrayList<com.mapbox.geojson.Point>()
    private var _binding: FragmentCreateEnumerationAreaBinding? = null
    private var allPolygonAnnotations = ArrayList<PolygonAnnotation>()
    private var droppedPointAnnotations = ArrayList<PointAnnotation?>()
    private var allPolylineAnnotations = ArrayList<PolylineAnnotation>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateEnumerationAreaBinding.inflate( inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

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

        arguments?.getBoolean(Keys.kEditMode.toString())?.let { editMode ->
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

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                    refreshMap()
                }
            }
        )

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager, polylineAnnotationManager )

        binding.mapView.gestures.addOnMapClickListener(this )
        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )

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
            if (createEnumArea || addHousehold || createMapTileCache)
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

            if (createEnumArea)
            {
                createEnumArea = false

                if (droppedPointAnnotations.size > 2)
                {
                    InputDialog( activity!!, resources.getString(R.string.enter_enum_area_name), "", null, this, false )
                    binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                    binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
                }
                else
                {
                    droppedPointAnnotations.map { pointAnnotation ->
                        pointAnnotation?.let{ pointAnnotation ->
                            pointAnnotationManager.delete( pointAnnotation )
                        }
                    }

                    droppedPointAnnotations.clear()
                    binding.createEnumAreaButton.setBackgroundResource( R.drawable.add_location_blue )
                    binding.createEnumAreaButton.setBackgroundTintList(defaultColorList);
                }
            }
            else
            {
                droppedPointAnnotations.clear()
                createEnumArea = true
                binding.createEnumAreaButton.setBackgroundResource( R.drawable.save_blue )
                binding.createEnumAreaButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.overlayView.setOnTouchListener(this)

        binding.mapTileRegionButton.setOnClickListener {
            if (createEnumArea || addHousehold)
            {
                return@setOnClickListener
            }

            if (createMapTileCache)
            {
                createMapTileCache = false
                binding.overlayView.visibility = View.GONE
                binding.mapTileRegionButton.setBackgroundTintList(defaultColorList);
            }
            else
            {
                createMapTileCache = true
                polyLinePoints.clear()
                if (this::polylineAnnotation.isInitialized)
                {
                    polylineAnnotation.points = polyLinePoints
                    polylineAnnotationManager.update(polylineAnnotation)
                }
                binding.overlayView.visibility = View.VISIBLE
                binding.mapTileRegionButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.mapTileCacheButton.setOnClickListener {
            if (createEnumArea || addHousehold || createMapTileCache)
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
            if (createEnumArea || createMapTileCache)
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

        binding.helpButton.setOnClickListener {
            CreateEnumAreaHelpDialog( activity!! )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            config.enumAreas.addAll( unsavedEnumAreas )
            config.mapTileRegions.addAll( unsavedMapTileRegions )
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.DefineEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onMapClick(point: com.mapbox.geojson.Point): Boolean
    {
        if (editMode)
        {
            if (createEnumArea)
            {
                droppedPointAnnotations.add( mapboxManager.addMarker( point, R.drawable.location_blue ))
                return true
            }
            else if (addHousehold)
            {
                addHousehold = false
                createLocation( LatLng( point.latitude(), point.longitude()))
                refreshMap()
                binding.addHouseholdButton.setBackgroundTintList(defaultColorList);
                return true
            }
        }

        return false
    }

    private fun refreshMap()
    {
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

        removeAllPolygonOnClickListeners()

        if (editMode)
        {
            polygonAnnotationManager?.apply {
                addClickListener(
                    OnPolygonAnnotationClickListener { polygonAnnotation ->
                        val obj = polygonHashMap[polygonAnnotation.id]

                        if (obj is EnumArea)
                        {
                            ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                                "${resources.getString(R.string.delete_enum_area_message)} ${obj.name}?",
                                resources.getString(R.string.no), resources.getString(R.string.yes), obj, this@CreateEnumerationAreaFragment)
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

        val allMapTileRegions = getAllMapTileRegions()

        for (mapTileRegion in allMapTileRegions)
        {
            addPolygon( mapTileRegion )
        }

        val allEnumAreas = getAllEnumAreas()

        for (enumArea in allEnumAreas)
        {
            addPolygon(enumArea)

            for (location in enumArea.locations)
            {
                var resourceId = R.drawable.home_black
                var isMultiFamily = false

                location.isMultiFamily?.let {
                    isMultiFamily = it
                }

                if (location.isLandmark)
                {
                    resourceId = R.drawable.location_blue
                }
                else if (!isMultiFamily)
                {
                    if (location.enumerationItems.isNotEmpty())
                    {
                        val enumerationItem = location.enumerationItems[0]

                        if (enumerationItem.samplingState == SamplingState.Sampled)
                        {
                            resourceId = if (enumerationItem.collectionState == CollectionState.Incomplete) R.drawable.home_orange else R.drawable.home_purple
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
                            resourceId = R.drawable.multi_home_green
                        }
                    }
                }

                if (resourceId > 0)
                {
                    val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                    val pointAnnotation = mapboxManager.addMarker( point, resourceId )

                    pointAnnotation?.let {
                        pointHashMap[pointAnnotation.id] = location
                        allPointAnnotations.add( pointAnnotation )
                    }

                    if (editMode)
                    {
                        pointAnnotationManager?.apply {
                            addClickListener(
                                OnPointAnnotationClickListener { pointAnnotation ->
                                    pointHashMap[pointAnnotation.id]?.let { location ->
                                        if (!location.isLandmark)
                                        {
                                            ConfirmationDialog( activity, resources.getString(R.string.please_confirm),
                                                "${resources.getString(R.string.delete_household_message)}?",
                                                resources.getString(R.string.no), resources.getString(R.string.yes), pointAnnotation, this@CreateEnumerationAreaFragment)
                                        }
                                    }
                                    true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (allEnumAreas.isNotEmpty())
        {
            val enumArea = allEnumAreas[0]
            val currentZoomLevel = sharedViewModel.currentZoomLevel?.value

            currentZoomLevel?.let { currentZoomLevel ->
                val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
                val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
                val cameraPosition = CameraOptions.Builder()
                    .zoom(currentZoomLevel)
                    .center(point)
                    .build()

                binding.mapView.getMapboxMap().setCamera(cameraPosition)
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
        allMapTileRegions.addAll( config.mapTileRegions )
        allMapTileRegions.addAll( unsavedMapTileRegions )
        return allMapTileRegions
    }
    fun createLocation( latLng: LatLng )
    {
        var enumArea = findEnumAreaOfLocation( config.enumAreas, latLng )

        if (enumArea == null)
        {
            enumArea = findEnumAreaOfLocation( unsavedEnumAreas, latLng )
        }

        enumArea?.let{  enumArea ->
            latLng?.let { latLng ->
                val location = Location( LocationType.Enumeration, latLng.latitude, latLng.longitude, false, "")
                enumArea.locations.add(location)
                refreshMap()
            }
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
            polygonAnnotationManager.clickListeners.removeAll {
                true
            }
        }
    }

    fun addPolygon( mapTileRegion: MapTileRegion )
    {
        val points = ArrayList<com.mapbox.geojson.Point>()
        val pointList = ArrayList<ArrayList<com.mapbox.geojson.Point>>()

        val vertices = ArrayList<LatLon>()

        vertices.add( LatLon( mapTileRegion.southWest.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( mapTileRegion.northEast.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( mapTileRegion.northEast.latitude, mapTileRegion.northEast.longitude ))
        vertices.add( LatLon( mapTileRegion.southWest.latitude, mapTileRegion.northEast.longitude ))

        vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        val polygonAnnotation = mapboxManager.addPolygon( pointList, "#000000", 0.0 )

        polygonAnnotation?.let { polygonAnnotation ->
            polygonHashMap[polygonAnnotation.id] = mapTileRegion
            allPolygonAnnotations.add( polygonAnnotation)
        }

        // create the polygon border
        val polylineAnnotation = mapboxManager.addPolyline( pointList[0], "#000000" )

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

        val polygonAnnotation = mapboxManager.addPolygon( pointList, "#000000", 0.25 )

        polygonAnnotation?.let { polygonAnnotation ->
            polygonHashMap[polygonAnnotation.id] = enumArea
            allPolygonAnnotations.add( polygonAnnotation)
        }

        // create the polygon border
        val polylineAnnotation = mapboxManager.addPolyline( pointList[0], "#ff0000" )

        polylineAnnotation?.let { polylineAnnotation ->
            allPolylineAnnotations.add( polylineAnnotation )
        }
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        p1?.let { p1 ->
            if (p1.action == MotionEvent.ACTION_UP)
            {
                val points = ArrayList<Coordinate>()

                // close the polygon
                polyLinePoints.add( polyLinePoints[0])

                // convert ArrayList<Point> to ArrayList<Coordinate>
                polyLinePoints.map {
                    points.add( Coordinate( it.longitude(), it.latitude()))
                }

                polylineAnnotation.points = polyLinePoints
                polylineAnnotationManager.update(polylineAnnotation)

                createMapTileCache = false
                binding.overlayView.visibility = View.GONE
                binding.mapTileRegionButton.setBackgroundTintList(defaultColorList);

                val vertices = ArrayList<LatLon>()

                val points1 = ArrayList<Coordinate>()

                polylineAnnotation.points.map { point ->
                    vertices.add( LatLon( point.latitude(), point.longitude()))
                    points1.add( Coordinate( point.longitude(), point.latitude()))
                }

                val latLngBounds = GeoUtils.findGeobounds(vertices)
                val northEast = LatLon( latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
                val southWest = LatLon( latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

                val mapTileRegion = MapTileRegion( northEast, southWest )

                val geometryFactory = GeometryFactory()
                val geometry1 = geometryFactory.createPolygon(points1.toTypedArray())

                val deleteList = ArrayList<MapTileRegion>()

                val allMapTileRegions = getAllMapTileRegions()

                for (mtr in allMapTileRegions)
                {
                    val points2 = ArrayList<Coordinate>()
                    points2.add( Coordinate( mtr.southWest.longitude, mtr.southWest.latitude ))
                    points2.add( Coordinate( mtr.northEast.longitude, mtr.southWest.latitude ))
                    points2.add( Coordinate( mtr.northEast.longitude, mtr.northEast.latitude ))
                    points2.add( Coordinate( mtr.southWest.longitude, mtr.northEast.latitude ))
                    points2.add( Coordinate( mtr.southWest.longitude, mtr.southWest.latitude ))
                    val geometry2 = geometryFactory.createPolygon(points2.toTypedArray())
                    if (geometry1.contains( geometry2 ))
                    {
                        deleteList.add( mtr )
                    }
                }

                for (mtr in deleteList)
                {
                    if (config.mapTileRegions.contains( mtr ))
                    {
                        config.mapTileRegions.remove( mtr )
                    }
                    else if (unsavedMapTileRegions.contains( mtr ))
                    {
                        unsavedMapTileRegions.remove( mtr )
                    }
                }

                unsavedMapTileRegions.add( mapTileRegion )

                polyLinePoints.clear()
                polylineAnnotation.points = polyLinePoints
                polylineAnnotationManager.update(polylineAnnotation)

                refreshMap()
            }
            else
            {
                val point = binding.mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(p1.x.toDouble(),p1.y.toDouble()))
                polyLinePoints.add( point )

                if (!this::polylineAnnotation.isInitialized)
                {
                    val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                        .withPoints(polyLinePoints)
                        .withLineColor("#ee4e8b")
                        .withLineWidth(5.0)

                    polylineAnnotation = polylineAnnotationManager.create(polylineAnnotationOptions)
                }
                else
                {
                    polylineAnnotation.points = polyLinePoints
                    polylineAnnotationManager.update(polylineAnnotation)
                }
            }
        }

        return true
    }

    override fun didCancelText( tag: Any? )
    {
        droppedPointAnnotations.map { pointAnnotation ->
            pointAnnotation?.let{ pointAnnotation ->
                pointAnnotationManager.delete( pointAnnotation )
            }
        }
        droppedPointAnnotations.clear()
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        if (tag != null)
        {
            Thread {
                val uri = tag as Uri

                val inputStream = activity!!.getContentResolver().openInputStream(uri)

                inputStream?.let { inputStream ->
                    try
                    {
                        parseGeoJson( inputStream.bufferedReader().readText(), name )
                    }
                    catch( ex: Exception)
                    {
                        activity!!.runOnUiThread {
                            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.start()
        }
        else
        {
            createEnumArea = false

            val vertices = ArrayList<LatLon>()

            droppedPointAnnotations.map { pointAnnotation ->
                pointAnnotation?.let{ pointAnnotation ->
                    vertices.add( LatLon( pointAnnotation.point.latitude(), pointAnnotation.point.longitude()))
                    pointAnnotationManager.delete( pointAnnotation )
                }
            }

            if (name.isEmpty())
            {
                val enumArea = EnumArea( "${resources.getString(R.string.enumeration_area)} ${unsavedEnumAreas.size + 1}", vertices )
                unsavedEnumAreas.add( enumArea )
            }
            else
            {
                val enumArea = EnumArea( name, vertices )
                unsavedEnumAreas.add( enumArea )
            }

            val latLngBounds = GeoUtils.findGeobounds(vertices)
            val northEast = LatLon( latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
            val southWest = LatLon( latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

            unsavedMapTileRegions.add( MapTileRegion( northEast, southWest ))

            refreshMap()
        }
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag is MapTileRegion)
        {
            config.mapTileRegions.remove( tag )
            unsavedMapTileRegions.remove( tag )
            DAO.mapTileRegionDAO.delete( tag )
        }
        else if (tag is EnumArea)
        {
            unsavedEnumAreas.remove( tag )
            config.enumAreas.remove( tag )
            DAO.enumAreaDAO.delete( tag )
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
        }

        refreshMap()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
        {
            data?.data?.let { uri ->
                InputDialog( activity!!, resources.getString(R.string.enum_area_name_property), "", uri, this, false )
            }
        }
    }

    fun parseGeoJson( text: String, nameKey: String )
    {
        val points = ArrayList<Point>()
        val featureCollection = FeatureCollection.fromJson( text )

        featureCollection.forEach { feature ->

            var name = "${resources.getString(R.string.enumeration_area)} ${unsavedEnumAreas.size + 1}"

            feature.getStringProperty(nameKey)?.let {
                name = it
            }

            feature.geometry?.let { geometry ->
                when( geometry ) {
                    is MultiPolygon -> {
                        val enumArea = EnumArea(name, ArrayList<LatLon>())
                        val multiPolygon = geometry as MultiPolygon

                        multiPolygon.coordinates[0][0].forEach { position ->
                            enumArea.vertices.add( LatLon( position.latitude, position.longitude ))
                        }

                        unsavedEnumAreas.add(enumArea)

                        val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
                        val northEast = LatLon( latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
                        val southWest = LatLon( latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

                        config.mapTileRegions.add( MapTileRegion( northEast, southWest ))
                    }
                    is Point -> {
                        val point = geometry as Point
                        points.add( point )
                    }
                    else -> {}
                }
            }
        }

        // figure out which enumArea contains each point

        var count = 0

        for (point in points)
        {
            Log.d( "xxx", "${count}/${points.size}")
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

                val coordinate = Coordinate( point.coordinates.longitude, point.coordinates.latitude )
                val geometry1 = geometryFactory.createPoint( coordinate )
                if (geometry.contains( geometry1 ))
                {
                    val location = Location( LocationType.Enumeration, point.coordinates.latitude, point.coordinates.longitude, false, "" )
                    DAO.locationDAO.createOrUpdateLocation( location, enumArea )

                    enumArea.locations.add( location )
                    break // found! assuming that it can only exist in a single EA, for now!
                }
            }
        }

        lifecycleScope.launch {
            refreshMap()
        }
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
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
                    com.mapbox.maps.R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    activity!!,
                    com.mapbox.maps.R.drawable.mapbox_user_icon_shadow,
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

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)

        _binding = null
    }
}