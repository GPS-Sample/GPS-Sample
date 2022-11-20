package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivityBarcodeScanBinding

class BarcodeScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var role: Role

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        role = Role.valueOf(intent.getStringExtra("role")!!)

        when (role) {
            Role.Admin -> binding.titleTextView.text = resources.getString( R.string.admin )
            Role.Supervisor -> binding.titleTextView.text = resources.getString( R.string.supervisor )
            Role.Enumerator -> binding.titleTextView.text = resources.getString( R.string.enumerator )
        }

        binding.scanButton.setOnClickListener {
            val intent = Intent(this, CameraXLivePreviewActivity::class.java)
            startActivityForResult( intent, 0 )
            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.continueButton.setOnClickListener {
            startNextActivityForRole( role )
        }

        binding.signOutButton.setOnClickListener {

            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == ResultCode.BarcodeScanned.value)
        {
            val payload = data!!.getStringExtra( "value" )
            binding.payloadTextView.text = payload!!
//            startNextActivityForRole( role )
        }
    }

    private fun startNextActivityForRole( role: Role )
    {
        finish()

        lateinit var intent: Intent

        when (role) {
            Role.Supervisor -> intent = Intent(this, SupervisorSelectRoleActivity::class.java)
            Role.Enumerator -> intent = Intent(this, EnumeratorActivity::class.java)
            else -> {}
        }

        intent.putExtra( "role", role.toString() )

        startActivity( intent )

        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}