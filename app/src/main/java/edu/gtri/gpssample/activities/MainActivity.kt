package edu.gtri.gpssample.activities

import android.app.Application
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.ActivityMainBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.receivers.NetworkStatusBroadcastReceiver
import edu.gtri.gpssample.services.NetworkMonitorService
import edu.gtri.gpssample.services.UDPBroadcastReceiverService
import edu.gtri.gpssample.utils.NetworkConnectionStatus
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), InfoDialog.InfoDialogDelegate
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var udpBroadcastReceiverService: UDPBroadcastReceiverService
    private lateinit var configurationViewModel : ConfigurationViewModel
    private lateinit var networkViewModel : NetworkViewModel
    lateinit var networkMonitorService: NetworkMonitorService

    var isBound = false

    private lateinit var networkStatusBroadcastReceiver: NetworkStatusBroadcastReceiver
    private lateinit var intentFilter : IntentFilter
    private var  networkConnectionStatus: NetworkConnectionStatus = NetworkConnectionStatus.UNKNOWN_STATUS

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

    private val networkMonitorConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder)
        {
            val binder = service as NetworkMonitorService.LocalBinder
            networkMonitorService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName)
        {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // build view models
        val viewModel: ConfigurationViewModel by viewModels()
        configurationViewModel = viewModel
        val networkVm : NetworkViewModel by viewModels()
        networkViewModel = networkVm

        if (savedInstanceState == null)
        {
            DAO.createSharedInstance(applicationContext)
            configurationViewModel.initializeConfigurations()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController( R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)





       // Maybe there's a way to configure this better?
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {supportActionBar?.setIcon(R.drawable.gps_sample_light)} // Night mode is not active, we're using the light theme.
            Configuration.UI_MODE_NIGHT_YES -> {supportActionBar?.setIcon(R.drawable.gps_sample_dark)} // Night mode is active, we're using dark theme.
        }

        binding.toolbar.setOnClickListener {
            if (BuildConfig.DEBUG) {
                (this.application as? MainApplication)?.currentFragment?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

//        val networkServiceIntent = Intent( this, NetworkMonitorService::class.java)
//        ContextCompat.startForegroundService(this, networkServiceIntent)
        val networkMonitorIntent = Intent(this, NetworkMonitorService::class.java)
        this.bindService( networkMonitorIntent, networkMonitorConnection, Context.BIND_AUTO_CREATE)

        networkStatusBroadcastReceiver = NetworkStatusBroadcastReceiver( ::updateConnectionStatus)

        intentFilter = IntentFilter()
        intentFilter.addAction(NetworkMonitorService.NETWORK_SERVICE_STATUS_KEY)
        registerReceiver(networkStatusBroadcastReceiver, intentFilter)

    }

    private fun updateConnectionStatus(status : NetworkConnectionStatus)
    {
        if(networkConnectionStatus != status)
        {
            networkConnectionStatus = status
            runOnUiThread {
                // pop up a dialog if the wifi is off
                if(status == NetworkConnectionStatus.WIFI_NOT_AVAILABLE)
                {
                    InfoDialog( this, resources.getString(R.string.wifi_disabled),
                        resources.getString(R.string.wifi_disabled_message), resources.getString(R.string.ok), null, this)
                }

            }

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

    override fun onDestroy() {
        super.onDestroy()
        this.unbindService( networkMonitorConnection)
        Log.d("xx", "DESTROY")

        networkViewModel.shutdown()
    }

    override fun didSelectOkButton(tag: Any?) {

    }

}