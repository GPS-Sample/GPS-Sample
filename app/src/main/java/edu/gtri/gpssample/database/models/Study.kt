package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Study(
    var id : Int? = null,
    var uuid: String,
    var config_uuid: String,
    var name: String,
    var samplingMethod: SamplingMethod,
    var sampleSize: Int,
    var sampleType : SampleType,
    var fields : ArrayList<Field>,
    var rules : ArrayList<Rule>,
    var filters : ArrayList<Filter>)
{
    constructor(uuid: String, config_uuid: String, name: String, samplingMethod: SamplingMethod,
                sampleSize: Int, sampleType: SampleType) : this(null, uuid, config_uuid,
                name, samplingMethod, sampleSize, sampleType,
                ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>())
    constructor(id: Int, uuid: String, config_uuid: String, name: String, samplingMethod: SamplingMethod,
                sampleSize: Int, sampleType: SampleType) : this(id, uuid, config_uuid,
                name, samplingMethod, sampleSize, sampleType,
                ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>())
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    fun equals(compare : Study) : Boolean
    {

        // TODO: needs to check all fields, rules and filters
        if(this.uuid != compare.uuid || this.name != compare.name ||
            this.samplingMethod != compare.samplingMethod ||
            this.sampleSize != compare.sampleSize || this.sampleType != compare.sampleType)
        {
            return false
        }

        return true
    }

    companion object
    {
        fun unpack( message: String ) : Study
        {
            return Json.decodeFromString<Study>( message )
        }
    }
}