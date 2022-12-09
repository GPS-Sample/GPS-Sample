package edu.gtri.gpssample.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.activities.CreateConfigurationActivity
import edu.gtri.gpssample.activities.ManageStudiesActivity
import edu.gtri.gpssample.adapters.ConfigurationsAdapter
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
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
//            val intent = Intent(this, CreateConfigurationActivity::class.java)
//            startActivity( intent )
//            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.backButton.setOnClickListener {

            findNavController().popBackStack()
        }
    }

    fun onItemSelected(configurationModel: ConfigurationModel, shouldDismissKeyboard: Boolean )
    {
//        val intent = Intent(this, ManageStudiesActivity::class.java)
//        intent.putExtra( Key.ConfigurationName.toString(), configurationModel.name )
//        startActivity( intent )
//        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }
}