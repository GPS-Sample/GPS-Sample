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
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.LatLon
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import java.util.ArrayList
import kotlin.math.*
import android.location.Location
import kotlin.div
import kotlin.random.Random
import kotlin.text.compareTo
import kotlin.times
import kotlin.unaryMinus

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

    fun createValidPolygon( coordinates: Array<Coordinate> ) : Polygon?
    {
        val polygon = GeometryFactory().createPolygon( coordinates )

        if (!polygon.isValid())
        {
            val g = polygon.buffer(0.0)

            if (g is Polygon && g.isValid)
            {
                return g
            }
            else
            {
                return null
            }
        }

        return polygon
    }

    fun createValidPolygon( geometry: Geometry ) : Polygon?
    {
        if (geometry is Polygon)
        {
            if (geometry.isValid)
            {
                return geometry
            }
            else
            {
                val g = geometry.buffer(0.0)

                if (g is Polygon && g.isValid)
                {
                    return g
                }
                else
                {
                    return null
                }
            }
        }

        return null
    }

    fun distance( point: Point, enumArea: EnumArea ) : Double
    {
        // convert point and enumArea from degrees to meters from the centroid
        // so that the distance will be computed in meters

        val latLngBounds = findGeobounds(enumArea.vertices)
        val center = com.mapbox.geojson.Point.fromLngLat( latLngBounds.center.longitude, latLngBounds.center.latitude )

        val refLat = center.latitude()
        val refLon = center.longitude()

        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLon = metersPerDegreeLat * cos(Math.toRadians(refLat))

        val xyPoints = ArrayList<Coordinate>()

        for (vertice in enumArea.vertices)
        {
            val x = (vertice.longitude - refLon) * metersPerDegreeLon
            val y = (vertice.latitude - refLat) * metersPerDegreeLat
            xyPoints.add(Coordinate(x,y,0.0))
        }

        // close the polygon
        xyPoints.add( xyPoints.first())

        val geometryFactory = GeometryFactory()
        val xyPolygon = geometryFactory.createPolygon( xyPoints.toTypedArray())

        val x = (point.longitude() - refLon) * metersPerDegreeLon
        val y = (point.latitude() - refLat) * metersPerDegreeLat
        val xyPoint = geometryFactory.createPoint(Coordinate(x,y,0.0))

        if (xyPolygon.contains( xyPoint ))
        {
            return 0.0
        }
        else
        {
            return xyPolygon.distance(xyPoint )
        }
    }

    private data class XYPoint(val x: Double, val y: Double)

    class RandomLocationGenerator(
        private val center: com.mapbox.geojson.Point,
        private val widthMeters: Double,
        private val heightMeters: Double,
        private val minDistanceMeters: Double = 10.0
    ) {
        private data class Cell(val x: Int, val y: Int)
        private val cellSize = minDistanceMeters
        private val minDistanceSquared = minDistanceMeters * minDistanceMeters

        fun generate(count: Int): List<com.mapbox.geojson.Point>
        {
            val grid = HashMap<Cell, MutableList<XYPoint>>()
            val accepted = ArrayList<XYPoint>(count)

            var attempts = 0
            val maxAttempts = count * 100

            while (accepted.size < count)
            {
                if (++attempts > maxAttempts)
                {
                    break
                }

                val candidate = XYPoint(
                    x = Random.nextDouble(-widthMeters / 2, widthMeters / 2),
                    y = Random.nextDouble(-heightMeters / 2, heightMeters / 2)
                )

                val cell = Cell(
                    floor(candidate.x / cellSize).toInt(),
                    floor(candidate.y / cellSize).toInt()
                )

                var valid = true

                outer@ for (dx in -1..1)
                {
                    for (dy in -1..1)
                    {
                        val neighbors = grid[Cell(cell.x + dx, cell.y + dy)] ?: continue

                        for (p in neighbors)
                        {
                            val x = candidate.x - p.x
                            val y = candidate.y - p.y

                            if (x * x + y * y < minDistanceSquared)
                            {
                                valid = false
                                break@outer
                            }
                        }
                    }
                }

                if (valid)
                {
                    accepted.add(candidate)
                    grid.getOrPut(cell) { ArrayList() }.add(candidate)
                }
            }

            return accepted.map(::toPoint)
        }

        /**
         * Converts local meter coordinates to WGS84.
         * Accurate to much better than 1 meter over a 10 km area.
         */
        private fun toPoint(point: XYPoint): com.mapbox.geojson.Point
        {
            val lat = center.latitude() + point.y / 111320.0
            val lon = center.longitude() + point.x / (111320.0 * cos(Math.toRadians(center.latitude())))

            return com.mapbox.geojson.Point.fromLngLat(lon, lat )
        }
    }

    class RandomLocationGenerator2( polygon: List<LatLon>, private val minDistanceMeters: Double = 10.0 ) {
        private data class Cell(val x: Int, val y: Int)
        private val cellSize = minDistanceMeters
        private val minDistanceSquared = minDistanceMeters * minDistanceMeters
        private data class XYPoint(val x: Double, val y: Double)
        private val origin = Point.fromLngLat(polygon.first().longitude, polygon.first().latitude)
        private val polygonXY = polygon.map(::toXY)
        val minX = polygonXY.minOf { it.x }
        val maxX = polygonXY.maxOf { it.x }
        val minY = polygonXY.minOf { it.y }
        val maxY = polygonXY.maxOf { it.y }

        fun generate( count: Int, reportProgress: (point: Point, count: Int)->Unit ): List<com.mapbox.geojson.Point>
        {
            val grid = HashMap<Cell, MutableList<XYPoint>>()
            val accepted = ArrayList<XYPoint>(count)

            var attempts = 0
            val maxAttempts = count * 100

            while (accepted.size < count)
            {
                if (++attempts > maxAttempts)
                {
                    break
                }

                val candidate = XYPoint(Random.nextDouble(minX, maxX), Random.nextDouble(minY, maxY))

                if (!insidePolygon(candidate, polygonXY))
                    continue

                val cell = Cell(floor(candidate.x / cellSize).toInt(), floor(candidate.y / cellSize).toInt())

                var valid = true

                outer@ for (dx in -1..1)
                {
                    for (dy in -1..1)
                    {
                        val neighbors = grid[Cell(cell.x + dx, cell.y + dy)] ?: continue

                        for (p in neighbors)
                        {
                            val x = candidate.x - p.x
                            val y = candidate.y - p.y

                            if (x * x + y * y < minDistanceSquared)
                            {
                                valid = false
                                break@outer
                            }
                        }
                    }
                }

                if (valid)
                {
                    accepted.add( candidate )
                    grid.getOrPut(cell) { ArrayList() }.add( candidate )
                    reportProgress( toPoint(candidate ), accepted.size )
                }
            }

            return accepted.map(::toPoint)
        }

        private fun toXY(latLon: LatLon): XYPoint
        {
            val point = Point.fromLngLat( latLon.longitude, latLon.latitude )
            val lat0 = Math.toRadians(origin.latitude())
            val x = (point.longitude() - origin.longitude()) * 111320.0 * cos(lat0)
            val y = (point.latitude() - origin.latitude()) * 110540.0
            return XYPoint(x, y)
        }

        private fun toPoint(xy: XYPoint): Point
        {
            val lat0 = Math.toRadians(origin.latitude())
            return Point.fromLngLat(origin.longitude() + xy.x / (111320.0 * cos(lat0)), origin.latitude() + xy.y / 110540.0)
        }

        private fun insidePolygon( point: XYPoint, polygon: List<XYPoint> ): Boolean
        {
            var inside = false
            var j = polygon.lastIndex

            for (i in polygon.indices)
            {
                val pi = polygon[i]
                val pj = polygon[j]

                if ((pi.y > point.y) != (pj.y > point.y) && point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x)
                {
                    inside = !inside
                }

                j = i
            }

            return inside
        }
    }
}