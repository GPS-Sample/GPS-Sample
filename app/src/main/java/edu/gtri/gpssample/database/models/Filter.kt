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
    var study_id : Int,
    var name: String,
    var samplingType : SampleType,
    var sampleSize: Int,
    var filterRules : ArrayList<FilterRule>)
{
    constructor(uuid: String, study_id: Int, name: String, sampleSize: Int,
                sampleSizeIndex: Int) : this(null, uuid, study_id, name, SampleType.None, sampleSize,
                ArrayList<FilterRule>())
    constructor(id: Int, uuid: String, study_id: Int, name: String, samplingType : SampleType, sampleSize: Int,
                sampleSizeIndex: Int) : this(id, uuid, study_id, name, samplingType,sampleSize,
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