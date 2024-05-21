package edu.gtri.gpssample.fragments.walk_enumeration_area

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.mapbox.maps.plugin.locationcomponent.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentWalkEnumerationAreaBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.GeoUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.dsl.point
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

class WalkEnumerationAreaFragment : Fragment(),
    OnCameraChangeListener,
    InputDialog.InputDialogDelegate,
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
    private lateinit var defaultColorList : ColorStateList

    private var inputDialog: InputDialog? = null
    private var startPointAnnotation : PointAnnotation? = null

    private var isRecording = false
    private val binding get() = _binding!!
    private var showCurrentLocation = true
    private var currentGPSAccuracy: Int? = null
    private var currentGPSLocation: com.mapbox.geojson.Point? = null
    private var _binding: FragmentWalkEnumerationAreaBinding? = null
    private val polyLinePoints = ArrayList<com.mapbox.geojson.Point>()
    private var allPolygonAnnotations = ArrayList<PolygonAnnotation>()
    private var droppedPointAnnotations = ArrayList<PointAnnotation?>()
    private var allPolylineAnnotations = ArrayList<PolylineAnnotation>()

    private val test = "test-OFF"
    private val kClearMapTag = 1
    private val kDeletePointTag = 2

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
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

        if (config.enumAreas.isNotEmpty())
        {
            binding.saveButton.isEnabled = false
            binding.addPointButton.isEnabled = false
            binding.deletePointButton.isEnabled = false
        }

        val currentZoomLevel = sharedViewModel.currentZoomLevel?.value

        if (currentZoomLevel == null)
        {
            sharedViewModel.setCurrentZoomLevel( 16.0 )
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

        val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(polyLinePoints)
            .withLineColor("#ee4e8b")
            .withLineWidth(5.0)

        polylineAnnotation = polylineAnnotationManager.create(polylineAnnotationOptions)

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.addPointButton.setOnClickListener {
            currentGPSLocation?.let { point ->

                if (test == "test-1")
                {
                    createTest1Point(point)?.let {
                        polyLinePoints.add( it )
                        polylineAnnotation.points = polyLinePoints
                        polylineAnnotationManager.update(polylineAnnotation)

                        if (polyLinePoints.size == 1)
                        {
                            startPointAnnotation = mapboxManager.addMarker( it, R.drawable.location_blue )
                        }
                    }
                }
                else if (test == "test-2")
                {
                    createTest2Point(point)?.let {
                        polyLinePoints.add( it )
                        polylineAnnotation.points = polyLinePoints
                        polylineAnnotationManager.update(polylineAnnotation)

                        if (polyLinePoints.size == 1)
                        {
                            startPointAnnotation = mapboxManager.addMarker( it, R.drawable.location_blue )
                        }
                    }
                }
                else
                {
                    polyLinePoints.add( point )
                    polylineAnnotation.points = polyLinePoints
                    polylineAnnotationManager.update(polylineAnnotation)

                    if (polyLinePoints.size == 1)
                    {
                        startPointAnnotation = mapboxManager.addMarker( point, R.drawable.location_blue )
                    }
                }
            }
        }

        binding.deletePointButton.setOnClickListener {
            if (polyLinePoints.size > 0)
            {
                ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_point), resources.getString(R.string.no), resources.getString(R.string.yes), kDeletePointTag, this@WalkEnumerationAreaFragment)
            }
        }

        binding.deleteEverythingButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.clear_map), resources.getString(R.string.no), resources.getString(R.string.yes), kClearMapTag, this@WalkEnumerationAreaFragment)
        }

        binding.centerOnLocationButton.backgroundTintList?.let {
            defaultColorList = it
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

                if (MapboxManager.isSelfIntersectingPolygon1( polyLinePoints ))
                {
                    polyLinePoints.removeLast()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.polygon_is_self_intersecting), Toast.LENGTH_SHORT).show()
                }
                else
                {
                    inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enter_enum_area_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this, false )

                    refreshMap()
                }
            }
        }
    }

    fun createTest1Point( point: com.mapbox.geojson.Point ) : com.mapbox.geojson.Point?
    {
        var p: com.mapbox.geojson.Point? = null

        if (polyLinePoints.size == 0)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()-0.1, point.latitude()+0.1 )
        }
        else if (polyLinePoints.size == 1)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()+0.1, point.latitude()+0.1 )
        }
        else if (polyLinePoints.size == 2)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()+0.1, point.latitude()-0.1 )
        }
        else if (polyLinePoints.size == 3)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()-0.1, point.latitude()-0.1 )
        }

        return p
    }

    fun createTest2Point( point: com.mapbox.geojson.Point ) : com.mapbox.geojson.Point?
    {
        var p: com.mapbox.geojson.Point? = null

        if (polyLinePoints.size == 0)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()-0.1, point.latitude()+0.3 )
        }
        else if (polyLinePoints.size == 1)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()+0.1, point.latitude()+0.3 )
        }
        else if (polyLinePoints.size == 2)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()+0.1, point.latitude()+0.1 )
        }
        else if (polyLinePoints.size == 3)
        {
            p = com.mapbox.geojson.Point.fromLngLat( point.longitude()-0.1, point.latitude()+0.1 )
        }

        return p
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.WalkEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
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

        for (enumArea in config.enumAreas)
        {
            addPolygon(enumArea)
        }

        for (mapTileRegion in config.mapTileRegions)
        {
            addPolygon( mapTileRegion )
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )
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
            allPolygonAnnotations.add( polygonAnnotation)
        }

        // create the polygon border
        val polylineAnnotation = mapboxManager.addPolyline( pointList[0], "#ff0000" )

        polylineAnnotation?.let { polylineAnnotation ->
            allPolylineAnnotations.add( polylineAnnotation )
        }
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

        val polygonAnnotation = mapboxManager.addPolygon( pointList, "#000000", 0.0 )

        polygonAnnotation?.let { polygonAnnotation ->
            allPolygonAnnotations.add( polygonAnnotation)
        }

        // create the polygon border
        val polylineAnnotation = mapboxManager.addPolyline( pointList[0], "#000000" )

        polylineAnnotation?.let { polylineAnnotation ->
            allPolylineAnnotations.add( polylineAnnotation )
        }
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

    override fun didPressQrButton()
    {
        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
        getResult.launch(intent)
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.toString())
                inputDialog?.editText?.let { editText ->
                    editText.setText( payload.toString())
                }
            }
        }

    override fun didEnterText( name: String, tag: Any? )
    {
        val vertices = ArrayList<LatLon>()

        var creationDate = Date().time

        for (point in polyLinePoints)
        {
            vertices.add( LatLon( creationDate++, point.latitude(), point.longitude()))
        }

        polyLinePoints.clear()
        polylineAnnotation.points = polyLinePoints
        polylineAnnotationManager.update(polylineAnnotation)

        startPointAnnotation?.let {
            pointAnnotationManager.delete( it )
            startPointAnnotation = null
        }

        if (vertices.size > 2)
        {
            var name2 = name

            if (name2.isEmpty())
            {
                name2 = "${resources.getString(R.string.enumeration_area)} 1"
            }

            val enumArea = EnumArea( config.uuid, name2, vertices )
            config.enumAreas.add( enumArea )

            val latLngBounds = GeoUtils.findGeobounds(vertices)
            val northEast = LatLon( 0, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude )
            val southWest = LatLon( 0, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude )

            config.mapTileRegions.add( MapTileRegion( northEast, southWest ))

            DAO.configDAO.createOrUpdateConfig( config )?.let { config ->
                config.enumAreas[0].let { enumArea ->
                    DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( EnumerationTeam( enumArea.uuid, "Auto Gen Team", enumArea.vertices, ArrayList<Location>()))?.let { enumerationTeam ->
                        enumArea.enumerationTeams.add( enumerationTeam )
                    }
                }
            }

            binding.saveButton.isEnabled = false
            binding.addPointButton.isEnabled = false
            binding.deletePointButton.isEnabled = false
        }

        refreshMap()
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag == kClearMapTag)
        {
            isRecording = false
            polyLinePoints.clear()
            polylineAnnotation.points = polyLinePoints
            polylineAnnotationManager.update(polylineAnnotation)
            binding.saveButton.isEnabled = true
            binding.addPointButton.isEnabled = true
            binding.deletePointButton.isEnabled = true

            startPointAnnotation?.let {
                pointAnnotationManager.delete( it )
                startPointAnnotation = null
            }

            if (config.enumAreas.isNotEmpty())
            {
                DAO.enumAreaDAO.delete( config.enumAreas[0] )
                config.enumAreas.clear()
            }

            if (config.mapTileRegions.isNotEmpty())
            {
                DAO.mapTileRegionDAO.delete( config.mapTileRegions[0] )
                config.mapTileRegions.clear()
            }

            DAO.configDAO.createOrUpdateConfig( config )
        }
        else if (tag == kDeletePointTag)
        {
            polyLinePoints.removeLast()
            polylineAnnotation.points = polyLinePoints
            polylineAnnotationManager.update(polylineAnnotation)

            if (polyLinePoints.isEmpty())
            {
                startPointAnnotation?.let {
                    pointAnnotationManager.delete( it )
                    startPointAnnotation = null
                }
            }
        }

        refreshMap()
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
                inputDialog = InputDialog( activity!!, true, resources.getString(R.string.enum_area_name_property), "", resources.getString(R.string.cancel), resources.getString(R.string.save), uri, this, false )
            }
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

    private val locationConsumer = object : LocationConsumer
    {
        override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) {
        }

        override fun onLocationUpdated(vararg location: com.mapbox.geojson.Point, options: (ValueAnimator.() -> Unit)?) {
            if (location.size > 0)
            {
                currentGPSLocation = location.last()
            }
        }

        override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {
        }

        override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {
        }
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