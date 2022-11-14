package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.ActivityDefineEnumerationFieldsBinding

class DefineEnumerationFieldsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDefineEnumerationFieldsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDefineEnumerationFieldsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {

            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
            finish()
        }

        binding.nextButton.setOnClickListener {
            val intent = Intent(this, DefineSamplingMethodActivity::class.java)
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
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