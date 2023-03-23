package edu.gtri.gpssample.viewmodels

import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.database.DAO

import java.util.*
import kotlin.collections.ArrayList
import edu.gtri.gpssample.R

import edu.gtri.gpssample.constants.*

import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule

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

    // live data for each screen being controlled by the view model
    private var _currentConfiguration : MutableLiveData<Config>? = null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentField : MutableLiveData<Field>? = null
    private var _currentFilter : MutableLiveData<Filter>? = null
    private var _currentFilterRule : MutableLiveData<FilterRule>? = null

    private var _distanceFormats : Array<String> = Array(2){ i ->
        when(i)
        {
            0 -> DistanceFormat.Meters.format
            1 -> DistanceFormat.Feet.format
            else -> String()
        }
    }

    private var _dateFormats : Array<String> = Array(3){ i ->
        when(i)
        {
            0 -> DateFormat.MonthDayYear.format
            1 -> DateFormat.DayMonthYear.format
            2 -> DateFormat.YearMonthDay.format
            else -> String()
        }
    }

    private var _timeFormats : Array<String> = Array(2){ i ->
        when(i)
        {
            0 -> TimeFormat.twelveHour.format;
            1 -> TimeFormat.twentyFourHour.format
            else -> String()
        }
    }

    private var _samplingMethods : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SamplingMethod.SimpleRandom.format
            1 -> SamplingMethod.Cluster.format
            2 -> SamplingMethod.Subsets.format
            3 -> SamplingMethod.Strata.format
            else -> String()
        }
    }

    private var _samplingTypes : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SampleType.NumberHouseholds.format
            1 -> SampleType.PercentHouseholds.format
            2 -> SampleType.PercentTotal.format
            else -> String()
        }
    }

    // Exposed LiveData each screen being controlled by the view model
    var currentConfiguration : LiveData<Config>? =_currentConfiguration
    var currentStudy : LiveData<Study>? = _currentStudy
    var currentField : LiveData<Field>? = _currentField
    var currentFilter : LiveData<Filter>? = _currentFilter
    var currentFilterRule : LiveData<FilterRule>? = _currentFilterRule

    val distanceFormats : Array<String>
        get() = _distanceFormats

    val timeFormats : Array<String>
        get() = _timeFormats

    val dateFormats : Array<String>
        get() = _dateFormats

    val configurations : ArrayList<Config>
        get() = _configurations

    val samplingMethods : Array<String>
        get() = _samplingMethods

    val sampleTypes : Array<String>
        get() = SampleTypeConverter.toArray()

    val fieldTypes : Array<String>
        get() = FieldTypeConverter.array

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

    val currentConfigurationTimeFormat : String
        get(){
            currentConfiguration?.value?.let {config ->
                return lookupTimeFormatFromKey(config.timeFormat)
            }
            return unavailable
        }
    val currentConfigurationDistanceFormat : String
        get(){
            currentConfiguration?.value?.let {config ->
                return lookupDistanceFormatFromKey(config.distanceFormat)
            }
            return unavailable
        }
    val currentConfigurationDateFormat : String
        get(){
            currentConfiguration?.value?.let {config ->
                return lookupDateFormatFromKey(config.dateFormat)
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
            val format : String = distanceFormats[position]
            var dist  = ""
            when(format){
                DistanceFormat.Feet.format -> dist = DistanceFormat.Feet.toString()
                DistanceFormat.Meters.format -> dist = DistanceFormat.Meters.toString()
            }
            _currentConfiguration?.value?.let {
                it.distanceFormat = dist
            }
        }

    }
    fun onTimeFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < timeFormats.size)
        {
            val format : String = timeFormats[position]
            var time = ""
            when(format){
                TimeFormat.twelveHour.format -> time = TimeFormat.twelveHour.toString()
                TimeFormat.twentyFourHour.format -> time = TimeFormat.twentyFourHour.toString()
            }
            _currentConfiguration?.value?.let {
                it.timeFormat = time
            }
        }

    }
    fun onDateFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < dateFormats.size)
        {
            val format : String = dateFormats[position]
            var date = ""

            when(format){
                DateFormat.DayMonthYear.format -> date = DateFormat.DayMonthYear.toString()
                DateFormat.MonthDayYear.format -> date = DateFormat.MonthDayYear.toString()
                DateFormat.YearMonthDay.format -> date = DateFormat.YearMonthDay.toString()
            }
            _currentConfiguration?.value?.let {
                it.dateFormat = date
            }
        }

    }

    fun onSamplingMethodSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > samplingMethods.size)
        {
            val format : String = samplingMethods[position]
            var samplingMethod = ""

            when(format){
                SamplingMethod.SimpleRandom.format -> samplingMethod = SamplingMethod.SimpleRandom.toString()
                SamplingMethod.Cluster.format -> samplingMethod = SamplingMethod.Cluster.toString()
                SamplingMethod.Subsets.format -> samplingMethod = SamplingMethod.Subsets.toString()
                SamplingMethod.Strata.format -> samplingMethod = SamplingMethod.Strata.toString()
            }
            _currentStudy?.value?.let {
                it.samplingMethod = samplingMethod
            }
        }

    }

    fun onSampleTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > _samplingTypes.size)
        {
            val format : String = _samplingTypes[position]
            var sampleType = ""

            when(format){
                SampleType.NumberHouseholds.format -> sampleType = SampleType.NumberHouseholds.toString()
                SampleType.PercentHouseholds.format -> sampleType = SampleType.PercentHouseholds.toString()
                SampleType.PercentTotal.format -> sampleType = SampleType.PercentTotal.toString()

            }
            _currentStudy?.value?.let {
                it.samplingMethod = sampleType
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
        val newConfig = Config( UUID.randomUUID().toString(), "", "", "", "", 0 )
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
        val newStudy = Study(UUID.randomUUID().toString(),"","","",0, SampleType.None,)
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

    // Enumerations
    fun addEnumerationAreas(enumAreas : List<EnumArea>? )
    {
        currentConfiguration?.value?.let{config ->
            config.enumAreas = enumAreas
        }
    }

    // Fields
    fun createNewField()
    {
        val newField = Field( UUID.randomUUID().toString(), "", "", "", false, false, false, false, false, "", "", "", "" )
        _currentField = MutableLiveData(newField)
        currentField = _currentField
    }

    fun onFieldTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position > FieldTypeConverter.array.size)
        {
            val format : String = FieldTypeConverter.array[position]
            var fieldType = ""

            when(format){
                FieldType.Text.format -> fieldType = FieldType.Text.toString()
                FieldType.Number.format -> fieldType = FieldType.Number.toString()
                FieldType.Date.format -> fieldType = FieldType.Date.toString()
                FieldType.Checkbox.format -> fieldType = FieldType.Date.toString()
                FieldType.Dropdown.format -> fieldType = FieldType.Date.toString()
            }
            _currentField?.value?.let {
                it.type = fieldType
            }
        }
    }

    // TODO: use the converters in the constant enums
    private fun lookupTimeFormatFromKey(key : String) : String
    {
        return when(key) {
            TimeFormat.twelveHour.toString() -> TimeFormat.twelveHour.format
            TimeFormat.twentyFourHour.toString() -> TimeFormat.twentyFourHour.format
            else -> unavailable
        }
    }

    private fun lookupDateFormatFromKey(key : String) : String
    {
        return when(key) {
            DateFormat.DayMonthYear.toString() -> DateFormat.DayMonthYear.format
            DateFormat.MonthDayYear.toString() -> DateFormat.MonthDayYear.format
            DateFormat.YearMonthDay.toString() -> DateFormat.YearMonthDay.format
            else -> unavailable
        }
    }

    private fun lookupDistanceFormatFromKey(key : String) : String
    {
        return when(key) {
            DistanceFormat.Meters.toString() -> DistanceFormat.Meters.format
            DistanceFormat.Feet.toString() -> DistanceFormat.Feet.format
            else -> unavailable
        }
    }


}