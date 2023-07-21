package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SampleArea(
    override var id : Int? = null,
    var creationDate: Long,
    var vertices : ArrayList<LatLon>,
    var collectionTeams: ArrayList<Team>,
    var locations : ArrayList<Location>

) : GeoArea()
{


    fun copy() : SampleArea?
    {
        val _copy = SampleArea.unpack(pack())

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
        fun unpack( string: String ) : SampleArea
        {
            return Json.decodeFromString<SampleArea>( string )
        }
    }
}