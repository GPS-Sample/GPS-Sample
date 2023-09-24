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
    private var _isLocationUpdateTimeValid : MutableLiveData<Boolean> = MutableLiveData(false)

    var currentLocation : LiveData<Location>? = _currentLocation
    var currentLocationUpdateTime : LiveData<Date>? = _currentLocationUpdateTime
    var isLocationUpdateTimeValid : LiveData<Boolean> = _isLocationUpdateTimeValid

    fun setIsLocationUpdateTimeValid( value: Boolean )
    {
        _isLocationUpdateTimeValid = MutableLiveData(value)
        isLocationUpdateTimeValid = _isLocationUpdateTimeValid
    }

    fun setCurrentLocation(location: Location)
    {
        _currentLocation = MutableLiveData(location)
        currentLocation = _currentLocation
    }

    fun setCurrentLocationUpdateTime(date: Date?)
    {
        date?.let {
            _currentLocationUpdateTime = MutableLiveData(it)
        } ?: {_currentLocationUpdateTime = null}

        currentLocationUpdateTime = _currentLocationUpdateTime
    }
}