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
    var collectionTeams: ArrayList<Team>,
    var locations : ArrayList<Location>

) : GeoArea()
{

    constructor(enumArea: EnumArea) : this(null, Date().time, ArrayList<LatLon>(), ArrayList<Team>(), ArrayList<Location>()) {
        this.vertices.addAll(enumArea.vertices)
        // build locations into smaple locations
        for(enumLoc in enumArea.locations)
        {
            val location = Location(LocationType.Sample, enumLoc.latitude,enumLoc.longitude,enumLoc.isLandmark)
            location.creationDate = Date().time
            for(item in enumLoc.items)
            {
                val enumItem = item as EnumerationItem
                val sampledItem = SampledItem(enumItem)
                location.items.add(sampledItem)
            }
            this.locations.add(location)
        }
    }
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