@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID

@Serializable
data class Image (
    var uuid : String,
    var creationDate : Long,
    var locationUuid: String,
    var data: String )
{
    constructor( locationUuid: String, data: String ) : this(UUID.randomUUID().toString(), Date().time, locationUuid, data )

    fun pack() : String
    {
        try
        {
            val jsonString = json.encodeToString( this )

            return jsonString
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return ""
    }

    companion object
    {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }

        fun unpack( jsonString: String ) : Image?
        {
            try
            {
                return json.decodeFromString<Image>( jsonString )
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
            }

            return null
        }
    }
}
