package edu.gtri.gpssample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
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
        _binding = FragmentManageConfigurationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

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

//        val intent = Intent(this, ManageStudiesActivity::class.java)
//        intent.putExtra( Key.ConfigurationName.toString(), configurationModel.name )
//        startActivity( intent )
//        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }
}