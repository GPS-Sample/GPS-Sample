package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.databinding.ActivityCreateConfigurationBinding
import edu.gtri.gpssample.models.ConfigurationModel

class CreateConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateConfigurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {

            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
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

                val intent = Intent(this, DefineEnumerationAreaActivity::class.java)
                startActivity( intent )
                overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}