package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class EnumData(
    var id : Int? = null,
    var creationDate: Long,
    var uuid : String,
    var userId : Int,
    var enumAreaId : Int,
    var teamId : Int,
    var valid : Boolean,
    var incomplete : Boolean,
    var incompleteReason : String,
    var notes : String,
    var latitude : Double,
    var longitude : Double,
    var isLocation: Boolean,
    var description : String,
    var imageFileName: String,
    var fieldDataList : ArrayList<FieldData>?)
{
    constructor( userId: Int, enumAreaId: Int, valid: Boolean, incomplete: Boolean, reasonIncomplete: String, notes: String, latitude: Double, longitude: Double) :
            this( null, Date().time, UUID.randomUUID().toString(), userId, enumAreaId, 0, valid, incomplete, reasonIncomplete, notes, latitude, longitude, false,"", "", null)

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