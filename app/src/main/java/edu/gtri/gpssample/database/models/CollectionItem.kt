package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.CollectionState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class CollectionItem(
    var id : Int? = null,
    var creationDate: Long,
    var uuid : String,
    var enumerationItemId : Int,
    var state : CollectionState,
    var incompleteReason : String,
    var notes : String)
{
    constructor( enumerationItemId: Int, state: CollectionState, incompleteReason: String, notes: String) :
            this( null, Date().time, UUID.randomUUID().toString(), enumerationItemId, state, incompleteReason, notes )

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : CollectionItem
        {
            return Json.decodeFromString<CollectionItem>( string )
        }
    }
}