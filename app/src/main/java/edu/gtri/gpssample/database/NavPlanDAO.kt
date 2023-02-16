package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.NavPlan
import edu.gtri.gpssample.database.models.Sample

class NavPlanDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createNavPlan( navPlan: NavPlan ) : Int
    {
        val values = ContentValues()

        putNavPlan( navPlan, values )

        return dao.writableDatabase.insert(DAO.TABLE_NAV_PLAN, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putNavPlan(navPlan: NavPlan, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, navPlan.uuid )
        values.put( DAO.COLUMN_NAV_PLAN_SAMPLE_UUID, navPlan.sample_uuid )
        values.put( DAO.COLUMN_NAV_PLAN_NAME, navPlan.name )
    }

    //--------------------------------------------------------------------------
    private fun createNavPlan( cursor: Cursor): NavPlan
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val sample_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_NAV_PLAN_SAMPLE_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_NAV_PLAN_NAME))

        return NavPlan( uuid, sample_uuid, name )
    }

    //--------------------------------------------------------------------------
    fun updateNavPlan( navPlan: NavPlan )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(navPlan.uuid)
        val values = ContentValues()

        putNavPlan( navPlan, values )

        db.update(DAO.TABLE_NAV_PLAN, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getNavPlan( uuid: String ): NavPlan?
    {
        var navPlan: NavPlan? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_NAV_PLAN} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            navPlan = createNavPlan( cursor )
        }

        cursor.close()
        db.close()

        return navPlan
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getNavPlan( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getNavPlans( sample_uuid: String ): List<NavPlan>
    {
        val navPlans = ArrayList<NavPlan>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_NAV_PLAN} WHERE ${DAO.COLUMN_NAV_PLAN_SAMPLE_UUID} = '$sample_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            navPlans.add( createNavPlan( cursor ))
        }

        cursor.close()
        db.close()

        return navPlans
    }

    //--------------------------------------------------------------------------
    fun getNavPlans(): List<NavPlan>
    {
        val navPlans = ArrayList<NavPlan>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_NAV_PLAN}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            navPlans.add( createNavPlan( cursor ))
        }

        cursor.close()
        db.close()

        return navPlans
    }

    //--------------------------------------------------------------------------
    fun deleteNavPlan( navPlan: NavPlan )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(navPlan.uuid.toString())

        db.delete(DAO.TABLE_NAV_PLAN, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
        val navPlans = getNavPlans()

        for (navPlan in navPlans)
        {
            if (DAO.sampleDAO.doesNotExist( navPlan.sample_uuid ))
            {
                deleteNavPlan( navPlan )
            }
        }
    }
}