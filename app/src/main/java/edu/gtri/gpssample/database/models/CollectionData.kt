package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class CollectionData(
    var id : Int? = null,
    var creationDate: Long,
    var enumDataId : Int,
    var incomplete : Boolean,
    var incompleteReason : String,
    var notes : String)
{
    constructor( enumDataId: Int, incomplete: Boolean, incompleteReason: String, notes: String ) :
            this( null, Date().time, enumDataId, incomplete, incompleteReason, notes)

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