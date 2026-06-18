/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_COLLECTION_TEAM_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_TEAM_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUM_AREA_MBTILESPATH
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUM_AREA_MBTILESSIZE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUM_AREA_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STRATA_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Location

class EnumAreaDAO(private var dao: DAO)
{
    fun createOrUpdateEnumArea( enumArea: EnumArea, version: String )
    {
        enumArea.version = version

        val values = ContentValues()
        putEnumArea( enumArea, values )

        dao.upsert( DAO.TABLE_ENUM_AREA, values )

        enumArea.mapTileRegion?.let {
            it.enumAreaUuid = enumArea.uuid
            DAO.mapTileRegionDAO.createOrUpdateMapTileRegion( it )
        }

        for (latLon in enumArea.vertices) {
            DAO.latLonDAO.createOrUpdateLatLon( latLon, enumArea,latLon.version )
        }

        for (location in enumArea.locations) {
            DAO.locationDAO.createOrUpdateLocation(location, enumArea, location.version)
        }

        for (enumerationTeam in enumArea.enumerationTeams) {
            DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( enumerationTeam, enumerationTeam.version )
        }

        for (collectionTeam in enumArea.collectionTeams) {
            DAO.collectionTeamDAO.createOrUpdateCollectionTeam( collectionTeam, collectionTeam.version )
        }

        for (breacrumb in enumArea.breadcrumbs)
        {
            DAO.breadcrumbDAO.createOrUpdateBreadcrumb( breacrumb, breacrumb.version )
        }
    }

