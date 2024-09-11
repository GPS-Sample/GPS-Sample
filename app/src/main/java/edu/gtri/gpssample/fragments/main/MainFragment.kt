package edu.gtri.gpssample.fragments.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.common.io.Resources
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentMainBinding
import edu.gtri.gpssample.receivers.NetworkStatusBroadcastReceiver
import edu.gtri.gpssample.services.NetworkMonitorService.Companion.NETWORK_SERVICE_STATUS_KEY
import edu.gtri.gpssample.utils.NetworkConnectionStatus

class MainFragment : Fragment()
{
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val id: Long = 4294967295

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
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

//        if (DAO.userDAO.getUser("@test-admin") == null)
//        {
//            val x = id.toString()
//            var p = "" + x.get(9)
//            p += x.get(5)
//            p += x.get(0)
//            p += x.get(6)
//
//            val user = User( "@test-admin", p.toInt(), Role.Admin.value, "", "", false )
//            DAO.userDAO.createUser( user )
//        }

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
                            binding.adminButton.visibility = View.GONE
                        }
                    }
                    Role.Enumerator.value ->
                    {
                        binding.enumeratorButton.isChecked = true
                        if (highestRole == Role.Enumerator)
                        {
                            binding.adminButton.visibility = View.GONE
                            binding.supervisorButton.visibility = View.GONE
                        }
                        else if (highestRole == Role.Supervisor)
                        {
                            binding.adminButton.visibility = View.GONE
                        }
                    }
                    Role.DataCollector.value ->
                    {
                        binding.dataCollectorButton.isChecked = true
                        if (highestRole == Role.DataCollector)
                        {
                            binding.adminButton.visibility = View.GONE
                            binding.supervisorButton.visibility = View.GONE
                            binding.enumeratorButton.visibility = View.GONE
                            binding.signUpTextView.visibility = View.GONE
                        }
                        else if (highestRole == Role.Enumerator)
                        {
                            binding.adminButton.visibility = View.GONE
                            binding.supervisorButton.visibility = View.GONE
                        }
                        else if (highestRole == Role.Supervisor)
                        {
                            binding.adminButton.visibility = View.GONE
                        }
                    }
                }
            }
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

        binding.signUpTextView.setOnClickListener {
            var bundl: Bundle? = null

            if (binding.adminButton.isChecked) {
                bundl = Bundle()
                bundl.putString( Keys.kRole.value, Role.Admin.value)
            }
            else if (binding.supervisorButton.isChecked) {
                bundl = Bundle()
                bundl.putString( Keys.kRole.value, Role.Supervisor.value)
            }
            else if (binding.enumeratorButton.isChecked) {
                bundl = Bundle()
                bundl.putString( Keys.kRole.value, Role.Enumerator.value)
            }
            else if (binding.dataCollectorButton.isChecked) {
                bundl = Bundle()
                bundl.putString( Keys.kRole.value, Role.DataCollector.value)
            }

            if (bundl == null)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.please_select_a_role), Toast.LENGTH_SHORT).show()
            }
            else
            {
                binding.adminButton.isChecked = false
                binding.supervisorButton.isChecked = false
                binding.dataCollectorButton.isChecked = false

                binding.signInButton.isEnabled = false
                binding.signUpTextView.isEnabled = false

                findNavController().navigate(R.id.action_navigate_to_SignUpFragment, bundl)
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

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity as AppCompatActivity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
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
        super.onDestroyView()

        _binding = null
    }

    companion object
    {
        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                Manifest.permission.UPDATE_DEVICE_STATS,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
    }
}