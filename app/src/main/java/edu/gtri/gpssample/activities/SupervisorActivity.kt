package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.ActivitySupervisorBinding

class SupervisorActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupervisorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySupervisorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {

            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}