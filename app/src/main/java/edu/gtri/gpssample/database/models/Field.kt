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
    var fieldOptions: ArrayList<FieldOption>)
{
    constructor(name: String, type: FieldType, pii: Boolean, required: Boolean, integerOnly: Boolean, date: Boolean, time: Boolean)
            : this(null,  name, type, false, null, pii, required, integerOnly, date, time, ArrayList<FieldOption>())

    fun copy() : Field
    {
        return unpack(pack())
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : Field
        {
            return Json.decodeFromString<Field>( string )
        }
    }
}