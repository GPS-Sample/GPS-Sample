package edu.gtri.gpssample.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.databinding.ActivityStudyBinding
import edu.gtri.gpssample.models.StudyModel

class StudyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studyName = intent.getStringExtra( Key.StudyName.toString())

        binding.studyNameTextView.text = studyName + " Study"

        binding.imageView.setImageBitmap( (application as MainApplication).bitmap )

        binding.backButton.setOnClickListener {

            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_study, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_study -> {
            }
            R.id.action_delete_study -> {
            }
        }

        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}