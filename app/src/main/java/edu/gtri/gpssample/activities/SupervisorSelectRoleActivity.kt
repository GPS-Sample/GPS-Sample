package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.databinding.ActivitySupervisorSelectRoleBinding

class SupervisorSelectRoleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupervisorSelectRoleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySupervisorSelectRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.supervisorButton.setOnClickListener {
            val intent = Intent(this, SupervisorActivity::class.java)
            startActivity( intent )
            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.signOutButton.setOnClickListener {
            finish()
            overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}