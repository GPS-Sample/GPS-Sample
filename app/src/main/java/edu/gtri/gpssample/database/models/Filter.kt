package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SampleType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Filter(
    var id : Int? = null,
    var name: String,
    var samplingType : SampleType,
    var sampleSize: Int,
    var rule : Rule?)
    //var filterRules : ArrayList<FilterRule>)
{
    constructor(name: String) : this(null, name, SampleType.None, 0, null)//, ArrayList<FilterRule>())
    constructor(id: Int, name: String, samplingType : SampleType, sampleSize: Int) : this(id, name, samplingType, sampleSize, null) //, ArrayList<FilterRule>())
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