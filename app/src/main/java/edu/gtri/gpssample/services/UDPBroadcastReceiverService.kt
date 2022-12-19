package edu.gtri.gpssample.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import edu.gtri.gpssample.R
import edu.gtri.gpssample.activities.MainActivity
import edu.gtri.gpssample.network.UDPBroadcastReceiver
import kotlinx.coroutines.*
import java.net.InetAddress

class UDPBroadcastReceiverService : Service()
{
    private val enabled = true;
    private val binder: IBinder? = LocalBinder()

    inner class LocalBinder : Binder()
    {
        fun getService(): UDPBroadcastReceiverService
        {
            return this@UDPBroadcastReceiverService
        }
    }

    override fun onCreate()
    {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID )
            .setContentTitle("UDP Broadcast Receiver Service")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder?
    {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        CoroutineScope(Dispatchers.IO +  SupervisorJob()).launch {
            beginListening()
        }

        return START_NOT_STICKY
    }

    suspend fun beginListening()
    {
//        Log.d( "xxx", "begin listening" )
//
//        Looper.prepare()
//
//        while (enabled)
//        {
//            Log.d( "xxx", "timer fired" )
//            delay( 1000 )
//        }
//
//        Log.d( "xxx", "endListening" )
    }

    override fun onDestroy()
    {
        super.onDestroy()

        Log.d( "xxx", "OnDestroy" )
    }

    companion object
    {
        const val SERVICE_CHANNEL_ID = "UDPBroadcastReceiverService"
    }
}