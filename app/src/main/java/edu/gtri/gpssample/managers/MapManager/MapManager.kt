package edu.gtri.gpssample.managers.MapManager

import android.view.View
import com.mapbox.maps.Style
import android.content.Context
import android.preference.PreferenceManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MapManager
{
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager?= null

    // public functions

    fun initialize( context: Context, mapView: View, style: String, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit) )
    {
        if (mapView is org.osmdroid.views.MapView)
        {
            initializeOsmMap( context, mapView, lat, lon, alt, zoom )
        }
        else if (mapView is com.mapbox.maps.MapView)
        {
            initializeMapboxMap( mapView, style, lat, lon, alt, zoom, completion )
        }
    }

    fun createPointAnnotationManager( mapView: MapView )
    {
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
    }

    fun createPolylineAnnotationManager( mapView: MapView )
    {
        polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager()
    }

    fun createPolygonAnnotationManager( mapView: MapView )
    {
        polygonAnnotationManager = mapView.annotations.createPolygonAnnotationManager()
    }

    fun createPolygon( points: List<List<Point>>, fillColor: String, fillOpacity: Double )
    {
        polygonAnnotationManager?.let {
            val polygonAnnotationOptions = PolygonAnnotationOptions()
                .withPoints( points )
                .withFillColor( fillColor )
                .withFillOpacity( fillOpacity )

            it.create(polygonAnnotationOptions)
        }
    }

    fun createPolyline( points: List<Point>, color: String )
    {
        polylineAnnotationManager?.let {
            val outlinePoints = ArrayList<Point>(points)
            outlinePoints.add( outlinePoints[0] )

            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(outlinePoints)
                .withLineColor(color)
                .withLineWidth(4.0)

            it.create(polylineAnnotationOptions)
        }
    }

    // private functions

    private fun initializeOsmMap( context: Context, mapView: org.osmdroid.views.MapView, lat: Double, lon: Double, alt: Double, zoom: Double )
    {
        org.osmdroid.config.Configuration.getInstance().load( context, PreferenceManager.getDefaultSharedPreferences(context))

        mapView.controller.setZoom( zoom )
        mapView.setMultiTouchControls( true )
        mapView.controller.setCenter( org.osmdroid.util.GeoPoint( lat, lon, alt ))
    }

    private fun initializeMapboxMap( mapView: com.mapbox.maps.MapView, style: String, lat: Double, lon: Double, alt: Double, zoom: Double, completion: (()->Unit))
    {
        createPointAnnotationManager( mapView )
        createPolygonAnnotationManager( mapView )
        createPolylineAnnotationManager( mapView )

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

    companion object
    {
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