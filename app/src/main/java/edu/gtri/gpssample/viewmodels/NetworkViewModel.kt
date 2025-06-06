/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.fragments.manageconfigurations.ManageConfigurationsFragment

class NetworkViewModel : ViewModel(), NetworkHotspotModel.NetworkCreationDelegate,
    NetworkClientModel.NetworkConnectDelegate{

    interface ManageConfigurationNetworkDelegate
    {
        fun didReceiveConfiguration(complete: Boolean)
    }

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
                val vm : ConfigurationViewModel by fragment.activityViewModels()
                networkHotspotModel.sharedViewModel =  vm
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
        networkClientModel.connectDelegate = this
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectHotspot(ssid : String, password : String, serverIpAddress : String)
    {
        _networkMode?.value = NetworkMode.NetworkClient
        startConnection(ssid, password, serverIpAddress)
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createHotspot(v : View)
    {
        _networkMode?.value = NetworkMode.NetworkHotspot
        startConnection("","","")
        navController?.navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startConnection(ssid : String, password : String, serverIpAddress : String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            var complete : Boolean = false
            var destination = -1

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

    fun setCurrentConfig(config : Config)
    {
        networkHotspotModel.config = config
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
                try {
                    navController?.navigate(networkHotspotModel.destination)
                }catch (ex : Exception)
                {
                    navController?.navigate(networkHotspotModel.destination)
                }

            }
        }else
        {
            // TODO: figure out destination
            runBlocking(Dispatchers.Main) {
                Log.d("xxx", "here")
                //   navController?.navigate(R.id.action_navigate_to_FirstFragment)
            }
        }
    }

    fun hotspotDonePopBack(v: View)
    {
        //runBlocking(Dispatchers.Main) {
        navController?.popBackStack()
        networkHotspotModel.shutdown()
        networkClientModel.shutdown()
       // }
    }

    override fun didReceiveConfiguration(error: Boolean)
    {
        runBlocking(Dispatchers.Main) {
            networkClientModel.resetState()
            navController?.popBackStack()

            currentFragment?.let {
                if (it is ManageConfigurationsFragment)
                {
                    val bundle = Bundle()
                    bundle.putBoolean( Keys.kError.value, error )
                    it.setFragmentResult( it.javaClass.simpleName, bundle )
                }
            }

            shutdown()
        }
    }

    override fun didSendData(complete: Boolean)
    {
        runBlocking(Dispatchers.Main) {
            networkClientModel.resetState()
            navController?.popBackStack()
            // tear down connection
            shutdown()
        }
    }

    fun shutdown()
    {
        networkClientModel.shutdown()
        networkHotspotModel.shutdown()
    }
}