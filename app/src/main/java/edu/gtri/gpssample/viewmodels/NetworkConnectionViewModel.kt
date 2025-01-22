/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.Socket

class NetworkConnectionViewModel(socket : Socket): ViewModel()  {
    private var _name : MutableLiveData<String> = MutableLiveData("")
    private var _connection : MutableLiveData<String> = MutableLiveData("")
    private var _socket : Socket? = null

//    val socket : Socket?
//        get() = _socket
//

//    constructor(socket: Socket)
//    {
//
//        _socket = socket
//    }
    val name : LiveData<String>
        get() = _name


    val connection : LiveData<String>
        get() = _connection

    var socket : Socket?
        get() = _socket
        set(value){_socket = value}
    fun updateName(name : String)
    {
        runBlocking(Dispatchers.Main) {
            _name.value = name
        }
        _name.postValue(name)
    }

    fun updateConnection(connection : String)
    {
       // _connection.value = connection
        _connection.postValue(connection)
    }

}