package edu.gtri.gpssample.fragments.add_multi_household

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddMultiHouseholdBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddMultiHouseholdFragment : Fragment()
{
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addMultiHouseholdAdapter: AddMultiHouseholdAdapter

    private var _binding: FragmentAddMultiHouseholdBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentAddMultiHouseholdBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        addMultiHouseholdAdapter = AddMultiHouseholdAdapter( location, location.enumerationItems )
        addMultiHouseholdAdapter.didSelectEnumerationItem = this::didSelectEnumerationItem

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = addMultiHouseholdAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.addButton.setOnClickListener {
            val enumerationItem = EnumerationItem()
            sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )
            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
        }

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageEnumerationTeamsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectEnumerationItem( enumerationItem: EnumerationItem )
    {
        findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}