/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels.models

import android.app.Activity
import edu.gtri.gpssample.constants.NetworkMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfo(
    var ssid: String,
    var password: String,
    var serverIP: String)
{
}


abstract class NetworkModel {
    abstract val type : NetworkMode

    abstract fun initializeNetwork()
    abstract fun startNetworking(networkInfo: NetworkInfo? = null) : Boolean

    protected var _activity : Activity? = null
    abstract var Activity : Activity?

    protected var _viewModelScope : CoroutineScope? = null
    var viewModelScope : CoroutineScope?
        get() = _viewModelScope
        set(value)
        {
            _viewModelScope = value
        }


}