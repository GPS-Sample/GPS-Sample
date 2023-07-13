package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Operator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Rule(
    var id : Int? = null,
    var studyId : Int,
    var fieldId: Int,
    var name: String,
    var operator: Operator,
    var value: String)
{
    constructor(studyId: Int, fieldId: Int, name: String, operator: Operator, value: String)
            : this(null, studyId, fieldId, name, operator, value)

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