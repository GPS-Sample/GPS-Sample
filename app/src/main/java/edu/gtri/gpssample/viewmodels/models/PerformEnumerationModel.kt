package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PerformEnumerationModel
{
    private var _currentZoomLevel : MutableLiveData<Double>? = null

    var currentZoomLevel : LiveData<Double>? = _currentZoomLevel

    fun setCurrentZoomLevel( zoomLevel: Double )
    {
        _currentZoomLevel = MutableLiveData(zoomLevel)
        currentZoomLevel = _currentZoomLevel
    }
}