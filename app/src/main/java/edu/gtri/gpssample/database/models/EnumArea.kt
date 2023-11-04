package edu.gtri.gpssample.database.models

import android.util.Log
import edu.gtri.gpssample.utils.EncryptionUtil
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
    var selectedEnumerationTeamId: Int,
    var vertices: ArrayList<LatLon>,
    var locations: ArrayList<Location>,
    var enumerationTeams: ArrayList<EnumerationTeam>) : GeoArea()
{
    constructor( id: Int, creationDate: Long, name: String, selectedEnumerationTeamId: Int)
            : this(id, UUID.randomUUID().toString(), creationDate, name, selectedEnumerationTeamId, ArrayList<LatLon>(), ArrayList<Location>(), ArrayList<EnumerationTeam>())

    constructor( name: String, vertices: ArrayList<LatLon>)
            : this(null, UUID.randomUUID().toString(), Date().time, name, -1, vertices, ArrayList<Location>(), ArrayList<EnumerationTeam>())

    fun copy() : EnumArea?
    {
        val _copy = unpack(pack())

        _copy?.let { _copy ->
            return _copy
        } ?: return null
    }

    fun pack() : String
    {
        val jsonString = Json.encodeToString( this )
        return EncryptionUtil.Encrypt(jsonString)
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
                val decrypted = EncryptionUtil.Decrypt(string)
                decrypted?.let { decrypted ->
                    return Json.decodeFromString<EnumArea>(decrypted)
                }
            }
            catch (ex: Exception)
            {
                Log.d( "xxXXx", ex.stackTrace.toString())
            }

            return null;
        }
    }
}