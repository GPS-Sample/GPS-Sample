package edu.gtri.gpssample.activities

import android.os.Bundle
import edu.gtri.gpssample.R
import edu.gtri.gpssample.MainApplication
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.databinding.ActivityCreateFieldBinding
import edu.gtri.gpssample.models.FieldModel

class CreateFieldActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityCreateFieldBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveButton.setOnClickListener {

            if (binding.fieldNameEditText.text.toString().length > 0)
            {
                var fieldModel = FieldModel()
                fieldModel.name = binding.fieldNameEditText.text.toString()
                (application as MainApplication).fields.add( fieldModel )

                finish()
                overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
            }
        }

        binding.cancelButton.setOnClickListener {
            finish()
            overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onBackPressed()
    {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}