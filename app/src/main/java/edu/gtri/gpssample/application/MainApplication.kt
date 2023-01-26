package edu.gtri.gpssample.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.services.UDPBroadcastReceiverService

class MainApplication : Application()
{
    var barcodeBitmap : Bitmap? = null

//    var users = mutableListOf<User>()
    var fields = mutableListOf<Field>()
    var studies = mutableListOf<Study>()
//    var configurations = mutableListOf<Configuration>()

    override fun onCreate()
    {
        super.onCreate()
        val serviceChannel = NotificationChannel(

            UDPBroadcastReceiverService.SERVICE_CHANNEL_ID,
            "Panic Button Service",
            NotificationManager.IMPORTANCE_LOW

        )

        serviceChannel.apply {
            setShowBadge(false)
        }

        val manager: NotificationManager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
