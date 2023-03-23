package edu.gtri.gpssample.viewmodels

import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.database.DAO

import java.util.*
import kotlin.collections.ArrayList
import edu.gtri.gpssample.R

import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*

class ConfigurationViewModel : ViewModel()
{
    private val unavailable = "Unavailable"
    private var _configurations : ArrayList<Config> = ArrayList()

    private var _distanceFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _timeFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _dateFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _samplingMethodPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _samplingTypePosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _fieldTypePosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _ruleFieldPosition : MutableLiveData<Int> = MutableLiveData(0)

    // live data for each screen being controlled by the view model
    private var _currentConfiguration : MutableLiveData<Config>? = null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentField : MutableLiveData<Field>? = null
    private var _currentFilter : MutableLiveData<Filter>? = null
    private var _currentRule : MutableLiveData<Rule>? = null

    // Exposed LiveData each screen being controlled by the view model
    var currentConfiguration : LiveData<Config>? =_currentConfiguration
    var currentStudy : LiveData<Study>? = _currentStudy
    var currentField : LiveData<Field>? = _currentField
    var currentFilter : LiveData<Filter>? = _currentFilter
    var currentRule : LiveData<Rule>? = _currentRule

    val configurations : ArrayList<Config>
        get() = _configurations

    val distanceFormats : Array<String>
        get() = DistanceFormatConverter.array

    val timeFormats : Array<String>
        get() = TimeFormatConverter.array

    val dateFormats : Array<String>
        get() = DateFormatConverter.array

    val samplingMethods : Array<String>
        get() = SamplingMethodConverter.array

    val sampleTypes : Array<String>
        get() = SampleTypeConverter.array

    val fieldTypes : Array<String>
        get() = FieldTypeConverter.array

    val operators : Array<String>
        get() = OperatorConverter.array

    val distanceFormatPosition : MutableLiveData<Int>
        get() = _distanceFormatPosition
    val dateFormatPosition : MutableLiveData<Int>
        get() = _dateFormatPosition
    val timeFormatPosition : MutableLiveData<Int>
        get() = _timeFormatPosition
    val samplingMethodPosition : MutableLiveData<Int>
        get() = _samplingMethodPosition
    val samplingTypePosition : MutableLiveData<Int>
        get() = _samplingTypePosition
    val fieldTypePosition : MutableLiveData<Int>
        get() = _fieldTypePosition
    val ruleFieldPosition : MutableLiveData<Int>
        get() = _ruleFieldPosition

    val currentConfigurationTimeFormat : String
        get(){
            currentConfiguration?.value?.let {config ->
                return config.timeFormat.format
            }
            return unavailable
        }
    val currentConfigurationDistanceFormat : String
        get(){
            currentConfiguration?.value?.let {config ->
                return config.distanceFormat.format
            }
            return unavailable
        }
    val currentConfigurationDateFormat : String
        get(){
            currentConfiguration?.value?.let {config ->
                return config.dateFormat.format
            }
            return unavailable
        }

