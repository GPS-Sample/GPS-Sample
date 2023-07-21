package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class EnumArea (
    override var id : Int? = null,
    var creationDate: Long,
    var name: String,
    var vertices: ArrayList<LatLon>,
    var enumerationTeams: ArrayList<Team>,
    var locations: ArrayList<Location>) : GeoArea()
{
    constructor(id: Int, creationDate: Long, name: String) : this(id, creationDate, name,
                ArrayList<LatLon>(), ArrayList<Team>(), ArrayList<Location>())
    constructor( name: String, vertices: ArrayList<LatLon>) : this(null,
                Date().time, name, vertices, ArrayList<Team>(), ArrayList<Location>())

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