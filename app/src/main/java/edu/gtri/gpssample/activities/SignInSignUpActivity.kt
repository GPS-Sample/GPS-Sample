package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivitySignInSignUpBinding

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
    }
}