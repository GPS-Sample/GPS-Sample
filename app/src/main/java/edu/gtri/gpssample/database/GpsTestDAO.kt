package edu.gtri.gpssample.database

import android.content.ContentValues
import android.util.Log
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study

class GpsTestDAO(private var dao: DAO) {
    fun addFieldData(lat1: Double, lon1: Double, lat2: Double, lon2: Double, distance: Double)
    {

            val values = ContentValues()
            putField( lat1,lon1,lat2,lon2,distance,values )
            dao.writableDatabase.insert("gps_test", null, values)




    }


    fun putField( lat1 : Double, lon1 : Double, lat2 : Double, lon2 : Double,
                  distance : Double, values: ContentValues )
    {

        values.put( "distance", distance )
        values.put( "lat1", lat1 )
        values.put( "lon1", lon1 )
        values.put( "lat2", lat2 )
        values.put( "lon2", lon2 )
    }
}