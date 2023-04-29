package edu.gtri.gpssample.fragments.manageconfigurations

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.FragmentManageConfigurationsBinding
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class ManageConfigurationsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var manageConfigurationsAdapter: ManageConfigurationsAdapter
    private lateinit var sharedViewModel: ConfigurationViewModel
    private var selectedConfig: Config? = null
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
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

        manageConfigurationsAdapter = ManageConfigurationsAdapter(listOf<Config>())
        manageConfigurationsAdapter.didSelectConfig = this::didSelectConfig
        manageConfigurationsAdapter.shouldDeleteConfig = this::shouldDeleteConfig
        manageConfigurationsAdapter.shouldEditConfig = this::shouldEditConfig

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageConfigurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        val user = (activity!!.application as MainApplication).user

        user?.let { user ->
            if (user.role == Role.Supervisor.toString())
            {
                binding.addButton.visibility = View.GONE
            }
        }

        binding.addButton.setOnClickListener {
            sharedViewModel.createNewConfiguration()
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageConfigurationsFragment.value.toString() + ": " + this.javaClass.simpleName

        // get this from the view controller
         manageConfigurationsAdapter.updateConfigurations(sharedViewModel.configurations)
    }

    private fun didSelectConfig( config: Config )
    {
        val bundle = Bundle()
        sharedViewModel.setCurrentConfig(config)
        findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment, bundle)
    }

    private fun shouldDeleteConfig( config: Config )
    {
        selectedConfig = config
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this configuration?", 0, this)
    }

    private fun shouldEditConfig(config: Config)
    {
        sharedViewModel.setCurrentConfig(config)
        findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Any? )
    {
        selectedConfig?.let {
            sharedViewModel.deleteConfig(it)
            manageConfigurationsAdapter.updateConfigurations( sharedViewModel.configurations)
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