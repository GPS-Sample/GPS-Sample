package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import androidx.navigation.NavController

import edu.gtri.gpssample.constants.NetworkMode
import edu.gtri.gpssample.viewmodels.models.NetworkClientModel
import edu.gtri.gpssample.viewmodels.models.NetworkHotspotModel
import edu.gtri.gpssample.viewmodels.models.NetworkInfo
import edu.gtri.gpssample.viewmodels.models.NetworkModel
import kotlinx.coroutines.*

class NetworkViewModel : ViewModel() {
    private var _activity : Activity? = null

    var _networkMode : MutableLiveData<NetworkMode>?

    var navController : NavController? = null


    var networkMode : LiveData<NetworkMode>

    val networkClientModel : NetworkClientModel = NetworkClientModel()
    val networkHotspotModel : NetworkHotspotModel = NetworkHotspotModel()

    var Activity : Activity?
        get() = _activity
        set(value)
        {
            _activity = value
            networkClientModel.Activity = value
            networkHotspotModel.Activity = value
        }

    init {
        _networkMode = MutableLiveData(NetworkMode.None)
        networkMode = _networkMode!!

        networkHotspotModel.viewModelScope = viewModelScope
        networkClientModel.viewModelScope = viewModelScope
    }

    fun connectHotspot(v : View)
    {
        //networkMode = NetworkMode.NetworkHotspot
        _networkMode?.value = NetworkMode.NetworkClient
        startConnection()
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)

        Log.d("xxxxxx","WE GET CLICKED connect")
    }

    fun createHotspot(v : View)
    {
        _networkMode?.value = NetworkMode.NetworkHotspot
        startConnection()
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
        Log.d("xxxxxx","WE GET CLICKED create")
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startConnection()
    {
        viewModelScope.launch(Dispatchers.IO) {
            var complete : Boolean = false
            var destination = R.id.action_navigate_to_FirstFragment

            when (networkMode.value) {
                NetworkMode.NetworkHotspot ->
                {
                    complete = networkHotspotModel.startNetworking()
                    destination = networkHotspotModel.destination
                }
                NetworkMode.NetworkClient  ->
                {

                    // TODO:  this needs to be set somewhere else
                    //val networkInfo = NetworkInfo("dlink", "")
                    //val networkInfo = NetworkInfo("AndroidShare_7405", "84zcixwuhsevvqg")
                    //val networkInfo = NetworkInfo("Cypress Guest Wifi", "cypresslovesyou")
                    val networkInfo = NetworkInfo("dlink", "")
                    complete = networkClientModel.startNetworking(networkInfo)
                    destination = networkClientModel.destination
                }
                else -> {complete = false}
            }
            if(complete)
            {
                runBlocking(Dispatchers.Main) {
                    navController?.navigate(destination)
                }
            }else
            {
                runBlocking(Dispatchers.Main) {
                    navController?.navigate(R.id.action_navigate_to_FirstFragment)
                }
            }

        }
    }

    fun stopConnection(v : View) {
        when (networkMode.value) {
            NetworkMode.NetworkHotspot -> {
                networkHotspotModel.closeHotspot()
            }
            NetworkMode.NetworkClient -> {

            }
            else -> {}
        }
       // runBlocking(Dispatchers.Main) {
            navController?.navigate(R.id.action_HotspotFragment_to_FirstFragment)
       // }

    }
}