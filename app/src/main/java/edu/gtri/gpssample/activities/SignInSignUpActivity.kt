package edu.gtri.gpssample.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import edu.gtri.gpssample.MainActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivitySignInSignUpBinding
import java.util.ArrayList

class SignInSignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInSignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.signInButton.isEnabled = false
        binding.signUpButton.isEnabled = false
        binding.signUpButton.setTextColor( resources.getColor( R.color.light_gray, null))

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
        binding.enumeratorButton.setOnClickListener {
            binding.signInButton.isEnabled = true
            binding.signUpButton.isEnabled = true
            binding.signUpButton.setTextColor( resources.getColor( R.color.blue, null))
        }

        binding.signInButton.setOnClickListener {

            val intent = Intent(this, SignInActivity::class.java)

            if (binding.adminButton.isChecked) {
                intent.putExtra( "role", Role.Admin.toString())
            }
            else if (binding.supervisorButton.isChecked) {
                intent.putExtra( "role", Role.Supervisor.toString())
            }
            else if (binding.enumeratorButton.isChecked) {
                intent.putExtra( "role", Role.Enumerator.toString())
            }

            startActivity( intent )

            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.signUpButton.setOnClickListener {

            val intent = Intent(this, SignUpActivity::class.java)

            if (binding.adminButton.isChecked) {
                intent.putExtra( "role", Role.Admin.toString())
            }
            else if (binding.supervisorButton.isChecked) {
                intent.putExtra( "role", Role.Supervisor.toString())
            }
            else if (binding.enumeratorButton.isChecked) {
                intent.putExtra( "role", Role.Enumerator.toString())
            }

            startActivity( intent )

            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in SignInSignUpActivity.REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in SignInSignUpActivity.REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                SignInSignUpActivity.PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(SignInSignUpActivity.TAG, "Permission granted: $permission")
            return true
        }
        Log.i(SignInSignUpActivity.TAG, "Permission NOT granted: $permission")
        return false
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val OBJECT_DETECTION = "Object Detection"
        private const val BARCODE_SCANNING = "Barcode Scanning"

        private const val STATE_SELECTED_MODEL = "selected_model"
        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
    }
}