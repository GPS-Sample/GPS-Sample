package edu.gtri.gpssample.viewmodels.models

import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Rule
import java.util.*

class CreateFilterModel {
    private var _currentFilter : MutableLiveData<Filter>? = null
    private var _samplingTypePosition : MutableLiveData<Int> = MutableLiveData(0)

    val samplingTypePosition : MutableLiveData<Int>
        get() = _samplingTypePosition

    var currentFilter : LiveData<Filter>? = _currentFilter

    var currentSampleSize : String
        get(){
            currentFilter?.value?.let{filter ->
                return filter.sampleSize.toString()
            }
            return ""
        }
        set(value){
            currentFilter?.value?.let{filter ->
                value.toIntOrNull()?.let {size ->
                    filter.sampleSize = size
                } ?: run{ filter.sampleSize = 0
                }
            }
        }

    fun createNewFiler()
    {
        val newFilter = Filter( UUID.randomUUID().toString(), -1,"", -1, 0 )
        _currentFilter = MutableLiveData(newFilter)
        currentFilter = _currentFilter
    }

    fun addFilterRule(order : Int, rule : Rule, connector : Connector)
    {
        _currentFilter?.value?.let{filter ->
            //val count
        }


    }

    fun deleteCurrentFilter()
    {
        _currentFilter = null
    }

    fun onSampleTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > SampleTypeConverter.array.size)
        {
            val sampleType : String = SampleTypeConverter.array[position]
            _currentFilter?.value?.let {
                it.samplingType = SampleTypeConverter.fromString(sampleType)
            }
        }

    }

}