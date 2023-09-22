package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.LocationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class SampleArea(
    override var id : Int? = null,
    var creationDate: Long,
    var vertices : ArrayList<LatLon>,
    var collectionTeams: ArrayList<Team>
) : GeoArea()
{

    constructor( id: Int, creationDate: Long ) : this( id, creationDate, ArrayList<LatLon>(), ArrayList<Team>())

    constructor(enumArea: EnumArea) : this(null, Date().time, ArrayList<LatLon>(), ArrayList<Team>())
    {
        this.vertices.addAll(enumArea.vertices)
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