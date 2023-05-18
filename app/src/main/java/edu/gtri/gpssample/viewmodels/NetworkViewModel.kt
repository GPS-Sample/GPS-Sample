package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import androidx.navigation.NavController
import edu.gtri.gpssample.R

import edu.gtri.gpssample.constants.NetworkMode
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.viewmodels.models.NetworkClientModel
import edu.gtri.gpssample.viewmodels.models.NetworkHotspotModel
import edu.gtri.gpssample.viewmodels.models.NetworkInfo
import edu.gtri.gpssample.viewmodels.models.NetworkModel
import kotlinx.coroutines.*
import java.util.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class NetworkViewModel : ViewModel(), NetworkHotspotModel.NetworkCreationDelegate,
    NetworkClientModel.NetworkConnectDelegate{


    private var _activity : Activity? = null

    private var _currentFragment : Fragment? = null
    private var _networkMode : MutableLiveData<NetworkMode> = MutableLiveData(NetworkMode.None)
    private var navController : NavController? = null

    var currentFragment : Fragment?
        get() = _currentFragment
        set(value){
            _currentFragment = value
            _currentFragment?.let {fragment ->
                navController = fragment.findNavController()
                Activity = fragment.activity
            }
        }

    val networkMode : LiveData<NetworkMode>
        get() = _networkMode

    val networkClientModel : NetworkClientModel = NetworkClientModel()
    val networkHotspotModel : NetworkHotspotModel = NetworkHotspotModel()

    private var Activity : Activity?
        get() = _activity
        set(value)
        {
            _activity = value
            networkClientModel.Activity = value
            networkHotspotModel.Activity = value
        }

    init {
        networkHotspotModel.viewModelScope = viewModelScope
        networkHotspotModel.creationDelegate = this
        networkClientModel.viewModelScope = viewModelScope
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectHotspot(v : View)
    {
        //networkMode = NetworkMode.NetworkHotspot
        _networkMode?.value = NetworkMode.NetworkClient
       // startConnection()
      //  navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)

        Log.d("xxxxxx","WE GET CLICKED connect")
    }

    fun connectHotspotFake()
    {
        _networkMode?.value = NetworkMode.NetworkClient

        viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(300)
            networkClientModel.fakeConnect()
            runBlocking(Dispatchers.Main) {
                navController?.popBackStack()
            }
        }
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectHotspot(ssid : String, password : String, serverIpAddress : String)
    {
        _networkMode?.value = NetworkMode.NetworkClient
        Log.d("xxxxx", "ssid ${ssid}, password ${password}, serverIp ${serverIpAddress}")
        startConnection(ssid, password, serverIpAddress)
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createHotspot(v : View)
    {
        _networkMode?.value = NetworkMode.NetworkHotspot
        startConnection("","","")
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
        Log.d("xxxxxx","WE GET CLICKED create")
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startConnection(ssid : String, password : String, serverIpAddress : String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            var complete : Boolean = false
            var destination = -1 //R.id.action_navigate_to_FirstFragment

            when (networkMode.value) {
                NetworkMode.NetworkHotspot ->
                {
                    complete = networkHotspotModel.startNetworking()

                    destination = networkHotspotModel.destination
                }
                NetworkMode.NetworkClient  ->
                {
                    val networkInfo = NetworkInfo(ssid, password, serverIpAddress)
                    networkClientModel.connectToWifi(networkInfo)

                }
                else -> {complete = false}
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
    }

    override fun didComplete(complete: Boolean) {
        if(complete)
        {
            runBlocking(Dispatchers.Main) {
                navController?.navigate(networkHotspotModel.destination)
            }
        }else
        {
            // TODO: figure out destination
            runBlocking(Dispatchers.Main) {

                //   navController?.navigate(R.id.action_navigate_to_FirstFragment)
            }
        }
    }

    override fun didConnect(complete: Boolean) {

    }
}