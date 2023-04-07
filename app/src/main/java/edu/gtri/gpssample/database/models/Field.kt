package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Field(
    var id : Int? = null,
    var uuid: String,
    var name: String,
    var type: FieldType,
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
    constructor(uuid: String, name: String, type: FieldType, pii: Boolean, required: Boolean,
                integerOnly: Boolean, date: Boolean, time: Boolean, option1: String, option2: String,
                option3: String, option4: String ) : this(null, uuid, name, type,
                pii, required, integerOnly, date, time, option1, option2, option3, option4)

    fun copy() : Field
    {
        return Field( uuid, name, type, pii, required, integerOnly, date, time, option1, option2, option3, option4 )
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    override fun toString(): String {
        return name
    }
    companion object
    {
        fun unpack( message: String ) : Field
        {
            return Json.decodeFromString<Field>( message )
        }
    }
}