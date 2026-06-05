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
import android.os.Bundle
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
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class MainActivity : AppCompatActivity(), InfoDialog.InfoDialogDelegate, ProviderInstaller.ProviderInstallListener
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        ProviderInstaller.installIfNeededAsync(this, this)

        // build view models
        val viewModel: ConfigurationViewModel by viewModels()

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
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        // TODO: Figure out how to save the ViewModel state.
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

    override fun onDestroy()
    {
        super.onDestroy()
    }
}