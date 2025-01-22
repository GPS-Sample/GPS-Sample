/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import java.util.*

class LocationViewModel
{
    private var _currentLocation : MutableLiveData<Location>? = null
    private var _currentEnumerationItem : MutableLiveData<EnumerationItem>? = null

    var currentLocation : LiveData<Location>? = _currentLocation
    var currentEnumerationItem : LiveData<EnumerationItem>? = _currentEnumerationItem

    fun setCurrentLocation(location: Location)
    {
        _currentLocation = MutableLiveData(location)
        currentLocation = _currentLocation
    }

    fun setCurrentEnumerationItem(enumerationItem: EnumerationItem)
    {
        _currentEnumerationItem = MutableLiveData(enumerationItem)
        currentEnumerationItem = _currentEnumerationItem
    }
}