    var currentSampleSize : String
        get(){
            currentStudy?.value?.let{study ->
                return study.sampleSize.toString()
            }
            return ""
        }
        set(value){
            currentStudy?.value?.let{study ->
                value.toIntOrNull()?.let {size ->
                    study.sampleSize = size
                } ?: run{ study.sampleSize = 0
                }
            }
        }
    fun onDistanceFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < distanceFormats.size)
        {
            val dist : String = distanceFormats[position]
            _currentConfiguration?.value?.let {
                it.distanceFormat = DistanceFormatConverter.fromString(dist)
            }
        }

    }
    fun onTimeFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < timeFormats.size)
        {
            val time : String = timeFormats[position]
            _currentConfiguration?.value?.let {
                it.timeFormat = TimeFormatConverter.fromString(time)
            }
        }

    }
    fun onDateFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < dateFormats.size)
        {
            val date : String = dateFormats[position]
            _currentConfiguration?.value?.let {
                it.dateFormat = DateFormatConverter.fromString(date)
            }
        }

    }

    fun onSamplingMethodSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > samplingMethods.size)
        {
            val samplingMethod : String = samplingMethods[position]
            _currentStudy?.value?.let {
                it.samplingMethod = SamplingMethodConverter.fromString(samplingMethod)
            }

        }

    }

    fun onSampleTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > sampleTypes.size)
        {
            val sampleType : String = SampleTypeConverter.array[position]
            _currentStudy?.value?.let {
                it.sampleType = SampleTypeConverter.fromString(sampleType)
            }
        }

    }

    // Configuration
    fun initializeConfigurations()
    {
        configurations.clear()
        val dbConfigs = DAO.configDAO.getConfigs()
        for(config in dbConfigs)
        {
            configurations.add(config)
        }
    }
    fun createNewConfiguration()
    {
        val newConfig = Config( UUID.randomUUID().toString(), "", DateFormat.None, TimeFormat.None,
                                DistanceFormat.None, 0 )
        _currentConfiguration = MutableLiveData(newConfig)
        currentConfiguration = _currentConfiguration
    }

    fun saveNewConfiguration()
    {
        _currentConfiguration?.value?.let{configuration ->
            configurations.add(configuration)

            // write to database
            DAO.configDAO.createConfig(configuration)
        }
    }

    fun updateConfiguration()
    {
        _currentConfiguration?.value?.let{configuration ->
            // write to database
            DAO.configDAO.updateConfig(configuration)
        }
    }

    fun setCurrentConfig(config : Config)
    {
        _currentConfiguration = MutableLiveData(config)
        currentConfiguration = _currentConfiguration
    }

    fun deleteConfig(config : Config)
    {
        DAO.configDAO.deleteConfig( config )
        configurations.remove(config)

    }

    // Study
    fun createNewStudy()
    {
        val newStudy = Study(UUID.randomUUID().toString(),"","",SamplingMethod.None,0, SampleType.None,)
        _currentStudy = MutableLiveData(newStudy)
        currentStudy = _currentStudy
    }

    fun setCurrentStudy(study : Study)
    {
        _currentStudy = MutableLiveData(study)
        currentStudy = _currentStudy
    }

    fun addStudy()
    {
        currentStudy?.value?.let{study ->
            currentConfiguration?.value?.let { config ->
                if(!config.studies.contains(study))
                {
                    config.studies.add(study)
                }
            }
        }
    }
    fun removeStudy(study: Study?)
    {
        study?.let { study ->
            currentConfiguration?.value?.let{config ->
                config.studies.remove(study)
            }
        }
    }

    //region Enumerations
    fun addEnumerationAreas(enumAreas : List<EnumArea>? )
    {
        currentConfiguration?.value?.let{config ->
            config.enumAreas = enumAreas
        }
    }
    //endregion

    //region Fields
    fun createNewField()
    {
        val newField = Field( UUID.randomUUID().toString(), null, "", FieldType.None, false, false, false, false, false, "", "", "", "" )
        _currentField = MutableLiveData(newField)
        currentField = _currentField
    }
    fun addField()
    {
        currentStudy?.value?.let{study ->
            currentField?.value?.let { field ->
                study.fields.add(field)

            }
        }
    }

    fun setSelectedField(field : Field)
    {
        _currentField = MutableLiveData(field)
        currentField = _currentField
    }
    fun onFieldTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        Log.d("TEST","xxxxx")
        if(position < FieldTypeConverter.array.size)
        {
            val fieldType : String = FieldTypeConverter.array[position]
            _currentField?.value?.let {
                it.type = FieldTypeConverter.fromString( fieldType)
            }
        }
    }
    fun onFieldPIISelected(buttonView : CompoundButton,  isChecked : Boolean)
    {
        Log.d("xxx", "PII SELECTED CHANGED $isChecked")
        currentField?.value?.let{field ->
            field.pii = isChecked
        }
    }

    fun onFieldRequiredSelected(buttonView : CompoundButton,  isChecked : Boolean)
    {
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
        currentField?.value?.let{field ->
            field.required = isChecked
        }
    }
    fun onFieldIntegerOnlySelected(buttonView : CompoundButton,  isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.integerOnly = isChecked
        }
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
    }
    fun onFieldDateSelected(buttonView : CompoundButton,  isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.date = isChecked
        }
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
    }
    fun onFieldTimeSelected(buttonView : CompoundButton,  isChecked : Boolean)
    {
        currentField?.value?.let{field ->
            field.time = isChecked
        }
        Log.d("xxx", "Required SELECTED CHANGED $isChecked")
    }
    //endregion

    //region Rule

    fun createNewRule() : Boolean
    {
        currentStudy?.value?.let{study ->
            val newRule = Rule( UUID.randomUUID().toString(), study.id, null, "", Operator.None, "" )
            _currentRule = MutableLiveData(newRule)
            currentRule = _currentRule
            return true
        }
        return false
    }

    fun addRule()
    {
        currentStudy?.value?.let{study ->
            currentRule?.value?.let { rule ->
                study.rules.add(rule)
            }
        }
    }

    fun onRuleFieldSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        currentRule?.value?.let{rule ->
            currentStudy?.value?.let{study ->
                rule.field_id = study.fields[position].id
            }

        }
        Log.d("HERE", "HERE")
    }
    fun onRuleOperatorSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        currentRule?.value?.let{rule ->
            currentStudy?.value?.let{study ->
                val operator = operators[position]
                rule.operator = OperatorConverter.fromString(operator)
            }

        }
        Log.d("HERE", "HERE")
    }
    //endregion
}