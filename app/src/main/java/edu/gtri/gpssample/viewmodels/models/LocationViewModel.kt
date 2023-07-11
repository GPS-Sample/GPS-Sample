package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.models.Location

class LocationViewModel
{
    private var _currentLocation : MutableLiveData<Location>? = null

    var currentLocation : LiveData<Location>? = _currentLocation

    fun setCurrentLocation(location: Location)
    {
        _currentLocation = MutableLiveData(location)
        currentLocation = _currentLocation
    }
}