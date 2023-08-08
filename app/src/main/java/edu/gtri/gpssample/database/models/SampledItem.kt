package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SamplingState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SampledItem (
    override var id: Int?,
    var enumItem : EnumerationItem,
    var samplingState : SamplingState = SamplingState.NotSampled
     ): GeoItem
{
    constructor(enumItem : EnumerationItem) :
            this(null, enumItem, SamplingState.None)

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