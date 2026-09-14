package edu.gtri.gpssample.managers

import android.content.Context
import edu.gtri.gpssample.constants.MapEngine
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.MapTileRegion
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

object MapTileCacheManager
{
    private const val PREFS = "map_cache"
    private const val KEY_REGIONS = "cached_regions"

    /**
     * Add a cached region without removing existing regions.
     */
    fun add(context: Context, engine: MapEngine, region: MapTileRegion)
    {
        val regions = loadAll(context).toMutableList()
        regions.add(engine to region)
        saveAll(context, regions)
    }

    /**
     * Add multiple cached regions without removing existing regions.
     */
    fun add(context: Context, engine: MapEngine, regions: List<MapTileRegion>)
    {
        if (regions.isEmpty()) return

        val allRegions = loadAll(context).toMutableList()

        regions.forEach { region ->
            allRegions.add(engine to region)
        }

        saveAll(context, allRegions)
    }

    /**
     * Replace all cached regions for an engine.
     */
    fun save(context: Context, engine: MapEngine, regions: List<MapTileRegion>)
    {
        val allRegions = loadAll(context)
            .filter { it.first != engine }
            .toMutableList()

        regions.forEach { region ->
            allRegions.add(engine to region)
        }

        saveAll(context, allRegions)
    }

    /**
     * Get cached regions for an engine.
     */
    fun load(context: Context, engine: MapEngine): List<MapTileRegion>
    {
        return loadAll(context)
            .filter { it.first == engine }
            .map { it.second }
    }

    /**
     * Remove all cached regions for an engine.
     */
    fun clear(context: Context, engine: MapEngine)
    {
        val remaining = loadAll(context)
            .filter { it.first != engine }

        saveAll(context, remaining)
    }

    /**
     * Remove a specific cached region.
     */
    fun remove(context: Context, engine: MapEngine, region: MapTileRegion)
    {
        val remaining = loadAll(context)
            .filterNot {
                it.first == engine && it.second == region
            }

        saveAll(context, remaining)
    }

    // -------------------------------------------------------------------------
    // Internal persistence
    // -------------------------------------------------------------------------

    private fun loadAll(context: Context): List<Pair<MapEngine, MapTileRegion>>
    {
        val string = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REGIONS, null)
            ?: return emptyList()

        val json = JSONArray(string)

        return buildList {
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)

                val engine = MapEngine.entries.first {
                    it.value == obj.getInt("engine")
                }

                val region = MapTileRegion(
                    northEast = LatLon(
                        obj.getDouble("neLat"),
                        obj.getDouble("neLon")
                    ),
                    southWest = LatLon(
                        obj.getDouble("swLat"),
                        obj.getDouble("swLon")
                    )
                )

                add(engine to region)
            }
        }
    }

    private fun saveAll(context: Context, regions: List<Pair<MapEngine, MapTileRegion>>)
    {
        val json = JSONArray()

        regions.forEach { (engine, region) ->
            json.put(
                JSONObject().apply {
                    put("engine", engine.value)
                    put("swLat", region.southWest.latitude)
                    put("swLon", region.southWest.longitude)
                    put("neLat", region.northEast.latitude)
                    put("neLon", region.northEast.longitude)
                }
            )
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_REGIONS, json.toString())
            }
    }
}