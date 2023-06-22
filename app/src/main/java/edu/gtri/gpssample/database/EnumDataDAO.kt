package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Team
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

class EnumDataDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateEnumData( enumData: EnumData) : EnumData?
    {
        if (exists( enumData ))
        {
            updateEnumData( enumData )
        }
        else
        {
            val values = ContentValues()

            putEnumData( enumData, values )

            enumData.id = dao.writableDatabase.insert(DAO.TABLE_ENUM_DATA, null, values).toInt()
            enumData.id?.let { id ->
                Log.d( "xxx", "new enumData id = ${id}")
                enumData.fieldDataList?.let { fieldDataList ->
                    for (fieldData in fieldDataList)
                    {
                        DAO.fieldDataDAO.createOrUpdateFieldData( fieldData )
                    }
                }
            } ?: return null
        }

        return enumData
    }

    //--------------------------------------------------------------------------
    fun importEnumData( enumData: EnumData ) : EnumData?
    {
        val existingEnumData = getEnumData( enumData.uuid )

        existingEnumData?.let {
            delete( it )
        }

        val values = ContentValues()

        enumData.id = null
//        enumData.notes = ""
//        enumData.valid = false
//        enumData.incomplete = false
        putEnumData( enumData, values )

        enumData.id = dao.writableDatabase.insert(DAO.TABLE_ENUM_DATA, null, values).toInt()
        enumData.id?.let { id ->
            Log.d( "xxx", "new enumData id = ${id}")
            enumData.fieldDataList?.let { fieldDataList ->
                for (fieldData in fieldDataList)
                {
                    fieldData.id = null
                    fieldData.enumDataId = id
                    DAO.fieldDataDAO.createOrUpdateFieldData( fieldData )
                }
            }
        } ?: return null

        return enumData
    }

    //--------------------------------------------------------------------------
    fun exists( enumData: EnumData ): Boolean
    {
        enumData.id?.let { id ->
            getEnumData( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    fun updateEnumData( enumData: EnumData)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(enumData.id!!.toString())
        val values = ContentValues()

        putEnumData( enumData, values )

        db.update(DAO.TABLE_ENUM_DATA, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun putEnumData(enumData: EnumData, values: ContentValues)
    {
        enumData.id?.let { id ->
            Log.d( "xxx", "existing enumData id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, enumData.creationDate )
        values.put( DAO.COLUMN_UUID, enumData.uuid )
        values.put( DAO.COLUMN_USER_ID, enumData.userId )
        values.put( DAO.COLUMN_ENUM_AREA_ID, enumData.enumAreaId )
        values.put( DAO.COLUMN_ENUM_DATA_ENUMERATION_TEAM_ID, enumData.enumerationTeamId )
        values.put( DAO.COLUMN_ENUM_DATA_COLLECTION_TEAM_ID, enumData.collectionTeamId )
        values.put( DAO.COLUMN_ENUM_DATA_COLLECTION_DATA_ID, enumData.collectionDataId )
        values.put( DAO.COLUMN_ENUM_DATA_VALID, enumData.valid.toInt())
        values.put( DAO.COLUMN_ENUM_DATA_INCOMPLETE, enumData.incomplete.toInt())
        values.put( DAO.COLUMN_ENUM_DATA_INCOMPLETE_REASON, enumData.incompleteReason)
        values.put( DAO.COLUMN_ENUM_DATA_NOTES, enumData.notes )
        values.put( DAO.COLUMN_ENUM_DATA_LATITUDE, enumData.latitude )
        values.put( DAO.COLUMN_ENUM_DATA_LONGITUDE, enumData.longitude )
        values.put( DAO.COLUMN_ENUM_DATA_IS_LOCATION, enumData.isLocation.toInt())
        values.put( DAO.COLUMN_ENUM_DATA_DESCRIPTION, enumData.description )
        values.put( DAO.COLUMN_ENUM_DATA_IMAGE_FILE_NAME, enumData.imageFileName )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumData(cursor: Cursor): EnumData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val userId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_ID))
        val enumAreaId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val enumerationTeamId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_ENUMERATION_TEAM_ID))
        val collectionTeamId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_COLLECTION_TEAM_ID))
        val collectionDataId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_COLLECTION_DATA_ID))
        val valid = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_VALID)).toBoolean()
        val incomplete = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_INCOMPLETE)).toBoolean()
        val incompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_INCOMPLETE_REASON))
        val notes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_NOTES))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_LONGITUDE))
        val isLocation = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_IS_LOCATION)).toBoolean()
        val description = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_DESCRIPTION))
        val imageFileName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_IMAGE_FILE_NAME))

        return EnumData( id, creationDate, uuid, userId, enumAreaId, enumerationTeamId, collectionTeamId, collectionDataId, valid, incomplete, incompleteReason, notes, latitude, longitude, isLocation, description, imageFileName, null )
    }

    fun getEnumData( uuid: String ) : EnumData?
    {
        var enumData : EnumData? = null
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumData = createEnumData( cursor )
        }

        cursor.close()

        db.close()

        return enumData
    }

    fun getEnumData( enumArea: EnumArea ) : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()
        val db = dao.writableDatabase

        enumArea.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val enumData = createEnumData( cursor )
                enumData.fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumData )
                enumDataList.add( enumData )
            }

            cursor.close()
        }

        db.close()

        return enumDataList
    }

    fun getEnumData( enumArea: EnumArea, team: Team ) : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()
        val db = dao.writableDatabase

        enumArea.id?.let { enumAreaId ->
            team.id?.let { teamId ->
                var query = ""

                if (team.isEnumerationTeam)
                {
                    query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_ENUM_DATA_ENUMERATION_TEAM_ID} = $teamId"
                }
                else
                {
                    query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_ENUM_DATA_COLLECTION_TEAM_ID} = $teamId"
                }

                val cursor = db.rawQuery(query, null)

                while (cursor.moveToNext())
                {
                    val enumData = createEnumData( cursor )
                    enumData.fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumData )
                    enumDataList.add( enumData )
                }

                cursor.close()
            }
        }

        db.close()

        return enumDataList
    }

    fun getEnumData() : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumData = createEnumData( cursor )
            enumDataList.add( enumData )
        }

        cursor.close()

        db.close()

        return enumDataList
    }

    fun getEnumData( userId: Int, enumAreaId: Int ) : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_USER_ID} = $userId AND ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumDataList.add( createEnumData( cursor ))
        }

        cursor.close()
        db.close()

        return enumDataList
    }

    fun getEnumData( id: Int ) : EnumData?
    {
        var enumData: EnumData? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumData = createEnumData( cursor )
        }

        cursor.close()
        db.close()

        return enumData
    }

    fun delete( enumData: EnumData )
    {
        enumData.id?.let { id ->
            Log.d( "xxx", "deleting enumData with ID $id" )

            DAO.fieldDataDAO.deleteAllFields( enumData )

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_ENUM_DATA, whereClause, args)
            db.close()
        }
    }
}