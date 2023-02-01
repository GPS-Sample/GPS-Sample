package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.FieldType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Field(
    var id: Int,
    var studyId: Int,
    var name: String,
    var type: String,
    var pii: Boolean,
    var required: Boolean,
    var integerOnly: Boolean,
    var date: Boolean,
    var time: Boolean,
    var option1: String,
    var option2: String,
    var option3: String,
    var option4: String )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( json: String ) : Field
        {
            return Json.decodeFromString<Field>( json )
        }
    }
}