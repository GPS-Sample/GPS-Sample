package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SampleState
import edu.gtri.gpssample.constants.SamplingState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class EnumerationItem(
    override var id : Int? = null,
    var creationDate: Long,
    var uuid : String,
    var locationId : Int,
    var collectionItemId: Int,
    var subAddress : String,

    var enumerationState : EnumerationState,
    var incompleteReason : String,
    var notes : String,
    var fieldDataList : ArrayList<FieldData> ) : GeoItem()
{
    constructor( locationId: Int ) :
            this(null,  Date().time, UUID.randomUUID().toString(), locationId, -1,
                "", EnumerationState.Undefined, "", "",
                ArrayList<FieldData>())

    constructor(id: Int, locationId: Int, subAddress: String, samplingState: SamplingState,
                 enumerationState: EnumerationState, incompleteReason: String, notes: String ) :
            this( id,  Date().time, UUID.randomUUID().toString(), locationId, -1,
                subAddress, enumerationState, incompleteReason, notes,
                ArrayList<FieldData>())

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