    fun putEnumArea( enumArea: EnumArea, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, enumArea.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumArea.creationDate )
        values.put( DAO.COLUMN_VERSION, enumArea.version )
        values.put( DAO.COLUMN_CONFIG_UUID, enumArea.configUuid )
        values.put( DAO.COLUMN_STRATA_UUID, enumArea.strataUuid )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
        values.put( DAO.COLUMN_ENUM_AREA_MBTILESPATH, enumArea.mbTilesPath )
        values.put( DAO.COLUMN_ENUM_AREA_MBTILESSIZE, enumArea.mbTilesSize )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_UUID, enumArea.selectedEnumerationTeamUuid )
        values.put( DAO.COLUMN_COLLECTION_TEAM_UUID, enumArea.selectedCollectionTeamUuid )
    }

    @SuppressLint("Range")
    private fun buildEnumArea(cursor: Cursor): EnumArea
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val configUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_UUID))
        val strataUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STRATA_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))
        val mbTilesPath = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_MBTILESPATH))
        val mbTilesSize = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_MBTILESSIZE))
        val selectedEnumerationTeamUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_UUID))
        val selectedCollectionTeamUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_TEAM_UUID))

        return EnumArea( uuid, creationDate, configUuid, strataUuid, name, mbTilesPath, mbTilesSize, selectedEnumerationTeamUuid, selectedCollectionTeamUuid, version )
    }

    fun getEnumArea( uuid: String ): EnumArea?
    {
        var enumArea: EnumArea? = null

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_UUID} = '${uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumArea = buildEnumArea( cursor )
            enumArea.mapTileRegion = DAO.mapTileRegionDAO.getMapTileRegion( enumArea )
            enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaUuid( enumArea.uuid )
            enumArea.locations = DAO.locationDAO.getLocations( enumArea )
            enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            enumArea.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( enumArea )
            enumArea.breadcrumbs = DAO.breadcrumbDAO.getBreadcrumbs( enumArea.uuid )
        }

        cursor.close()

        return enumArea
    }

    fun getEnumAreas( config: Config ): ArrayList<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_UUID} = '${config.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumArea = buildEnumArea( cursor )
            enumArea.mapTileRegion = DAO.mapTileRegionDAO.getMapTileRegion( enumArea )
            enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaUuid( enumArea.uuid )
            enumArea.locations = DAO.locationDAO.getLocations( enumArea )
            enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            enumArea.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( enumArea )
            enumArea.breadcrumbs = DAO.breadcrumbDAO.getBreadcrumbs( enumArea.uuid )
            enumAreas.add( enumArea )
        }

        cursor.close()

        return enumAreas
    }

    fun loadLazyLocations( enumArea: EnumArea )
    {
        for (location in enumArea.locations)
        {
            if (location.enumerationItems.isEmpty())
            {
                location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            }
        }
    }

    data class EnumAreaSummary(
        val uuid: String,
        val name: String,
        val enumeratedCount: Int,
        val eligibleCount: Int,
        val sampledCount: Int,
        val surveyedCount: Int
    )

    fun getEnumAreaSummary(configUuid: String): List<EnumAreaSummary> {

        val db = dao.readableDatabase

        val query = """
        SELECT
            lea.enum_area_uuid AS enum_area_uuid,
            ea.enum_area_name AS name,

            SUM(CASE
                WHEN ei.enumeration_item_enumeration_state IN ('Enumerated', 'Incomplete')
                THEN 1 ELSE 0 END) AS enumerated_count,

            SUM(CASE
                WHEN ei.enumeration_item_enumeration_eligible_for_sampling = 1
                  OR ei.enumeration_item_enumeration_eligible_for_subset_sampling = 1
                THEN 1 ELSE 0 END) AS eligible_count,

            SUM(CASE
                WHEN ei.enumeration_item_sampling_state = 'Sampled'
                  OR ei.enumeration_item_subset_sampling_state = 'Sampled'
                THEN 1 ELSE 0 END) AS sampled_count,

            SUM(CASE
                WHEN ei.enumeration_item_collection_state = 'Complete'
                THEN 1 ELSE 0 END) AS surveyed_count

        FROM enumeration_item ei
        JOIN location l
            ON ei.location_uuid = l.uuid
        JOIN location__enum_area lea
            ON lea.location_uuid = l.uuid
        JOIN enum_area ea
            ON ea.uuid = lea.enum_area_uuid
        WHERE ea.config_uuid = ?
        GROUP BY lea.enum_area_uuid
        ORDER BY ea.creation_date ASC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(configUuid))

        val result = ArrayList<EnumAreaSummary>()

        cursor.use { c ->

            val uuidIdx = c.getColumnIndexOrThrow("enum_area_uuid")
            val nameIdx = c.getColumnIndexOrThrow("name")
            val enumIdx = c.getColumnIndexOrThrow("enumerated_count")
            val eligIdx = c.getColumnIndexOrThrow("eligible_count")
            val sampIdx = c.getColumnIndexOrThrow("sampled_count")
            val survIdx = c.getColumnIndexOrThrow("surveyed_count")

            while (c.moveToNext()) {
                result.add(
                    EnumAreaSummary(
                        uuid = c.getString(uuidIdx),
                        name = c.getString(nameIdx),
                        enumeratedCount = c.getInt(enumIdx),
                        eligibleCount = c.getInt(eligIdx),
                        sampledCount = c.getInt(sampIdx),
                        surveyedCount = c.getInt(survIdx)
                    )
                )
            }
        }

        return result
    }

    fun delete( enumArea: EnumArea )
    {
        enumArea.mapTileRegion?.let {
            DAO.mapTileRegionDAO.delete( it )
        }

        for (vertice in enumArea.vertices)
        {
            DAO.latLonDAO.delete( vertice )
        }

        for (enumerationTeam in enumArea.enumerationTeams)
        {
            DAO.enumerationTeamDAO.deleteTeam( enumerationTeam )
        }

        for (collectionTeam in enumArea.collectionTeams)
        {
            DAO.collectionTeamDAO.deleteTeam( collectionTeam )
        }

        for (location in enumArea.locations)
        {
            DAO.locationDAO.delete( location )
        }

        for (breadcrumb in enumArea.breadcrumbs)
        {
            DAO.breadcrumbDAO.delete( breadcrumb )
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(enumArea.uuid)

        dao.writableDatabase.delete(DAO.TABLE_ENUM_AREA, whereClause, args)
    }

    companion object
    {
        // Note!! CREATION_DATE was moved up to the standard column position
        val columnBindings = listOf(
            ColumnBinding<EnumArea>(COLUMN_CREATION_DATE, "INTEGER",EnumArea::creationDate ),
            ColumnBinding<EnumArea>(COLUMN_VERSION,"TEXT",EnumArea::version ),
            ColumnBinding<EnumArea>(COLUMN_CONFIG_UUID,"TEXT",EnumArea::configUuid ),
            ColumnBinding<EnumArea>(COLUMN_STRATA_UUID,"TEXT",EnumArea::strataUuid ),
            ColumnBinding<EnumArea>(COLUMN_ENUM_AREA_NAME,"TEXT",EnumArea::name ),
            ColumnBinding<EnumArea>(COLUMN_ENUM_AREA_MBTILESPATH,"TEXT",EnumArea::mbTilesPath ),
            ColumnBinding<EnumArea>(COLUMN_ENUM_AREA_MBTILESSIZE,"INTEGER",EnumArea::mbTilesSize ),
            ColumnBinding<EnumArea>(COLUMN_ENUMERATION_TEAM_UUID,"TEXT",EnumArea::selectedEnumerationTeamUuid ),
            ColumnBinding<EnumArea>(COLUMN_COLLECTION_TEAM_UUID,"TEXT",EnumArea::selectedCollectionTeamUuid ),
        )
    }
}