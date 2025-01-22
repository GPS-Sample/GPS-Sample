/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.*
import android.util.Log
import edu.gtri.gpssample.activities.MainActivity
import edu.gtri.gpssample.utils.NetworkConnectionStatus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import android.provider.Settings

class NetworkMonitorService : Service()
{
    private val enabled = true;
    private val binder: IBinder? = LocalBinder()
    private val networklDisposable = CompositeDisposable()
    private var statusIntent = Intent()
    private var netStatus: NetworkConnectionStatus = NetworkConnectionStatus.INITIALIZING
    private var netStatusBkp: NetworkConnectionStatus? = null
    inner class LocalBinder : Binder()
    {
        fun getService(): NetworkMonitorService
        {
            return this@NetworkMonitorService
        }
    }

    override fun onCreate()
    {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)


        Observable.interval(800L, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .map {
                ConnectionStatus.updateStatus(
                    checkOnline()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                try {
                    if ((ConnectionStatus.hasChanged && !ConnectionStatus.isConnected ) )
                    {
                        netStatus = NetworkConnectionStatus.WIFI_NOT_AVAILABLE

                    }else if(ConnectionStatus.hasChanged && ConnectionStatus.isConnected)
                    {
                        netStatus = NetworkConnectionStatus.CONNECTED
                    }
                    if(netStatusBkp != netStatus)
                    {
                        reportButtonConnectionStatusWithEnum(netStatus)
                        netStatusBkp = netStatus
                    }

                } catch (ex: Exception) {
                }
            }, { throwable -> //handle error
                println("${throwable}")
            }).addTo(networklDisposable)

//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
//            PendingIntent.FLAG_IMMUTABLE
//        )

//        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID )
//            .setContentTitle("Network Monitor Service")
//            .setSmallIcon(R.mipmap.ic_launcher)
//            .setContentIntent(pendingIntent)
//            .build()
//
//        startForeground(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder?
    {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
//        CoroutineScope(Dispatchers.IO +  SupervisorJob()).launch {
//            beginListening()
//        }

        return START_NOT_STICKY
    }

    private fun checkOnline(): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {

            var wifiEnabled = false
            var airplaneModeOff = false

            airplaneModeOff =
                Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 0

            wifiEnabled = Settings.Global.getInt(contentResolver, Settings.Global.WIFI_ON, 0) != 0

            return wifiEnabled and airplaneModeOff
//            val mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//            val mData = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
//

//            val capabilities =
//                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
//            if (capabilities != null) {
//                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
//                    return true
//                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                    return true
//                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
//                    return true
//                }
//            }else
//            {
//                return true
//            }
        }
        return false
    }
    private fun reportButtonConnectionStatusWithEnum(status: NetworkConnectionStatus) {
        statusIntent.action = NETWORK_SERVICE_STATUS_KEY
        statusIntent.putExtra(NETWORK_SERVICE_STATUS_KEY, status.ordinal)
        sendBroadcast(statusIntent)
    }
    override fun onDestroy()
    {
        super.onDestroy()

        Log.d( "xxx", "OnDestroy" )
    }

    companion object
    {
        const val SERVICE_CHANNEL_ID = "NetworkMonitorService"
        const val NETWORK_SERVICE_STATUS_KEY : String = "NetworkServiceStatus"
        const val FILTER_DATE_KEY: String = "Date"
        const val FILTER_TIME_KEY: String = "Time"
        const val FILTER_DATE_TIME_KEY: String = "DateTime"

        const val BUTTON_CONNECTION_STATUS_KEY: String = "ButtonConnectionStatus"

        const val REGISTRATION_COMPLETE_KEY: String = "RegistrationComplete"

        const val TIME_FORMAT: String = "h:mm a"
        const val DATE_FORMAT: String = "E, MMM dd"

        const val INTENDED_LOCATION_SAVED_MODE_KEY = "Suggested Locations" //"Available Intended Locations"
        const val INTENDED_LOCATION_SUGGESTED_MODE_KEY = "Suggested Locations"

        const val BLUETOOTH_NOT_AVAILABLE_ERROR = 1
    }

    object ConnectionStatus {
        private var lastNetworkStatus: Boolean = true

        var hasChanged: Boolean = false
            private set

        var isConnected: Boolean = false
            private set

        private var networkStatus: Boolean = false

        fun updateStatus( networkStatus: Boolean) {
            this.networkStatus = networkStatus

            hasChanged = (ConnectionStatus.networkStatus != lastNetworkStatus)
            isConnected = ConnectionStatus.networkStatus

            lastNetworkStatus = networkStatus

        }

    }
}