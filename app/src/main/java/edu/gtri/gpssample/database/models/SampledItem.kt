package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SamplingState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SampledItem(
    var id : Int? = null,
    var enumerationItem: EnumerationItem,
    var samplingState : SamplingState = SamplingState.NotSampled
     )
{
    constructor(  enumerationItem: EnumerationItem ) :
            this( null, enumerationItem, SamplingState.NotSampled)


    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : SampledItem
        {
            return Json.decodeFromString<SampledItem>( string )
        }
    }
}