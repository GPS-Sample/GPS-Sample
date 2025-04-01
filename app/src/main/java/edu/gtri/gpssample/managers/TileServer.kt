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
import java.io.FileOutputStream
import kotlin.io.path.name

class TileServer( mbtilesPath: String ) : NanoHTTPD(8080), BusyIndicatorDialog.BusyIndicatorDialogDelegate
{
    private val db: SQLiteDatabase
    data class Bounds(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double)

    init
    {
        val mbtilesFile = File( mbtilesPath )

        if (!mbtilesFile.exists()) throw IllegalArgumentException("MBTiles file not found!")

        db = SQLiteDatabase.openDatabase(mbtilesFile.path, null, SQLiteDatabase.OPEN_READONLY)

        start()

        instance = this
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
        var mbTilesPath: String? = null
        var instance: TileServer? = null
        var rasterLayer: RasterLayer? = null
        var rasterSource: RasterSource? = null

        fun startServer( activity: Activity, uri: Uri, mapboxMap: MapboxMap, completion: (()->Unit)? )
        {
            val busyIndicatorDialog = BusyIndicatorDialog( activity, activity.resources.getString(edu.gtri.gpssample.R.string.loading_mapbox_tiles), null, false )

            Thread {
                mbTilesPath = copyMbTilesToCache( activity, uri )

                val sharedPreferences: SharedPreferences = activity.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kMBTilesPath.value, mbTilesPath!! )
                editor.commit()

                instance = TileServer( mbTilesPath!! )

                activity.runOnUiThread {
                    loadMapboxStyle( activity, mapboxMap, completion )
                    busyIndicatorDialog.alertDialog.cancel()
                }
            }.start()
        }

        fun startServer( activity: Activity, mbTilesPath: String, mapboxMap: MapboxMap, completion: (()->Unit)? )
        {
            instance?.let {
                if (started)
                {
                    loadMapboxStyle( activity, mapboxMap, completion )
                    return
                }
            }

            mbTilesPath.let {
                instance = TileServer( mbTilesPath )

                loadMapboxStyle( activity, mapboxMap, completion )
            }
        }

        fun stopServer()
        {
            instance?.let {
                if (started)
                {
                    instance!!.stop()
                    instance!!.db.close()
                }
            }
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

            activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo( outputStream )
                }
            }

            return tempFile.absolutePath
        }

        fun loadMapboxStyle( context: Context, mapboxMap: MapboxMap, completion: (()->Unit)?)
        {
            if (rasterLayer == null)
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

            mapboxMap.loadStyle(
                style(styleUri = style) {
                    +rasterSource!!
                    +rasterLayer!!
                },
                object : Style.OnStyleLoaded {
                    override fun onStyleLoaded(style: Style) {
                        completion?.let {
                            it()
                        }
                    }
                }
            )
        }

        fun getCachedFiles( context: Context ): List<String>
        {
            val cacheDir = context.cacheDir
            return cacheDir.listFiles { _, name -> name.endsWith(".mbtiles") } ?.map { it.name } ?: emptyList()
        }

        fun getBounds(mbtilesPath: String): Bounds?
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
                    val lon = parts[0].toDouble()
                    val lat = parts[1].toDouble()
                    bounds = Bounds(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                }
            }

            cursor.close()
            db.close()

            return bounds
        }
    }

    override fun didPressCancelButton()
    {
    }
}