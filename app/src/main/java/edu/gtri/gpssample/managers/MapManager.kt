package edu.gtri.gpssample.managers

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
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
import com.mapbox.maps.plugin.locationcomponent.location
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.utils.GeoUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection


class MapManager
{
    var mapboxPointAnnotationManager: PointAnnotationManager? = null
    var mapboxPolygonAnnotationManager: PolygonAnnotationManager? = null
    var mapboxPolylineAnnotationManager: PolylineAnnotationManager?= null

    // public functions

    fun initialize( context: Context, mapView: View, style: String, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit) )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            initializeOsmMap( context, mapView, lat, lon, alt, zoom, completion )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            initializeMapboxMap( mapView, style, lat, lon, alt, zoom, completion )
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

    fun addMarker( context: Context, mapView: View, point: Point, @DrawableRes resourceId: Int ) : Any?
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

    // private functions

    private fun initializeOsmMap( context: Context, mapView: org.osmdroid.views.MapView, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit) )
    {
        org.osmdroid.config.Configuration.getInstance().load( context, PreferenceManager.getDefaultSharedPreferences(context))

        mapView.controller.setZoom( zoom )
        mapView.setMultiTouchControls( true )
        mapView.controller.setCenter( org.osmdroid.util.GeoPoint( lat, lon, alt ))
        completion()
    }

    private fun initializeMapboxMap( mapView: com.mapbox.maps.MapView, style: String, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit))
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

                    completion()
                }
            }
        )
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