package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val roleVal = intent.getStringExtra("role")
        val role = Role.valueOf(roleVal!!.toString())

        binding.pinEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.titleTextView.text = roleVal + " Sign In"

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.pinEditText.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (binding.pinEditText.getText().toString().length >= 4) {
                lateinit var intent: Intent

                when (role) {
                    Role.Admin -> intent = Intent(this, AdminActivity::class.java)
                    Role.Supervisor -> intent = Intent(this, SupervisorActivity::class.java)
                    Role.Enumerator -> intent = Intent(this, EnumeratorActivity::class.java)
                    else -> {}
                }

                startActivity( intent )
                overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)

                // clear the back stack of all previous activities
                ActivityCompat.finishAffinity(this)
            }

            false
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}