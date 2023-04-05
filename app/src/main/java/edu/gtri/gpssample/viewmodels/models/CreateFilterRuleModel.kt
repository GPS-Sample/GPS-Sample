package edu.gtri.gpssample.viewmodels.models

import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import java.util.*

class CreateFilterRuleModel {
    private var _currentFilterRule : MutableLiveData<FilterRule>? = null
    private var _ruleFieldPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _ruleConditionPosition : MutableLiveData<Int> = MutableLiveData(0)

    var currentFilterRule : LiveData<FilterRule>? = _currentFilterRule

    val ruleFieldPosition : MutableLiveData<Int>
        get() = _ruleFieldPosition

    val ruleConditionPosition : MutableLiveData<Int>
        get() = _ruleConditionPosition

    val connectors : Array<String>
        get() {
            return ConnectorConverter.array
        }

    fun createNewFilterRule()
    {
        _currentFilterRule = MutableLiveData(FilterRule(UUID.randomUUID().toString(),-1))
        currentFilterRule = _currentFilterRule
    }

    fun addFilterRule(filter : Filter)
    {
        currentFilterRule?.value?.let { filterRule ->
            filterRule.rule?.let { rule ->
                if (filterRule.connector != Connector.None) {
                    filter.filterRules.add(filterRule)
                }
            }
        }
    }

    fun onFilterRuleFieldSelected (study : Study, position : Int)
    {
        currentFilterRule?.value?.let{filterRule ->
            filterRule.rule = study.rules[position]
        }
    }

    fun onFilterConnectorFieldSelected (parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        currentFilterRule?.value?.let { filterRule ->
            val connector = ConnectorConverter.array[position]
            filterRule.connector = ConnectorConverter.fromString(connector)

        }
    }
}