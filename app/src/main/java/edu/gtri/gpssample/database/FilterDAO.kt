package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.extensions.toBoolean

class FilterDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilter( filter: Filter ) : Int
    {
        val values = ContentValues()

        putFilter( filter, values )

        return dao.writableDatabase.insert(DAO.TABLE_FILTER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putFilter( filter: Filter, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, filter.uuid )
        values.put( DAO.COLUMN_FILTER_STUDY_UUID, filter.study_id )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_SAMPLE_SIZE, filter.sampleSize )
        values.put( DAO.COLUMN_FILTER_SAMPLE_SIZE_INDEX, filter.sampleSizeIndex )
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(filter.uuid.toString())
        val values = ContentValues()

        putFilter( filter, values )

        db.update(DAO.TABLE_FILTER, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getFilter( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getFilter( uuid: String ) : Filter?
    {
        var filter: Filter? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            filter = buildFilter( cursor )
        }

        cursor.close()
        db.close()

        return filter
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildFilter(cursor: Cursor ): Filter
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE))
        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE_INDEX))

        return Filter( id, uuid, study_id, name, sampleSize, sampleSizeIndex )
    }

    //--------------------------------------------------------------------------
    fun getFilters( study_id: Int ): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_FILTER_STUDY_UUID} = '$study_id'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filters.add( buildFilter( cursor ))
        }

        cursor.close()
        db.close()

        return filters
    }

    //--------------------------------------------------------------------------
    fun getFilters(): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filters.add( buildFilter( cursor ))
        }

        cursor.close()
        db.close()

        return filters
    }

    //--------------------------------------------------------------------------
    fun deleteFilter( filter: Filter )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(filter.uuid)

        db.delete(DAO.TABLE_FILTER, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
//        val filters = getFilters()
//
//        for (filter in filters)
//        {
//            if (DAO.studyDAO.doesNotExist( filter.study_uuid ))
//            {
//                deleteFilter( filter )
//            }
//        }
    }
}