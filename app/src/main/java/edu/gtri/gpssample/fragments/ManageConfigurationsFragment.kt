package edu.gtri.gpssample.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.ConfigurationsAdapter
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentManageConfigurationsBinding
import edu.gtri.gpssample.models.ConfigurationModel

class ManageConfigurationsFragment : Fragment()
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var configurationsAdapter: ConfigurationsAdapter

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
                Toast.makeText(activity!!.applicationContext, "ManageConfigurationsFragment", Toast.LENGTH_SHORT).show()
            }
        }

        configurationsAdapter = ConfigurationsAdapter((activity!!.application as MainApplication).configurations)
        configurationsAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = configurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.createButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.backButton.setOnClickListener {

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        if ((activity!!.application as MainApplication).configurations.isEmpty())
        {
            binding.recyclerView.visibility = View.GONE
            binding.relativeLayout.visibility = View.VISIBLE
        }
        else
        {
            binding.recyclerView.visibility = View.VISIBLE
            binding.relativeLayout.visibility = View.GONE
        }

        configurationsAdapter.updateConfigurations((activity!!.application as MainApplication).configurations)
    }

    fun onItemSelected(configurationModel: ConfigurationModel, shouldDismissKeyboard: Boolean )
    {
        var bundle = Bundle()

        bundle.putString( "config_name", configurationModel.name )

        findNavController().navigate( R.id.action_navigate_to_ManageStudiesFragment, bundle )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_configuration, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_create_configuration -> {
                findNavController().navigate( R.id.action_navigate_to_CreateConfigurationFragment )
                return true
            }
        }

        return false
    }
}