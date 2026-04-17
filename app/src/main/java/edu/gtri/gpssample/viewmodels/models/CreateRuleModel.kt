/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels.models

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study

class CreateRuleModel {
    private var _ruleFieldPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _ruleOperatorPosition : MutableLiveData<Int> = MutableLiveData(0)

    private var _dropdownPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _currentRule : MutableLiveData<Rule>? = null

    var fragment : Fragment? = null
    var currentRule : LiveData<Rule>? = _currentRule
    val ruleFieldPosition : MutableLiveData<Int>
        get() = _ruleFieldPosition

    val ruleOperationPosition : MutableLiveData<Int>
        get() = _ruleOperatorPosition

    val dropdownPosition : MutableLiveData<Int>
        get() = _dropdownPosition

    val operators : Array<String>
        get(){
            val englishArray = OperatorConverter.array
            fragment?.let { fragment ->

                val array: Array<String> = Array(englishArray.size)
                { i ->
                    when (i) {
                        0 -> fragment.getString(R.string.equal)
                        1 -> fragment.getString(R.string.not_equal)
                        2 -> fragment.getString(R.string.less_than)
                        3 -> fragment.getString(R.string.greater_than)
                        4 -> fragment.getString(R.string.less_than_equal)
                        5 -> fragment.getString(R.string.greater_than_equal)
                        6 -> fragment.getString(R.string.contains)
                        else -> String()
                    }
                }
                return array
            }
            return englishArray
        }

    fun addPrimaryRule( study : Study )
    {
        currentRule?.value?.let { rule ->
            // rule.studyId = id
            // DAO.ruleDAO.createOrUpdateRule( rule )
            if (!study.primaryRules.contains(rule))
            {
                study.primaryRules.add(rule)
            }
        }
    }

    fun addSubsetRule( study : Study )
    {
        currentRule?.value?.let { rule ->
            // rule.studyId = id
            // DAO.ruleDAO.createOrUpdateRule( rule )
            if (!study.subsetRules.contains(rule))
            {
                study.subsetRules.add(rule)
            }
        }
    }

    fun setSelectedRule(rule : Rule)
    {
        _currentRule = MutableLiveData(rule)
        currentRule = _currentRule
       // _ruleOperatorPosition.value =  OperatorConverter.toArrayPosition(rule.operator)
    }

    fun deleteSelectedRule(study : Study)
    {
        _currentRule?.value?.let { rule ->
            if (rule.isSubsetRule)
            {
                study.subsetRules.remove( rule )
            }
            else
            {
                study.primaryRules.remove(rule )
            }
            DAO.ruleDAO.deleteRule(rule)
        }
    }

//    fun deleteRule(rule:Rule, study : Study)
//    {
//        study.rules.remove(rule)
//        DAO.ruleDAO.deleteRule(rule)
//    }

    fun createNewPrimaryRule(study: Study) : Boolean
    {
        val newRule = Rule()
        newRule.isSubsetRule = false
        _currentRule = MutableLiveData(newRule)
        currentRule = _currentRule

        return true
    }

    fun createNewSubsetRule(study: Study) : Boolean
    {
        val newRule = Rule()
        newRule.isSubsetRule = true
        _currentRule = MutableLiveData(newRule)
        currentRule = _currentRule

        return true
    }

    fun onRuleOperatorSelected(study : Study, position: Int)
    {
        currentRule?.value?.let{rule ->
            rule.operator = OperatorConverter.fromIndex(position)
        }
    }
}