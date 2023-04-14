package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
data class EnumData(
    var id : Int? = null,
    var userId : Int,
    var enumAreaId : Int,
    var latitude : Double,
    var longitude : Double
)
{
    constructor( userId: Int, enumAreaId: Int, latitude: Double, longitude: Double) :
            this( null, userId, enumAreaId, latitude, longitude )
}