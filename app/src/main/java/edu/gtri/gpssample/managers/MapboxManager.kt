/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.managers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.Value
import com.mapbox.common.*
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.MapTileRegion
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.util.concurrent.atomic.AtomicInteger

class MapboxManager(
    var context: Context,
    var pointAnnotationManager: PointAnnotationManager?,
    var polygonAnnotationManager: PolygonAnnotationManager?,
    var polylineAnnotationManager: PolylineAnnotationManager?
)
{
    interface MapTileCacheDelegate
    {
        fun stylePackLoaded( error: String )
        fun tilePacksLoaded( error: String )
        fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    }

    fun addMarker( point: Point, @DrawableRes resourceId: Int ) : PointAnnotation?
    {
        val pointAnnotationOptions = PointAnnotationOptions().withPoint( point )

        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))?.let { bitmap ->
            pointAnnotationOptions.withIconImage( bitmap )
        }

        pointAnnotationManager?.let { pointAnnotationManager ->
            return pointAnnotationManager.create(pointAnnotationOptions)
        }

        return null
    }

    fun addPolygon( points: List<List<Point>>, fillColor: String, fillOpacity: Double ) : PolygonAnnotation?
    {
        val polygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints( points )
            .withFillColor( fillColor )
            .withFillOpacity( fillOpacity )

        polygonAnnotationManager?.let { polygonAnnotationManager ->
            return polygonAnnotationManager.create(polygonAnnotationOptions)
        }

        return null
    }

    fun addPolyline( points: List<Point>, color: String ) : PolylineAnnotation?
    {
        val outlinePoints = ArrayList<Point>(points)
        outlinePoints.add( outlinePoints[0] )

        val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(outlinePoints)
            .withLineColor(color)
            .withLineWidth(4.0)

        polylineAnnotationManager?.let { polylineAnnotationManager ->
            return polylineAnnotationManager.create(polylineAnnotationOptions)
        }

        return null
    }

    fun addViewAnnotationToPoint(viewAnnotationManager: ViewAnnotationManager, point: com.mapbox.geojson.Point, label: String, backgroundColor: String )
    {
        if (label.isNotEmpty())
        {
            val viewAnnotation = viewAnnotationManager.addViewAnnotation(
                resId = R.layout.view_text_view,
                options = viewAnnotationOptions
                {
                    allowOverlap(true)
                    geometry(point)
                }
            )

            viewAnnotation.rootView.findViewById<TextView>( R.id.text_view )?.let {
                it.text = label
                it.backgroundTintList = ColorStateList.valueOf(Color.parseColor(backgroundColor))
            }
        }
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap?
    {
        if (sourceDrawable == null)
        {
            return null
        }

        return if (sourceDrawable is BitmapDrawable)
        {
            sourceDrawable.bitmap
        }
        else
        {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    companion object
    {
        private val STYLE_PACK_METADATA = "STYLE_PACK_METADATA"
        private val TILE_REGION_METADATA = "TILE_REGION_METADATA"

        private var stylePackCancelable: Cancelable? = null
        private var tileRegionsCancelable = ArrayList<Cancelable>()

        fun isSelfIntersectingPolygon1( polylinePoints: java.util.ArrayList<Point> ) : Boolean
        {
            val coordinates = java.util.ArrayList<Coordinate>()

            polylinePoints.map { point ->
                coordinates.add( Coordinate( point.longitude(), point.latitude()))
            }

            return isSelfIntersectingPolygon( coordinates )
        }

        fun isSelfIntersectingPolygon2( pointAnnotations: java.util.ArrayList<PointAnnotation?>) : Boolean
        {
            val coordinates = java.util.ArrayList<Coordinate>()

            pointAnnotations.map { pointAnnotation ->
                pointAnnotation?.let{ pointAnnotation ->
                    coordinates.add( Coordinate( pointAnnotation.point.longitude(), pointAnnotation.point.latitude()))
                }
            }

            coordinates.add( coordinates[0] )

            return isSelfIntersectingPolygon( coordinates )
        }

        fun isSelfIntersectingPolygon3( latLons: java.util.ArrayList<LatLon> ) : Boolean
        {
            val coordinates = ArrayList<Coordinate>()
            for (latLon in latLons)
            {
                coordinates.add( Coordinate( latLon.longitude, latLon.latitude ))
            }

            return isSelfIntersectingPolygon( coordinates )
        }

        fun isSelfIntersectingPolygon( coordinates: java.util.ArrayList<Coordinate> ) : Boolean
        {
            try
            {
                val last = coordinates.size - 1

                // close the polygon, if necc...
                if (coordinates[0].x != coordinates[last].x || coordinates[0].y != coordinates[last].y)
                {
                    coordinates.add( coordinates[0] )
                }

                return !GeometryFactory().createPolygon(coordinates.toTypedArray()).isSimple
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
                return true
            }
        }

        fun loadStylePack( context: Context, delegate: MapTileCacheDelegate )
        {
            val stylePackLoadOptions = StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .metadata(Value(STYLE_PACK_METADATA))
                .build()

            val offlineManager = OfflineManager(MapInitOptions.getDefaultResourceOptions(context))

            stylePackCancelable = offlineManager.loadStylePack(
                Style.OUTDOORS,
                stylePackLoadOptions,
                { progress ->
                },
                { expected ->
                    if (expected.isValue) {
                        expected.value?.let { stylePack ->
                            // Style pack download finished successfully
                            Log.d( "xxx", "Style Pack download finished")
                            delegate.stylePackLoaded("")
                        }
                    }
                    expected.error?.let {
                        // Handle errors that occurred during the style pack download.
                        Log.d( "xxx", it.message )
                        delegate.stylePackLoaded( it.message )
                    }
                }
            )
        }

        fun loadTilePacks( context: Context, mapTileRegions: ArrayList<MapTileRegion>, delegate: MapTileCacheDelegate )
        {
            val offlineManager = OfflineManager(MapInitOptions.getDefaultResourceOptions(context))

            val tilesetDescriptor = offlineManager.createTilesetDescriptor(
                TilesetDescriptorOptions.Builder()
                    .styleURI(Style.OUTDOORS)
                    .minZoom(9)
                    .maxZoom(16)
                    .build()
            )

            // You need to keep a reference of the created tileStore and keep it during the download process.
            // You are also responsible for initializing the TileStore properly, including setting the proper access token.
            val tileStore = TileStore.create().also {
                // Set default access token for the created tile store instance
                it.setOption(
                    TileStoreOptions.MAPBOX_ACCESS_TOKEN,
                    TileDataDomain.MAPS,
                    Value(context.resources.getString(R.string.mapbox_access_token))
                )
            }

            var id = 0
            tileRegionsCancelable.clear()
            val numRegionsLeft = AtomicInteger(mapTileRegions.size)

            for (mapTileRegion in mapTileRegions)
            {
                id += 1

                Log.d( "xxx", "downloading region ${id}")

                val points = java.util.ArrayList<Point>()
                points.add( Point.fromLngLat( mapTileRegion.southWest.longitude, mapTileRegion.southWest.latitude ))
                points.add( Point.fromLngLat( mapTileRegion.northEast.longitude, mapTileRegion.southWest.latitude ))
                points.add( Point.fromLngLat( mapTileRegion.northEast.longitude, mapTileRegion.northEast.latitude ))
                points.add( Point.fromLngLat( mapTileRegion.southWest.longitude, mapTileRegion.northEast.latitude ))
                points.add( Point.fromLngLat( mapTileRegion.southWest.longitude, mapTileRegion.southWest.latitude ))

                val pointList = java.util.ArrayList<java.util.ArrayList<Point>>()
                pointList.add( points )
                val geometry = com.mapbox.geojson.Polygon.fromLngLats(pointList as List<MutableList<Point>>)

                val tileRegionCancelable = tileStore.loadTileRegion(
                    id.toString(),
                    TileRegionLoadOptions.Builder()
                        .geometry(geometry)
                        .descriptors(listOf(tilesetDescriptor))
                        .metadata(Value(TILE_REGION_METADATA))
                        .acceptExpired(true)
                        .networkRestriction(NetworkRestriction.NONE)
                        .build(),
                    { progress ->
                        Log.d( "xxx", " ${progress.completedResourceCount} / ${progress.requiredResourceCount}" )
                        delegate.mapLoadProgress( progress.completedResourceCount, progress.requiredResourceCount )
                    }
                ) { expected ->
                    if (expected.isValue) {
                        if (numRegionsLeft.decrementAndGet() <= 0)
                        {
                            delegate.tilePacksLoaded("")
                            Log.d( "xxx", "Tile Regions download finished")
                        }
                    }
                    expected.error?.let {
                        // Handle errors that occurred during the tile region download.
                        Log.d( "xxx", it.message )
                        cancelTilePackDownload()
                        delegate.tilePacksLoaded( it.message )
                    }
                }

                tileRegionsCancelable.add( tileRegionCancelable )
            }
        }

        fun cancelStylePackDownload()
        {
            stylePackCancelable?.let {
                it.cancel()
                stylePackCancelable = null
            }
        }

        fun cancelTilePackDownload()
        {
            for (tileRegionCancelable in tileRegionsCancelable)
            {
                tileRegionCancelable.cancel()
            }

            tileRegionsCancelable.clear()
        }

        fun ArrayListOfLatLonToArrayListOfCoordinate( latLons: ArrayList<LatLon> ) : ArrayList<Coordinate>
        {
            val coordinates = ArrayList<Coordinate>()

            latLons.map { latLon ->
                coordinates.add( Coordinate( latLon.longitude, latLon.latitude ))
            }

            return coordinates
        }

        fun ArrayListOfPointToArrayListOfCoordinate( points: ArrayList<Point> ) : ArrayList<Coordinate>
        {
            val coordinates = ArrayList<Coordinate>()

            points.map { point ->
                coordinates.add( Coordinate( point.longitude(), point.latitude()))
            }

            return coordinates
        }

        fun ArrayListOfCoordinateToArrayListOfPoint( coordinates: Array<Coordinate> ) : ArrayList<Point>
        {
            val points = ArrayList<Point>()

            coordinates.map {
                points.add( Point.fromLngLat(it.x, it.y))
            }

            return points
        }
    }
}

