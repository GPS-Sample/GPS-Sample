package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EnumArea (
    var id : Int? = null,
    var config_id: Int,
    var name: String,
    var vertices: ArrayList<LatLon>,
    var teams: ArrayList<Team>,
    var enumDataList: ArrayList<EnumData>)
{
    constructor(id: Int, config_id: Int, name: String) : this(id, config_id, name, ArrayList<LatLon>(), ArrayList<Team>(), ArrayList<EnumData>())
    constructor(config_id: Int, name: String, vertices: ArrayList<LatLon>) : this(null, config_id, name, vertices, ArrayList<Team>(), ArrayList<EnumData>())

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