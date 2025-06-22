package edu.gtri.gpssample.managers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
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
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.managers.TileServer.Companion.rasterLayer
import edu.gtri.gpssample.managers.TileServer.Companion.rasterSource
import edu.gtri.gpssample.utils.GeoUtils
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

    var mapboxPointAnnotationManager: PointAnnotationManager? = null
    var mapboxPolygonAnnotationManager: PolygonAnnotationManager? = null
    var mapboxPolylineAnnotationManager: PolylineAnnotationManager?= null

    // public functions

    fun selectMap( activity: Activity, config: Config, osmMapView: org.osmdroid.views.MapView, mapBoxMapView: com.mapbox.maps.MapView, delegate: MapManagerDelegate? = null, completion: ((mapView: View)->Unit) )
    {
        this.delegate = delegate

        val sharedPreferences: SharedPreferences = activity.getSharedPreferences("default", 0)
        val mapStyle = sharedPreferences.getString( Keys.kMapStyle.value, Style.MAPBOX_STREETS)

        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
        {
            mapBoxMapView.visibility = View.GONE
            osmMapView.visibility = View.VISIBLE

            initializeOsmMap( activity, osmMapView, mapStyle!! ) {
                completion( osmMapView )
            }
        }
        else if (config.mapEngineIndex == MapEngine.MapBox.value)
        {
            mapBoxMapView.visibility = View.VISIBLE
            osmMapView.visibility = View.GONE

            initializeMapboxMap( activity, mapBoxMapView, mapStyle!! ) {
                completion( mapBoxMapView )
            }
        }
    }

    fun initializeOsmMap( activity: Activity, mapView: org.osmdroid.views.MapView, mapStyle: String, completion: (()->Unit) )
    {
        org.osmdroid.config.Configuration.getInstance().load( activity, PreferenceManager.getDefaultSharedPreferences(activity))

        mapView.overlays.clear()
        mapView.minZoomLevel = 0.0
        mapView.maxZoomLevel = 24.0
        mapView.setMultiTouchControls( true )

        var tileSource = TileSourceFactory.MAPNIK

        if (mapStyle == Style.SATELLITE_STREETS)
        {
            tileSource = TileSourceFactory.USGS_SAT
        }

        // Mapnik (OSM standard tiles)
        val osmProvider = MapTileProviderBasic(activity)
        osmProvider.tileSource = tileSource
        val osmOverlay = TilesOverlay(osmProvider, activity)
        mapView.overlays.add(osmOverlay)

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

        completion()
    }

    fun initializeMapboxMap( activity: Activity, mapView: com.mapbox.maps.MapView, style: String, completion: (()->Unit))
    {
        createMapboxPointAnnotationManager( mapView )
        createMapboxPolygonAnnotationManager( mapView )
        createMapboxPolylineAnnotationManager( mapView )

        mapView.getMapboxMap().loadStyle(
            com.mapbox.maps.extension.style.style(styleUri = style) {
                rasterSource?.let {
                    +it
                }
                rasterLayer?.let {
                    +it
                }
            },
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
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

    fun disableLocationUpdates( activity: Activity, mapView: View )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            disableOsmMapLocationUpdates( activity, mapView )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            disableMapboxLocationUpdates( activity, mapView )
        }
    }

    fun enableOsmMapLocationUpdates( activity: Activity, mapView: org.osmdroid.views.MapView )
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

        myLocationNewOverlay.enableMyLocation()
        myLocationNewOverlay.enableFollowLocation()

        mapView.getOverlays().add( myLocationNewOverlay )
    }

    fun disableOsmMapLocationUpdates( activity: Activity, mapView: org.osmdroid.views.MapView )
    {
        for (overlay in mapView.overlays)
        {
            if (overlay is MyLocationNewOverlay)
            {
                mapView.overlays.remove( overlay )
                break
            }
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

        mapView.location.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    activity,
                    edu.gtri.gpssample.R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    activity,
                    edu.gtri.gpssample.R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = Expression.interpolate {
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

        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener!!)
    }

    fun disableMapboxLocationUpdates( activity: Activity, mapView: com.mapbox.maps.MapView )
    {
        if (onIndicatorPositionChangedListener != null)
        {
            mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener!!)
            onIndicatorPositionChangedListener = null
        }
    }

    fun createMapboxPointAnnotationManager( mapView: MapView )
    {
        mapboxPointAnnotationManager = mapView.annotations.createPointAnnotationManager()
    }

    fun createMapboxPolylineAnnotationManager( mapView: MapView )
    {
        mapboxPolylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager()
    }

    fun createMapboxPolygonAnnotationManager( mapView: MapView )
    {
        mapboxPolygonAnnotationManager = mapView.annotations.createPolygonAnnotationManager()
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
        }
    }

    fun createTextLabel(context: Context, text: String): Bitmap {
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
                outlinePaint.strokeWidth = 8f
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

    fun createPolyline( mapView: View, points: List<Point>, color: Int ) : Any?
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
            polyline.setWidth(10f)

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

    fun createMarker( context: Context, mapView: View, point: Point, @DrawableRes resourceId: Int, title: String = "" ) : Any?
    {
        val location = Location( point.latitude(), point.longitude(), point.altitude())
        return createMarker( context, mapView, location, resourceId, title )
    }

    fun createMarker( context: Context, mapView: View, location: Location, @DrawableRes resourceId: Int, title: String = "" ) : Any?
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val marker = org.osmdroid.views.overlay.Marker(mapView)
            marker.position = GeoPoint( location.latitude, location.longitude )
            marker.setAnchor( org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)

            val icon: Drawable? = ContextCompat.getDrawable(context, resourceId)

            marker.icon = icon
            marker.title = title
            mapView.overlays.add(marker)

            val textOverlay = object : Overlay() {
                override fun draw(
                    canvas: Canvas,
                    mapView: org.osmdroid.views.MapView,
                    shadow: Boolean
                ) {
                    if (!shadow) {
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
            mapView.invalidate()

            marker.setOnMarkerClickListener { clickedMarker, mapView ->
                delegate?.onMarkerTapped( location )
                true
            }
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPointAnnotationManager?.let { pointAnnotationManager ->
                val point = Point.fromLngLat( location.longitude, location.latitude )
                val jsonElement = JsonObject().apply { addProperty( "uuid", location.uuid ) }
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint( point )
                    .withData( jsonElement )

                convertDrawableToBitmap( AppCompatResources.getDrawable(context, resourceId))?.let { bitmap ->
                    pointAnnotationOptions.withIconImage( bitmap )
                }

                val pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
                pointAnnotation.textField = title

                pointAnnotationManager.apply {
                    addClickListener(
                        OnPointAnnotationClickListener { pointAnnotation ->
                            pointAnnotation.getData()?.asJsonObject?.get("uuid")?.asString?.let { uuid ->
                                DAO.locationDAO.getLocation( uuid )?.let {
                                    delegate?.onMarkerTapped( it )
                                }
                            }
                            true
                        }
                    )
                }

                return pointAnnotation
            }
        }

        return null
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

    // private functions

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

    companion object
    {
        private var _instance: MapManager? = null

        val GEORGIA_TECH = LatLng( 33.77577524978659, -84.39630379821243 )

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