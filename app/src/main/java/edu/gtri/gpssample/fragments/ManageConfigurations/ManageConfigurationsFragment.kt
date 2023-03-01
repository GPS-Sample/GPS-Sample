package edu.gtri.gpssample.fragments.ManageConfigurations

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentManageConfigurationsBinding
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.dialogs.ConfirmationDialog

class ManageConfigurationsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var manageConfigurationsAdapter: ManageConfigurationsAdapter
    private lateinit var viewModel: ManageConfigurationsViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageConfigurationsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentManageConfigurationsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        manageConfigurationsAdapter = ManageConfigurationsAdapter(listOf<Config>())
        manageConfigurationsAdapter.didSelectConfig = this::didSelectConfig
        manageConfigurationsAdapter.shouldDeleteConfig = this::shouldDeleteConfig

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageConfigurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.quickStartButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean( Keys.kQuickStart.toString(), true )
            findNavController().navigate( R.id.action_navigate_to_CreateConfigurationFragment, bundle )
        }

        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        val configurations = DAO.configDAO.getConfigs()

        manageConfigurationsAdapter.updateConfigurations(configurations)
    }

    fun didSelectConfig( config: Config )
    {
        val bundle = Bundle()

        bundle.putString( Keys.kConfig_uuid.toString(), config.uuid )

        findNavController().navigate( R.id.action_navigate_to_ManageStudiesFragment, bundle )
    }

    private var selectedConfig: Config? = null

    fun shouldDeleteConfig( config: Config )
    {
        selectedConfig = config
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this configuration?", 0, this)
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Int )
    {
        selectedConfig?.let {
            DAO.configDAO.deleteConfig( it )
            manageConfigurationsAdapter.updateConfigurations(DAO.configDAO.getConfigs())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_manage_configurations, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
//        when (item.itemId) {
//            R.id.action_create_configuration -> {
//                findNavController().navigate( R.id.action_navigate_to_CreateConfigurationFragment )
//                return true
//            }
//            R.id.action_quick_start -> {
//                val bundle = Bundle()
//                bundle.putBoolean( Key.kQuickStart.toString(), true )
//                findNavController().navigate( R.id.action_navigate_to_CreateConfigurationFragment, bundle )
//                return true
//            }
//        }

        return false
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}