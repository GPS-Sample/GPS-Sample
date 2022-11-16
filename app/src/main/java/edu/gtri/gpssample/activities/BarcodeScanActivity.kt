package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
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