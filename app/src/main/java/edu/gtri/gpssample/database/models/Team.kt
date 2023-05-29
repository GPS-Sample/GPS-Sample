package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@kotlinx.serialization.Serializable
data class Team(
    var id : Int? = null,
    var creationDate: Long,
    var studyId: Int,
    var enumAreaId: Int,
    var name: String)
{
    constructor(studyId: Int, enumAreaId: Int, name: String) : this(null, Date().time, studyId, enumAreaId, name)

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Team
        {
            return Json.decodeFromString<Team>( message )
        }
    }
}