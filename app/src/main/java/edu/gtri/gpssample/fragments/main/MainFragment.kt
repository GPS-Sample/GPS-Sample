/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.main

import android.Manifest
import android.content.*
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mapbox.maps.Style
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentMainBinding
import androidx.core.content.edit
import edu.gtri.gpssample.ui.compose.ComposableNotificationDialogHost

class MainFragment : Fragment()
{
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var composableNotificationDialogHost: ComposableNotificationDialogHost

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val key = "IsFirstRun"
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("default", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean(key, true ))
        {
            sharedPreferences.edit(commit = true) { putBoolean(key, false) }

            var privacyPolicyStatement = resources.getString( R.string.privacy_policy_statement_1_5 ) + "\n"
            privacyPolicyStatement += resources.getString( R.string.privacy_policy_statement_2_5 ) + "\n"
            privacyPolicyStatement += resources.getString( R.string.privacy_policy_statement_3_5 ) + "\n"
            privacyPolicyStatement += resources.getString( R.string.privacy_policy_statement_4_5 ) + "\n"
            privacyPolicyStatement += resources.getString( R.string.privacy_policy_statement_5_5 ) + "\n"

            composableNotificationDialogHost.show(
                title = resources.getString(R.string.background_location_permission),
                message = privacyPolicyStatement,
                buttonText = resources.getString(R.string.ok),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)

        if (sharedPreferences.getString( Keys.kMapStyle.value, null ) == null)
        {
            sharedPreferences.edit(commit = true) {
                putString(Keys.kMapStyle.value, Style.MAPBOX_STREETS)
            }
        }
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().setTitle( "GPSSample" )

        composableNotificationDialogHost = ComposableNotificationDialogHost()

        binding.dialogComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        binding.dialogComposeView.setContent {
            composableNotificationDialogHost.Content()
        }

        binding.appVersionTextView.text = resources.getString(R.string.app_version) + " " + BuildConfig.VERSION_NAME
        binding.dbVersionTextView.text = resources.getString(R.string.db_version) + " #" + DAO.DATABASE_VERSION

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)

        val termsAccepted = sharedPreferences.getBoolean( Keys.kTermsAccepted.value, false )

        if (!termsAccepted)
        {
            val bundle = Bundle()
            bundle.putBoolean( Keys.kIsOnBoarding.value, true )
            findNavController().navigate(R.id.action_navigate_to_AboutFragment,bundle)
            return
        }

        var highestRole = Role.Undefined

        val users = DAO.userDAO.getUsers()

        users.find{ user ->  user.role == Role.Admin.value}?.let {
            highestRole = Role.Admin
        }

        if (highestRole == Role.Undefined)
        {
            users.find{ user ->  user.role == Role.Supervisor.value}?.let {
                highestRole = Role.Supervisor
            }
        }

        if (highestRole == Role.Undefined)
        {
            users.find{ user ->  user.role == Role.Enumerator.value}?.let {
                highestRole = Role.Enumerator
            }
        }

        if (highestRole == Role.Undefined)
        {
            users.find{ user ->  user.role == Role.DataCollector.value}?.let {
                highestRole = Role.DataCollector
            }
        }

        val userName = sharedPreferences.getString( Keys.kUserName.value, null)

