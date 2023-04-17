package edu.gtri.gpssample.fragments.add_household

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
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddHouseholdFragment : Fragment()
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var enumData: EnumData
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )
        _binding = FragmentAddHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.enumDataViewModel.currentEnumData?.value?.let {
            enumData = it
        }

        val fieldDataMap = HashMap<Int, FieldData>()
        val fieldDataMapCopy = HashMap<Int, FieldData>()

        if (this::study.isInitialized && this::enumData.isInitialized)
        {
            for (field in study.fields)
            {
                val fieldData = DAO.fieldDataDAO.getOrCreateFieldData(field.id!!, enumData.id!!)
                fieldDataMap[field.id!!] = fieldData
                val fieldDataCopy = fieldData.copy()
                fieldDataMapCopy[field.id!!] = fieldDataCopy
            }

            addHouseholdAdapter = AddHouseholdAdapter( study.fields, fieldDataMapCopy )
            binding.recyclerView.adapter = addHouseholdAdapter
            binding.recyclerView.itemAnimator = DefaultItemAnimator()
            binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            for (key in fieldDataMap.keys)
            {
                fieldDataMapCopy[key]?.let {
                    DAO.fieldDataDAO.updateFieldData( it.copy())
                }
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddHouseholdFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_delete -> {
                DAO.enumDataDAO.delete( enumData )
                findNavController().popBackStack()
                return true
            }
        }

        return false
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}