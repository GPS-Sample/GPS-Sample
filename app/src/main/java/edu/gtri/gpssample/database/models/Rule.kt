package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Rule(
    var id : Int? = null,
    var uuid: String,
    var study_uuid: String,
    var field_uuid: String,
    var name: String,
    var operator: String,
    var value: String)
{
    constructor(uuid: String, study_uuid: String, field_uuid: String, name: String, operator: String,
                value: String) : this(null, uuid, study_uuid, field_uuid, name, operator, value)
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Rule
        {
            return Json.decodeFromString<Rule>( message )
        }
    }
}