package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SampleType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Filter(
    var id : Int? = null,
    var uuid: String,
    var name: String,
    var samplingType : SampleType,
    var sampleSize: Int,
    var filterRules : ArrayList<FilterRule>)
{
    constructor(uuid: String,  name: String) :
                this(null, uuid, name, SampleType.None, 0, ArrayList<FilterRule>())
    constructor(id: Int, uuid: String, name: String, samplingType : SampleType, sampleSize: Int) :
            this(id, uuid, name, samplingType,sampleSize,
        ArrayList<FilterRule>())
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Filter
        {
            return Json.decodeFromString<Filter>( message )
        }
    }
}