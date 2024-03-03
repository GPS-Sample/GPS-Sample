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
    var id : Int? = null,
    var uuid : String,
    var creationDate: Long,
    var name: String,
    var selectedEnumerationTeamId: Int,
    var vertices: ArrayList<LatLon>,
    var locations: ArrayList<Location>,
    var enumerationTeams: ArrayList<EnumerationTeam>)
{
    constructor( id: Int, uuid: String, creationDate: Long, name: String, selectedEnumerationTeamId: Int)
            : this(id, uuid, creationDate, name, selectedEnumerationTeamId, ArrayList<LatLon>(), ArrayList<Location>(), ArrayList<EnumerationTeam>())

    constructor( name: String, vertices: ArrayList<LatLon>)
            : this(null, UUID.randomUUID().toString(), Date().time, name, -1, vertices, ArrayList<Location>(), ArrayList<EnumerationTeam>())

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

    fun pack(password: String) : String
    {
        val jsonString = Json.encodeToString( this )

        if (password.isEmpty())
        {
            return jsonString
        }
        else
        {
            return EncryptionUtil.Encrypt(jsonString,password)
        }
    }

    companion object
    {
        fun unpack( message: String, password: String ) : EnumArea?
        {
            try
            {
                if (password.isEmpty())
                {
                    return Json.decodeFromString<EnumArea>(message)
                }
                else
                {
                    val clearText = EncryptionUtil.Decrypt(message,password)
                    clearText?.let { clearText ->
                        return Json.decodeFromString<EnumArea>(clearText)
                    }
                }
            }
            catch (ex: Exception)
            {
                Log.d( "xxx", ex.stackTrace.toString())
            }

            return null;
        }
    }
}