package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Connector
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FilterRule(
    var id : Int? = null,
    var order : Int,
    var rule : Rule?,
    var connector: Connector)
{
    constructor(order : Int) : this(null, order,null, Connector.None)
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