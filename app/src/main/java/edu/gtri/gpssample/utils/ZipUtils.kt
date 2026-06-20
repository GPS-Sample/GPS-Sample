@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.utils

import android.app.Activity
import android.net.Uri
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Image
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.ErrorCode
import edu.gtri.gpssample.managers.NearbySessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive

class ZipUtils()
{
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentJob: Job? = null
    private val _state = MutableStateFlow<NearbySessionState>(NearbySessionState.Idle)

    val state: StateFlow<NearbySessionState> = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class Header(
        val count: Int
    )

    private fun writeHeader( numItems: Int, zipOut: ZipOutputStream )
    {
        val header = json.encodeToString(Header(numItems))
        zipOut.write(header.toByteArray())
        zipOut.write('\n'.code)
    }

    private fun writeEnumAreas(zipOut: ZipOutputStream, config: Config, fileName: String)
    {
        val entry = ZipEntry("$fileName-enumAreas.json")

        zipOut.putNextEntry(entry)

        try
        {
            if (config.enumAreas.size == 1)
            {
                _state.value = NearbySessionState.Message("Exporting EnumArea 1/1" )
                writeHeader(1, zipOut )
                val packedEnumArea = config.enumAreas[0].pack( config.encryptionPassword )
                zipOut.write(packedEnumArea.toByteArray())
                zipOut.write('\n'.code)
            }
            else
            {
                val  query = "SELECT ${DAO.COLUMN_UUID} FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_UUID} = '${config.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"

                DAO.instance().readableDatabase.rawQuery(query, null ).use { cursor ->
                    var count = 1
                    val totalCount = cursor.count

                    writeHeader( totalCount, zipOut )

                    while (cursor.moveToNext())
                    {
                        if (currentJob == null) { break }

                        _state.value = NearbySessionState.Message("Exporting EnumArea ${count++}/${totalCount}" )

                        val uuid = cursor.getString(cursor.getColumnIndexOrThrow(DAO.COLUMN_UUID))

                        DAO.enumAreaDAO.getEnumArea(uuid)?.let { enumArea ->
                            val packedEnumArea = enumArea.pack( config.encryptionPassword )
                            zipOut.write(packedEnumArea.toByteArray())
                            zipOut.write('\n'.code)
                        }
                    }
                }
            }
        }
        catch( ex: Exception ) {}
        finally
        {
            zipOut.closeEntry()
        }
    }

    private fun writeImages(zipOut: ZipOutputStream, config: Config, fileName: String)
    {
        val entry = ZipEntry("$fileName-img.json")

        zipOut.putNextEntry(entry)

        try
        {
            val  query = "SELECT ${ImageDAO.COLUMN_UUID} FROM ${ImageDAO.TABLE_IMAGE}"

            ImageDAO.instance().readableDatabase.rawQuery(query, null ).use { cursor ->
                var count = 1
                val totalCount = cursor.count

                writeHeader( totalCount, zipOut )

                while (cursor.moveToNext())
                {
                    if (currentJob == null) { break }

                    _state.value = NearbySessionState.Message("Exporting Image ${count++}/${totalCount}" )

                    val uuid = cursor.getString(cursor.getColumnIndexOrThrow(ImageDAO.COLUMN_UUID))

                    ImageDAO.instance().getImage(uuid)?.let { image ->
                        val packedImage = image.pack()
                        zipOut.write(packedImage.toByteArray())
                        zipOut.write('\n'.code)
                    }
                }
            }
        }
        catch( ex: Exception ) {}
        finally
        {
            zipOut.closeEntry()
        }
    }

