package edu.gtri.gpssample.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentCreateConfigurationBinding
import edu.gtri.gpssample.models.ConfigurationModel

class CreateConfigurationFragment : Fragment()
{
    private var _binding: FragmentCreateConfigurationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "CreateConfigurationFragment", Toast.LENGTH_SHORT).show()
            }
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        binding.backButton.setOnClickListener {

            findNavController().popBackStack()
        }

        ArrayAdapter.createFromResource(activity!!, R.array.preferred_units, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.preferredUnitsSpinner.adapter = adapter
            }

        ArrayAdapter.createFromResource(activity!!, R.array.date_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.dateFormatSpinner.adapter = adapter
            }

        ArrayAdapter.createFromResource(activity!!, R.array.time_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.timeFormatSpinner.adapter = adapter
            }

        binding.nextButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().length > 0)
            {
                val mainApplication = activity!!.application as MainApplication
                val configurationModel = ConfigurationModel()
                configurationModel.name = binding.configNameEditText.text.toString()
                mainApplication.configurations.add( configurationModel )

                findNavController().navigate(R.id.action_navigate_to_DefineEnumerationAreaFragment)
            }
        }
    }
}