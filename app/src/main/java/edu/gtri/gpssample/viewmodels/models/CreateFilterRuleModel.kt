package edu.gtri.gpssample.viewmodels.models

import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterOperator
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import java.util.*
import edu.gtri.gpssample.utils.FilterUtils

class CreateFilterRuleModel {

    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentRule : MutableLiveData<Rule>? = null
    private var _secondRule : MutableLiveData<Rule>? = null
    private var currentConnector : Connector = Connector.NONE

    private var _currentFilter : MutableLiveData<Filter>? = null
//    private var _currentFilterRule : MutableLiveData<FilterRule>? = null
    private var _ruleFieldPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _secondRuleFieldPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _ruleConditionPosition : MutableLiveData<Int> = MutableLiveData(0)

    private var _createFilterAdapter : CreateFilterAdapter? = null

    private var _firstStringRuleList : MutableLiveData<ArrayList<String>> = MutableLiveData(ArrayList<String>())

    private var allRules = ArrayList<String>()
   // private var secondRules = ArrayList<String>()

    //private var _stringRuleList : MutableLiveData<ArrayList>

    var currentRule : LiveData<Rule>? = null
            get() = _currentRule

    var createFilterAdapter : CreateFilterAdapter?
        get() = _createFilterAdapter
        set(value)
        {
            _createFilterAdapter = value
        }

//    var currentFilterRule : LiveData<FilterRule>? = _currentFilterRule
    var filterHasRules : ObservableBoolean = ObservableBoolean(false)
        get (){
            _currentFilter?.value?.let{filter->
                filter.rule?.let{rule->
                    return ObservableBoolean(true)
                }
            }
            return ObservableBoolean(false)
        }

    val studyHasMultipleRules : ObservableBoolean
        get (){
            _currentStudy?.value?.let{study->
                if(study.rules.size > 1)
                {
                    return ObservableBoolean(true)
                }
            }
            return ObservableBoolean(false)
        }

    val ruleFieldPosition : MutableLiveData<Int>
        get() = _ruleFieldPosition
    val secondRuleFieldPosition : MutableLiveData<Int>
        get() = _secondRuleFieldPosition

    val ruleConditionPosition : MutableLiveData<Int>
        get() = _ruleConditionPosition
//
    val connectors : Array<String>
        get() {
            return ConnectorConverter.array
        }


    private lateinit var ruleList : Array<Rule>
       // get() = getRules()

//    val secondRuleList : Array<Rule>
//        get() = getSecondRules()

    val firstStringRuleList : MutableLiveData<ArrayList<String>>
        get(){

            return _firstStringRuleList
        }

    fun createNewFilterRule(filter : Filter, study : Study)
    {

        _createFilterAdapter?.updateRules(null)
        _currentFilter?.let{ filterObservable->
            filterObservable.value = filter
            filterObservable.postValue(filter)
        } ?: run { _currentFilter = MutableLiveData(filter) }

        _currentStudy?.let{ studyObservable->
            studyObservable.value = study
            studyObservable.postValue(study)
        } ?: run { _currentStudy = MutableLiveData(study) }


        // this list needs to be a copy every time.
        ruleList = getRules()

        // set the current rule to the first one
        allRules.clear()
       // secondRules.clear()
        _currentFilter?.value?.let { filter ->

            val rule = FilterUtils.findLastRule(filter)
            rule?.let{rule ->

                _currentRule = MutableLiveData(rule)
                val index = ruleList.indexOf(rule)
                setupSecondRuleList(index)

            } ?: run{
                _currentRule = MutableLiveData(ruleList[0])
                _ruleFieldPosition = MutableLiveData(0)
                if(ruleList.size > 1)
                {
                    _secondRule = MutableLiveData(ruleList[1])
                    _secondRuleFieldPosition = MutableLiveData(1)
                }
            }
            Log.d("xxxxxx", "ummmmmm")
        }
        // build list of rules
        for (rule in ruleList)
        {
            allRules.add(rule.name)

        }
        _firstStringRuleList.value = allRules
        _firstStringRuleList.postValue(allRules)
    }

    fun addFilterRule(filter : Filter)
    {
        Log.d("xxxxx", "test")

        // stitch it all together
        _currentRule?.value?.let{currentRule ->
            // we save the first rule
            if(filter.rule == null)
            {
                // do we find where we are in the stack?
                // if we are here, then this is the first rule
                filter.rule = currentRule
            }

                // walk down the tree of rule -> connector -> rule
                // the first rule in the UI is the last rule in the list normally,

            _secondRule?.value?.let{secondRule ->
                Log.d("xxxxx", "we can stitch together")
                // build operator
                val operator = FilterOperator(null, 0, currentConnector, secondRule)
                // add add operator to first rule
                currentRule.filterOperator = operator
            }
            filter.rule?.let{rule ->
                _createFilterAdapter?.updateRules(rule)
            }


        }

    }

    fun setupFirstRuleList(newPosition : Int)
    {
        _currentRule?.value?.let{firstRule ->
            _secondRule?.value?.let{secondRule ->
                if(firstRule == secondRule)
                {

                    val newIndex = (newPosition + 1 ) % ruleList.size
                    _currentRule = MutableLiveData(ruleList[newIndex])

                    _ruleFieldPosition.value = newIndex
                    _ruleFieldPosition.postValue(newIndex)
                }
            }
        }
    }

    fun setupSecondRuleList(newPosition : Int)
    {

        _currentRule?.value?.let{firstRule ->
            _secondRule?.value?.let{secondRule ->
                if(firstRule == secondRule)
                {

                    val newIndex = (newPosition + 1 ) % ruleList.size
                    _secondRule = MutableLiveData(ruleList[newIndex])

                    _secondRuleFieldPosition.value = newIndex
                    _secondRuleFieldPosition.postValue(newIndex)
                }
            }
        }

    }

    fun onFirstRuleFieldSelected (study : Study, position : Int)
    {

        _currentRule?.let{liveRule->
            liveRule.value = ruleList[position]
            liveRule.postValue(ruleList[position])


        } ?: run{
            _currentRule = MutableLiveData(ruleList[position])
        }
        setupSecondRuleList(position)


    }

    fun onSecondRuleFieldSelected (study : Study, position : Int)
    {
        _secondRule?.let{liveRule->
            val selected = ruleList[position]
            liveRule.value = selected
            liveRule.postValue(selected)
        } ?: run{
            _secondRule = MutableLiveData(ruleList[position])
        }
        setupFirstRuleList(position)
    }


    private fun getRules() : Array<Rule>
    {
        val ruleList = ArrayList<Rule>()
        _currentStudy?.value?.rules?.let { rules ->
            for (rule in rules)
            {
                val ruleCopy = rule.copy()
                ruleCopy?.let{ruleCopy->
                    ruleList.add( ruleCopy )
                }
            }
        }
        return ruleList.toTypedArray()
    }


//    private fun getSecondRules() : Array<Rule>
//    {
//        val ruleList = ArrayList<Rule>()
//        _currentStudy?.value?.rules?.let { rules ->
//            _currentRule?.value?.let{currentRule->
//                for (rule in rules)
//                {
//                    ruleList.add( rule )
//                }
//            }
//        }
//
//        return ruleList.toTypedArray()
//    }
//
    fun onFilterConnectorFieldSelected (parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        currentConnector = ConnectorConverter.fromString(ConnectorConverter.array[position])

    }
}