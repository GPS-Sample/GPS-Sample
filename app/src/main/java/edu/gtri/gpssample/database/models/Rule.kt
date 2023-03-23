package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Operator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Rule(
    var id : Int? = null,
    var uuid: String,

    // TODO: these aren't really necessary
    var study_id: Int?,
    var field_id: Int?,

    var name: String,
    var operator: Operator,
    var value: String)
{
    constructor(uuid: String, study_id: Int?, field_id: Int?, name: String, operator: Operator,
                value: String) : this(null, uuid, study_id, field_id, name, operator, value)
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