/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.main

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class MainFragment : Fragment()
{
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        val zoomLevel = sharedPreferences.getInt( Keys.kZoomLevel.value, 16 )

        val sharedViewModel : ConfigurationViewModel by activityViewModels()
        sharedViewModel.setCurrentZoomLevel( zoomLevel.toDouble())

        if (sharedPreferences.getString( Keys.kMapStyle.value, null ) == null)
        {
            val editor = sharedPreferences.edit()
            editor.putString( Keys.kMapStyle.value, Style.MAPBOX_STREETS )
            editor.commit()
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

        activity!!.setTitle( "GPSSample" )

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
            NotificationDialog( requireActivity(), "", resources.getString(R.string.role_hint))
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
            NotificationDialog( requireActivity(), "", resources.getString(R.string.admin_hint))
        }

        binding.supervisorTip.setOnClickListener {
            NotificationDialog( requireActivity(), "", resources.getString(R.string.supervisor_hint))
        }

        binding.enumeratorTip.setOnClickListener {
            NotificationDialog( requireActivity(), "", resources.getString(R.string.enumerator_hint))
        }

        binding.dataCollectorTip.setOnClickListener {
            NotificationDialog( requireActivity(), "", resources.getString(R.string.data_collector_hint))
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

        val key = "IsFirstRun"

        view.post {
            if (sharedPreferences.getBoolean(key, true ))
            {
                val editor = sharedPreferences.edit()
                editor.putBoolean( key, false )
                editor.commit()
                view.post {
                    if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        NotificationDialog(requireActivity(), resources.getString(R.string.background_location_permission), resources.getString(R.string.privacy_policy_statement))
                    }
                }
            }
        }

        if (!allRuntimePermissionsGranted())
        {
            getRuntimePermissions()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.MainFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            if (!isPermissionGranted(activity as AppCompatActivity, permission)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            if (!isPermissionGranted(activity as AppCompatActivity, permission)) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty())
        {
            requestPermissions( permissionsToRequest.toTypedArray(), REQUEST_CODE)
        }

        if (isPermissionGranted( activity as AppCompatActivity, Manifest.permission.ACCESS_FINE_LOCATION)
            && !isPermissionGranted( activity as AppCompatActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                requestPermissions( arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1001 )
            }
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean
    {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
        {
            return true
        }

        return false
    }

    override fun onDestroyView()
    {
        _binding = null

        super.onDestroyView()
    }

    companion object
    {
        private const val REQUEST_CODE = 1

        private val REQUIRED_RUNTIME_PERMISSIONS: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
    }
}