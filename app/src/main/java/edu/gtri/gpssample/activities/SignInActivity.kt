package edu.gtri.gpssample.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pinEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        val role = Role.valueOf(intent.getStringExtra("role")!!)

        when (role) {
            Role.Admin -> binding.titleTextView.text = resources.getString( R.string.admin_sign_in )
            Role.Supervisor -> binding.titleTextView.text = resources.getString( R.string.supervisor_sign_in )
            Role.Enumerator -> binding.titleTextView.text = resources.getString( R.string.enumerator_sign_in )
        }

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.pinEditText.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->

            if (binding.pinEditText.getText().toString().length >= 4)
            {
                (application as MainApplication).fields.clear()
                (application as MainApplication).studies.clear()
                (application as MainApplication).configurations.clear()

                lateinit var intent: Intent

                when (role) {
                    Role.Admin -> intent = Intent(this, AdminSelectRoleActivity::class.java)
                    Role.Supervisor -> intent = Intent(this, BarcodeScanActivity::class.java)
                    Role.Enumerator -> intent = Intent(this, BarcodeScanActivity::class.java)
                }

                intent.putExtra( "role", role.toString())

                startActivity( intent )

                finish()

                overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)

//                // clear the back stack of all previous activities
//                ActivityCompat.finishAffinity(this)
            }

            false
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}