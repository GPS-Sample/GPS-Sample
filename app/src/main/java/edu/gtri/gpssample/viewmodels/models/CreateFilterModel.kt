package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.models.Filter
import java.util.*

class CreateFilterModel {
    private var _currentFilter : MutableLiveData<Filter>? = null

    var currentFilter : LiveData<Filter>? = _currentFilter
    fun createNewStudy()
    {
        val newFilter = Filter( UUID.randomUUID().toString(), -1,"", -1, 0 )
        _currentFilter = MutableLiveData(newFilter)
        currentFilter = _currentFilter
    }
    fun deleteCurrentFilter()
    {
        _currentFilter = null
    }
}