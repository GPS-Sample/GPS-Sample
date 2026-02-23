package edu.gtri.gpssample.utils

import android.app.Activity
import android.net.Uri
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
    private fun zipToFile( files: List<File>, zipFile: File )
    {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            for (file in files)
            {
                FileInputStream(file).use { fis ->
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    fun zipToUri( activity: Activity, files: List<File>, zipUri: Uri, completion: ( error: String )->Unit )
    {
        Thread {
            try
            {
                activity.contentResolver.openOutputStream(zipUri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                        for (file in files)
                        {
                            FileInputStream(file).use { input ->
                                val entry = ZipEntry(file.name)
                                zipOut.putNextEntry(entry)
                                input.copyTo(zipOut)
                                zipOut.closeEntry()
                            }
                        }
                    }
                }

                activity.runOnUiThread {
                    completion( "" )
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
                activity.runOnUiThread {
                    completion( "Error writing to zip file")
                }
            }
        }.start()
    }

    fun unzip( activity: Activity, zipUri: Uri, password: String, completion: (config: Config?)->Unit )
    {
        Thread {
            try
            {
                var config: Config? = null

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
                                config = Config.unpack( content, password )
                                Log.d( "xxx", "imported Config" )
                            }

                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }

                activity.runOnUiThread {
                    completion( config )
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
                activity.runOnUiThread {
                    completion( null )
                }
            }
        }.start()
    }

    fun exportToDefaultLocation( activity: Activity, config: Config, fileName: String, shouldPackMinimal: Boolean, completion: (success: Boolean )->Unit )
    {
        Thread {
            try
            {
                saveToDefaultLocation( activity, config, fileName, shouldPackMinimal ) { configFile, imageFile ->
                    val zipFileName = fileName + ".zip"
                    val zipFile = File(zipFileName)

                    if (configFile != null)
                    {
                        if (imageFile != null)
                        {
                            zipToFile( listOf( configFile, imageFile ), zipFile )
                            imageFile.delete()
                            activity.runOnUiThread {
                                completion( true )
                            }
                        }
                        else
                        {
                            zipToFile( listOf( configFile ), zipFile )
                        }

                        configFile.delete()

                        activity.runOnUiThread {
                            completion( true )
                        }
                    }
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
                activity.runOnUiThread {
                    completion( false )
                }
            }
        }.start()
    }

    fun saveToDefaultLocation( activity: Activity, config: Config, fileName: String, shouldPackMinimal: Boolean, completion: (configFile: File?, imageFile: File? )->Unit )
    {
        Thread {
            try
            {
                val configFileName = fileName + ".json"
                val imageFileName = fileName + "-img.json"

                val packedConfig = if (shouldPackMinimal) config.packMinimal() else config.pack()

                val configFile = File(configFileName)
                val writer = FileWriter(configFile)
                writer.append(packedConfig)
                writer.flush()
                writer.close()

                val imageList = ImageList( config.uuid, ArrayList<Image>())

                // check for images
                for (enumArea in config.enumAreas)
                {
                    for (location in enumArea.locations)
                    {
                        ImageDAO.instance().getImage( location )?.let {
                            imageList.images.add( it )
                        }
                    }
                }

                var imageFile: File? = null

                if (imageList.images.isNotEmpty())
                {
                    val payload = imageList.pack( config.encryptionPassword )
                    imageFile = File(imageFileName)
                    val writer = FileWriter(imageFile)
                    writer.append(payload)
                    writer.flush()
                    writer.close()
                }

                activity.runOnUiThread {
                    completion( configFile, imageFile )
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
                activity.runOnUiThread {
                    completion( null, null )
                }
            }
        }.start()
    }

    fun exportToPublicDownloads(
        activity: Activity,
        config: Config,
        fileName: String,
        shouldPackMinimal: Boolean,
        completion: (Boolean) -> Unit
    ) {
        Log.d("EXPORT", "Starting export")
        Thread {
            try {
                val justFileName = File(fileName).name

                val zipName = "$justFileName.zip"

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, zipName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/GPSSample/Configurations"
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
                        val packedConfig =
                            if (shouldPackMinimal) config.packMinimal()
                            else config.pack()

                        val configEntry = ZipEntry("$fileName.json")
                        zipOut.putNextEntry(configEntry)
                        zipOut.write(packedConfig.toByteArray())
                        zipOut.closeEntry()

                        // ---- IMAGE JSON (if exists) ----
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

                            val imageEntry = ZipEntry("$justFileName-img.json")
                            zipOut.putNextEntry(imageEntry)
                            zipOut.write(payload.toByteArray())
                            zipOut.closeEntry()
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
