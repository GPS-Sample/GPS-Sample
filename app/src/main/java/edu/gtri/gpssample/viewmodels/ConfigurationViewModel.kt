package edu.gtri.gpssample.viewmodels
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import java.util.*
import kotlin.collections.ArrayList
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.Study

class ConfigurationViewModel : ViewModel()
{
    private var configurations : ArrayList<Config> = ArrayList()
    private var _currentConfiguration : MutableLiveData<Config>? = null
    private var _distanceFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _timeFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _dateFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _samplingMethodPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _samplingTypePosition : MutableLiveData<Int> = MutableLiveData(0)

    private var _currentStudy : MutableLiveData<Study>? = null

    private var distanceFormats : Array<String> = Array(2){ i ->
        when(i)
        {
            0 -> DistanceFormat.Meters.format
            1 -> DistanceFormat.Feet.format
            else -> String()
        }
    }

    private var dateFormats : Array<String> = Array(3){ i ->
        when(i)
        {
            0 -> DateFormat.MonthDayYear.format
            1 -> DateFormat.DayMonthYear.format
            2 -> DateFormat.YearMonthDay.format
            else -> String()
        }
    }

    private var timeFormats : Array<String> = Array(2){ i ->
        when(i)
        {
            0 -> TimeFormat.twelveHour.format;
            1 -> TimeFormat.twentyFourHour.format
            else -> String()
        }
    }

    private var samplingMethods : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SamplingMethod.SimpleRandom.format
            1 -> SamplingMethod.Cluster.format
            2 -> SamplingMethod.Subsets.format
            3 -> SamplingMethod.Strata.format
            else -> String()
        }
    }

    private var samplingTypes : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SampleType.NumberHouseholds.format
            1 -> SampleType.PercentHouseholds.format
            2 -> SampleType.PercentTotal.format
            else -> String()
        }
    }

    val DistanceFormats : Array<String>
        get() = distanceFormats

    val TimeFormats : Array<String>
        get() = timeFormats

    val DateFormats : Array<String>
        get() = dateFormats

    val Configurations : ArrayList<Config>
        get() = configurations

    val SamplingMethods : Array<String>
        get() = samplingMethods

    val SampleTypes : Array<String>
        get() = samplingTypes

    var currentConfiguration : LiveData<Config>? = _currentConfiguration
    var currentStudy : LiveData<Study>? = _currentStudy

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

    fun onDistanceFormatSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    {
        if(position < distanceFormats.size)
        {
            val format : String = distanceFormats[position]
            var dist : String = ""
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
        if(position > dateFormats.size)
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
        if(position > samplingTypes.size)
        {
            val format : String = samplingTypes[position]
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

    fun createNewStudy()
    {
        val newStudy = Study(UUID.randomUUID().toString(),"","","",0, SampleType.None,)
        _currentStudy = MutableLiveData(newStudy)
        currentStudy = _currentStudy
    }

}