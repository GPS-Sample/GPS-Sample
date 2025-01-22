/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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

        var creationDate = Date().time

        enumArea.vertices.map {
            polygon.add( LatLon( creationDate++, it.latitude, it.longitude ))
        }

        DAO.instance().writableDatabase.beginTransaction()

        for (location in enumArea.locations)
        {
            if (location.enumerationItems.isEmpty())
            {
                location.enumerationItems.add( EnumerationItem())
            }

            for (enumerationItem in location.enumerationItems)
            {
                enumerationItem.subAddress = count.toString()
                enumerationItem.enumerationDate = Date().time
                enumerationItem.enumerationEligibleForSampling = true
                enumerationItem.syncCode = enumerationItem.syncCode + 1
                enumerationItem.enumerationState = EnumerationState.Enumerated
                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
                count += 1
            }
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()
    }

    fun surveyAll( enumArea: EnumArea )
    {
        val polygon = ArrayList<LatLon>()

        var creationDate = Date().time

        enumArea.vertices.map {
            polygon.add( LatLon( creationDate++, it.latitude, it.longitude ))
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