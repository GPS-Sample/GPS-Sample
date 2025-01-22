/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels.models

import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Filter
//import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
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

    val createFilterAdapter = CreateFilterAdapter(listOf<Rule>())
    fun createNewFilter()
    {
        val newFilter = Filter("" )
        _currentFilter = MutableLiveData(newFilter)
        currentFilter = _currentFilter
    }

    fun addFilter(study : Study)
    {
        currentFilter?.value?.let { filter ->
           // DAO.filterDAO.createOrUpdateFilter(filter)
            if (!study.filters.contains( filter ))
            {
                study.filters.add(filter)
            }
        }
    }

    fun setSelectedFilter(filter: Filter)
    {
        _currentFilter = MutableLiveData(filter)
        currentFilter = _currentFilter
        createFilterAdapter.updateRules(filter.rule)

    }

    fun addFilterRule(order : Int, rule : Rule, connector : Connector)
    {
        _currentFilter?.value?.let{filter ->
            //val count
        }
    }

    fun deleteSelectedFilter( study: Study )
    {
        _currentFilter?.value?.let { filter ->

            study.filters.remove(filter)
            DAO.filterDAO.deleteFilter(filter)
            _currentFilter = null
        }
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