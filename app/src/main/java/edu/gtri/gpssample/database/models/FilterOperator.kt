package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Connector
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FilterOperator (
    var id : Int? = null,
    var conenctor : Connector,
    var rule: Rule?
    ){
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