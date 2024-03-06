package edu.gtri.gpssample.fragments.map

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.MapTileRegion
import edu.gtri.gpssample.databinding.FragmentMapBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.MapHelpDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.ArrayList


class MapFragment : Fragment(),
    View.OnTouchListener,
    InputDialog.InputDialogDelegate,
    MapboxManager.MapTileCacheDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var point: Point? = null
    private var centerOnLocation = true
    private var defineMapRegion = false
    private var mapTileRegion: MapTileRegion? = null
    private var polygonAnnotation: PolygonAnnotation? = null
    private var polylineAnnotation: PolylineAnnotation? = null
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var droppedPointAnnotation: PointAnnotation? = null

    private lateinit var mapboxManager: MapboxManager
    private lateinit var defaultColorList : ColorStateList
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var circleAnnotationManager: CircleAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentMapBinding.inflate( inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner
        }

        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initLocationComponent()
                }
            }
        )

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        polygonAnnotationManager = binding.mapView.annotations.createPolygonAnnotationManager()
        polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        mapboxManager = MapboxManager( activity!!, pointAnnotationManager, polygonAnnotationManager, polylineAnnotationManager )

        circleAnnotationManager = binding.mapView.annotations.createCircleAnnotationManager()

        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.addOnMoveListener(onMoveListener)
        binding.centerOnLocationButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));

        binding.defineMapTileRegionButton.backgroundTintList?.let {
            defaultColorList = it
        }

        binding.overlayView.setOnTouchListener(this)

        binding.defineMapTileRegionButton.setOnClickListener {
            if (defineMapRegion)
            {
                defineMapRegion = false
                binding.overlayView.visibility = View.GONE
                binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);
            }
            else
            {
                defineMapRegion = true
                binding.overlayView.visibility = View.VISIBLE
                binding.defineMapTileRegionButton.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_light)));
            }
        }

        binding.cacheMapTilesButton.setOnClickListener {
            mapTileRegion?.let {
                defineMapRegion = false
                binding.overlayView.visibility = View.GONE
                binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);
                busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
                MapboxManager.loadStylePack( activity!!, this )
            }
        }

        binding.helpButton.setOnClickListener {
            MapHelpDialog( activity!! )
        }

        binding.clearMapButton.setOnClickListener {

            defineMapRegion = false
            binding.overlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

            droppedPointAnnotation?.let {
                pointAnnotationManager.delete( it )
            }

            polylineAnnotation?.let {
                polylineAnnotationManager.delete(it)
            }

            polygonAnnotation?.let {
                polygonAnnotationManager.delete(it)
            }
        }

        binding.centerOnLocationButton.setOnClickListener {
            defineMapRegion = false
            binding.overlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

            centerOnLocation = !centerOnLocation
            if (centerOnLocation)
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
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.MapFragment.value.toString() + ": " + this.javaClass.simpleName
    }
    
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean
    {
        if (defineMapRegion)
        {
            p1?.let { p1 ->
                if (p1.action == MotionEvent.ACTION_DOWN)
                {
                    droppedPointAnnotation?.let {
                        pointAnnotationManager.delete( it )
                    }

                    point = binding.mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(p1.x.toDouble(),p1.y.toDouble()))
                    point?.let {
                        droppedPointAnnotation =  mapboxManager.addMarker( it, R.drawable.location_blue )
                    }
                }
            }

            defineMapRegion = false
            binding.overlayView.visibility = View.GONE
            binding.defineMapTileRegionButton.setBackgroundTintList(defaultColorList);

            val inputDialog = InputDialog( activity!!, false, resources.getString(R.string.map_tile_boundary), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this@MapFragment )
            inputDialog.editText?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        return true
    }

    override fun didCancelText( tag: Any? )
    {
        defineMapRegion = false
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        defineMapRegion = false

        name.toDoubleOrNull()?.let {

            val radius = it * 1000

            point?.let { point ->
                val r_earth = 6378000.0

                var latitude  = point.latitude()  + (radius / r_earth) * (180.0 / Math.PI)
                var longitude = point.longitude() + (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                val northEast = LatLon( latitude, longitude )

                latitude  = point.latitude()  - (radius / r_earth) * (180.0 / Math.PI)
                longitude = point.longitude() - (radius / r_earth) * (180.0 / Math.PI) / Math.cos(latitude * Math.PI/180.0)
                val southWest = LatLon( latitude, longitude )

                mapTileRegion = MapTileRegion( northEast, southWest )

                mapTileRegion?.let {
                    addPolygon( it )
                }
            }
        }
    }

    fun addPolygon( mapTileRegion: MapTileRegion )
    {
        polylineAnnotation?.let {
            polylineAnnotationManager.delete(it)
        }

        polygonAnnotation?.let {
            polygonAnnotationManager.delete(it)
        }

        val points = ArrayList<Point>()
        val pointList = ArrayList<ArrayList<Point>>()

        val vertices = ArrayList<LatLon>()

        vertices.add( LatLon( mapTileRegion.southWest.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( mapTileRegion.northEast.latitude, mapTileRegion.southWest.longitude ))
        vertices.add( LatLon( mapTileRegion.northEast.latitude, mapTileRegion.northEast.longitude ))
        vertices.add( LatLon( mapTileRegion.southWest.latitude, mapTileRegion.northEast.longitude ))

        vertices.map {
            points.add( com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude ) )
        }

        pointList.add( points )

        polygonAnnotation = mapboxManager.addPolygon( pointList, "#000000", 0.0 )

        // create the polygon border
        polylineAnnotation = mapboxManager.addPolyline( pointList[0], "#000000" )
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

    private fun onCameraTrackingDismissed() {
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
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

    override fun didPressCancelButton()
    {
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    override fun stylePackLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(edu.gtri.gpssample.R.string.style_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                mapTileRegion?.let { mapTileRegion ->
                    val mapTileRegions = ArrayList<MapTileRegion>()
                    mapTileRegions.add(mapTileRegion)
                    MapboxManager.loadTilePacks( activity!!, mapTileRegions, this )
                }
            }
        }
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress(resources.getString(edu.gtri.gpssample.R.string.downloading_map_tiles) + " ${numLoaded}/${numNeeded}")
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
                    Toast.makeText(activity!!.applicationContext,  resources.getString(edu.gtri.gpssample.R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView()
    {
        super.onDestroyView()

        binding.mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)

        _binding = null
    }
}