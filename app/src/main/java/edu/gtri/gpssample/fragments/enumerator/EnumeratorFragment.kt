package edu.gtri.gpssample.fragments.enumerator

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.databinding.FragmentEnumeratorBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class EnumeratorFragment : Fragment()
{
    private var _binding: FragmentEnumeratorBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: ConfigurationViewModel
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentEnumeratorBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageConfigurationsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun didSelectConfig( config: Config)
    {
        val bundle = Bundle()
        sharedViewModel.setCurrentConfig(config)
        findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment, bundle)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}