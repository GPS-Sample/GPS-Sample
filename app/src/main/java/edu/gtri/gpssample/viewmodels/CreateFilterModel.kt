/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter

class CreateFilterModel
{
    private var _currentFilter : MutableLiveData<Filter>? = null
    var currentFilter : LiveData<Filter>? = _currentFilter
    val createFilterAdapter = CreateFilterAdapter(listOf<Rule>())

    fun createNewFilter()
    {
        val newFilter = Filter("","" )
        _currentFilter = MutableLiveData(newFilter)
        currentFilter = _currentFilter
    }

    fun addPrimaryFilter(study : Study)
    {
        currentFilter?.value?.let { filter ->
            if (!study.filters.contains( filter ))
            {
                study.filters.add(filter)
            }
        }
    }

    fun addSubsetFilter(study : Study)
    {
        currentFilter?.value?.let { filter ->
            if (!study.subsetFilters.contains( filter ))
            {
                study.subsetFilters.add(filter)
            }
        }
    }

    fun setSelectedFilter(filter: Filter)
    {
        _currentFilter = MutableLiveData(filter)
        currentFilter = _currentFilter
        createFilterAdapter.updateRules(filter.rule)

    }

    fun deleteSelectedFilter( study: Study )
    {
        _currentFilter?.value?.let { filter ->
            filter.rule?.let { rule ->
                if (rule.isSubsetRule)
                {
                    study.subsetFilters.remove(filter)
                }
                else
                {
                    study.filters.remove(filter)
                }
            }
            DAO.filterDAO.deleteFilter(filter)
            _currentFilter = null
        }
    }
}