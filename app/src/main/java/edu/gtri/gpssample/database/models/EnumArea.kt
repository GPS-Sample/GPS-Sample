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
    var uuid : String,
    var creationDate: Long,
    var name: String,
    var vertices: ArrayList<LatLon>,
    var enumerationTeams: ArrayList<Team>,
    var locations: ArrayList<Location>,
    var selectedTeamId: Int ) : GeoArea()
{
    constructor(id: Int, creationDate: Long, name: String, selectedTeamId: Int) : this(id, UUID.randomUUID().toString(), creationDate, name,
                ArrayList<LatLon>(), ArrayList<Team>(), ArrayList<Location>(), selectedTeamId)
    constructor( name: String, vertices: ArrayList<LatLon>) : this(null,UUID.randomUUID().toString(),
                Date().time, name, vertices, ArrayList<Team>(), ArrayList<Location>(), -1)

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

    override fun equals(other: Any?): Boolean {
        if(other is EnumArea)
        {
            if (this.uuid == other.uuid)
            {
                return true
            }
        }
        return false
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