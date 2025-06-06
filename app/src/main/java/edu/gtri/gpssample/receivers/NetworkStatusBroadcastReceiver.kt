/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import edu.gtri.gpssample.services.NetworkMonitorService.Companion.NETWORK_SERVICE_STATUS_KEY
import edu.gtri.gpssample.utils.NetworkConnectionStatus

class NetworkStatusBroadcastReceiver(netStatusFunc: (connectionStatus : NetworkConnectionStatus) -> Unit) :
BroadcastReceiver()
{
    private var netConnectionCallback: (connectionStatus : NetworkConnectionStatus) -> Unit
    init {
        netConnectionCallback = netStatusFunc
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(NETWORK_SERVICE_STATUS_KEY)) {
            if(intent != null)
            {
                val statusIndex : Int = intent!!.getIntExtra(NETWORK_SERVICE_STATUS_KEY,0)
                netConnectionCallback(NetworkConnectionStatus.values()[statusIndex])
            }else
            {
               // netConnectionCallback(BLEStatus.UNKNOWN_STATUS)
            }

            //bleConnectionCallback(intent?.getBooleanExtra(BUTTON_CONNECTION_STATUS_KEY, false) ?: false)
            //bleConnectionCallback(intent?.getBooleanExtra(BUTTON_CONNECTION_STATUS_KEY, false) ?: false)
        }
        //netConnectionCallback(p1)
    }
}