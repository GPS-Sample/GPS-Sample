package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class EnumArea (
    var id : Int? = null,
    var creationDate: Long,
    var config_id: Int,
    var name: String,
    var vertices: ArrayList<LatLon>,
    var enumerationTeams: ArrayList<Team>,
    var collectionTeams: ArrayList<Team>,
    var locations: ArrayList<Location>)
{
    constructor(id: Int, creationDate: Long, config_id: Int, name: String) : this(id, creationDate, config_id, name, ArrayList<LatLon>(), ArrayList<Team>(), ArrayList<Team>(), ArrayList<Location>())
    constructor(config_id: Int, name: String, vertices: ArrayList<LatLon>) : this(null, Date().time, config_id, name, vertices, ArrayList<Team>(), ArrayList<Team>(), ArrayList<Location>())

    fun copy() : EnumArea?
    {
        val _copy = unpack(pack())

        _copy?.let { _copy ->
            return _copy
        } ?: return null
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : EnumArea?
        {
            try
            {
                return Json.decodeFromString<EnumArea>( string )
            }
            catch (e: Exception) {}

            return null;
        }
    }
}