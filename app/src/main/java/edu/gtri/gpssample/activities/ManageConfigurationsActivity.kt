package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.ConfigurationsAdapter
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.databinding.ActivityManageConfigurationsBinding
import edu.gtri.gpssample.models.ConfigurationModel

class ManageConfigurationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageConfigurationsBinding
    private lateinit var configurationsAdapter: ConfigurationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManageConfigurationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurationsAdapter = ConfigurationsAdapter((application as MainApplication).configurations)
        configurationsAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = configurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this )

        binding.createButton.setOnClickListener {
            val intent = Intent(this, CreateConfigurationActivity::class.java)
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

        if ((application as MainApplication).configurations.isEmpty())
        {
            binding.recyclerView.visibility = View.GONE
            binding.relativeLayout.visibility = View.VISIBLE
        }
        else
        {
            binding.recyclerView.visibility = View.VISIBLE
            binding.relativeLayout.visibility = View.GONE
        }

        configurationsAdapter.updateConfigurations((application as MainApplication).configurations)
    }

    fun onItemSelected( configurationModel: ConfigurationModel, shouldDismissKeyboard: Boolean )
    {
        val intent = Intent(this, ManageStudiesActivity::class.java)
        intent.putExtra( Key.ConfigurationName.toString(), configurationModel.name )
        startActivity( intent )
        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_configuration, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_create_configuration -> {
                val intent = Intent(this, CreateConfigurationActivity::class.java)
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