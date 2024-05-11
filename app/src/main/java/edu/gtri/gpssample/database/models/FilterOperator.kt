package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.SampleType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class FilterOperator (
    var uuid : String,
    var order : Int,
    var connector : Connector,
    var rule: Rule?
    )
{
    constructor(order: Int, connector: Connector, rule: Rule) : this(UUID.randomUUID().toString(), order, connector, rule)

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