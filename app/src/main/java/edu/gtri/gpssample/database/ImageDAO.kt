package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import edu.gtri.gpssample.database.DAO.Companion.TABLE_CONFIG
import edu.gtri.gpssample.database.models.Image
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.database.models.MapTileRegion
import edu.gtri.gpssample.utils.CameraUtils

class ImageDAO(private var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    override fun onCreate( db: SQLiteDatabase )
    {
        try {
            val createTableImage = ("CREATE TABLE " +
                    TABLE_IMAGE + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_LOCATION_UUID + " TEXT" + "," +
                    COLUMN_DATA + " TEXT" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableImage)
        }
        catch(ex: Exception)
        {
            Log.d("xxx", ex.stackTraceToString())
        }
    }

    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int )
    {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGE")
        onCreate(db)
    }

    fun createImage( image: Image ) : Image?
    {
        val values = ContentValues()
        putImage( image, values )
        if (this.writableDatabase.insert( TABLE_IMAGE, null, values ) < 0)
        {
            return null
        }
        return image
    }

    fun putImage( image: Image, values: ContentValues )
    {
        values.put( COLUMN_UUID, image.uuid )
        values.put( COLUMN_CREATION_DATE, image.creationDate )
        values.put( COLUMN_LOCATION_UUID, image.locationUuid )
        values.put( COLUMN_DATA, image.data )
    }

    @SuppressLint("Range")
    private fun buildImage(cursor: Cursor) : Image
    {
        val uuid = cursor.getString(cursor.getColumnIndex(COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(COLUMN_CREATION_DATE))
        val locationUuid = cursor.getString(cursor.getColumnIndex(COLUMN_LOCATION_UUID))
        val data = cursor.getString(cursor.getColumnIndex(COLUMN_DATA))

        return Image( uuid, creationDate, locationUuid, data )
    }

    fun getImage( uuid: String ): Image?
    {
        var image: Image? = null
        val query = "SELECT * FROM ${TABLE_IMAGE} WHERE ${COLUMN_UUID} = '$uuid'"
        val cursor = writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            image = buildImage( cursor )
        }

        cursor.close()

        return image
    }

    fun getImage( location: Location): Image?
    {
        if (location.imageUuid.isNotEmpty())
        {
            return getImage( location.imageUuid )
        }

        return null
    }

    fun delete( image: Image )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(image.uuid)

        writableDatabase.delete(DAO.TABLE_MAP_TILE_REGION, whereClause, args)
    }

    companion object
    {
        const val TABLE_IMAGE = "image"
        const val COLUMN_UUID = "uuid"
        const val COLUMN_UUID_TYPE = " TEXT PRIMARY KEY NOT NULL"
        const val COLUMN_CREATION_DATE = "creation_date"
        const val COLUMN_LOCATION_UUID = "location_uuid"
        const val COLUMN_DATA = "data"

        private var _instance: ImageDAO? = null

        fun instance() : ImageDAO
        {
            return _instance!!
        }

        fun createSharedInstance( context: Context ): ImageDAO
        {
            if (_instance == null)
            {
                _instance = ImageDAO( context, null, null, DATABASE_VERSION )
            }

            return _instance!!
        }

        fun deleteAll()
        {
            _instance?.let {
                it.writableDatabase.execSQL("DELETE FROM $TABLE_IMAGE")
            }
        }

        private const val DATABASE_NAME = "GPSSampleImageDB.db"

        private const val DATABASE_VERSION = 2
    }
}