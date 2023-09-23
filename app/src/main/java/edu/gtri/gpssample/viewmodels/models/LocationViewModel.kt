package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Location
import java.util.*

class LocationViewModel
{
    private var _currentLocation : MutableLiveData<Location>? = null
    private var _currentLocationUpdateTime : MutableLiveData<Date>? = null

    var currentLocation : LiveData<Location>? = _currentLocation
    var currentLocationUpdateTime : LiveData<Date>? = _currentLocationUpdateTime

    fun setCurrentLocation(location: Location)
    {
        _currentLocation = MutableLiveData(location)
        currentLocation = _currentLocation

        _currentLocationUpdateTime = MutableLiveData(Date())
        currentLocationUpdateTime = _currentLocationUpdateTime

    }

    fun removeCurrentLocation(location: Location)
    {
        _currentLocation = null
        currentLocation = _currentLocation
    }
}