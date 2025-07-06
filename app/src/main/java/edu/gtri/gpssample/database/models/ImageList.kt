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
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class ImageList (
    var configUuid: String,
    var images: ArrayList<Image>)
{
    fun pack( encryptionPassword: String ) : String
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

            if (encryptionPassword.isEmpty())
            {
                return  compressedString
            }
            else
            {
                return  EncryptionUtil.Encrypt(compressedString, encryptionPassword)
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return ""
    }

    companion object
    {
        fun unpack( jsonString: String, password: String ) : ImageList?
        {
            try
            {
                // check for a cleartext string

                if (jsonString.isNotEmpty() && jsonString.first() == '{')
                {
                    return Json.decodeFromString<ImageList>( jsonString )
                }

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

                return Json.decodeFromString<ImageList>( uncompressedString )
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
            }

            return null
        }
    }
}