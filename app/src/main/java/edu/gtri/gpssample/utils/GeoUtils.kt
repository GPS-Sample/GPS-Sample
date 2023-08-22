package edu.gtri.gpssample.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.LatLon
import kotlin.math.*

const val kMinimumDistance = 20.0
data class HaversineCheck(val distance : Double, var withinBounds : Boolean, val start : LatLng, val end : LatLng)
{
    val minimumDistance = kMinimumDistance
}

object GeoUtils {
    private const val earthRadius =  6371009.0 //6378100
    private const val degreeConversion = PI / 180.0//0.017453292519943

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

    fun isCloseTo( latLng1: LatLng, latLng2: LatLng): HaversineCheck
    {


        val lat1Rad = latLng1.latitude * degreeConversion
        val lat2Rad = latLng2.latitude * degreeConversion
        val lon1Rad = latLng1.longitude * degreeConversion
        val lon2Rad = latLng2.longitude * degreeConversion

        val sinDLat : Double = sin((lat2Rad - lat1Rad) / 2.0)
        val sinDLon : Double = sin((lon2Rad - lon1Rad) / 2.0)

        val a : Double = (sinDLat * sinDLat)  + (cos(lat1Rad) * cos(lat2Rad) * (sinDLon * sinDLon))

        val ssrt : Double = asin(sqrt(a))
        val distance : Double = 2.0 * earthRadius * ssrt
        val haversineCheck = HaversineCheck(distance, false, latLng1, latLng2)
        if(distance < kMinimumDistance)
        {
            haversineCheck.withinBounds = true
        }
        return haversineCheck
    }
}