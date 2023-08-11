package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.database.DAO

import kotlin.collections.ArrayList

import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.viewmodels.models.*

class ConfigurationViewModel : ViewModel()
{
    private val unavailable = "Unavailable"
    private var _configurations : ArrayList<Config> = ArrayList()

    private var _distanceFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _timeFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _dateFormatPosition : MutableLiveData<Int> = MutableLiveData(0)

    // live data for each screen being controlled by the view model
    private var _currentConfiguration : MutableLiveData<Config>? = null
    private var _actualConfig : Config? = null
    // Since we are utilizing a main view model per "section" of the app, i.e. the set up of a
    // configuration with enumerations, studies, fields, rules and filters, each with its own
    // fragment, we employ what we call "fragment models" that control each fragment data, but
    // are commonly available across all fragments, since they share data between themselves.

    val createFilterModel : CreateFilterModel = CreateFilterModel()
    val createStudyModel : CreateStudyModel = CreateStudyModel()
    val createFieldModel : CreateFieldModel = CreateFieldModel()
    val createRuleModel : CreateRuleModel = CreateRuleModel()
    val enumAreaViewModel : EnumAreaViewModel = EnumAreaViewModel()
    val locationViewModel : LocationViewModel = LocationViewModel()
    var teamViewModel : TeamViewModel = TeamViewModel()
    val createFilterRuleModel : CreateFilterRuleModel = CreateFilterRuleModel()
    val performEnumerationModel: PerformEnumerationModel = PerformEnumerationModel()

    // Exposed LiveData each screen being controlled by the view model
    val currentConfiguration : LiveData<Config>?
        get() = _currentConfiguration

    val configurations : ArrayList<Config>
        get() = _configurations

    val distanceFormats : Array<String>
        get() = DistanceFormatConverter.array

    val timeFormats : Array<String>
        get() = TimeFormatConverter.array

    val dateFormats : Array<String>
        get() = DateFormatConverter.array




    val distanceFormatPosition : MutableLiveData<Int>
        get() = _distanceFormatPosition
    val dateFormatPosition : MutableLiveData<Int>
        get() = _dateFormatPosition
    val timeFormatPosition : MutableLiveData<Int>
        get() = _timeFormatPosition


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
    private var _currentFragment : Fragment? = null
    private var activity : Activity? = null
    var currentFragment : Fragment?
        get() = _currentFragment
        set(value){
            _currentFragment = value
            _currentFragment?.let {fragment ->

                activity = fragment.activity
            }
        }

    val currentUserRole : Role
        get(){
            activity?.let{activity ->
                val _user = (activity.application as MainApplication).user
                _user?.let{

                    Log.d("XXXX  ROLE", "the ROLE ${it.role}")
                    return RoleConverter.getRole(it.role)
                }

            }
            return Role.Undefined

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
        val newConfig = Config("", DateFormat.None, TimeFormat.None, DistanceFormat.None, 0 )
        _currentConfiguration = MutableLiveData(newConfig)
        //saveNewConfiguration()
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
            if(!configurations.contains(configuration))
            {
                configurations.add(configuration)
            }
        }
    }

    fun setCurrentConfig(config : Config)
    {
        _actualConfig = config
        _currentConfiguration?.let {
            it.value = _actualConfig!!
        }?: run{
            _currentConfiguration = MutableLiveData(_actualConfig!!)
        }
    }

    fun deleteConfig(config : Config)
    {
        DAO.configDAO.deleteConfig( config )
        configurations.remove(config)
    }

    // Study

    fun addStudy()
    {
        currentConfiguration?.let{configuration->
            createStudyModel.addStudy(configuration.value)
        }
    }
    fun removeStudy(study: Study?)
    {
        currentConfiguration?.let { configuration ->
            createStudyModel.removeStudy(study, configuration.value)
        }
    }
    fun deleteCurrentStudy()
    {
        _currentConfiguration?.value?.let { configuration ->
            createStudyModel.currentStudy?.value?.let{study ->
                createFilterModel.deleteSelectedFilter(study)
                createRuleModel.deleteSelectedRule(study)
                createFieldModel.deleteSelectedField(study)
            }
            createStudyModel.deleteCurrentStudy(configuration)
        }
    }

    //endregion
    //region Enumerations
    fun addEnumerationAreas(enumAreas : ArrayList<EnumArea> )
    {
        currentConfiguration?.value?.let{config ->
            config.enumAreas = enumAreas
        }
    }
    //endregion

    //region Fields
    fun addField()
    {
        createStudyModel.currentStudy?.value?.let{study ->
            createFieldModel.addField(study)
        }
    }
    fun deleteSelectedField()
    {
        createStudyModel.currentStudy?.value?.let { study ->
            createFieldModel.deleteSelectedField(study)
        }
    }
    //endregion

    //region Rule

    fun addRule()
    {
        createStudyModel.currentStudy?.value?.let{study ->
            createRuleModel.addRule(study)
        }
    }
    fun setSelectedRule(rule : Rule)
    {
        createRuleModel.setSelectedRule(rule)
    }

    fun deleteRule(rule:Rule)
    {
        createStudyModel.currentStudy?.value?.let { study ->
            createRuleModel.deleteRule(rule, study)
        }
    }

    fun deleteSelectedRule()
    {
        createStudyModel.currentStudy?.value?.let { study ->
            createRuleModel.deleteSelectedRule(study)
        }
    }
    fun onRuleFieldSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
         createStudyModel.currentStudy?.value?.let{study ->
             createRuleModel.onRuleFieldSelected(study, position)
        }

    }
    fun onRuleOperatorSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        createStudyModel.currentStudy?.value?.let{study ->
             createRuleModel.onRuleOperatorSelected(study, position)
        }
    }
    //endregion

    //region FilterRule
    fun onFirstRuleFieldSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        createStudyModel.currentStudy?.value?.let{study ->
            createFilterRuleModel.onFirstRuleFieldSelected(study, position)
        }

    }

    fun onSecondRuleFieldSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        createStudyModel.currentStudy?.value?.let{study ->
            createFilterRuleModel.onSecondRuleFieldSelected(study, position)
        }

    }

    fun createNewFilterRule()
    {
        createFilterModel.currentFilter?.value?.let { filter ->
            createStudyModel.currentStudy?.value?.let{study ->
                // we set the filter on the CreateFilterRuleModel
                createFilterRuleModel.createNewFilterRule(filter, study)
            }

        }
    }

    fun addFilerRule()
    {
        createFilterModel.currentFilter?.value?.let{filter ->
            createFilterRuleModel.addFilterRule(filter)
//            createFilterModel.createFilterAdapter.updateFilterRules(filter.filterRules)
        }
    }

    fun addFilter()
    {
        createStudyModel.currentStudy?.value?.let{study ->
            createFilterModel.addFilter(study)
        }
    }

    fun replaceEnumArea(enumArea : EnumArea)
    {
        currentConfiguration?.value?.let{config ->
            for(ea in config.enumAreas)
            {

            }
            config.enumAreas.remove(enumArea)
            config.enumAreas.add(enumArea)
        }
    }
}