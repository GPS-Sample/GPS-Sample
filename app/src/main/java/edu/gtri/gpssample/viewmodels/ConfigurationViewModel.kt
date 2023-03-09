package edu.gtri.gpssample.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import java.util.*
import kotlin.collections.ArrayList

class ConfigurationViewModel : ViewModel()
{
    private var configurations : ArrayList<Config> = ArrayList()

    private var _currentConfiguration : MutableLiveData<Config>? = null


    val Configurations : ArrayList<Config>
        get() = configurations

    var currentConfiguration : LiveData<Config>? = _currentConfiguration
    var test : Int = 0

    var _test : MutableLiveData<String> = MutableLiveData("Test")
    var testString : LiveData<String> = _test
    init
    {

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
        Log.d("SAVE", "We are saving the config ${test }")
        Log.d("SAVE", "We are saving the config ${currentConfiguration?.value?.name }")
        Log.d("SAVE", "We are saving the config ${_currentConfiguration?.value?.name }")
        _currentConfiguration?.value.let{configuration ->
            configurations.add(configuration!!)
            // write to database
            DAO.configDAO.createConfig(configuration)
        }

    }
}