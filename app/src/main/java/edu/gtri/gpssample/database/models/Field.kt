package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// NOTE!!
// This class defines the Field questions and is a part of the study

@Serializable
data class Field(
    var id : Int? = null,
    var name: String,
    var type: FieldType,
    var fieldBlockContainer: Boolean,
    var fieldBlockUUID: String?,
    var pii: Boolean,
    var required: Boolean,
    var integerOnly: Boolean,
    var date: Boolean,
    var time: Boolean,
    var fieldOptions: ArrayList<FieldOption>,
    var option1: String,
    var option2: String,
    var option3: String,
    var option4: String )
{
    constructor(name: String, type: FieldType, pii: Boolean, required: Boolean, integerOnly: Boolean, date: Boolean, time: Boolean,
                option1: String, option2: String, option3: String, option4: String )
            : this(null,  name, type, false, null, pii, required, integerOnly, date, time, ArrayList<FieldOption>(),
                option1, option2, option3, option4)

    fun copy() : Field
    {
        return unpack(pack())
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

//    override fun toString(): String {
//        return name
//    }

    companion object
    {
        fun unpack( string: String ) : Field
        {
            return Json.decodeFromString<Field>( string )
        }
    }
}