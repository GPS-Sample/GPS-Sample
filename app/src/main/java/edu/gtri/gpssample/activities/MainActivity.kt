package edu.gtri.gpssample.activities

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.core.content.ContextCompat
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.ActivityMainBinding
import edu.gtri.gpssample.services.UDPBroadcastReceiverService

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var udpBroadcastReceiverService: UDPBroadcastReceiverService

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null)
        {
            DAO.createSharedInstance(applicationContext)

            val configurations = DAO.configDAO.getConfigs()
            Log.d( "xxx", "found ${configurations.size} configurations" )

            val studies = DAO.studyDAO.getStudies()
            Log.d( "xxx", "found ${studies.size} studies" )

            val fields = DAO.fieldDAO.getFields()
            Log.d( "xxx", "found ${fields.size} fields" )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController( R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val serviceIntent = Intent( this, UDPBroadcastReceiverService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        val intent = Intent(this, UDPBroadcastReceiverService::class.java)
        this.bindService( intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder)
        {
            val binder = service as UDPBroadcastReceiverService.LocalBinder
            udpBroadcastReceiverService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName)
        {
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}