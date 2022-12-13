package edu.gtri.gpssample.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.FragmentSignInSignUpBinding

class SignInSignUpFragment : Fragment()
{
    private var _binding: FragmentSignInSignUpBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentSignInSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.adminButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpButton.isEnabled = true
            binding.signUpButton.setTextColor( resources.getColor( R.color.blue, null))
        }
        binding.supervisorButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpButton.isEnabled = true
            binding.signUpButton.setTextColor( resources.getColor( R.color.blue, null))
        }
        binding.dataCollectorButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpButton.isEnabled = true
            binding.signUpButton.setTextColor( resources.getColor( R.color.blue, null))
        }

        binding.signInButton.setOnClickListener {
            var bundle = Bundle()

            if (binding.adminButton.isChecked) {
                bundle.putInt( "role", Role.Admin.value)
            }
            else if (binding.supervisorButton.isChecked) {
                bundle.putInt( "role", Role.Supervisor.value)
            }
            else if (binding.dataCollectorButton.isChecked) {
                bundle.putInt( "role", Role.Enumerator.value)
            }

            findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
        }

        binding.signUpButton.setOnClickListener {
        }

        if (!allRuntimePermissionsGranted())
        {
            getRuntimePermissions()
        }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in SignInSignUpFragment.REQUIRED_RUNTIME_PERMISSIONS) {
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
        for (permission in SignInSignUpFragment.REQUIRED_RUNTIME_PERMISSIONS) {
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
                SignInSignUpFragment.PERMISSION_REQUESTS
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