        userName?.let {
            DAO.userDAO.getUser(userName)?.let { user ->
                view.post {
                    when (user.role)
                    {
                        Role.Admin.value ->
                        {
                            binding.adminButton.isChecked = true
                        }
                        Role.Supervisor.value ->
                        {
                            binding.supervisorButton.isChecked = true
                            if (highestRole == Role.Supervisor)
                            {
                                binding.adminRow.visibility = View.GONE
                            }
                        }
                        Role.Enumerator.value ->
                        {
                            binding.enumeratorButton.isChecked = true
                            if (highestRole == Role.Enumerator)
                            {
                                binding.adminRow.visibility = View.GONE
                                binding.supervisorRow.visibility = View.GONE
                            }
                            else if (highestRole == Role.Supervisor)
                            {
                                binding.adminRow.visibility = View.GONE
                            }
                        }
                        Role.DataCollector.value ->
                        {
                            binding.dataCollectorButton.isChecked = true
                            if (highestRole == Role.DataCollector)
                            {
                                binding.adminRow.visibility = View.GONE
                                binding.supervisorRow.visibility = View.GONE
                                binding.enumeratorRow.visibility = View.GONE
                                binding.signUpButton.visibility = View.GONE
                            }
                            else if (highestRole == Role.Enumerator)
                            {
                                binding.adminRow.visibility = View.GONE
                                binding.supervisorRow.visibility = View.GONE
                            }
                            else if (highestRole == Role.Supervisor)
                            {
                                binding.adminRow.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }

        binding.selectRoleTip.setOnClickListener {
            composableNotificationDialogHost.show(
                title = "",
                message = resources.getString(R.string.role_hint),
                buttonText = resources.getString(R.string.ok),
            )
        }

        val radioButtons = listOf(
            binding.adminButton,
            binding.supervisorButton,
            binding.enumeratorButton,
            binding.dataCollectorButton
        )

        fun select(selected: RadioButton) {
            radioButtons.forEach { it.isChecked = it == selected }
        }

        binding.adminRow.setOnClickListener {
            select(binding.adminButton )
        }

        binding.supervisorRow.setOnClickListener {
            select(binding.supervisorButton )
        }

        binding.enumeratorRow.setOnClickListener {
            select(binding.enumeratorButton )
        }

        binding.dataCollectorRow.setOnClickListener {
            select(binding.dataCollectorButton )
        }

        binding.adminTip.isClickable = true
        binding.adminTip.isFocusable = true

        binding.supervisorTip.isClickable = true
        binding.supervisorTip.isFocusable = true

        binding.enumeratorTip.isClickable = true
        binding.enumeratorTip.isFocusable = true

        binding.dataCollectorTip.isClickable = true
        binding.dataCollectorTip.isFocusable = true

        binding.adminTip.setOnClickListener {
            composableNotificationDialogHost.show(
                title = "",
                message = resources.getString(R.string.admin_hint),
                buttonText = resources.getString(R.string.ok),
            )
        }

        binding.supervisorTip.setOnClickListener {
            composableNotificationDialogHost.show(
                title = "",
                message = resources.getString(R.string.supervisor_hint),
                buttonText = resources.getString(R.string.ok),
            )
        }

        binding.enumeratorTip.setOnClickListener {
            composableNotificationDialogHost.show(
                title = "",
                message = resources.getString(R.string.enumerator_hint),
                buttonText = resources.getString(R.string.ok),
            )
        }

        binding.dataCollectorTip.setOnClickListener {
            composableNotificationDialogHost.show(
                title = "",
                message = resources.getString(R.string.data_collector_hint),
                buttonText = resources.getString(R.string.ok),
            )
        }

        binding.webpageLink.paintFlags = binding.webpageLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        binding.webpageLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gpssample.org"))
            startActivity(intent)
        }

        binding.signInButton.setOnClickListener {
            var bundle: Bundle? = null

            if (binding.adminButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.Admin.value)
            }
            else if (binding.supervisorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.Supervisor.value)
            }
            else if (binding.enumeratorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.Enumerator.value)
            }
            else if (binding.dataCollectorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.DataCollector.value)
            }

            if (bundle == null)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.please_select_a_role), Toast.LENGTH_SHORT).show()
            }
            else
            {
                findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
            }
        }

        binding.signUpButton.setOnClickListener {
            var bundle: Bundle? = null

            if (binding.adminButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.Admin.value)
            }
            else if (binding.supervisorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.Supervisor.value)
            }
            else if (binding.enumeratorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.Enumerator.value)
            }
            else if (binding.dataCollectorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.value, Role.DataCollector.value)
            }

            if (bundle == null)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.please_select_a_role), Toast.LENGTH_SHORT).show()
            }
            else
            {
                binding.adminButton.isChecked = false
                binding.supervisorButton.isChecked = false
                binding.dataCollectorButton.isChecked = false

                binding.signInButton.isEnabled = false
                binding.signUpButton.isEnabled = false

                findNavController().navigate(R.id.action_navigate_to_SignUpFragment, bundle)
            }
        }

        permissionLauncher.launch( REQUIRED_RUNTIME_PERMISSIONS )
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.MainFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onDestroyView()
    {
        _binding = null

        super.onDestroyView()
    }

    companion object
    {
        private val REQUIRED_RUNTIME_PERMISSIONS: Array<String> =
            buildList {
                add(Manifest.permission.CAMERA)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)

                // Android 12+ (API 31)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.BLUETOOTH_ADVERTISE)
                }

                // Android 13+ (API 33)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }

                // Android 9 and below
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            }.toTypedArray()
    }
}