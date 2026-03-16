package edu.gtri.gpssample.managers

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import androidx.core.content.edit
import edu.gtri.gpssample.application.MainApplication

class PreferencesManager
{
    companion object
    {
        private val prefName = "geojson_imports"

        fun computeHash( input: String ): String
        {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun saveHash( configUuid: String, hash: String )
        {
            val key = configUuid
            val prefs = MainApplication.instance.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val hashes = prefs.getStringSet(key, mutableSetOf())!!.toMutableSet()
            hashes.add(hash)
            prefs.edit { putStringSet(key, hashes) }
            Log.d( "xxx", "saved geoJson hash for ${configUuid}")
        }

        fun removeHash( configUuid: String, hash: String )
        {
            val key = configUuid
            val prefs = MainApplication.instance.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val hashes = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

            if (hashes.remove(hash))
            {
                prefs.edit { putStringSet(key, hashes) }
                Log.d( "xxx", "removed geoJson hash for ${configUuid}")
            }
        }

        fun removeAllHashes( configUuid: String )
        {
            val key = configUuid
            val prefs = MainApplication.instance.getSharedPreferences(prefName, Context.MODE_PRIVATE)

            if (prefs.contains(key))
            {
                prefs.edit { remove(key) }
                Log.d( "xxx", "removed geoJson hashes for ${configUuid}")
            }
        }

        fun isHashImported( configUuid: String, hash: String ): Boolean
        {
            val key = configUuid
            val prefs = MainApplication.instance.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val hashes = prefs.getStringSet(key, emptySet())!!
            return hashes.contains(hash)
        }
    }
}