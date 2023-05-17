package edu.gtri.gpssample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NetworkConnectionViewModel: ViewModel()  {
    private var _name : MutableLiveData<String> = MutableLiveData("TEST")
    private var _connection : MutableLiveData<String> = MutableLiveData("")

    val name : LiveData<String>
        get() = _name


    val connection : LiveData<String>
        get() = _connection

    fun updateName(name : String)
    {
       // _name.value = name
        _name.postValue(name)
    }

    fun updateConnection(connection : String)
    {
       // _connection.value = connection
        _connection.postValue(connection)
    }

    fun areItemsTheSame(other: NetworkConnectionViewModel): Boolean = false

    fun areContentsTheSame(other: NetworkConnectionViewModel): Boolean = false
}