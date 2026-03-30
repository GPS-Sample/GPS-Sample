package edu.gtri.gpssample.managers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.not
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextJustify
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Breadcrumb
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.database.models.MapTileRegion
import edu.gtri.gpssample.managers.TileServer.Companion.rasterLayer
import edu.gtri.gpssample.managers.TileServer.Companion.rasterSource
import edu.gtri.gpssample.utils.GeoUtils
import io.github.dellisd.spatialk.geojson.dsl.feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.concurrent.atomic.AtomicInteger

class MapManager
{
    interface MapManagerDelegate
    {
        fun onMarkerTapped( location: Location )
        fun onZoomLevelChanged( zoomLevel: Double )
    }

    class MapboxPolygon
    {
        var polygonAnnotation: PolygonAnnotation? = null
        var polylineAnnotation: PolylineAnnotation? = null
    }

    private var delegate: MapManagerDelegate? = null

    private val MIN_ZOOM = 8
    private val MAX_ZOOM = 18

    var mapboxBreadcrumbAnnotationManager: PointAnnotationManager? = null
    var mapboxPointAnnotationManager: PointAnnotationManager? = null
    var mapboxPolygonAnnotationManager: PolygonAnnotationManager? = null
    var mapboxPolylineAnnotationManager: PolylineAnnotationManager?= null

    // public functions

    fun selectOsmMap( activity: Activity, osmMapView: org.osmdroid.views.MapView, northUpImageView: ImageView, completion: ((mapView: View)->Unit) )
    {
        val sharedPreferences: SharedPreferences = activity.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)

