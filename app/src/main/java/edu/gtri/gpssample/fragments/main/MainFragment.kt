package edu.gtri.gpssample.fragments.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.FragmentMainBinding

class MainFragment : Fragment()
{
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.versionTextView.text = BuildConfig.VERSION_NAME

        binding.signInButton.isEnabled = false

        binding.adminButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpTextView.isEnabled = true
            binding.signUpTextView.setTextColor( resources.getColor( R.color.primary_textcolor, null))
        }

        binding.supervisorButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpTextView.isEnabled = true
            binding.signUpTextView.setTextColor( resources.getColor( R.color.primary_textcolor, null))
        }

        binding.dataCollectorButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpTextView.isFocusable = false
            binding.signUpTextView.setTextColor( resources.getColor( R.color.primary_textcolor, null))
        }

        binding.signInButton.setOnClickListener {
            val bundle = Bundle()

            if (binding.adminButton.isChecked) {
                bundle.putString( Keys.kRole.toString(), Role.Admin.toString())
            }
            else if (binding.supervisorButton.isChecked) {
                bundle.putString( Keys.kRole.toString(), Role.Supervisor.toString())
            }
            else if (binding.dataCollectorButton.isChecked) {
                bundle.putString( Keys.kRole.toString(), Role.DataCollector.toString())
            }

            binding.adminButton.isChecked = false
            binding.supervisorButton.isChecked = false
            binding.dataCollectorButton.isChecked = false

            binding.signInButton.isEnabled = false
            binding.signUpTextView.isEnabled = false

            binding.signUpTextView.setTextColor( resources.getColor( R.color.light_gray, null))

            findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
        }

        binding.signUpTextView.setOnClickListener {
            var bundle: Bundle? = null

            if (binding.adminButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.toString(), Role.Admin.toString())
            }
            else if (binding.supervisorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.toString(), Role.Supervisor.toString())
            }
            else if (binding.dataCollectorButton.isChecked) {
                bundle = Bundle()
                bundle.putString( Keys.kRole.toString(), Role.DataCollector.toString())
            }

            if (bundle != null)
            {
                binding.adminButton.isChecked = false
                binding.supervisorButton.isChecked = false
                binding.dataCollectorButton.isChecked = false

                binding.signInButton.isEnabled = false
                binding.signUpTextView.isEnabled = false

                binding.signUpTextView.setTextColor( resources.getColor( R.color.light_gray, null))

                findNavController().navigate(R.id.action_navigate_to_SignUpFragment, bundle)
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
            permission?.let {
                if (!isPermissionGranted(activity as AppCompatActivity, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(activity as AppCompatActivity, it)) {
                    permissionsToRequest.add(permission)
                }
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
        private const val OBJECT_DETECTION = "Object Detection"
        private const val BARCODE_SCANNING = "Barcode Scanning"

        private const val STATE_SELECTED_MODEL = "selected_model"
        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
    }
}