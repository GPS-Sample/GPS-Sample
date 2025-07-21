package edu.gtri.gpssample.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.database.ConfigDAO
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Image
import edu.gtri.gpssample.database.models.ImageList
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    fun zipToUri( context: Context, files: List<File>, zipUri: Uri )
    {
        context.contentResolver.openOutputStream(zipUri)?.use { outputStream ->
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
    }

    fun unzip( context: Context, zipUri: Uri, password: String ) : Config?
    {
        var config: Config? = null

        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
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

        return config
    }

    fun exportToDefaultLocation( activity: Activity, config: Config, fileName: String, shouldPackMinimal: Boolean )
    {
        try
        {
            val pair = saveToDefaultLocation( activity, config, fileName, shouldPackMinimal )

            val configFile = pair.first
            val imageFile = pair.second
            val zipFileName = fileName + ".zip"

            val zipFile = File(zipFileName)

            if (configFile != null)
            {
                if (imageFile != null)
                {
                    zipToFile( listOf( configFile, imageFile ), zipFile )
                    imageFile.delete()
                }
                else
                {
                    zipToFile( listOf( configFile ), zipFile )
                }

                configFile.delete()
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }
    }

    fun saveToDefaultLocation( activity: Activity, config: Config, fileName: String, shouldPackMinimal: Boolean ) : Pair<File?, File?>
    {
        try
        {
//            config.selectedEnumAreaUuid = ""
//
//            for (enumArea in config.enumAreas)
//            {
//                enumArea.selectedEnumerationTeamUuid = ""
//                enumArea.selectedCollectionTeamUuid = ""
//            }

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

            return Pair( configFile, imageFile )
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Toast.makeText( activity.applicationContext, ex.stackTraceToString(), Toast.LENGTH_LONG).show()
        }

        return Pair( null, null )
    }
}