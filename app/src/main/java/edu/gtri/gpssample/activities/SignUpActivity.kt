package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
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

        val role = Role.valueOf(intent.getStringExtra("role")!!)

        when (role) {
            Role.Admin -> binding.titleTextView.text = resources.getString( R.string.admin_sign_up )
            Role.Supervisor -> binding.titleTextView.text = resources.getString( R.string.supervisor_sign_up )
            Role.Enumerator -> binding.titleTextView.text = resources.getString( R.string.data_collector_sign_up )
        }

        ArrayAdapter.createFromResource(this, R.array.forgot_pin_questions, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.questionSpinner.adapter = adapter
            }

        binding.nextButton.setOnClickListener {

            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra( "role", role.toString())
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.backButton.setOnClickListener {
            this.setResult(RESULT_CANCELED )
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}