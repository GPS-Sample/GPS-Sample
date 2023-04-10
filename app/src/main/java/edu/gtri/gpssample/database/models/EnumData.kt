package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable

@Serializable
data class EnumData(
    var id : Int? = null,
    var userId : Int,
    var studyId : Int,
    var latitude : Double,
    var longitude : Double
)
{
    constructor( userId: Int, studyId: Int, latitude: Double, longitude: Double) :
            this( null, userId, studyId, latitude, longitude )
}