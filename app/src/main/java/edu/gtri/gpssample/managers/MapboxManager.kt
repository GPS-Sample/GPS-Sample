/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.managers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.utils.GeoUtils

class MapboxManager( var context: Context )
{
    fun addMarker( pointAnnotationManager: PointAnnotationManager?, point: Point, @DrawableRes resourceId: Int ) : PointAnnotation?
    {
        pointAnnotationManager?.let {
            val pointAnnotationOptions = PointAnnotationOptions().withPoint( point )

            convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))?.let { bitmap ->
                pointAnnotationOptions.withIconImage( bitmap )
            }

            return pointAnnotationManager.create(pointAnnotationOptions)
        }

        return null
    }

    fun addPolygon( polygonAnnotationManager: PolygonAnnotationManager?, points: List<List<Point>>, fillColor: String, fillOpacity: Double ) : PolygonAnnotation?
    {
        polygonAnnotationManager?.let {
            val polygonAnnotationOptions = PolygonAnnotationOptions()
                .withPoints( points )
                .withFillColor( fillColor )
                .withFillOpacity( fillOpacity )

            return polygonAnnotationManager.create(polygonAnnotationOptions)
        }

        return null
    }

    fun addPolyline( polylineAnnotationManager: PolylineAnnotationManager?, points: List<Point>, color: String ) : PolylineAnnotation?
    {
        polylineAnnotationManager?.let {
            val outlinePoints = ArrayList<Point>(points)
            outlinePoints.add( outlinePoints[0] )

            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(outlinePoints)
                .withLineColor(color)
                .withLineWidth(4.0)

            return polylineAnnotationManager.create(polylineAnnotationOptions)
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

    fun createPointAnnotationManager( pointAnnotationManager: PointAnnotationManager?, mapView: MapView ) : PointAnnotationManager
    {
        pointAnnotationManager?.let {
            mapView.annotations.removeAnnotationManager( it )
        }

        return mapView.annotations.createPointAnnotationManager()
    }

    fun createPolygonAnnotationManager( polygonAnnotationManager: PolygonAnnotationManager?, mapView: MapView ) : PolygonAnnotationManager
    {
        polygonAnnotationManager?.let {
            mapView.annotations.removeAnnotationManager( it )
        }

        return mapView.annotations.createPolygonAnnotationManager()
    }

    fun createPolylineAnnotationManager( polylineAnnotationManager: PolylineAnnotationManager?, mapView: MapView ) : PolylineAnnotationManager
    {
        polylineAnnotationManager?.let {
            mapView.annotations.removeAnnotationManager( it )
        }

        return mapView.annotations.createPolylineAnnotationManager()
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
        var _instance: MapboxManager? = null

        fun instance( context: Context ) : MapboxManager
        {
            if (_instance == null)
            {
                _instance = MapboxManager( context )
            }

            return _instance!!
        }

        fun centerMap(enumArea: EnumArea, zoomLevel: Double?, mapboxMap: MapboxMap)
        {
            val latLngBounds = GeoUtils.findGeobounds(enumArea.vertices)
            val point = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )
            val cameraPosition = CameraOptions.Builder()
                .zoom(zoomLevel)
                .center(point)
                .build()

            mapboxMap.setCamera(cameraPosition)
        }
    }
}

