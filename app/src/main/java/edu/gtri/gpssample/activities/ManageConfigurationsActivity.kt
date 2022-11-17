package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.ConfigurationsAdapter
import edu.gtri.gpssample.databinding.ActivityManageConfigurationsBinding
import edu.gtri.gpssample.models.ConfigurationModel

class ManageConfigurationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageConfigurationsBinding
    private lateinit var configurationsAdapter: ConfigurationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManageConfigurationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainApplication = application as MainApplication

        for (i in 1..3) {
            val configurationModel = ConfigurationModel()

            configurationModel.name = "Configuration " + i

            mainApplication.configurations.add( configurationModel )
        }

        configurationsAdapter = ConfigurationsAdapter(mainApplication.configurations)
        configurationsAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = configurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this )

        binding.backButton.setOnClickListener {

            finish()
            overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    fun onItemSelected( configurationModel: ConfigurationModel, shouldDismissKeyboard: Boolean )
    {
        Log.d( "xxx", configurationModel.name!! )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
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