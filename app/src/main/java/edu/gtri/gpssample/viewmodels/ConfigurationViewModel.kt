/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels

import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.viewmodels.models.*
import java.util.*

class ConfigurationViewModel : ViewModel()
{
    private val unavailable = "Unavailable"

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

    // Exposed LiveData each screen being controlled by the view model
    val currentConfiguration : LiveData<Config>?
        get() = _currentConfiguration

    var currentLocationUuid = ""
    var currentCollectionTeamUuid = ""
    var currentEnumerationTeamUuid = ""
    var currentEnumerationItemUuid = ""

    val timeFormats = arrayOf( "", "" )
    val dateFormats = arrayOf( "", "", "" )
    val distanceFormats = arrayOf( "", "" )
    val minimumGpsPrecisionFormats = arrayOf( "", "" )

    private var _centerOnCurrentLocation : MutableLiveData<Boolean>? = null

    private var _currentCenterPoint : MutableLiveData<Point>? = null
    private var _currentZoomLevel : MutableLiveData<Double>? = null

    var currentZoomLevel : LiveData<Double>? = _currentZoomLevel
    var currentCenterPoint : LiveData<Point>? = _currentCenterPoint
    var centerOnCurrentLocation : LiveData<Boolean>? = _centerOnCurrentLocation

    fun setCurrentZoomLevel( zoomLevel: Double )
    {
        _currentZoomLevel = MutableLiveData(zoomLevel)
        currentZoomLevel = _currentZoomLevel
    }

    fun setCurrentCenterPoint( point: Point? )
    {
        _currentCenterPoint = MutableLiveData(point)
        currentCenterPoint = _currentCenterPoint
    }

    fun setCenterOnCurrentLocation( value: Boolean )
    {
        _centerOnCurrentLocation = MutableLiveData( value )
        centerOnCurrentLocation = _centerOnCurrentLocation
    }

    val distanceFormatPosition : MutableLiveData<Int>
        get() = _distanceFormatPosition

    val dateFormatPosition : MutableLiveData<Int>
        get() = _dateFormatPosition

    val timeFormatPosition : MutableLiveData<Int>
        get() = _timeFormatPosition

    val currentConfigurationMinimumGpsPrecision : String
        get() {
            currentConfiguration?.value?.let {config ->
                if (config.distanceFormat == DistanceFormat.Meters)
                {
                    return "${config.minGpsPrecision} ${minimumGpsPrecisionFormats[0]}"
                }
                else
                {
                    return "${config.minGpsPrecision} ${minimumGpsPrecisionFormats[1]}"
                }
            }
            return unavailable
        }

    val currentConfigurationProximityWarning : String
        get() {
            currentConfiguration?.value?.let {config ->
                if (config.distanceFormat == DistanceFormat.Meters)
                {
                    return "${config.proximityWarningValue} ${minimumGpsPrecisionFormats[0]}"
                }
                else
                {
                    return "${config.proximityWarningValue} ${minimumGpsPrecisionFormats[1]}"
                }
            }
            return unavailable
        }

    val currentConfigurationTimeFormat : String
        get() {
            currentConfiguration?.value?.let {config ->
                if (config.timeFormat == TimeFormat.twelveHour)
                {
                    return timeFormats[0]
                }
                else
                {
                    return timeFormats[1]
                }
            }
            return unavailable
        }

    val currentConfigurationDistanceFormat : String
        get() {
            currentConfiguration?.value?.let {config ->
                if (config.distanceFormat == DistanceFormat.Meters)
                {
                    return distanceFormats[0]
                }
                else
                {
                    return distanceFormats[1]
                }
            }
            return unavailable
        }

    val currentConfigurationDateFormat : String
        get() {
            currentConfiguration?.value?.let {config ->
                if (config.dateFormat == DateFormat.DayMonthYear)
                {
                    return dateFormats[0]
                }
                else if (config.dateFormat == DateFormat.MonthDayYear)
                    return dateFormats[1]
                else
                {
                    return dateFormats[2]
                }
            }
            return unavailable
        }

    private var _currentFragment : Fragment? = null
    var currentFragment : Fragment?
        get() = _currentFragment
        set(value){
            _currentFragment = value
        }

    fun onDistanceFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        _currentConfiguration?.value?.let {
            if (position == 0)
            {
                it.distanceFormat = DistanceFormat.Meters
            }
            else
            {
                it.distanceFormat = DistanceFormat.Feet
            }
        }
    }

    fun onTimeFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        _currentConfiguration?.value?.let {
            if (position == 0)
            {
                it.timeFormat = TimeFormat.twelveHour
            }
            else
            {
                it.timeFormat = TimeFormat.twentyFourHour
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

    fun createNewConfiguration()
    {
        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60 / 60
        val newConfig = Config(timeZone,"", DAO.DATABASE_VERSION, 0, DateFormat.None, TimeFormat.None, DistanceFormat.None, 20, "", false, false, false, true, 10 )
        _currentConfiguration = MutableLiveData(newConfig)
        //saveNewConfiguration()
    }

    fun updateConfiguration()
    {
        _currentConfiguration?.value?.let{ configuration ->
            DAO.instance().writableDatabase.beginTransaction()
            DAO.configDAO.createOrUpdateConfig(configuration)
            DAO.instance().writableDatabase.setTransactionSuccessful()
            DAO.instance().writableDatabase.endTransaction()
        }
    }

    fun setCurrentConfig(config : Config)
    {
        _currentConfiguration = MutableLiveData(config)
    }

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
                createFieldModel.deleteCurrentField(study)
            }
            createStudyModel.deleteCurrentStudy(configuration)
        }
    }

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

    fun onRuleOperatorSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        createStudyModel.currentStudy?.value?.let{study ->
             createRuleModel.onRuleOperatorSelected(study, position)
        }
    }

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
}