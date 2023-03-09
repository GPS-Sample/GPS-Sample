package edu.gtri.gpssample.viewmodels
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import java.util.*
import kotlin.collections.ArrayList
import edu.gtri.gpssample.R
class ConfigurationViewModel : ViewModel()
{
    private var configurations : ArrayList<Config> = ArrayList()
    private var _currentConfiguration : MutableLiveData<Config>? = null
    private var _distanceFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _timeFormatPosition : MutableLiveData<Int> = MutableLiveData(0)
    private var _dateFormatPosition : MutableLiveData<Int> = MutableLiveData(0)

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

    val DistanceFormats : Array<String>
        get() = distanceFormats

    val TimeFormats : Array<String>
        get() = timeFormats

    val DateFormats : Array<String>
        get() = dateFormats

    val Configurations : ArrayList<Config>
        get() = configurations

    var currentConfiguration : LiveData<Config>? = _currentConfiguration

    val distanceFormatPosition : MutableLiveData<Int>
        get() = _distanceFormatPosition
    val dateFormatPosition : MutableLiveData<Int>
        get() = _dateFormatPosition
    val timeFormatPosition : MutableLiveData<Int>
        get() = _timeFormatPosition

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

            Log.d("test", "the distance ${dist}")
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
    fun Test()
    {
        val index : Int = distanceFormatPosition.value!!
        val dist : String = distanceFormats[index]


        Log.d("TEST", "this is ${distanceFormatPosition.value}  ${dist}  ")

        _currentConfiguration?.value?.let {
            Log.d("THE BIG TEST "," distance format ${it.distanceFormat}  ${it.minGpsPrecision}")
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


}