package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivityBarcodeScanBinding
import java.util.*

class BarcodeScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var role: Role;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        role = Role.valueOf(intent.getStringExtra("role")!!)

        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        when (role) {
            Role.Admin -> binding.titleTextView.text = "Admin"
            Role.Supervisor -> binding.titleTextView.text = "Supervisor"
            Role.Enumerator -> binding.titleTextView.text = "Enumerator"
            else -> {}
        }

        binding.scanButton.setOnClickListener {

            val optionsBuilder = GmsBarcodeScannerOptions.Builder()
            val gmsBarcodeScanner = GmsBarcodeScanning.getClient(this, optionsBuilder.build())
            gmsBarcodeScanner
                .startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    Log.d( "xxx", getSuccessfulMessage(barcode)!!)
                }
                .addOnFailureListener { e: Exception -> Log.d( "xxx", getErrorMessage(e)!!) }
                .addOnCanceledListener {
                    Log.d( "xxx", getString(R.string.error_scanner_cancelled) );
                    startNextActivity()
                }
        }

        binding.continueButton.setOnClickListener {
            startNextActivity()
        }
    }

    private fun startNextActivity()
    {
        lateinit var intent: Intent

        when (role) {
            Role.Supervisor -> intent = Intent(this, SupervisorActivity::class.java)
            Role.Enumerator -> intent = Intent(this, EnumeratorActivity::class.java)
            else -> {}
        }

        intent.putExtra( "role", role.toString() )

        startActivity( intent )

        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)

        finish()
    }

    private fun getSuccessfulMessage(barcode: Barcode): String {
        val barcodeValue =
            String.format(
                Locale.US,
                "Display Value: %s\nRaw Value: %s\nFormat: %s\nValue Type: %s",
                barcode.displayValue,
                barcode.rawValue,
                barcode.format,
                barcode.valueType
            )
        return getString(R.string.barcode_result, barcodeValue)
    }

    private fun getErrorMessage(e: Exception): String? {
        return if (e is MlKitException) {
            when (e.errorCode) {
                MlKitException.CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED ->
                    getString(R.string.error_camera_permission_not_granted)
                MlKitException.CODE_SCANNER_APP_NAME_UNAVAILABLE ->
                    getString(R.string.error_app_name_unavailable)
                else -> getString(R.string.error_default_message, e)
            }
        } else {
            e.message
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        val intent = Intent(this, SignInSignUpActivity::class.java)
        startActivity( intent )
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}