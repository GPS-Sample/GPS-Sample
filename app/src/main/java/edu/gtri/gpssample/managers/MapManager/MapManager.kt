package edu.gtri.gpssample.managers.MapManager

import android.view.View
import com.mapbox.maps.Style
import android.content.Context
import android.preference.PreferenceManager
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location

class MapManager
{
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