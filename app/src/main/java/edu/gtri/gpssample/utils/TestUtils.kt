package edu.gtri.gpssample.utils

import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.LatLon
import java.util.*

object TestUtils
{
    fun enumerateAll( enumArea: EnumArea )
    {
        var count = 1

        val polygon = ArrayList<LatLon>()

        var index = 0

        enumArea.vertices.map {
            polygon.add( LatLon( index++, it.latitude, it.longitude ))
        }

        DAO.instance().writableDatabase.beginTransaction()

        for (location in enumArea.locations)
        {
            val enumerationItem = EnumerationItem()
            enumerationItem.subAddress = count.toString()
            enumerationItem.enumerationDate = Date().time
            enumerationItem.enumerationEligibleForSampling = true
            enumerationItem.syncCode = enumerationItem.syncCode + 1
            enumerationItem.enumerationState = EnumerationState.Enumerated
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
            location.enumerationItems.add(enumerationItem)
            count += 1
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
    }

    fun surveyAll( enumArea: EnumArea )
    {
        val polygon = ArrayList<LatLon>()

        var index = 0

        enumArea.vertices.map {
            polygon.add( LatLon( index++, it.latitude, it.longitude ))
        }

        DAO.instance().writableDatabase.beginTransaction()

        for (location in enumArea.locations)
        {
            for (enumerationItem in location.enumerationItems)
            {
                enumerationItem.collectionDate = Date().time
                enumerationItem.syncCode = enumerationItem.syncCode + 1
                enumerationItem.collectionState = CollectionState.Complete
                enumerationItem.samplingState = SamplingState.Sampled
                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
            }
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
    }
}