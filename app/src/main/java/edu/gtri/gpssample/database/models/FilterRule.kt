package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FilterRule(
    var id : Int? = null,
    var uuid: String,
    var study_id: Int,
    var filter_uuid: String,
    var rule_uuid: String,
    var connector: String)
{
    constructor(uuid: String, study_id: Int, filter_uuid: String,
                rule_uuid: String, connector: String) : this(null, uuid, study_id, filter_uuid,
                rule_uuid, connector)
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