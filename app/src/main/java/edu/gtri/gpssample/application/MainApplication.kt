/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.services.UDPBroadcastReceiverService

class MainApplication : Application()
{
    val defaultSubAddress = "9999"
    val defaultEnumerationAreaName = "9999"
    val defaultEnumerationItemUUID = "999999999999999999999999999999999999"

    var user: User? = null
    var currentFragment = ""
    var currentSubAddress = defaultSubAddress
    var currentEnumerationAreaName = defaultEnumerationAreaName
    var currentEnumerationItemUUID = defaultEnumerationItemUUID

    override fun onCreate()
    {
        super.onCreate()

        instance = this

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        val serviceChannel = NotificationChannel(

            UDPBroadcastReceiverService.SERVICE_CHANNEL_ID,
            "UDP Broadcast Receiver Service",
            NotificationManager.IMPORTANCE_LOW

        )

        serviceChannel.apply {
            setShowBadge(false)
        }

        val manager: NotificationManager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        private var instance: MainApplication? = null

        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }}
