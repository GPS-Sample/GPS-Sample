package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.NetworkStatus

class SamplingMethodModel {

    var _configuration : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val configuration : LiveData<NetworkStatus>
        get() = _configuration


}