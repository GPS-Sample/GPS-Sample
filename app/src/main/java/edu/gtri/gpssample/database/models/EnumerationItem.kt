package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class EnumerationItem(
    var id : Int? = null,
    var creationDate: Long,
    var uuid : String,
    var locationId : Int,
    var collectionItemId: Int,
    var subAddress : String,
    var valid : Boolean,
    var incompleteReason : String,
    var notes : String,
    var fieldDataList : ArrayList<FieldData>?)
{
    constructor( locationId: Int ) :
            this( null, Date().time, UUID.randomUUID().toString(), locationId, -1, "", false, "", "", ArrayList<FieldData>())

    constructor( locationId: Int, subAddress: String, valid: Boolean, incompleteReason: String, notes: String ) :
            this( null, Date().time, UUID.randomUUID().toString(), locationId, -1, subAddress, valid, incompleteReason, notes, ArrayList<FieldData>())

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : EnumerationItem
        {
            return Json.decodeFromString<EnumerationItem>( string )
        }
    }
}