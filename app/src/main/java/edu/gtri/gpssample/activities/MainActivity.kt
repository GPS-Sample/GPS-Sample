/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.activities

import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.os.Build
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
import com.google.android.gms.security.ProviderInstaller
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.databinding.ActivityMainBinding
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.receivers.NetworkStatusBroadcastReceiver
import edu.gtri.gpssample.services.NetworkMonitorService
import edu.gtri.gpssample.services.UDPBroadcastReceiverService
import edu.gtri.gpssample.utils.NetworkConnectionStatus
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel

class MainActivity : AppCompatActivity(), InfoDialog.InfoDialogDelegate, ProviderInstaller.ProviderInstallListener
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var udpBroadcastReceiverService: UDPBroadcastReceiverService
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        ProviderInstaller.installIfNeededAsync(this, this)

        // build view models
        val viewModel: ConfigurationViewModel by viewModels()
        val networkVm : NetworkViewModel by viewModels()
        networkViewModel = networkVm

        DAO.createSharedInstance( applicationContext )
        ImageDAO.createSharedInstance( applicationContext )

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
            (this.application as? MainApplication)?.currentFragment?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        val networkMonitorIntent = Intent(this, NetworkMonitorService::class.java)
        this.bindService( networkMonitorIntent, networkMonitorConnection, Context.BIND_AUTO_CREATE)

        networkStatusBroadcastReceiver = NetworkStatusBroadcastReceiver( ::updateConnectionStatus)

        intentFilter = IntentFilter()
        intentFilter.addAction(NetworkMonitorService.NETWORK_SERVICE_STATUS_KEY)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU)
        {
            registerReceiver(networkStatusBroadcastReceiver, intentFilter, RECEIVER_EXPORTED)
        }
        else
        {
            registerReceiver(networkStatusBroadcastReceiver, intentFilter)
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        // TODO: Figure out how to save the ViewModel state.
    }

    private fun updateConnectionStatus(status : NetworkConnectionStatus)
    {
        if(networkConnectionStatus != status)
        {
            networkConnectionStatus = status

            val sharedPreferences: SharedPreferences = getSharedPreferences("default", 0)
            val isFirstRun = sharedPreferences.getBoolean("IsFirstRun", true )

            if (!isFirstRun)
            {
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
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed()
    {
        if (supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.backStackEntryCount == 1)
        {
            finish()
        }
        else
        {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        this.unbindService( networkMonitorConnection)
        networkViewModel.shutdown()
    }

    override fun didSelectOkButton(tag: Any?)
    {
    }

    override fun onProviderInstallFailed(p0: Int, p1: Intent?)
    {
        Log.d( "xxx", "onProviderInstallFailed")
    }

    override fun onProviderInstalled()
    {
        Log.d( "xxx", "onProviderInstalledPassed" )
    }
}