        initializeOsmMap( activity, osmMapView, northUpImageView, mapStyle!! ) {
            completion( osmMapView )
        }
    }

    fun selectMapboxMap( activity: Activity, mapboxMapView: com.mapbox.maps.MapView, enumArea: EnumArea?, zoom: Double, completion: ((mapView: View)->Unit) )
    {
        val sharedPreferences: SharedPreferences = activity.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)

        initializeMapboxMap( mapboxMapView, mapStyle!!, enumArea, zoom ) {
            completion( mapboxMapView )
        }
    }

    fun selectMap( activity: Activity, config: Config, osmMapView: org.osmdroid.views.MapView, mapBoxMapView: com.mapbox.maps.MapView, northUpImageView: ImageView, enumArea: EnumArea?, zoom: Double, delegate: MapManagerDelegate? = null, completion: ((mapView: View)->Unit) )
    {
        this.delegate = delegate

        val sharedPreferences: SharedPreferences = activity.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)

        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
        {
            mapBoxMapView.visibility = View.GONE
            osmMapView.visibility = View.VISIBLE

            initializeOsmMap( activity, osmMapView, northUpImageView, mapStyle!! ) {
                completion( osmMapView )
            }
        }
        else if (config.mapEngineIndex == MapEngine.MapBox.value)
        {
            mapBoxMapView.visibility = View.VISIBLE
            osmMapView.visibility = View.GONE

            initializeMapboxMap( mapBoxMapView, mapStyle!!, enumArea, zoom ) {
                completion( mapBoxMapView )
            }
        }
    }

    private lateinit var osmTilesOverlay: TilesOverlay

    private fun initializeOsmMap( activity: Activity, mapView: org.osmdroid.views.MapView, northUpImageView: ImageView, mapStyle: String, completion: (()->Unit) )
    {
        org.osmdroid.config.Configuration.getInstance().load( activity, PreferenceManager.getDefaultSharedPreferences(activity))

        mapView.overlays.clear()
        mapView.minZoomLevel = 0.0
        mapView.maxZoomLevel = 24.0
        mapView.setMultiTouchControls( true )
        mapView.setBuiltInZoomControls(false)

        var tileSource = TileSourceFactory.MAPNIK

        if (mapStyle == Style.SATELLITE_STREETS)
        {
            tileSource = TileSourceFactory.USGS_SAT
        }

        // Mapnik (OSM standard tiles)
        val osmProvider = MapTileProviderBasic(activity)
        osmProvider.tileSource = tileSource
        osmTilesOverlay = TilesOverlay(osmProvider, activity)
        mapView.overlays.add(osmTilesOverlay)

        // Custom tiles
        if (TileServer.started)
        {
            val customTileSource = XYTileSource(
                "CustomTiles",
                0, 19, 256, ".png",
                arrayOf("http://localhost:8080/tiles/"),
                "© Custom Tiles"
            )

            val customProvider = MapTileProviderBasic(activity)
            customProvider.tileSource = customTileSource
            val customOverlay = TilesOverlay(customProvider, activity)
            customOverlay.setLoadingBackgroundColor(Color.TRANSPARENT)
            mapView.overlays.add(customOverlay)
        }

        delegate?.let {
            mapView.setMapListener(object : org.osmdroid.events.MapListener {
                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                    return true // You can handle scroll events here if needed
                }

                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                    event?.let {
                        delegate!!.onZoomLevelChanged( it.zoomLevel )
                    }
                    return true
                }
            })
        }

        // --- Rotation overlay ---
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        northUpImageView.setOnClickListener {
            mapView.mapOrientation = 0f   // reset to North-up
            northUpImageView.visibility = View.GONE
        }

        mapView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (mapView.mapOrientation != 0f)
                {
                    northUpImageView.visibility = View.VISIBLE
                }
                return true
            }
        })

        completion()
    }

    private fun initializeMapboxMap( mapView: com.mapbox.maps.MapView, style: String, enumArea: EnumArea?, zoom: Double, completion: (()->Unit))
    {
        mapView.compass.marginTop = 50.0f

        if (enumArea == null)
        {
            centerMap( GEORGIA_TECH, zoom, mapView )
        }
        else
        {
            centerMap( enumArea,zoom, mapView )
        }

        mapView.getMapboxMap().loadStyle(
            com.mapbox.maps.extension.style.style(style) {
                rasterSource?.let {
                    +it
                }
                rasterLayer?.let {
                    +it
                }
            },
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    createMapboxPolygonAnnotationManager( mapView )
                    createMapboxPolylineAnnotationManager( mapView )
                    createMapboxBreadcrumbAnnotationManager( mapView )
                    createMapboxPointAnnotationManager( mapView )

                    mapView.location.updateSettings {
                        this.enabled = true
                        this.locationPuck = LocationPuck2D(
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

                    mapView.getMapboxMap().addOnCameraChangeListener { cameraChangedEventData ->
                        delegate?.onZoomLevelChanged( mapView.getMapboxMap().cameraState.zoom )
                    }

                    completion()
                }
            }
        )
    }

    fun enableLocationUpdates( activity: Activity, mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            enableOsmMapLocationUpdates( activity, mapView )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            enableMapboxLocationUpdates( activity, mapView )
        }
    }

    fun disableLocationUpdates( mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            disableOsmMapLocationUpdates( mapView )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            disableMapboxLocationUpdates( mapView )
        }
    }

    private fun enableOsmMapLocationUpdates( activity: Activity, mapView: org.osmdroid.views.MapView )
    {
        val locationProvider = GpsMyLocationProvider( activity )
        locationProvider.locationUpdateMinTime = 1000 // 1 second
        locationProvider.locationUpdateMinDistance = 5f // 5 meters

        for (overlay in mapView.overlays)
        {
            if (overlay is MyLocationNewOverlay)
            {
                mapView.overlays.remove( overlay )
                break
            }
        }

        val myLocationNewOverlay = MyLocationNewOverlay(locationProvider, mapView)

        ContextCompat.getDrawable(activity, R.drawable.osm_location)?.let { arrow ->
            convertDrawableToBitmap( arrow )?.let { bitmap: Bitmap ->
                myLocationNewOverlay.setDirectionIcon(bitmap)
                myLocationNewOverlay.setPersonAnchor(0.5f, 0.5f)
                myLocationNewOverlay.setDirectionAnchor(0.5f, 0.5f)
            }
        }

        myLocationNewOverlay.enableMyLocation()

        mapView.getOverlays().add( myLocationNewOverlay )
    }

    private fun disableOsmMapLocationUpdates( mapView: org.osmdroid.views.MapView )
    {
        for (overlay in mapView.overlays)
        {
            if (overlay is MyLocationNewOverlay)
            {
                overlay.disableMyLocation()
                overlay.disableFollowLocation()
                mapView.overlays.remove( overlay )
                break
            }
        }
    }

    fun startCenteringOnLocation( activity: Activity, mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            for (overlay in mapView.getOverlays())
            {
                if (overlay is MyLocationNewOverlay)
                {
                    overlay.enableFollowLocation()
                    break
                }
            }
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            enableMapboxLocationUpdates( activity, mapView )
        }
    }

    fun stopCenteringOnLocation( mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            for (overlay in mapView.getOverlays())
            {
                if (overlay is MyLocationNewOverlay)
                {
                    overlay.disableFollowLocation()
                    break
                }
            }
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            disableMapboxLocationUpdates( mapView )
        }
    }

    private var onIndicatorPositionChangedListener: OnIndicatorPositionChangedListener? = null

    fun enableMapboxLocationUpdates( activity: Activity, mapView: com.mapbox.maps.MapView )
    {
        if (onIndicatorPositionChangedListener != null)
        {
            mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener!!)
        }

        onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        }

        mapView.location.apply {
            locationPuck = createDefault2DPuck(withBearing = true)
            puckBearingEnabled = true
            enabled = true
        }

        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener!!)
    }

    private fun disableMapboxLocationUpdates( mapView: com.mapbox.maps.MapView )
    {
        if (onIndicatorPositionChangedListener != null)
        {
            mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener!!)
            onIndicatorPositionChangedListener = null
        }
    }

    fun createMapboxPointAnnotationManager( mapView: MapView )
    {
        mapboxPointAnnotationManager = mapView.annotations.createPointAnnotationManager(
            AnnotationConfig(layerId = "markers")
        )

        mapboxPointAnnotationManager!!.apply {
            addClickListener(
                OnPointAnnotationClickListener { pointAnnotation ->
                    var retVal = false
                    try {
                        pointAnnotation.getData()?.asJsonObject?.let { jsonObject ->
                            jsonObject.get("uuid")?.asString?.let { uuid ->
                                DAO.locationDAO.getLocation( uuid )?.let {
                                    retVal = true
                                    delegate?.onMarkerTapped( it )
                                }
                            }
                        }
                    }
                    catch( ex: Exception ) {
                        Log.d( "xxx", ex.stackTraceToString())
                    }
                    retVal
                }
            )
        }
    }

    fun createMapboxBreadcrumbAnnotationManager( mapView: MapView )
    {
        mapboxBreadcrumbAnnotationManager = mapView.annotations.createPointAnnotationManager(
            AnnotationConfig(layerId = "breadcrumbs")
        )
    }

    fun createMapboxPolylineAnnotationManager( mapView: MapView )
    {
        mapboxPolylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager(AnnotationConfig(layerId = "polylines"))
    }

    fun createMapboxPolygonAnnotationManager( mapView: MapView )
    {
        mapboxPolygonAnnotationManager = mapView.annotations.createPolygonAnnotationManager(AnnotationConfig(layerId = "polygons"))
    }

    fun clearMap( mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val overlays = ArrayList<Overlay>()

            for (overlay in mapView.overlays)
            {
                if (overlay is Polygon)
                {
                    overlays.add( overlay )
                }
                else if (overlay is Polyline)
                {
                    overlays.add( overlay )
                }
                else if (overlay is Marker)
                {
                    overlays.add( overlay )
                }
                else if (overlay is TextOverlay)
                {
                    overlays.add(overlay)
                }
            }

            for (overlay in overlays)
            {
                mapView.overlays.remove( overlay )
            }

            mapView.invalidate()
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPolygonAnnotationManager?.deleteAll()
            mapboxPolylineAnnotationManager?.deleteAll()
            mapboxPointAnnotationManager?.deleteAll()
            mapboxBreadcrumbAnnotationManager?.deleteAll()
        }
    }

    fun createTextLabel(context: Context, text: String): Bitmap
    {
        val textView = TextView(context)
        textView.text = text
        textView.setTextColor(Color.BLACK)
        textView.setBackgroundColor(Color.argb(128, 255, 255, 255))
        textView.setPadding(10, 5, 10, 5)
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)
        val bitmap = Bitmap.createBitmap(textView.width, textView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        textView.draw(canvas)
        return bitmap
    }

    fun createPolygon( mapView: View, points: List<List<Point>>, fillColor: Int, fillOpacity: Int, borderColor: Int = Color.RED, label: String = "" ) : Any?
    {
        val mapboxPolygon = MapboxPolygon()

        if (mapView is org.osmdroid.views.MapView)
        {
            val geoPoints = ArrayList<GeoPoint>()
            for (point in points[0])
            {
                geoPoints.add( GeoPoint(point.latitude(), point.longitude()))
            }

            val polygon = Polygon().apply {
                setPoints(geoPoints)
                fillPaint.color = fillColor
                fillPaint.alpha = fillOpacity
                outlinePaint.color = borderColor
                outlinePaint.strokeWidth = 5f
            }

            mapView.overlays.add(polygon)

            if (label.isNotEmpty())
            {
                val geoBounds = GeoUtils.findGeobounds( points[0] )
                val marker = Marker(mapView)
                marker.position = GeoPoint( geoBounds.center.latitude, geoBounds.center.longitude )
                marker.icon = BitmapDrawable( MainApplication.getContext().resources, createTextLabel( MainApplication.getContext(), label ))
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.infoWindow = null // Optional: disable default popup

                mapView.overlays.add(marker)
            }

            mapView.invalidate()

            return polygon
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val newPoints = ArrayList<Point>( points[0] )
            val newPointList = ArrayList<ArrayList<Point>>()

            if (points[0].last() != points[0].first())
            {
                newPoints.add( newPoints.first())
            }

            newPointList.add( newPoints )

            mapboxPolygonAnnotationManager?.let {
                val polygonAnnotationOptions = PolygonAnnotationOptions()
                    .withPoints( newPointList )
                    .withFillColor( fillColor )
                    .withFillOpacity( fillOpacity.toDouble() / 255.0 )

                mapboxPolygon.polygonAnnotation = it.create(polygonAnnotationOptions)
            }

            mapboxPolylineAnnotationManager?.let {
                val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                    .withPoints(newPointList[0])
                    .withLineColor( borderColor )
                    .withLineWidth(4.0)

                mapboxPolygon.polylineAnnotation = it.create(polylineAnnotationOptions)
            }

            if (label.isNotEmpty())
            {
                val latLngBounds = GeoUtils.findGeobounds( points[0] )
                val labelBitmap = createTextLabel(MainApplication.getContext(), label)

                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude ))
                    .withIconImage(labelBitmap)

                mapboxPointAnnotationManager?.create(pointAnnotationOptions)
            }

            return mapboxPolygon
        }

        return null
    }

    fun removePolygon( mapView: View, polygon: Any )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.overlays.remove( polygon )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val mapboxPolygon = polygon as MapboxPolygon
            mapboxPolygonAnnotationManager?.delete( mapboxPolygon.polygonAnnotation!! )
            mapboxPolylineAnnotationManager?.delete( mapboxPolygon.polylineAnnotation!! )
        }
    }

    fun createPolyline( mapView: View, breadcrumbs: List<Breadcrumb> )
    {
        val points = ArrayList<Point>()

        for (breadcrumb in breadcrumbs)
        {
            points.add( Point.fromLngLat( breadcrumb.longitude, breadcrumb.latitude ))
        }

        createPolyline( mapView, points, Color.BLUE, 2f )
    }

    fun createPolyline( mapView: View, points: List<Point>, color: Int, lineWidth: Float = 4.0f ) : Any?
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val geoPoints = ArrayList<GeoPoint>()
            for (point in points)
            {
                geoPoints.add( GeoPoint(point.latitude(), point.longitude()))
            }

            val polyline = Polyline()
            polyline.setPoints(geoPoints)
            polyline.setColor(color)
            polyline.setWidth(lineWidth)

            mapView.getOverlayManager().add(polyline);
            mapView.invalidate();

            return polyline
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPolylineAnnotationManager?.let {

                val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                    .withPoints(points)
                    .withLineColor(color)
                    .withLineWidth(4.0)

                return it.create(polylineAnnotationOptions)
            }
        }

        return null
    }

    fun updatePolyline( mapView: View, polyline: Any, point: Point )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val x = (polyline as Polyline).points
            (polyline as Polyline).addPoint( GeoPoint( point.latitude(), point.longitude()))
            val y = (polyline as Polyline).points
            mapView.invalidate()
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val polylineAnnotation = polyline as PolylineAnnotation
            val points = ArrayList<Point>( polylineAnnotation.points )
            points.add( point )
            polylineAnnotation.points = points
            mapboxPolylineAnnotationManager?.update( polylineAnnotation )
        }
    }

    fun removePolyline( mapView: View, polyline: Any )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.overlays.remove( polyline as Polyline )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPolylineAnnotationManager?.delete( polyline as PolylineAnnotation )
        }
    }

    fun createMarker( context: Context, mapView: View, point: Point, @DrawableRes resourceId: Int, title: String = "" ) : Any?
    {
        val location = Location( point.latitude(), point.longitude(), point.altitude())
        return createMarker( context, mapView, location, resourceId, title )
    }

    open class TextOverlay : Overlay()
    {
    }

    fun createMarker( context: Context, mapView: View, location: Location, @DrawableRes resourceId: Int, title: String = "" )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val marker = org.osmdroid.views.overlay.Marker(mapView)
            marker.position = GeoPoint( location.latitude, location.longitude )
            marker.setAnchor( org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)

            val icon: Drawable? = ContextCompat.getDrawable(context, resourceId)
            val paddedIcon = InsetDrawable(icon, 36, 36, 36, 36)

            marker.title = title
            marker.icon = paddedIcon
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays.add(marker)

            if (title.isNotEmpty())
            {
                val textOverlay = object : TextOverlay()
                {
                    override fun draw( canvas: Canvas, mapView: org.osmdroid.views.MapView, shadow: Boolean)
                    {
                        if (!shadow)
                        {
                            val point = GeoPoint(location.latitude, location.longitude)
                            val screenPoint = android.graphics.Point()
                            mapView.projection.toPixels(point, screenPoint)

                            val paint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 30f
                                isAntiAlias = true
                                textAlign = Paint.Align.LEFT
                            }

                            val textBounds = Rect()
                            paint.getTextBounds(title, 0, title.length, textBounds)
                            val textWidth = textBounds.width()
                            val textHeight = textBounds.height()

                            val x = screenPoint.x.toFloat() - textWidth / 2
                            val y = screenPoint.y.toFloat() + textHeight / 2

                            canvas.drawText(title, x, y, paint)
                        }
                    }
                }

                mapView.overlays.add(textOverlay)
            }

            mapView.invalidate()

            marker.setOnMarkerClickListener { clickedMarker, mapView ->
                delegate?.onMarkerTapped( location )
                true
            }
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapView.getMapboxMap().getStyle { style ->
                val iconId = "custom-marker-${resourceId}"

                if (style.getStyleImage(iconId) == null)
                {
                    val bitmap = convertDrawableToBitmap(AppCompatResources.getDrawable(context,resourceId))!!
                    style.addImage(iconId, bitmap)
                }

                mapboxPointAnnotationManager?.let { pointAnnotationManager ->
                    val point = Point.fromLngLat( location.longitude, location.latitude )
                    val jsonElement = JsonObject().apply { addProperty( "uuid", location.uuid ) }
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint( point )
                        .withData( jsonElement )
                        .withIconImage( iconId )
                        .withTextField( title )

                    pointAnnotationManager.create(pointAnnotationOptions)
                }
            }
        }
    }

    fun createBreadcrumb( context: Context, mapView: View, point: Point, @DrawableRes resourceId: Int, title: String = "" )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val marker = org.osmdroid.views.overlay.Marker(mapView)
            marker.position = GeoPoint( point.latitude(), point.longitude())
            marker.setAnchor( org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)

            val icon: Drawable? = ContextCompat.getDrawable(context, resourceId)
            val paddedIcon = InsetDrawable(icon, 36, 36, 36, 36)

            marker.title = title
            marker.icon = paddedIcon
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays.add(marker)

            if (title.isNotEmpty())
            {
                val textOverlay = object : TextOverlay()
                {
                    override fun draw( canvas: Canvas, mapView: org.osmdroid.views.MapView, shadow: Boolean)
                    {
                        if (!shadow)
                        {
                            val point = GeoPoint(point.latitude(), point.longitude())
                            val screenPoint = android.graphics.Point()
                            mapView.projection.toPixels(point, screenPoint)

                            val paint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 30f
                                isAntiAlias = true
                                textAlign = Paint.Align.LEFT
                            }

                            val textBounds = Rect()
                            paint.getTextBounds(title, 0, title.length, textBounds)
                            val textWidth = textBounds.width()
                            val textHeight = textBounds.height()

                            val x = screenPoint.x.toFloat() - textWidth / 2
                            val y = screenPoint.y.toFloat() + textHeight / 2

                            canvas.drawText(title, x, y, paint)
                        }
                    }
                }

                mapView.overlays.add(textOverlay)
            }

            mapView.invalidate()
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapView.getMapboxMap().getStyle { style ->
                val iconId = "custom-marker-${resourceId}"

                if (style.getStyleImage(iconId) == null)
                {
                    val bitmap = convertDrawableToBitmap(AppCompatResources.getDrawable(context,resourceId))!!
                    style.addImage(iconId, bitmap)
                }

                mapboxBreadcrumbAnnotationManager?.let { pointAnnotationManager ->
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint( point )
                        .withIconImage( iconId )
                        .withTextField( title )

                    pointAnnotationManager.create( pointAnnotationOptions )
                }
            }
        }
    }

    fun getResourceName( id: Int ) : String
    {
        when (id) {
            R.drawable.home_black -> return "home_black"
            R.drawable.home_red -> return "home_red"
            R.drawable.home_green -> return "home_green"
            R.drawable.home_light_blue -> return "home_light_blue"
            R.drawable.home_orange -> return "home_orange"
            R.drawable.home_purple -> return "home_purple"
            R.drawable.multi_home_black -> return "multi_home_black"
            R.drawable.multi_home_red -> return "multi_home_red"
            R.drawable.multi_home_green -> return "multi_home_green"
            R.drawable.multi_home_light_blue -> return "multi_home_light_blue"
            R.drawable.multi_home_orange -> return "multi_home_orange"
            R.drawable.multi_home_purple -> return "multi_home_purple"
        }

        return ""
    }

    fun getResourceName(location: Location) : String
    {
        var resourceName = "home_black"

        if (location.isLandmark) {
            resourceName = "location_blue"
        } else if (location.enumerationItems.size == 1) {
            val enumerationItem = location.enumerationItems[0]

            if (enumerationItem.samplingState == SamplingState.Sampled) {
                when (enumerationItem.collectionState) {
                    CollectionState.Undefined -> resourceName = "home_light_blue"
                    CollectionState.Incomplete -> resourceName = "home_orange"
                    CollectionState.Complete -> resourceName = "home_purple"
                }
            } else if (enumerationItem.enumerationState == EnumerationState.Undefined) {
                resourceName = "home_black"
            } else if (enumerationItem.enumerationState == EnumerationState.Incomplete) {
                resourceName = "home_red"
            } else if (enumerationItem.enumerationState == EnumerationState.Enumerated) {
                resourceName = "home_green"
            }
        }
        else
        {
            for (enumerationItem in location.enumerationItems) {
                if (enumerationItem.samplingState == SamplingState.Sampled) {
                    if (enumerationItem.collectionState == CollectionState.Undefined) {
                        if (resourceName == "home_black") { // set only if not already set
                            resourceName = "multi_home_light_blue"
                        }
                    } else if (enumerationItem.collectionState == CollectionState.Incomplete) {
                        resourceName = "multi_home_orange"
                        break // any Incomplete makes it incomplete
                    } else if (enumerationItem.collectionState == CollectionState.Complete) {
                        resourceName = "multi_home_purple"
                    }
                }
            }

            if (resourceName == "home_black") // still not set
            {
                for (enumerationItem in location.enumerationItems) {
                    if (enumerationItem.enumerationState == EnumerationState.Undefined) {
                        resourceName = "multi_home_black"
                    } else if (enumerationItem.enumerationState == EnumerationState.Incomplete) {
                        resourceName = "multi_home_red"
                        break
                    } else if (enumerationItem.enumerationState == EnumerationState.Enumerated) {
                        resourceName = "multi_home_green"
                    }
                }
            }
        }

        return resourceName
    }

    fun createFeature( location: Location, icon: String ) : JSONObject
    {
        val geometry = JSONObject()
        geometry.put( "type", "Point" )

        val coordinates = JSONArray()
        coordinates.put( location.longitude )
        coordinates.put( location.latitude )

        geometry.put( "coordinates", coordinates )

        var title = ""
        if (location.enumerationItems.isNotEmpty())
        {
            title = location.enumerationItems.first().subAddress.toString()
        }

        val properties = JSONObject()
        properties.put("title", title )
        properties.put("iconName", icon )
        properties.put("POI", icon )
        properties.put("locationUuid", location.uuid)

        val feature = JSONObject()
        feature.put( "type", "Feature" )
        feature.put( "geometry", geometry )
        feature.put( "properties", properties )

        return feature
    }

    fun loadMarkers( context: Context, mapView: View, markerProperties: ArrayList<MarkerProperty>, showDetailButton: Boolean )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val clusterer = MyRadiusMarkerClusterer(context)
            clusterer.textPaint.color = Color.WHITE

            for (markerProperty in markerProperties)
            {
                val marker = Marker(mapView)

                marker.position = GeoPoint(markerProperty.location.latitude, markerProperty.location.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon = ContextCompat.getDrawable(context, markerProperty.resourceId)
                marker.title = markerProperty.title

                delegate?.let { delegate ->
                    marker.setOnMarkerClickListener { clickedMarker, mapView ->
                        delegate.onMarkerTapped( markerProperty.location )
                        true
                    }
                }

                clusterer.add(marker)
            }

            mapView.overlays.add(clusterer)
            mapView.invalidate()
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val features = JSONArray()

            for (markerProperty in markerProperties) {
                // Create the base feature
                val feature = MapManager.instance().createFeature(
                    markerProperty.location,
                    MapManager.instance().getResourceName(markerProperty.resourceId)
                )

                // Get the properties JSONObject
                val properties = feature.getJSONObject("properties")

                // Add the label property
                properties.put("title", markerProperty.title) // the text you want over the icon

                // Add feature to array
                features.put(feature)
            }

            val geoJson = JSONObject()
            geoJson.put("type", "FeatureCollection")
            geoJson.put("features", features)

            loadMarkers( context, mapView, geoJson.toString(), showDetailButton )
        }
    }

    fun loadMarkers( activity: Activity, mapView: MapView, enumAreas: ArrayList<EnumArea> )
    {
        val geoJson = JSONObject()
        geoJson.put("type", "FeatureCollection")

        val features = JSONArray()

        for (enumArea in enumAreas)
        {
            for (location in enumArea.locations)
            {
                features.put(instance().createFeature(location, MapManager.instance().getResourceName(location)))
            }
        }

        geoJson.put("features", features)

        loadMarkers( activity, mapView, geoJson.toString(), false )
    }

    fun loadMarkers( context: Context, mapView: MapView, geoJson: String, showDetailButton: Boolean )
    {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)

        mapView.getMapboxMap().loadStyle(style(mapStyle!!) {

            TileServer.rasterSource?.let { +it }
            TileServer.rasterLayer?.let { +it }

            val iconNames = listOf(
                "home_black", "home_blue", "home_green", "home_light_blue",
                "home_orange", "home_purple", "home_red",
                "multi_home_black", "multi_home_blue", "multi_home_green",
                "multi_home_light_blue", "multi_home_orange",
                "multi_home_purple", "multi_home_red"
            )

            iconNames.forEach { name ->
                +image(name) {
                    bitmap(BitmapFactory.decodeResource(context.resources, context.resources.getIdentifier(name, "drawable", context.packageName)))
                }
            }

            +geoJsonSource("SOURCE_ID") {
                data(geoJson)
                cluster(true)
                clusterRadius(20)
                clusterMaxZoom(16)
                build()
            }

            +circleLayer("CLUSTER_LAYER", "SOURCE_ID") {
                filter(has("point_count"))
                circleRadius(15.0)
                circleColor(Color.BLUE)
                circleOpacity(0.5)
            }

            +symbolLayer("UNCLUSTERED_LAYER", "SOURCE_ID") {
                filter(not(has("point_count"))) // only single points

                // Icon
                iconImage(get("iconName"))       // your property in GeoJSON
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
                iconAnchor(IconAnchor.CENTER)
            }
        }) { style ->
            mapView.gestures.addOnMapClickListener { point ->

                currentPopup?.let {
                    currentPopup!!.visibility = View.GONE
                    currentPopup = null
                }

                val rectSize = 30.0 // pixels
                val screenCoord = mapView.mapboxMap.pixelForCoordinate(point)
                val queryRect = ScreenBox(
                    ScreenCoordinate(screenCoord.x - rectSize / 2, screenCoord.y - rectSize / 2),
                    ScreenCoordinate(screenCoord.x + rectSize / 2, screenCoord.y + rectSize / 2)
                )

                mapView.mapboxMap.queryRenderedFeatures(
                    RenderedQueryGeometry(queryRect),
                    RenderedQueryOptions(listOf("CLUSTER_LAYER", "UNCLUSTERED_LAYER"), null)
                ) { result ->
                    val features = result.value

                    if (!features.isNullOrEmpty())
                    {
                        for (queriedFeature in features)
                        {
                            var title = ""
                            var isHH = true
                            val feature = queriedFeature.queriedFeature.feature

                            if (feature.hasProperty( "title" ))
                            {
                                isHH = true
                                title = feature.getStringProperty( "title" )
                            }
                            else if (feature.hasProperty( "point_count" ))
                            {
                                isHH = false
                                title = "# items: " + feature.getNumberProperty( "point_count" )
                            }

                            val locationUuid = feature.getStringProperty("locationUuid")

                            if (title.isEmpty() && locationUuid.isNotEmpty())
                            {
                                DAO.locationDAO.getLocation(locationUuid)?.let {
                                    delegate?.onMarkerTapped(it)
                                }
                            }
                            else
                            {
                                val featurePoint = feature.geometry() as? Point
                                featurePoint?.let { point ->
                                    val screen = mapView.mapboxMap.pixelForCoordinate(point)

                                    showHHPopup( mapView, screen, title, isHH && showDetailButton )
                                    {
                                        currentPopup!!.visibility = View.GONE
                                        currentPopup = null
                                        DAO.locationDAO.getLocation(locationUuid)?.let {
                                            delegate?.onMarkerTapped(it)
                                        }
                                    }
                                }

                                if (currentPopup != null)
                                {
                                    break
                                }
                            }
                        }
                    }
                }

                true
            }
        }
    }

    private var currentPopup : View? = null

    fun showHHPopup( mapView: MapView, screenPoint: ScreenCoordinate, title: String, showDetailButton: Boolean, onNavigate: () -> Unit)
    {
        (mapView.parent as View).findViewById<View>(R.id.hh_popup)?.let { popup ->
            val textView = popup.findViewById<TextView>(R.id.title)
            val button = popup.findViewById<Button>(R.id.details_button)

            currentPopup = popup
            textView.text = title

            if (!showDetailButton) {
                button.visibility = View.GONE
            } else {
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    popup.visibility = View.GONE
                    onNavigate()
                }
            }

            // Force measure and position correctly
            popup.post {
                // Measure the popup to get actual width/height
                popup.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val popupWidth = popup.measuredWidth
                val popupHeight = popup.measuredHeight

                popup.x = screenPoint.x.toFloat() - popupWidth / 2
                popup.y = screenPoint.y.toFloat() - popupHeight - 20
                popup.visibility = View.VISIBLE
            }
        }
    }

    data class MarkerProperty( var location: Location, var resourceId: Int, var title: String )
    {
    }

    class MyRadiusMarkerClusterer(context: Context) : RadiusMarkerClusterer(context) {

        private val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        private val labelZoomThreshold = 18.0  // adjust to taste

        override fun draw(canvas: Canvas, mapView: org.osmdroid.views.MapView, shadow: Boolean) {
            super.draw(canvas, mapView, shadow)

            if (shadow) return

            // Only draw labels when zoomed in
            if (mapView.zoomLevelDouble < labelZoomThreshold) return

            for (marker in items) {
                val point = mapView.projection.toPixels(marker.position, null)

                marker.title?.let { title ->
                    val icon = marker.icon
                    val iconHeight = icon?.intrinsicHeight ?: 0

                    val textBounds = Rect()
                    labelPaint.getTextBounds(title, 0, title.length, textBounds)
                    val textHeight = textBounds.height()

                    val yOffset = iconHeight / 2f - textHeight / 2f
                    canvas.drawText(title, point.x.toFloat(), point.y.toFloat() - yOffset, labelPaint)
                }
            }
        }
    }

    fun setZoomLevel( mapView: View, zoomLevel: Double )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.controller.setZoom( zoomLevel )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val cameraPosition = CameraOptions.Builder()
                .zoom(zoomLevel)
                .build()

            mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    fun centerMap( polygon: ArrayList<LatLon>, zoomLevel: Double, mapView: View )
    {
        val latLngBounds = GeoUtils.findGeobounds(polygon)

        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.controller.setZoom( zoomLevel )
            mapView.controller.setCenter( org.osmdroid.util.GeoPoint( latLngBounds.center.latitude, latLngBounds.center.longitude, 0.0 ))
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            val cameraPosition = CameraOptions.Builder()
                .zoom( zoomLevel )
                .center(point)
                .build()

            mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    fun centerMap( enumArea: EnumArea, zoomLevel: Double, mapView: View )
    {
        val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)

        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.controller.setZoom( zoomLevel )
            mapView.controller.setCenter( org.osmdroid.util.GeoPoint( latLngBounds.center.latitude, latLngBounds.center.longitude, 0.0 ))
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            val cameraPosition = CameraOptions.Builder()
                .zoom( zoomLevel )
                .center(point)
                .build()

            mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    fun centerMap( point: Point, zoomLevel: Double, mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.controller.setZoom( zoomLevel )
            mapView.controller.setCenter( org.osmdroid.util.GeoPoint( point.latitude(), point.longitude(), point.altitude() ))
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val cameraPosition = CameraOptions.Builder()
                .zoom( zoomLevel )
                .center(point)
                .build()

            mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    fun getLocationFromPixelPoint( mapView: View, motionEvent: MotionEvent ) : Point
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val geoPoint = mapView.getProjection().fromPixels(motionEvent.x.toInt(), motionEvent.y.toInt()) as GeoPoint
            return Point.fromLngLat( geoPoint.getLongitude(), geoPoint.getLatitude())
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            return mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(motionEvent.x.toDouble(),motionEvent.y.toDouble()))
        }

        return Point.fromLngLat( 0.0, 0.0 )
    }

    fun getIconDrawable(context: Context, iconName: String): Drawable?
    {
        val resId = context.resources.getIdentifier( iconName, "drawable", context.packageName)
        return if (resId != 0) ContextCompat.getDrawable(context, resId) else null
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap?
    {
        if (sourceDrawable == null)
        {
            return null
        }

        return if (sourceDrawable is BitmapDrawable)
        {
            sourceDrawable.bitmap
        }
        else
        {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    fun cacheMapTiles( activity: Activity, mapView: View, mapTileRegions: ArrayList<MapTileRegion>, delegate: MapManager.MapTileCacheDelegate )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            CoroutineScope(Dispatchers.Main).launch {
                for (mapTileRegion in mapTileRegions)
                {
                    val boundingBox = BoundingBox(
                        mapTileRegion.northEast.latitude,
                        mapTileRegion.northEast.longitude,
                        mapTileRegion.southWest.latitude,
                        mapTileRegion.southWest.longitude
                    )

                    val center = GeoPoint(boundingBox.centerLatitude, boundingBox.centerLongitude )

                    for (zoom in MIN_ZOOM..MAX_ZOOM)
                    {
                        mapView.controller.setCenter(center)
                        mapView.controller.setZoom(zoom)
                        mapView.invalidate()
                        delay(2000)
                    }
                }

                delegate.tilePacksLoaded("")
            }
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            cacheMapboxTiles(activity, mapView, mapTileRegions, delegate )
        }
    }

    fun cacheMapboxTiles( context: Context, mapView: com.mapbox.maps.MapView, mapTileRegions: ArrayList<MapTileRegion>, delegate: MapManager.MapTileCacheDelegate )
    {
        val STYLE_PACK_METADATA = "STYLE_PACK_METADATA"

        val stylePackLoadOptions = StylePackLoadOptions.Builder()
            .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
            .metadata(Value(STYLE_PACK_METADATA))
            .build()

        val offlineManager = OfflineManager()
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)

        stylePackCancelable = offlineManager.loadStylePack(
            mapStyle!!,
            stylePackLoadOptions,
            { progress ->
            },
            { expected ->
                if (expected.isValue) {
                    expected.value?.let { stylePack ->
                        // Style pack download finished successfully
                        Log.d( "xxx", "Style Pack download finished")
                        loadMapboxTilePacks( context, mapTileRegions, delegate )
                    }
                }
                expected.error?.let {
                    // Handle errors that occurred during the style pack download.
                    Log.d( "xxx", it.message )
                    delegate.tilePacksLoaded( it.message )
                }
            }
        )
    }

    fun loadMapboxTilePacks( context: Context, mapTileRegions: ArrayList<MapTileRegion>, delegate: MapTileCacheDelegate )
    {
        val offlineManager = OfflineManager()
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)
        MapboxOptions.accessToken = context.resources.getString(R.string.mapbox_access_token)

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(mapStyle!!)
                .minZoom(MIN_ZOOM.toByte())
                .maxZoom(MAX_ZOOM.toByte())
                .build()
        )

        val tileStore = TileStore.create()

        var id = 0
        tileRegionsCancelable.clear()
        val numRegionsLeft = AtomicInteger(mapTileRegions.size)

        for (mapTileRegion in mapTileRegions)
        {
            id += 1

            Log.d( "xxx", "downloading region ${id}")

            val points = java.util.ArrayList<Point>()
            points.add( Point.fromLngLat( mapTileRegion.southWest.longitude, mapTileRegion.southWest.latitude ))
            points.add( Point.fromLngLat( mapTileRegion.northEast.longitude, mapTileRegion.southWest.latitude ))
            points.add( Point.fromLngLat( mapTileRegion.northEast.longitude, mapTileRegion.northEast.latitude ))
            points.add( Point.fromLngLat( mapTileRegion.southWest.longitude, mapTileRegion.northEast.latitude ))
            points.add( Point.fromLngLat( mapTileRegion.southWest.longitude, mapTileRegion.southWest.latitude ))

            val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()
            pointList.add( points )
            val geometry = com.mapbox.geojson.Polygon.fromLngLats(pointList as List<MutableList<Point>>)

            val TILE_REGION_METADATA = "TILE_REGION_METADATA"

            val tileRegionCancelable = tileStore.loadTileRegion(
                id.toString(),
                TileRegionLoadOptions.Builder()
                    .geometry(geometry)
                    .descriptors(listOf(tilesetDescriptor))
                    .metadata(Value(TILE_REGION_METADATA))
                    .acceptExpired(true)
                    .networkRestriction(NetworkRestriction.NONE)
                    .build(),
                { progress ->
                    Log.d( "xxx", " ${progress.completedResourceCount} / ${progress.requiredResourceCount}" )
                    delegate.mapLoadProgress( progress.completedResourceCount, progress.requiredResourceCount )
                }
            ) { expected ->
                if (expected.isValue) {
                    if (numRegionsLeft.decrementAndGet() <= 0)
                    {
                        delegate.tilePacksLoaded("")
                        Log.d( "xxx", "Tile Regions download finished")
                    }
                }
                expected.error?.let {
                    // Handle errors that occurred during the tile region download.
                    Log.d( "xxx", it.message )
                    cancelTilePackDownload()
                    delegate.tilePacksLoaded( it.message )
                    return@let
                }
            }

            tileRegionsCancelable.add( tileRegionCancelable )
        }
    }

    fun cancelTilePackDownload()
    {
        stylePackCancelable?.let {
            it.cancel()
            stylePackCancelable = null
        }

        for (tileRegionCancelable in tileRegionsCancelable)
        {
            tileRegionCancelable.cancel()
        }

        tileRegionsCancelable.clear()
    }

    private var stylePackCancelable: Cancelable? = null
    private var tileRegionsCancelable = ArrayList<Cancelable>()

    interface MapTileCacheDelegate
    {
        fun tilePacksLoaded( error: String )
        fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    }

    companion object
    {
        val GEORGIA_TECH = Point.fromLngLat(-84.39801338134015, 33.778349807286304 )

        private var _instance: MapManager? = null

        fun instance() : MapManager
        {
            if (_instance == null)
            {
                _instance = MapManager()
            }

            return _instance!!
        }
    }
}