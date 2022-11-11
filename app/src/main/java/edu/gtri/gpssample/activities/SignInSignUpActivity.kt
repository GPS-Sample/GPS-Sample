package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

        binding.adminSignInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra( "role", Role.Admin.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.adminSignUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra( "role", Role.Admin.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.supervisorSignInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra( "role", Role.Supervisor.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.supervisorSignUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra( "role", Role.Supervisor.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.enumeratorSignInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra( "role", Role.Enumerator.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.enumeratorSignUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra( "role", Role.Enumerator.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }
    }
}