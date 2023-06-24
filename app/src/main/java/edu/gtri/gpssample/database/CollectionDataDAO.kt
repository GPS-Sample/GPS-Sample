package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

class CollectionDataDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateCollectionData( collectionData: CollectionData) : CollectionData?
    {
        if (exists( collectionData ))
        {
            updateCollectionData( collectionData )
        }
        else
        {
            val values = ContentValues()

            putCollectionData( collectionData, values )

            collectionData.id = dao.writableDatabase.insert(DAO.TABLE_COLLECTION_DATA, null, values).toInt()

            collectionData.id?.let { id ->
                Log.d( "xxx", "new collectionData id = ${id}")
            } ?: return null
        }

        return collectionData
    }

    fun exists( CollectionData: CollectionData ): Boolean
    {
        CollectionData.id?.let { id ->
            getCollectionData( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    fun updateCollectionData( collectionData: CollectionData)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(collectionData.id!!.toString())
        val values = ContentValues()

        putCollectionData( collectionData, values )

        db.update(DAO.TABLE_COLLECTION_DATA, values, whereClause, args )
        db.close()
    }

    fun putCollectionData( collectionData: CollectionData, values: ContentValues )
    {
        collectionData.id?.let { id ->
            Log.d( "xxx", "existing collectionData id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, collectionData.creationDate )
        values.put( DAO.COLUMN_COLLECTION_DATA_ENUM_DATA_ID, collectionData.enumDataId )
        values.put( DAO.COLUMN_COLLECTION_DATA_INCOMPLETE, collectionData.incomplete.toInt())
        values.put( DAO.COLUMN_COLLECTION_DATA_INCOMPLETE_REASON, collectionData.incompleteReason)
        values.put( DAO.COLUMN_COLLECTION_DATA_NOTES, collectionData.notes )
    }

    @SuppressLint("Range")
    private fun createCollectionData(cursor: Cursor): CollectionData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enumDataId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_DATA_ENUM_DATA_ID))
        val incomplete = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_DATA_INCOMPLETE)).toBoolean()
        val incompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_DATA_INCOMPLETE_REASON))
        val notes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_DATA_NOTES))

        return CollectionData( id, creationDate, enumDataId, incomplete, incompleteReason, notes )
    }

    fun getCollectionData( id: Int ) : CollectionData?
    {
        var collectionData: CollectionData? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_DATA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            collectionData = createCollectionData( cursor )
        }

        cursor.close()
        db.close()

        return collectionData
    }

    fun getCollectionData() : ArrayList<CollectionData>
    {
        var collectionDataList = ArrayList<CollectionData>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_DATA}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val collectionData = createCollectionData( cursor )
            collectionDataList.add( collectionData )
        }

        cursor.close()

        db.close()

        return collectionDataList
    }

    fun delete( collectionData: CollectionData )
    {
        collectionData.id?.let { id ->
            Log.d( "xxx", "deleting collectionData with ID $id" )

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_COLLECTION_DATA, whereClause, args)
            db.close()
        }
    }
}