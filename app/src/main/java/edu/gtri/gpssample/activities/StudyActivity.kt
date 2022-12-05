package edu.gtri.gpssample.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.OnlineStatusAdapter
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.databinding.ActivityStudyBinding
import edu.gtri.gpssample.models.StudyModel
import edu.gtri.gpssample.models.UserModel

class StudyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyBinding
    private lateinit var onlineStatusAdapter: OnlineStatusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studyName = intent.getStringExtra( Key.StudyName.toString())

        binding.studyNameTextView.text = studyName + " Study"

        binding.imageView.setImageBitmap( (application as MainApplication).barcodeBitmap )

        val user1 = UserModel()
        user1.name = "x"
        (application as MainApplication).users.add( user1 )

        val user2 = UserModel()
        user2.name = "y"
        (application as MainApplication).users.add( user2 )

        val user3 = UserModel()
        user3.name = "z"
        (application as MainApplication).users.add( user3 )

        onlineStatusAdapter = OnlineStatusAdapter((application as MainApplication).users)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = onlineStatusAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this )

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