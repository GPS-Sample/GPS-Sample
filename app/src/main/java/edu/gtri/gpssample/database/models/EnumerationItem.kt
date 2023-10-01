package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
@SerialName("EnumerationItem")
data class EnumerationItem(
    var id : Int? = null,
    var creationDate: Long,
    var uuid : String,
    var subAddress : String,
    var enumerationState : EnumerationState,
    var samplingState : SamplingState,
    var collectionState : CollectionState,
    var incompleteReason : String,
    var notes : String,
    var fieldDataList : ArrayList<FieldData>,
    var locationId : Int )
{
    constructor() : this(null,  Date().time, UUID.randomUUID().toString(), "", EnumerationState.Undefined, SamplingState.NotSampled, CollectionState.Incomplete, "", "", ArrayList<FieldData>(), -1)

//    constructor(id: Int, subAddress: String, enumerationState: EnumerationState, samplingState: SamplingState, collectionState: CollectionState, incompleteReason: String, notes: String, location: Location ) :
//            this( id,  Date().time, UUID.randomUUID().toString(), subAddress, enumerationState, samplingState, collectionState, incompleteReason, notes, ArrayList<FieldData>(), location )

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