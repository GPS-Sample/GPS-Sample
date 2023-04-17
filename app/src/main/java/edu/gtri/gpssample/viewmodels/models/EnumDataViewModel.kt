package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.models.EnumData

class EnumDataViewModel
{
    private var _currentEnumData : MutableLiveData<EnumData>? = null

    var currentEnumData : LiveData<EnumData>? = _currentEnumData

    fun setCurrentEnumData(enumData: EnumData)
    {
        _currentEnumData = MutableLiveData(enumData)
        currentEnumData = _currentEnumData
    }
}