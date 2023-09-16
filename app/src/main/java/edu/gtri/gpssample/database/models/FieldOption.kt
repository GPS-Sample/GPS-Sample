package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FieldOption(
    var id : Int? = null,
    var name: String )
{
    constructor(name: String) : this( null, name )

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : FieldOption
        {
            return Json.decodeFromString<FieldOption>( string )
        }
    }
}