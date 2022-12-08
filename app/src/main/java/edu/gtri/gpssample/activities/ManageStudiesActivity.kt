package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.StudiesAdapter
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.databinding.ActivityManageStudiesBinding
import edu.gtri.gpssample.models.StudyModel

class ManageStudiesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStudiesBinding
    private lateinit var studiesAdapter: StudiesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManageStudiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val configName = intent.getStringExtra( Key.ConfigurationName.toString())
        binding.configNameTextView.text = configName + " Studies"

        studiesAdapter = StudiesAdapter((application as MainApplication).studies)
        studiesAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = studiesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this )

        binding.createButton.setOnClickListener {
            val intent = Intent(this, CreateStudyActivity::class.java)
            startActivity( intent )
            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.backButton.setOnClickListener {

            finish()
            overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onResume()
    {
        super.onResume()

        if ((application as MainApplication).studies.isEmpty())
        {
            binding.recyclerView.visibility = View.GONE
            binding.relativeLayout.visibility = View.VISIBLE
        }
        else
        {
            binding.recyclerView.visibility = View.VISIBLE
            binding.relativeLayout.visibility = View.GONE
        }

        studiesAdapter.updateStudies((application as MainApplication).studies)
    }

    fun onItemSelected(studyModel: StudyModel, shouldDismissKeyboard: Boolean )
    {
        val intent = Intent(this, StudyActivity::class.java)
        intent.putExtra( Key.StudyName.toString(), studyModel.name )
        startActivity( intent )
        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_study, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_create_study -> {
                val intent = Intent(this, CreateStudyActivity::class.java)
                startActivity( intent )
                overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
            }
        }

        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}