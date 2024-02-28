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
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddMultiHouseholdBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddMultiHouseholdFragment : Fragment()
{
    private lateinit var config: Config
    private lateinit var enumArea: EnumArea
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addMultiHouseholdAdapter: AddMultiHouseholdAdapter

    private var editMode = true
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

        arguments?.getBoolean(Keys.kEditMode.toString())?.let { editMode ->
            this.editMode = editMode
        }

        if (!editMode)
        {
            binding.addButton.visibility = View.GONE
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let {
            enumArea = it
        }

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        addMultiHouseholdAdapter = AddMultiHouseholdAdapter( location, location.enumerationItems, enumArea.name )
        addMultiHouseholdAdapter.didSelectEnumerationItem = this::didSelectEnumerationItem

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = addMultiHouseholdAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.addButton.setOnClickListener {

            val enumerationItem = EnumerationItem()

            if (config.autoIncrementSubaddress)
            {
                var enumerationCount = 0

                for (location in enumArea.locations)
                {
                    for (enumItem in location.enumerationItems)
                    {
                        if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                        {
                            enumerationCount += 1
                        }
                    }
                }

                enumerationItem.subAddress = "${enumerationCount + 1}"
            }

            sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )

            findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment)
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddMultiHouseholdFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun didSelectEnumerationItem( enumerationItem: EnumerationItem )
    {
        sharedViewModel.locationViewModel.setCurrentEnumerationItem( enumerationItem )
        val bundle = Bundle()
        bundle.putBoolean( Keys.kEditMode.toString(), editMode )
        findNavController().navigate(R.id.action_navigate_to_AddHouseholdFragment,bundle)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}