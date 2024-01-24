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
import kotlinx.coroutines.launch
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

    private var isRecording = false
    private var createEnumArea = false
    private val binding get() = _binding!!
    private var showCurrentLocation = false
    private var currentGPSAccuracy: Int? = null
    private val unsavedEnumAreas = ArrayList<EnumArea>()
    private val unsavedMapTileRegions = ArrayList<MapTileRegion>()
    private var allPointAnnotations = ArrayList<PointAnnotation>()
    private var currentGPSLocation: com.mapbox.geojson.Point? = null
    private var _binding: FragmentWalkEnumerationAreaBinding? = null
    private val polyLinePoints = ArrayList<com.mapbox.geojson.Point>()
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

        val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(polyLinePoints)
            .withLineColor("#ee4e8b")
            .withLineWidth(5.0)

        polylineAnnotation = polylineAnnotationManager.create(polylineAnnotationOptions)

        binding.mapView.getMapboxMap().addOnCameraChangeListener( this )

        binding.legendTextView.setOnClickListener {
            MapLegendDialog( activity!! )
        }

        binding.recordButton.setOnClickListener {
            if (isRecording)
            {
                isRecording = false
                binding.recordButton.setBackgroundResource( R.drawable.record )
            }
            else
            {
                isRecording = true
                binding.recordButton.setBackgroundResource( R.drawable.pause )
            }
        }

        binding.clearButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.please_confirm), resources.getString(R.string.clear_map), resources.getString(R.string.no), resources.getString(R.string.yes), null, this@WalkEnumerationAreaFragment)
        }

        binding.centerOnLocationButton.backgroundTintList?.let {
            defaultColorList = it
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
            CreateEnumAreaHelpDialog( activity!! )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            isRecording = false
            binding.recordButton.setBackgroundResource( R.drawable.record )

            InputDialog( activity!!, resources.getString(R.string.enter_enum_area_name), "", null, this, false )

            refreshMap()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.WalkEnumerationAreaFragment.value.toString() + ": " + this.javaClass.simpleName
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

        for (enumArea in unsavedEnumAreas)
        {
            addPolygon(enumArea)
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

    override fun didEnterText( name: String, tag: Any? )
    {
        val vertices = ArrayList<LatLon>()

        polyLinePoints.map {
            vertices.add( LatLon( it.latitude(), it.longitude()))
        }

        polyLinePoints.clear()
        polylineAnnotation.points = polyLinePoints
        polylineAnnotationManager.update(polylineAnnotation)

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

        refreshMap()
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        isRecording = false
        polyLinePoints.clear()
        polylineAnnotation.points = polyLinePoints
        polylineAnnotationManager.update(polylineAnnotation)
        binding.recordButton.setBackgroundResource( R.drawable.record )
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
                InputDialog( activity!!, resources.getString(R.string.enum_area_name_property), "", uri, this, false )
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
            binding.accuracyLabelTextView.text = resources.getString(R.string.good)
            binding.accuracyLabelTextView.setTextColor( Color.parseColor("#0000ff"))
        }
        else
        {
            binding.accuracyLabelTextView.text = resources.getString(R.string.poor)
            binding.accuracyLabelTextView.setTextColor( Color.parseColor("#ff0000") )
        }

        binding.accuracyValueTextView.text = " : ${accuracy.toString()}m"
    }

    private val locationConsumer = object : LocationConsumer
    {
        override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) {
        }

        override fun onLocationUpdated(vararg location: com.mapbox.geojson.Point, options: (ValueAnimator.() -> Unit)?) {
            if (isRecording && location.size > 0)
            {
                val point = location.last()

//                if (polyLinePoints.size > 1)
//                {
//                    val last = polyLinePoints.last()
//                    val distance = GeoUtils.distanceBetween( LatLng( last.latitude(), last.longitude()), LatLng(point.latitude(),point.longitude()))
//                    if (distance < 5)
//                    {
//                        return
//                    }
//                }

                currentGPSLocation = point
                polyLinePoints.add( point )
                polylineAnnotation.points = polyLinePoints
                polylineAnnotationManager.update(polylineAnnotation)
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