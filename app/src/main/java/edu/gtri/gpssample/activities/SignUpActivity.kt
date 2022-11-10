package edu.gtri.gpssample.activities

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val roleVal = intent.getStringExtra("role" );
        val role = Role.valueOf( roleVal!!.toString() );

        binding.titleTextView.text = roleVal + " Sign Up";

        ArrayAdapter.createFromResource(
            this,
            R.array.forgot_pin_questions,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.questionSpinner.adapter = adapter
        }

        binding.backButton.setOnClickListener {
            onBackPressed();
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}