package edu.gtri.gpssample.network.models

import edu.gtri.gpssample.database.models.Filter
//import edu.gtri.gpssample.database.models.FilterRule
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

//@Serializable
//data class NetworkFilterRules( var filterRules: List<FilterRule> )
//{
//    fun pack() : String
//    {
//        return Json.encodeToString( this )
//    }
//
//    companion object
//    {
//        fun unpack( message: String ) : NetworkFilterRules
//        {
//            return Json.decodeFromString<NetworkFilterRules>( message )
//        }
//    }
//}