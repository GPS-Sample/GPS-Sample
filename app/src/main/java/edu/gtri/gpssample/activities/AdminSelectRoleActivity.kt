package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.ActivityAdminSelectRoleBinding


class AdminSelectRoleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSelectRoleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminSelectRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.adminButton.setOnClickListener {
            val intent = Intent(this, ManageConfigurationsActivity::class.java)
            startActivity( intent )
            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.supervisorButton.setOnClickListener {
            val intent = Intent(this, SupervisorSelectRoleActivity::class.java)
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