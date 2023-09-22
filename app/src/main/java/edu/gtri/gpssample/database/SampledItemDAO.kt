package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.models.*

class SampledItemDAO(private var dao: DAO)
{
    fun createOrUpdateSampledItem( sampledItem: SampledItem ) : SampledItem?
    {
        if (exists( sampledItem ))
        {
            updateSampledItemItem( sampledItem )
        }
        else
        {
            val values = ContentValues()

            putSampledItem( sampledItem, values )

            sampledItem.id = dao.writableDatabase.insert(DAO.TABLE_SAMPLED_ITEM, null, values).toInt()
            sampledItem.id?.let { id ->
                Log.d( "xxx", "new SampledItem id = ${id}")
            } ?: return null
        }

        return sampledItem
    }

    fun exists( sampledItem: SampledItem ): Boolean
    {
        sampledItem.id?.let { id ->
            getSampledItem( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    fun updateSampledItemItem( sampledItem: SampledItem )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(sampledItem.id!!.toString())
        val values = ContentValues()

        putSampledItem( sampledItem, values )

        db.update(DAO.TABLE_SAMPLED_ITEM, values, whereClause, args )
        db.close()
    }

    fun putSampledItem( sampledItem: SampledItem, values: ContentValues )
    {
        sampledItem.id?.let { id ->
            Log.d( "xxx", "existing SampledItem id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        sampledItem.location?.id?.let { id ->
            values.put( DAO.COLUMN_LOCATION_ID, id )
        }

        sampledItem.enumerationItem?.id?.let { id ->
            values.put( DAO.COLUMN_ENUMERATION_ITEM_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, sampledItem.creationDate )
        values.put( DAO.COLUMN_SAMPLED_ITEM_SAMPLING_STATE, sampledItem.samplingState.format )
    }

    @SuppressLint("Range")
    private fun createSampledItem(cursor: Cursor): SampledItem
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val samplingState = SamplingState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_SAMPLED_ITEM_SAMPLING_STATE)))
        val locationId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_ID))
        val enumerationItemId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_ID))

        val location = DAO.locationDAO.getLocation( locationId )
        val enumerationItem = DAO.enumerationItemDAO.getEnumerationItem( enumerationItemId )

        return SampledItem( id, creationDate, location, enumerationItem, samplingState )
    }

    fun getSampledItem( id: Int ) : SampledItem?
    {
        var sampledItem: SampledItem? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_SAMPLED_ITEM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            sampledItem = createSampledItem( cursor )
        }

        cursor.close()
        db.close()

        return sampledItem
    }

    fun getSampledItems( location: Location ) : ArrayList<SampledItem>
    {
        val sampledItems = ArrayList<SampledItem>()
        val db = dao.writableDatabase

        location.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_SAMPLED_ITEM} WHERE ${DAO.COLUMN_LOCATION_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val sampledItem = createSampledItem( cursor )
                sampledItems.add( sampledItem )
            }

            cursor.close()
        }

        db.close()

        return sampledItems
    }

    fun getSampledItems() : ArrayList<SampledItem>
    {
        val sampledItems = ArrayList<SampledItem>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_SAMPLED_ITEM}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val sampledItem = createSampledItem( cursor )
            sampledItems.add( sampledItem )
        }

        cursor.close()

        db.close()

        return sampledItems
    }

    fun delete( sampledItem: SampledItem )
    {
        sampledItem.id?.let { id ->
            Log.d( "xxx", "deleting SampledItem with ID $id" )

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_SAMPLED_ITEM, whereClause, args)
            db.close()
        }
    }
}