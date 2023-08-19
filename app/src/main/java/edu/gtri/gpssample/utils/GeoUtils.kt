package edu.gtri.gpssample.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import edu.gtri.gpssample.database.models.LatLon
import java.lang.Math.*

object GeoUtils {
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

    fun isEqual( latLng1: LatLng, latLng2: LatLng): Boolean
    {
        val deltaLat = abs( latLng1.latitude - latLng2.latitude )
        val deltaLon = abs( latLng1.longitude - latLng2.longitude )

        if (deltaLat > 1.0e-4 || deltaLon > 1.0e-4)
        {
            return false
        }

        return true
    }
}