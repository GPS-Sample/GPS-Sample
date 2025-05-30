package edu.gtri.gpssample.managers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
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
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.utils.GeoUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapManager
{
    var mapboxPointAnnotationManager: PointAnnotationManager? = null
    var mapboxPolygonAnnotationManager: PolygonAnnotationManager? = null
    var mapboxPolylineAnnotationManager: PolylineAnnotationManager?= null

    // public functions

    fun selectMap( activity: Activity, config: Config, osmMapView: org.osmdroid.views.MapView, mapBoxMapView: com.mapbox.maps.MapView, completion: (()->Unit) )
    {
        if (config.mapEngineIndex == MapEngine.OpenStreetMap.value)
        {
            mapBoxMapView.visibility = View.GONE
            osmMapView.visibility = View.VISIBLE

            initializeOsmMap( activity, osmMapView, GEORGIA_TECH.latitude, GEORGIA_TECH.longitude, 0.0, 18.0 ) {
                completion()
            }
        }
        else if (config.mapEngineIndex == MapEngine.MapBox.value)
        {
            mapBoxMapView.visibility = View.VISIBLE
            osmMapView.visibility = View.GONE

            val sharedPreferences: SharedPreferences = activity.getSharedPreferences("default", 0)
            var style = Style.MAPBOX_STREETS

            sharedPreferences.getString( Keys.kMapStyle.value, null)?.let {
                style = it
            }

            initializeMapboxMap( activity, mapBoxMapView, style, GEORGIA_TECH.latitude, GEORGIA_TECH.longitude, 0.0, 18.0 ) {
                completion()
            }
        }
    }

    fun initializeOsmMap( activity: Activity, mapView: org.osmdroid.views.MapView, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit) )
    {
        org.osmdroid.config.Configuration.getInstance().load( activity, PreferenceManager.getDefaultSharedPreferences(activity))

        mapView.controller.setZoom( zoom )
        mapView.setMultiTouchControls( true )
        mapView.controller.setCenter( org.osmdroid.util.GeoPoint( lat, lon, alt ))

        enableOsmMapLocationUpdates( activity, mapView )
        completion()
    }

    fun initializeMapboxMap( activity: Activity, mapView: com.mapbox.maps.MapView, style: String, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit))
    {
        createMapboxPointAnnotationManager( mapView )
        createMapboxPolygonAnnotationManager( mapView )
        createMapboxPolylineAnnotationManager( mapView )

        mapView.getMapboxMap().loadStyleUri(
            style,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    val point = com.mapbox.geojson.Point.fromLngLat( lon, lat, alt )
                    val cameraPosition = CameraOptions.Builder()
                        .zoom(zoom)
                        .center(point)
                        .build()

                    mapView.getMapboxMap().setCamera( cameraPosition )

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

                    enableMapboxLocationUpdates( activity, mapView )

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

    private var myLocationNewOverlay: MyLocationNewOverlay? = null

    fun enableOsmMapLocationUpdates( activity: Activity, mapView: org.osmdroid.views.MapView )
    {
        val locationProvider = GpsMyLocationProvider( activity )
        locationProvider.locationUpdateMinTime = 1000 // 1 second
        locationProvider.locationUpdateMinDistance = 5f // 5 meters

        if (myLocationNewOverlay != null)
        {
            mapView.getOverlays().remove( myLocationNewOverlay )
        }

        myLocationNewOverlay = MyLocationNewOverlay(locationProvider, mapView)

        myLocationNewOverlay!!.enableMyLocation()
        myLocationNewOverlay!!.enableFollowLocation()

        mapView.getOverlays().add( myLocationNewOverlay )
    }

    fun disableOsmMapLocationUpdates( activity: Activity, mapView: org.osmdroid.views.MapView )
    {
        if (myLocationNewOverlay != null)
        {
            mapView.getOverlays().remove( myLocationNewOverlay )
            myLocationNewOverlay = null
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

    fun getLocationFromPoint( mapView: View, motionEvent: MotionEvent ) : Pair<Double, Double>
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            val point = mapView.getProjection().fromPixels(motionEvent.x.toInt(), motionEvent.y.toInt()) as GeoPoint
            return Pair( point.getLatitude(), point.getLongitude())
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val point = mapView.getMapboxMap().coordinateForPixel(ScreenCoordinate(motionEvent.x.toDouble(),motionEvent.y.toDouble()))
            return Pair( point.latitude(), point.longitude())
        }

        return Pair( 0.0, 0.0 )
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

    fun createPolygon( mapView: View, points: List<List<Point>>, fillColor: String, fillOpacity: Double ) : Any?
    {
        if (mapView is org.osmdroid.views.MapView)
        {
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPolygonAnnotationManager?.let {
                val polygonAnnotationOptions = PolygonAnnotationOptions()
                    .withPoints( points )
                    .withFillColor( fillColor )
                    .withFillOpacity( fillOpacity )

                return it.create(polygonAnnotationOptions)
            }
        }

        return null
    }

    fun createPolyline( mapView: View, points: List<Point>, color: String ) : Any?
    {
        if (mapView is org.osmdroid.views.MapView)
        {
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPolylineAnnotationManager?.let {
                val outlinePoints = ArrayList<Point>(points)
                outlinePoints.add( outlinePoints[0] )

                val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                    .withPoints(outlinePoints)
                    .withLineColor(color)
                    .withLineWidth(4.0)

                return it.create(polylineAnnotationOptions)
            }
        }

        return null
    }

    fun centerMap( enumArea: EnumArea, zoomLevel: Double?, mapView: View )
    {
        val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)

        if (mapView is org.osmdroid.views.MapView)
        {
            mapView.controller.setCenter( org.osmdroid.util.GeoPoint( latLngBounds.center.latitude, latLngBounds.center.longitude, 0.0 ))
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            val cameraPosition = CameraOptions.Builder()
                .zoom(zoomLevel)
                .center(point)
                .build()

            mapView.getMapboxMap().setCamera(cameraPosition)
        }
    }

    fun createMarker( context: Context, mapView: View, point: Point, @DrawableRes resourceId: Int ) : Any?
    {
        if (mapView is org.osmdroid.views.MapView)
        {
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            mapboxPointAnnotationManager?.let {
                val pointAnnotationOptions = PointAnnotationOptions().withPoint( point )

                convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))?.let { bitmap ->
                    pointAnnotationOptions.withIconImage( bitmap )
                }

                return it.create(pointAnnotationOptions)
            }
        }

        return null
    }

    fun removeViewAnnotation(viewAnnotationManager: ViewAnnotationManager, label: String )
    {
        if (label.isNotEmpty())
        {
            for (annotation in viewAnnotationManager.annotations)
            {
                annotation.key.rootView.findViewById<TextView>( R.id.text_view )?.let {
                    if (it.text == label)
                    {
                        viewAnnotationManager.removeViewAnnotation( annotation.key )
                    }
                }
            }
        }
    }

    fun removeLabel( viewAnnotationManager: ViewAnnotationManager, mapView: View, label: String )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            if (label.isNotEmpty())
            {
                for (annotation in viewAnnotationManager.annotations)
                {
                    annotation.key.rootView.findViewById<TextView>( R.id.text_view )?.let {
                        if (it.text == label)
                        {
                            viewAnnotationManager.removeViewAnnotation( annotation.key )
                        }
                    }
                }
            }
        }
    }

    fun addLabelToPoint(viewAnnotationManager: ViewAnnotationManager, mapView: View, point: com.mapbox.geojson.Point, label: String, backgroundColor: String )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            addViewAnnotationToPoint( viewAnnotationManager, point, label, backgroundColor )
        }
    }

    fun addViewAnnotationToPoint(viewAnnotationManager: ViewAnnotationManager, point: com.mapbox.geojson.Point, label: String, backgroundColor: String )
    {
        if (label.isNotEmpty())
        {
            removeViewAnnotation(viewAnnotationManager, label)

            val view = viewAnnotationManager.addViewAnnotation(
                resId = R.layout.view_text_view,
                options = viewAnnotationOptions
                {
                    allowOverlap(true)
                    geometry(point)
                }
            )

            view.rootView.findViewById<TextView>( R.id.text_view )?.let {
                it.text = label
                it.backgroundTintList = ColorStateList.valueOf(Color.parseColor(backgroundColor))
            }
        }
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