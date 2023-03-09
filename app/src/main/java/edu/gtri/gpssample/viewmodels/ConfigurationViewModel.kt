package edu.gtri.gpssample.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import java.util.*
import kotlin.collections.ArrayList

class ConfigurationViewModel : ViewModel()
{
    private var configurations : ArrayList<Config> = ArrayList()
    private var _currentConfiguration : MutableLiveData<Config>? = null
    private var _deviceposition : MutableLiveData<String> = MutableLiveData(String())

    private var distanceFormats : Array<String>

    val DistanceFormats : Array<String>
        get() = distanceFormats
//    private var dateFormats : Array<String>
//    private var timeFormats : Array<String>

    val Configurations : ArrayList<Config>
        get() = configurations

    var currentConfiguration : LiveData<Config>? = _currentConfiguration

    var devicePosition : LiveData<String> = _deviceposition

    init
    {
        distanceFormats = Array(2){i ->
            when(i)
            {
                0 -> DistanceFormat.Meters.toString()
                1 -> DistanceFormat.Feet.toString()
                else -> String()
            }
        }

    }

    fun Test()
    {
        Log.d("TEST", "this is ${devicePosition}")
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
        _currentConfiguration?.value.let{configuration ->
            configurations.add(configuration!!)
            // write to database
            DAO.configDAO.createConfig(configuration)
        }

    }
}