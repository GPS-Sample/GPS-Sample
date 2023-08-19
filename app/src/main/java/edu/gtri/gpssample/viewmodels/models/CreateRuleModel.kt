package edu.gtri.gpssample.viewmodels.models

import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.Operator
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import java.util.*

class CreateRuleModel {
    private var _ruleFieldPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _ruleOperatorPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _currentRule : MutableLiveData<Rule>? = null

    var fragment : Fragment? = null
    var currentRule : LiveData<Rule>? = _currentRule
    val ruleFieldPosition : MutableLiveData<Int>
        get() = _ruleFieldPosition

    val ruleOperationPosition : MutableLiveData<Int>
        get() = _ruleOperatorPosition

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
                        4 -> fragment.getString(R.string.greater_than_equal)
                        else -> String()
                    }
                }
                return array
            }
            return englishArray

        }



    fun addRule(study : Study)
    {
        //study.id?.let { id ->
            currentRule?.value?.let { rule ->
               // rule.studyId = id
               // DAO.ruleDAO.createOrUpdateRule( rule )
                if(!study.rules.contains(rule))
                {
                    study.rules.add(rule)
                }
            }
       // }
    }
    fun setSelectedRule(rule : Rule)
    {
        _currentRule = MutableLiveData(rule)
        currentRule = _currentRule
       // _ruleOperatorPosition.value =  OperatorConverter.toArrayPosition(rule.operator)
    }

    fun deleteSelectedRule(study : Study)
    {
        _currentRule?.value?.let{rule ->
            study.rules.remove(rule)
            DAO.ruleDAO.deleteRule(rule)
        }
    }

    fun deleteRule(rule:Rule, study : Study)
    {

            study.rules.remove(rule)
            DAO.ruleDAO.deleteRule(rule)
    }

    fun createNewRule() : Boolean
    {
        val newRule = Rule(null,  null, "", "" , Operator.None, null)
        _currentRule = MutableLiveData(newRule)
        currentRule = _currentRule

        return true
    }

    fun onRuleFieldSelected(study : Study, position: Int,)
    {
        currentRule?.value?.let{ rule ->
            val field = study.fields[position]
            rule.field = field
        }
    }

    fun onRuleOperatorSelected(study : Study, position: Int,)
    {
        currentRule?.value?.let{rule ->
            rule.operator = OperatorConverter.fromArrayPosition(position)
        }
    }
}