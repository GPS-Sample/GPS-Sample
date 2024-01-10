package edu.gtri.gpssample.database.models

import android.util.Log
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.utils.EncryptionUtil
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
    var samplingMethod: SamplingMethod,
    var sampleSize: Int,
    var sampleType : SampleType,
    var fields : ArrayList<Field>,
    var rules : ArrayList<Rule>,
    var filters : ArrayList<Filter>,
    var collectionTeams: ArrayList<CollectionTeam>,
    var selectedCollectionTeamId: Int
)
{
    constructor(name: String, samplingMethod: SamplingMethod, sampleSize: Int, sampleType: SampleType)
            : this(null, Date().time, name, samplingMethod, sampleSize, sampleType, ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>(), ArrayList<CollectionTeam>(), -1)

    constructor(id: Int, creationDate: Long, name: String, samplingMethod: SamplingMethod,
                sampleSize: Int, sampleType: SampleType, selectedCollectionTeamId: Int )
            : this(id, creationDate, name, samplingMethod, sampleSize, sampleType, ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>(),
        ArrayList<CollectionTeam>(), selectedCollectionTeamId)

    fun pack() : String
    {
        val jsonString = Json.encodeToString( this )
        return  EncryptionUtil.Encrypt(jsonString)
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
        fun unpack( message: String ) : Study?
        {
            try
            {
                val decrypted = EncryptionUtil.Decrypt(message)
                decrypted?.let {decrypted ->
                    return Json.decodeFromString<Study>( decrypted )
                }
            }
            catch (ex: Exception)
            {
                Log.d( "xxXXx", ex.stackTrace.toString())
            }
            return null
        }
    }
}