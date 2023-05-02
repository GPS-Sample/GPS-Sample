package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EnumData(
    var id : Int? = null,
    var userId : Int,
    var enumAreaId : Int,
    var latitude : Double,
    var longitude : Double,
    var isLocation: Boolean,
    var description : String,
    var imageFileName: String,
    var fieldDataList : ArrayList<FieldData>?)
{
    constructor( userId: Int, enumAreaId: Int, latitude: Double, longitude: Double) :
            this( null, userId, enumAreaId, latitude, longitude, false,"", "", null)

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : EnumData
        {
            return Json.decodeFromString<EnumData>( string )
        }
    }
}