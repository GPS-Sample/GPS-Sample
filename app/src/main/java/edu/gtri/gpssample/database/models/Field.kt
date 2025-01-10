package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

// NOTE!!
// This class defines the Field questions and is a part of the study

@Serializable
data class Field(
    var uuid: String,
    var creationDate: Long,
    var parentUUID: String?,
    var index: Int,
    var name: String,
    var type: FieldType,
    var pii: Boolean,
    var required: Boolean,
    var integerOnly: Boolean,
    var numberOfResidents: Boolean,
    var date: Boolean,
    var time: Boolean,
    var minimum: Double?,
    var maximum: Double?,
    var fieldOptions: ArrayList<FieldOption>,
    var fields: ArrayList<Field>?)
{
    constructor(parentUUID: String?, index: Int, name: String, type: FieldType, pii: Boolean, required: Boolean, integerOnly: Boolean, numberOfResidents: Boolean, date: Boolean, time: Boolean, minimum: Double?, maximum: Double?)
            : this(UUID.randomUUID().toString(), Date().time, parentUUID, index, name, type, pii, required, integerOnly, numberOfResidents, date, time, null, null, ArrayList<FieldOption>(), null)

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