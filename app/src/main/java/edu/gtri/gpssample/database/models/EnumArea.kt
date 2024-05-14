package edu.gtri.gpssample.database.models

import android.util.Base64
import android.util.Log
import edu.gtri.gpssample.utils.EncryptionUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList

@Serializable
data class EnumArea (
    var uuid : String,
    var creationDate: Long,
    var configUuid: String,
    var name: String,
    var vertices: ArrayList<LatLon>,
    var locations: ArrayList<Location>,
    var enumerationTeams: ArrayList<EnumerationTeam>,
    var selectedEnumerationTeamUuid: String,
    var collectionTeams: ArrayList<CollectionTeam>,
    var selectedCollectionTeamUuid: String )
{
    constructor( uuid: String, creationDate: Long, configUuid: String, name: String, selectedEnumerationTeamUuid: String, selectedCollectionTeamUuid: String )
            : this(uuid, creationDate, configUuid, name, ArrayList<LatLon>(), ArrayList<Location>(), ArrayList<EnumerationTeam>(), selectedEnumerationTeamUuid, ArrayList<CollectionTeam>(), selectedCollectionTeamUuid)

    constructor( configUuid: String, name: String, vertices: ArrayList<LatLon>)
            : this(UUID.randomUUID().toString(), Date().time, configUuid, name, vertices, ArrayList<Location>(), ArrayList<EnumerationTeam>(), "", ArrayList<CollectionTeam>(), "")

    override fun equals(other: Any?): Boolean
    {
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
        try
        {
            // step 1: create the json string

            val jsonString = Json.encodeToString( this )

            // step 2: compress the json string

            val byteArrayOutputStream = ByteArrayOutputStream(jsonString.length)
            val gzipOutputStream = GZIPOutputStream( byteArrayOutputStream )
            gzipOutputStream.write(jsonString.toByteArray())
            gzipOutputStream.close()
            val byteArray = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()

            val compressedString = Base64.encodeToString( byteArray, Base64.DEFAULT )

            // step 3: encrypt the json string, if necessary

            if (password.isEmpty())
            {
                return compressedString
            }
            else
            {
                return EncryptionUtil.Encrypt(compressedString,password)
            }
        }
        catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return ""
    }

    companion object
    {
        fun unpack( jsonString: String, password: String ) : EnumArea?
        {
            try
            {
                var clearText = jsonString

                // step 1: decrypt the json string, if necessary

                if (password.isNotEmpty())
                {
                    EncryptionUtil.Decrypt(jsonString, password)?.let {
                        clearText = it
                    }
                }

                // step 2: decompress the json string

                val byteArray = Base64.decode( clearText, Base64.DEFAULT )
                val byteArrayInputStream = ByteArrayInputStream( byteArray )
                val gzipInputStream = GZIPInputStream( byteArrayInputStream, byteArray.size )
                val bytes = gzipInputStream.readBytes()
                val uncompressedString = bytes.decodeToString()
                gzipInputStream.close()
                byteArrayInputStream.close()

                // step 3: decode the JSON string into a Config object

                return Json.decodeFromString<EnumArea>( uncompressedString )
            }
            catch (ex: Exception)
            {
                Log.d( "xxx", ex.stackTraceToString())
            }

            return null
        }
    }
}