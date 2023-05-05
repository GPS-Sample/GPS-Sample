package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Field

class EnumAreaDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateEnumArea( enumArea: EnumArea ) : EnumArea?
    {
        if (exists( enumArea ))
        {
            updateEnumArea( enumArea )
        }
        else
        {
            val values = ContentValues()
            putEnumArea( enumArea, values )
            enumArea.id = dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA, null, values).toInt()

            enumArea.id?.let {id ->
                Log.d( "xxx", "new enumArea id = ${id}")

                for (latLon in enumArea.vertices)
                {
                    latLon.enumAreaId = id
                    DAO.latLonDAO.createOrUpdateLatLon(latLon)
                }

                for (team in enumArea.teams)
                {
                    team.enumAreaId = id
                    DAO.teamDAO.createOrUpdateTeam(team)
                }

                return enumArea
            } ?: return null
        }

        return enumArea
    }

    //--------------------------------------------------------------------------
    fun putEnumArea( enumArea: EnumArea, values: ContentValues )
    {
        enumArea.id?.let { id ->
            Log.d( "xxx", "existing enumArea id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, enumArea.creationDate )
        values.put( DAO.COLUMN_CONFIG_ID, enumArea.config_id )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumArea(cursor: Cursor): EnumArea
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val config_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))

        return EnumArea( id, creationDate, config_id, name )
    }

    //--------------------------------------------------------------------------
    fun exists( enumArea: EnumArea ): Boolean
    {
        enumArea.id?.let { id ->
            getEnumArea( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    fun getEnumArea( id: Int ): EnumArea?
    {
        var enumArea: EnumArea? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            enumArea = createEnumArea( cursor )
        }

        cursor.close()
        db.close()

        return enumArea
    }

    //--------------------------------------------------------------------------
    fun getEnumAreas( config: Config ): ArrayList<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()
        val db = dao.writableDatabase

        config.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val enumArea = createEnumArea( cursor )
                enumArea.id?.let { id ->
                    enumArea.vertices = DAO.latLonDAO.getLatLons( id )
                    enumArea.teams = DAO.teamDAO.getTeams( id )
                    enumAreas.add( enumArea )
                }
            }

            cursor.close()
        }

        db.close()

        return enumAreas
    }

    //--------------------------------------------------------------------------
    fun updateEnumArea( enumArea: EnumArea )
    {
        val db = dao.writableDatabase

        enumArea.id?.let{ id ->
            Log.d( "xxx", "update enumArea id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putEnumArea( enumArea, values )

            db.update(DAO.TABLE_ENUM_AREA, values, whereClause, args )
        }

        db.close()
    }

    //--------------------------------------------------------------------------
//    fun updateTeams(enumArea : EnumArea)
//    {
//        enumArea.id?.let{ id ->
//
//            enumArea.teams = DAO.teamDAO.getTeams( id )
//        }
//    }

    //--------------------------------------------------------------------------
    fun delete( enumArea: EnumArea )
    {
        enumArea.id?.let {enumAreaId ->

            // latLon's are dependent on EnumAreas
            DAO.latLonDAO.getLatLons(enumAreaId).map {
                DAO.latLonDAO.delete( it )
            }

            // enumData's are dependent on EnumAreas
            DAO.enumDataDAO.getEnumData(enumArea).map {
                DAO.enumDataDAO.delete( it )
            }

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(enumAreaId.toString())

            db.delete(DAO.TABLE_ENUM_AREA, whereClause, args)
            db.close()
        }
    }
}