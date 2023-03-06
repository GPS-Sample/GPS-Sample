package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Sample(
    var id : Int? = null,
    var uuid: String,
    var study_uuid: String,
    var name: String,
    var numEnumerators: Int)
{
    constructor(uuid: String, study_uuid: String, name: String, numEnumerators: Int) :
            this(null, uuid, study_uuid, name, numEnumerators)
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Sample
        {
            return Json.decodeFromString<Sample>( message )
        }
    }
}