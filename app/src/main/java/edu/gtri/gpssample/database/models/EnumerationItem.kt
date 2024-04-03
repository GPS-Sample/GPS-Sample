package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
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
    var syncCode : Int,
    var distance : Double,
    var distanceUnits: String,
    var subAddress : String,
    var enumeratorName : String,
    var enumerationState : EnumerationState,
    var enumerationDate: Long,
    var enumerationIncompleteReason : String,
    var enumerationNotes : String,
    var enumerationEligibleForSampling : Boolean,
    var samplingState : SamplingState,
    var collectorName : String,
    var collectionState : CollectionState,
    var collectionDate: Long,
    var collectionIncompleteReason : String,
    var collectionNotes : String,
    var fieldDataList : ArrayList<FieldData>,
    var locationId : Int )
{
    constructor() : this(null,
        Date().time,
        UUID.randomUUID().toString(),
        0,
        0.0,
        "",
        "",
        "",
        EnumerationState.Undefined,
        0,
        "",
        "",
        false,
        SamplingState.None,
        "",
        CollectionState.Undefined,
        0,
        "",
        "",
        ArrayList<FieldData>(),
        -1)

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