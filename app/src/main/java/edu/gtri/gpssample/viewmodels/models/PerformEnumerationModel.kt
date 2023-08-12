package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PerformEnumerationModel
{
    private var _currentZoomLevel : MutableLiveData<Float>? = null

    var currentZoomLevel : LiveData<Float>? = _currentZoomLevel

    fun setCurrentZoomLevel( zoomLevel: Float )
    {
        _currentZoomLevel = MutableLiveData(zoomLevel)
        currentZoomLevel = _currentZoomLevel
    }
}