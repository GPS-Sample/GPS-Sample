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
    var vertices: ArrayList<LatLon>)
//  var enumDataList: ArrayList<EnumData>
{
    constructor(id: Int, config_id: Int, name: String) : this(id, config_id, name, ArrayList<LatLon>())
    constructor(config_id: Int, name: String, vertices: ArrayList<LatLon>) : this(null, config_id, name, vertices)

    fun copy() : EnumArea
    {
        return unpack(pack())
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : EnumArea
        {
            return Json.decodeFromString<EnumArea>( string )
        }
    }
}