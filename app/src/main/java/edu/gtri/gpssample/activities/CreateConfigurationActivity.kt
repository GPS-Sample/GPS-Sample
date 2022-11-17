package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.ActivityCreateConfigurationBinding
import edu.gtri.gpssample.models.ConfigurationModel

class CreateConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateConfigurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {

            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
            finish()
        }

        ArrayAdapter.createFromResource(this, R.array.preferred_units, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.preferredUnitsSpinner.adapter = adapter
            }

        ArrayAdapter.createFromResource(this, R.array.date_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.dateFormatSpinner.adapter = adapter
            }

        ArrayAdapter.createFromResource(this, R.array.time_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.timeFormatSpinner.adapter = adapter
            }

        binding.nextButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().length > 0) {
                val mainApplication = application as MainApplication

                val configurationModel = ConfigurationModel()

                configurationModel.name = binding.configNameEditText.text.toString()

                mainApplication.configurations.add( configurationModel )

                Log.d( "xxx", mainApplication.configurations[0].name!!)

                onBackPressed()
            }

//            val intent = Intent(this, DefineEnumerationAreaActivity::class.java)
//            startActivity( intent )
//            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
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