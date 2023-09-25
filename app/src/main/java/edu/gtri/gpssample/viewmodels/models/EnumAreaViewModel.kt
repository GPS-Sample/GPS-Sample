package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea

class EnumAreaViewModel
{
    private var _currentEnumArea : MutableLiveData<EnumArea>? = null

    var currentEnumArea : LiveData<EnumArea>? = _currentEnumArea

    fun setCurrentEnumArea(enumArea: EnumArea)
    {
        _currentEnumArea = MutableLiveData(enumArea)
        currentEnumArea = _currentEnumArea
    }
}