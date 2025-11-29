/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.managers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.sources.TileSet
import com.mapbox.maps.extension.style.sources.generated.RasterSource
import com.mapbox.maps.extension.style.style
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.io.path.name

class TileServer( mbtilesPath: String ) : NanoHTTPD(8080), BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private lateinit var db: SQLiteDatabase

    data class Bounds(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double)

    init
    {
        val mbtilesFile = File( mbtilesPath )

        if (mbtilesFile.exists())
        {
            db = SQLiteDatabase.openDatabase(mbtilesFile.path, null, SQLiteDatabase.OPEN_READONLY)

            start()

            instance = this
        }
    }

    override fun serve( session: IHTTPSession ): Response
    {
        val uri = session.uri
        val match = Regex("/(\\d+)/(\\d+)/(\\d+)\\.png").find(uri)

        if (match != null)
        {
            val (z, x, y) = match.destructured
            val tile = getTile(z.toInt(), x.toInt(), y.toInt())

            return if (tile != null)
            {
                newFixedLengthResponse(Response.Status.OK, "image/png", tile.inputStream(), tile.size.toLong())
            }
            else
            {
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Tile not found")
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Invalid request")
    }

    private fun getTile( z: Int, x: Int, y: Int ): ByteArray?
    {
        val flippedY = (1 shl z) - 1 - y  // Flip Y for MBTiles

        val cursor = db.rawQuery(
            "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
            arrayOf(z.toString(), x.toString(), flippedY.toString())
        )

        return if (cursor.moveToFirst()) cursor.getBlob(0) else null.also { cursor.close() }
    }

    override fun start() {
        started = true
        super.start()
    }

    override fun stop() {
        started = false
        super.stop()
    }

    companion object
    {
        var started: Boolean = false
        var rasterLayer: RasterLayer? = null
        var rasterSource: RasterSource? = null

        private var mbTilesPath: String = ""
        private var instance: TileServer? = null

        fun stopServer()
        {
            instance?.let {
                if (started)
                {
                    it.stop()
                    it.db.close()
                }
            }
        }

        fun startServer( tilesPath: String )
        {
            mbTilesPath = tilesPath

            stopServer()

            instance = TileServer( mbTilesPath )

            if (rasterLayer == null)
            {
                val tileSet = TileSet.Builder("2.0", listOf("http://localhost:8080/{z}/{x}/{y}.png")).build()

                rasterSource = RasterSource.Builder("raster-source")
                    .tileSet(tileSet)
                    .tileSize(256)
                    .build()

                rasterLayer = RasterLayer("raster-layer", "raster-source").rasterOpacity(1.0)
            }
        }

        fun startServer( activity: Activity, uri: Uri?, tilesPath: String, mapboxMap: MapboxMap, completion: (()->Unit)? )
        {
            val busyIndicatorDialog = BusyIndicatorDialog( activity, activity.resources.getString(edu.gtri.gpssample.R.string.loading_mapbox_tiles), null, false )

            Thread {
                mbTilesPath = tilesPath

                uri?.let {
                    mbTilesPath = copyMbTilesToCache( activity, uri )
                }

                stopServer()

                instance = TileServer( mbTilesPath )

                activity.runOnUiThread {
                    loadMapboxStyle( activity, mapboxMap, completion )
                    busyIndicatorDialog.alertDialog.cancel()
                }
            }.start()
        }

        private fun copyMbTilesToCache( activity: Activity, uri: Uri ): String
        {
            var fileName: String = ""

            activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (nameIndex != -1)
                {
                    cursor.moveToFirst()
                    fileName = cursor.getString(nameIndex)
                }
            }

            val tempFile = File( activity.cacheDir, fileName )

            if (tempFile.exists())
            {
                tempFile.delete()
            }

            activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo( outputStream )
                }
            }

            return tempFile.absolutePath
        }

        fun loadMapboxStyle( context: Context, mapboxMap: MapboxMap, completion: (()->Unit)?)
        {
            if (instance != null && rasterLayer == null)
            {
                val tileSet = TileSet.Builder("2.0", listOf("http://localhost:8080/{z}/{x}/{y}.png")).build()

                rasterSource = RasterSource.Builder("raster-source")
                    .tileSet(tileSet)
                    .tileSize(256)
                    .build()

                rasterLayer = RasterLayer("raster-layer", "raster-source").rasterOpacity(1.0)
            }

            var style = Style.MAPBOX_STREETS

            val sharedPreferences: SharedPreferences = context.getSharedPreferences("default", 0)

            sharedPreferences.getString( Keys.kMapStyle.value, null)?.let {
                style = it
            }

            mapboxMap.loadStyle(style(style) {
                rasterSource?.let { +it }
                rasterLayer?.let { +it }
            }) { style ->
                // style is now the loaded Style object
                completion?.invoke()
            }
        }

        fun getCachedFiles( context: Context ): List<String>
        {
            val cacheDir = context.cacheDir
            return cacheDir.listFiles { _, name -> name.endsWith(".mbtiles") } ?.map { it.name } ?: emptyList()
        }

        fun getBounds(mbtilesPath: String): Bounds?
        {
            if (mbtilesPath.isNotEmpty())
            {
                val db = SQLiteDatabase.openDatabase( mbtilesPath, null, SQLiteDatabase.OPEN_READONLY )
                var bounds: Bounds? = null

                val cursor = db.rawQuery("SELECT value FROM metadata WHERE name = 'bounds'", null)

                // returns minLon,minLat,maxLon,maxLat

                if (cursor.moveToFirst())
                {
                    val boundsStr = cursor.getString(0)
                    val parts = boundsStr.split(",")

                    if (parts.size == 4)
                    {
                        bounds = Bounds(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                    }
                }

                cursor.close()
                db.close()

                return bounds
            }

            return null
        }

        fun filePathSize( context: Context, uri: Uri ) : Pair<String, Long> {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )

            var fileSize = 0L
            var fileName: String = ""

            cursor?.use {
                if (it.moveToFirst()) {
                    fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    fileSize = it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
                }
            }

            val tempFile = File(context.cacheDir, fileName)

            return Pair( tempFile.absolutePath, fileSize )
        }

        fun fileExists( context: Context, uri: Uri ) : Boolean {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )

            var fileSize = 0L
            var fileName: String = ""

            cursor?.use {
                if (it.moveToFirst()) {
                    fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    fileSize = it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
                }
            }

            val tempFile = File(context.cacheDir, fileName)

            return tempFile.exists() && tempFile.length() == fileSize
        }

        fun centerMap( mapboxMap: MapboxMap, zoomLevel: Double? = null )
        {
            val zLevel = if (zoomLevel == null) 8.0 else zoomLevel

            getBounds( mbTilesPath )?.let { bounds ->
                val latLngBounds = LatLngBounds(LatLng(bounds.minLat, bounds.minLon), LatLng(bounds.maxLat, bounds.maxLon))
                val point = com.mapbox.geojson.Point.fromLngLat(latLngBounds.center.longitude,latLngBounds.center.latitude)
                val cameraPosition = CameraOptions.Builder()
                    .zoom(zLevel)
                    .center(point)
                    .build()

                mapboxMap.setCamera(cameraPosition)
            }
        }
    }

    override fun didPressCancelButton()
    {
    }
}