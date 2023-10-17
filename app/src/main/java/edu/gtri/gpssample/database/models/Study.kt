package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class Study(
    var id : Int? = null,
    var creationDate: Long,
    var name: String,
    var totalPopulationSize: Int,
    var samplingMethod: SamplingMethod,
    var sampleSize: Int,
    var sampleType : SampleType,
    var fields : ArrayList<Field>,
    var rules : ArrayList<Rule>,
    var filters : ArrayList<Filter>,
    var sampleAreas : ArrayList<SampleArea>,
    var collectionTeams: ArrayList<CollectionTeam>,
    var selectedCollectionTeamId: Int
)
{
    constructor(name: String, samplingMethod: SamplingMethod, sampleSize: Int, sampleType: SampleType)
            : this(null, Date().time, name, 0, samplingMethod, sampleSize, sampleType, ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>(),
        ArrayList<SampleArea>(), ArrayList<CollectionTeam>(), -1)

    constructor(id: Int, creationDate: Long, name: String, totalPopulationSize: Int, samplingMethod: SamplingMethod,
                sampleSize: Int, sampleType: SampleType, selectedCollectionTeamId: Int )
            : this(id, creationDate, name, totalPopulationSize, samplingMethod, sampleSize, sampleType, ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>(),
        ArrayList<SampleArea>(), ArrayList<CollectionTeam>(), selectedCollectionTeamId)

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    fun equals(compare : Study) : Boolean
    {

        // TODO: needs to check all fields, rules and filters
        if(this.name != compare.name ||
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