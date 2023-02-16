package edu.gtri.gpssample.activities

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.core.content.ContextCompat
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.ActivityMainBinding
import edu.gtri.gpssample.services.UDPBroadcastReceiverService

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var udpBroadcastReceiverService: UDPBroadcastReceiverService

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null)
        {
            DAO.createSharedInstance(applicationContext)

            DAO.studyDAO.deleteOrphans()
            DAO.fieldDAO.deleteOrphans()
            DAO.filterDAO.deleteOrphans()
            DAO.ruleDAO.deleteOrphans()
            DAO.filterRuleDAO.deleteOrphans()
            DAO.sampleDAO.deleteOrphans()
            DAO.navPlanDAO.deleteOrphans()

            val configurations = DAO.configDAO.getConfigs()
            Log.d( "xxx", "found ${configurations.size} Configurations" )

            val studies = DAO.studyDAO.getStudies()
            Log.d( "xxx", "found ${studies.size} Studies" )

            val fields = DAO.fieldDAO.getFields()
            Log.d( "xxx", "found ${fields.size} Fields" )

            val rules = DAO.ruleDAO.getRules()
            Log.d( "xxx", "found ${rules.size} Rules" )

            val filters = DAO.filterDAO.getFilters()
            Log.d( "xxx", "found ${filters.size} Filters" )

            val filterRules = DAO.filterRuleDAO.getFilterRules()
            Log.d( "xxx", "found ${filterRules.size} FilterRules" )

            val samples = DAO.sampleDAO.getSamples()
            Log.d( "xxx", "found ${samples.size} Samples" )

            val navPlans = DAO.navPlanDAO.getNavPlans()
            Log.d( "xxx", "found ${navPlans.size} NavPlans" )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController( R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val serviceIntent = Intent( this, UDPBroadcastReceiverService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        val intent = Intent(this, UDPBroadcastReceiverService::class.java)
        this.bindService( intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder)
        {
            val binder = service as UDPBroadcastReceiverService.LocalBinder
            udpBroadcastReceiverService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName)
        {
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}