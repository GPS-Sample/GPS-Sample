/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.utils

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mapbox.geojson.Point
import edu.gtri.gpssample.database.models.LatLon
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
}