/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.constants.NetworkStatus

class SamplingMethodModel {

    var _configuration : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val configuration : LiveData<NetworkStatus>
        get() = _configuration


}