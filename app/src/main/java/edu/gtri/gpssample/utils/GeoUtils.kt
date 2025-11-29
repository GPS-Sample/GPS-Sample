/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import edu.gtri.gpssample.database.models.LatLon
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.util.ArrayList
import kotlin.math.*

data class HaversineCheck(val distance : Double, var withinBounds : Boolean, val start : LatLng, val end : LatLng)
{
}

object GeoUtils {
    private const val earthRadius =  6371009.0
    private const val degreeConversion = PI / 180.0

    fun findGeobounds(points : List<Point>) : LatLngBounds
    {
        val latLons = ArrayList<LatLon>()

        for (point in points)
        {
            latLons.add( LatLon( 0, point.latitude(), point.longitude()))
        }

        return findGeobounds( latLons )
    }

    fun findGeobounds(points : ArrayList<LatLon>) : LatLngBounds
    {
        try{
            var minLat : Double = 99999999.0
            var maxLat : Double = -99999999.0
            var minLon : Double = 99999999.0
            var maxLon : Double = -99999999.0

            for (i in 0 until points.size)
            {
                val pos = points[i].toLatLng()
                minLat =  if (pos.latitude < minLat) pos.latitude else  minLat
                minLon =  if (pos.longitude < minLon) pos.longitude else  minLon
                maxLat =  if (pos.latitude > maxLat) pos.latitude else  maxLat
                maxLon =  if (pos.longitude > maxLon) pos.longitude else maxLon
            }
            return LatLngBounds(LatLng(minLat, minLon), LatLng(maxLat,maxLon))
        }catch (ex : Exception)
        {

        }
        return LatLngBounds(LatLng(0.0,0.0), LatLng(0.0,0.0))
    }

    fun isCloseTo( latLng1: LatLng, latLng2: LatLng, minimumDistance: Int): HaversineCheck
    {
        val distance = distanceBetween( latLng1, latLng2 )

        val haversineCheck = HaversineCheck(distance, false, latLng1, latLng2)

        if(distance < minimumDistance)
        {
            haversineCheck.withinBounds = true
        }

        return haversineCheck
    }

    fun distanceBetween( latLng1: LatLng, latLng2: LatLng ) : Double
    {
        val lat1Rad = latLng1.latitude * degreeConversion
        val lat2Rad = latLng2.latitude * degreeConversion
        val lon1Rad = latLng1.longitude * degreeConversion
        val lon2Rad = latLng2.longitude * degreeConversion

        val sinDLat : Double = sin((lat2Rad - lat1Rad) / 2.0)
        val sinDLon : Double = sin((lon2Rad - lon1Rad) / 2.0)

        val a : Double = (sinDLat * sinDLat)  + (cos(lat1Rad) * cos(lat2Rad) * (sinDLon * sinDLon))

        val ssrt : Double = asin(sqrt(a))

        return 2.0 * earthRadius * ssrt
    }

    fun isSelfIntersectingPolygon( coordinates: java.util.ArrayList<Coordinate> ) : Boolean
    {
        try
        {
            val last = coordinates.size - 1

            // close the polygon, if necc...
            if (coordinates[0].x != coordinates[last].x || coordinates[0].y != coordinates[last].y)
            {
                coordinates.add( coordinates[0] )
            }

            return !GeometryFactory().createPolygon(coordinates.toTypedArray()).isSimple
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            return true
        }
    }

    fun isSelfIntersectingPolygon1( polylinePoints: java.util.ArrayList<Point> ) : Boolean
    {
        val coordinates = java.util.ArrayList<Coordinate>()

        polylinePoints.map { point ->
            coordinates.add( Coordinate( point.longitude(), point.latitude()))
        }

        return isSelfIntersectingPolygon( coordinates )
    }

    fun isSelfIntersectingPolygon2( pointAnnotations: java.util.ArrayList<PointAnnotation?>) : Boolean
    {
        val coordinates = java.util.ArrayList<Coordinate>()

        pointAnnotations.map { pointAnnotation ->
            pointAnnotation?.let{ pointAnnotation ->
                coordinates.add( Coordinate( pointAnnotation.point.longitude(), pointAnnotation.point.latitude()))
            }
        }

        coordinates.add( coordinates[0] )

        return isSelfIntersectingPolygon( coordinates )
    }

    fun isSelfIntersectingPolygon3( latLons: java.util.ArrayList<LatLon> ) : Boolean
    {
        val coordinates = ArrayList<Coordinate>()
        for (latLon in latLons)
        {
            coordinates.add( Coordinate( latLon.longitude, latLon.latitude ))
        }

        return isSelfIntersectingPolygon( coordinates )
    }

    fun ArrayListOfLatLonToArrayListOfCoordinate( latLons: kotlin.collections.ArrayList<LatLon>) : kotlin.collections.ArrayList<Coordinate>
    {
        val coordinates = ArrayList<Coordinate>()

        latLons.map { latLon ->
            coordinates.add( Coordinate( latLon.longitude, latLon.latitude ))
        }

        return coordinates
    }

    fun ArrayListOfPointToArrayListOfCoordinate( points: kotlin.collections.ArrayList<Point>) : kotlin.collections.ArrayList<Coordinate>
    {
        val coordinates = ArrayList<Coordinate>()

        points.map { point ->
            coordinates.add( Coordinate( point.longitude(), point.latitude()))
        }

        return coordinates
    }

    fun ArrayListOfCoordinateToArrayListOfPoint( coordinates: Array<Coordinate> ) : kotlin.collections.ArrayList<Point>
    {
        val points = ArrayList<Point>()

        coordinates.map {
            points.add( Point.fromLngLat(it.x, it.y))
        }

        return points
    }
}