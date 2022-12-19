package edu.gtri.gpssample.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import edu.gtri.gpssample.models.FieldModel
import edu.gtri.gpssample.models.StudyModel
import edu.gtri.gpssample.models.ConfigurationModel
import edu.gtri.gpssample.models.UserModel
import edu.gtri.gpssample.services.UDPBroadcastReceiverService

class MainApplication : Application()
{
    var barcodeBitmap : Bitmap? = null

    var users = mutableListOf<UserModel>()
    var fields = mutableListOf<FieldModel>()
    var studies = mutableListOf<StudyModel>()
    var configurations = mutableListOf<ConfigurationModel>()

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
