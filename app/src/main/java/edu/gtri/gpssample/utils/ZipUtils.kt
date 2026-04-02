package edu.gtri.gpssample.utils

import android.app.Activity
import android.net.Uri
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Image
import edu.gtri.gpssample.database.models.ImageList
import java.io.*
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils
{
    fun zipToUri( activity: Activity, config: Config, fileName: String, includeConfig: Boolean, includeImages: Boolean, shouldPackMinimal: Boolean, zipUri: Uri, completion: (error: String) -> Unit)
    {
        Thread {
            try {
                activity.contentResolver.openOutputStream(zipUri)?.use { outputStream ->

                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                        // ---- CONFIG JSON ----
                        if (includeConfig)
                        {
                            val packedConfig = if (shouldPackMinimal) config.packMinimal() else config.pack()

                            val configEntry = ZipEntry("$fileName.json")
                            zipOut.putNextEntry(configEntry)
                            zipOut.write(packedConfig.toByteArray())
                            zipOut.closeEntry()
                        }

                        // ---- IMAGE JSON ----
                        if (includeImages)
                        {
                            val imageList = ImageList(config.uuid, ArrayList<Image>())

                            for (enumArea in config.enumAreas) {
                                for (location in enumArea.locations) {
                                    ImageDAO.instance().getImage(location)?.let {
                                        imageList.images.add(it)
                                    }
                                }
                            }

                            if (imageList.images.isNotEmpty()) {

                                val payload = imageList.pack(config.encryptionPassword)

                                val imageEntry = ZipEntry("$fileName-img.json")
                                zipOut.putNextEntry(imageEntry)
                                zipOut.write(payload.toByteArray())
                                zipOut.closeEntry()
                            }
                        }
                    }
                }

                activity.runOnUiThread { completion("") }

            } catch (ex: Exception) {

                Log.d("xxx", ex.stackTraceToString())

                activity.runOnUiThread {
                    completion("Error writing zip")
                }
            }

        }.start()
    }

    fun unzip( activity: Activity, zipUri: Uri, password: String, completion: (Pair<Config?, Config.ErrorCode>)->Unit )
    {
        Thread {
            try
            {
                var config: Config? = null
                var errorCode = Config.ErrorCode.None

                activity.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                        var entry = zis.nextEntry

                        while (entry != null)
                        {
                            val reader = BufferedReader(InputStreamReader(zis))
                            val content = reader.readText()

                            if (entry.name.contains( "-img"))
                            {
                                ImageList.unpack( content, password )?.let { imageList ->
                                    for (image in imageList.images)
                                    {
                                        if (ImageDAO.instance().getImage( image.uuid ) == null)
                                        {
                                            Log.d( "xxx", "imported image with uuid: ${image.uuid}")
                                            ImageDAO.instance().createImage( image )
                                        }
                                    }
                                }
                            }
                            else
                            {
                                val (cfg, eCode) = Config.unpack( content, password )
                                config = cfg
                                errorCode = eCode
                            }

                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }

                activity.runOnUiThread {
                    completion( Pair(config, errorCode ))
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
                activity.runOnUiThread {
                    completion( Pair(null,Config.ErrorCode.UnknownError))
                }
            }
        }.start()
    }

    fun zipToPublicDocuments( activity: Activity, config: Config, fileName: String, subDirectory: String, includeConfig: Boolean, includeImages: Boolean, shouldPackMinimal: Boolean, completion: (Boolean) -> Unit)
    {
        Thread {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + "/GPSSample/$subDirectory"
                    )
                }

                val resolver = activity.contentResolver
                val uri = resolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    values
                ) ?: throw Exception("Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                        // ---- CONFIG JSON ----
                        if (includeConfig)
                        {
                            val packedConfig = if (shouldPackMinimal) config.packMinimal() else config.pack()

                            val configEntry = ZipEntry("$fileName.json")
                            zipOut.putNextEntry(configEntry)
                            zipOut.write(packedConfig.toByteArray())
                            zipOut.closeEntry()
                        }

                        // ---- IMAGE JSON (if exists) ----
                        if (includeImages)
                        {
                            val imageList = ImageList(config.uuid, ArrayList<Image>())

                            for (enumArea in config.enumAreas) {
                                for (location in enumArea.locations) {
                                    ImageDAO.instance().getImage(location)?.let {
                                        imageList.images.add(it)
                                    }
                                }
                            }

                            if (imageList.images.isNotEmpty()) {
                                val payload = imageList.pack(config.encryptionPassword)

                                val imageEntry = ZipEntry("$fileName-img.json")
                                zipOut.putNextEntry(imageEntry)
                                zipOut.write(payload.toByteArray())
                                zipOut.closeEntry()
                            }
                        }
                    }
                }

                activity.runOnUiThread { completion(true) }

            } catch (ex: Exception) {
                Log.d("xxx", ex.stackTraceToString())
                activity.runOnUiThread { completion(false) }
            }
        }.start()
    }
}
