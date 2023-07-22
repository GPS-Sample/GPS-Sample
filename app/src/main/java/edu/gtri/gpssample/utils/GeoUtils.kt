package edu.gtri.gpssample.utils

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import edu.gtri.gpssample.database.models.LatLon

object GeoUtils {
    fun findGeobounds(points : ArrayList<LatLon>) : LatLngBounds
    {
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
    }
}