    fun zipToUri( activity: Activity, config: Config, fileName: String, includeConfig: Boolean, includeImages: Boolean, zipUri: Uri, completion: (success: Boolean) -> Unit)
    {
        currentJob?.cancel()

        currentJob = scope.launch {
            try
            {
                _state.value = NearbySessionState.Message("Exporting Config..." )

                activity.contentResolver.openOutputStream(zipUri)?.use { outputStream ->

                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                        // ---- CONFIG JSON ----
                        if (includeConfig)
                        {
                            val packedConfig = config.pack()
                            val configEntry = ZipEntry("$fileName.json")
                            zipOut.putNextEntry(configEntry)
                            zipOut.write(packedConfig.toByteArray())
                            zipOut.closeEntry()

                            writeEnumAreas(zipOut, config, fileName)
                        }

                        // ---- IMAGE JSON ----
                        if (includeImages)
                        {
                            writeImages(zipOut, config, fileName)
                        }
                    }
                }

                withContext(Dispatchers.Main) { completion(true) }
            }
            catch (ex: Exception)
            {
                withContext(Dispatchers.Main) { completion(false) }
            }
        }
    }

    fun zipToPublicDocuments( activity: Activity, config: Config, fileName: String, subDirectory: String, includeConfig: Boolean, includeImages: Boolean, completion: (Boolean) -> Unit)
    {
        currentJob?.cancel()

        currentJob = scope.launch {
            try {
                _state.value = NearbySessionState.Message("Exporting Config..." )

                val resolver = activity.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GPSSample/$subDirectory")
                    }
                    else
                    {
                        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"GPSSample/$subDirectory")

                        if (!dir.exists())
                        {
                            dir.mkdirs()
                        }

                        val file = File(dir, fileName)
                        put(MediaStore.MediaColumns.DATA, file.absolutePath)
                    }
                }

                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: throw Exception("Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                        // ---- CONFIG JSON ----
                        if (includeConfig)
                        {
                            val packedConfig = config.pack()
                            val configEntry = ZipEntry("$fileName.json")
                            zipOut.putNextEntry(configEntry)
                            zipOut.write(packedConfig.toByteArray())
                            zipOut.closeEntry()

                            writeEnumAreas(zipOut, config, fileName)
                        }

                        // ---- IMAGE JSON ----
                        if (includeImages)
                        {
                            writeImages(zipOut, config, fileName)
                        }
                    }
                }

                withContext(Dispatchers.Main) { completion(true) }
            }
            catch (ex: Exception)
            {
                withContext(Dispatchers.Main) { completion(false) }
            }
        }
    }

    fun unzip( activity: Activity, zipUri: Uri, password: String, completion: (Pair<Config?, ErrorCode>)->Unit )
    {
        currentJob?.cancel()

        currentJob = scope.launch {
            var config: Config? = null
            var errorCode = ErrorCode.None
            var imageTransactionStarted = false
            var enumAreaTransactionStarted = false

            try {
                _state.value = NearbySessionState.Message("Initializing..." )

                activity.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                        var entry = zis.nextEntry

                        while (errorCode == ErrorCode.None && entry != null)
                        {
                            if (entry.name.contains("-enumAreas"))
                            {
                                val reader = BufferedReader(InputStreamReader(zis))
                                val header = json.decodeFromString<Header>(reader.readLine())
                                val totalCount = header.count
                                var count = 1

                                DAO.instance().writableDatabase.beginTransaction()
                                enumAreaTransactionStarted = true

                                while (true)
                                {
                                    ensureActive()

                                    val line = reader.readLine() ?: break

                                    if (line.isBlank()) continue

                                    _state.value = NearbySessionState.Message("Importing EnumArea ${count++}/${totalCount}" )

                                    val (ea, eCode) = EnumArea.unpack(line, password )
                                    if (eCode != ErrorCode.None)
                                    {
                                        config= null
                                        errorCode = eCode
                                        break
                                    }
                                    ea?.let { enumArea ->
                                        DAO.enumAreaDAO.createOrUpdateEnumArea(enumArea, enumArea.version)
                                    }
                                }

                                if (errorCode == ErrorCode.None)
                                {
                                    DAO.instance().writableDatabase.setTransactionSuccessful()
                                }
                            }
                            else if (entry.name.contains("-img"))
                            {
                                val reader = BufferedReader(InputStreamReader(zis))
                                val header = json.decodeFromString<Header>(reader.readLine())
                                val totalCount = header.count
                                var count = 1

                                ImageDAO.instance().writableDatabase.beginTransaction()
                                imageTransactionStarted = true

                                while (true)
                                {
                                    ensureActive()

                                    val line = reader.readLine() ?: break

                                    if (line.isBlank()) continue

                                    _state.value = NearbySessionState.Message("Importing Image ${count++}/${totalCount}" )

                                    Image.unpack(line )?.let { image ->
                                        ImageDAO.instance().createImage( image )
                                    }
                                }

                                ImageDAO.instance().writableDatabase.setTransactionSuccessful()
                            }
                            else
                            {
                                _state.value = NearbySessionState.Message("Importing Configuration..." )
                                val reader = BufferedReader(InputStreamReader(zis))
                                val content = reader.readText()
                                val (cfg, eCode) = Config.unpack(content, password )
                                config = cfg
                                errorCode = eCode
                            }

                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
            }
            catch (ce: CancellationException)
            {
                config = null
                errorCode = ErrorCode.None
            }
            catch( ex: Exception )
            {
                config = null
                errorCode = ErrorCode.UnknownError
            }
            finally
            {
                if (enumAreaTransactionStarted)
                {
                    DAO.instance().writableDatabase.endTransaction()
                }
                if (imageTransactionStarted)
                {
                    ImageDAO.instance().writableDatabase.endTransaction()
                }
                withContext(NonCancellable + Dispatchers.Main) { completion( Pair(config,errorCode)) }
            }
        }
    }

    fun cancel()
    {
        currentJob?.cancel()
        currentJob = null
        _state.value = NearbySessionState.Idle
    }
}
