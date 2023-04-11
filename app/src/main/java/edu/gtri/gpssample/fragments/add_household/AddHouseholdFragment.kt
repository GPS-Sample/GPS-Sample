package edu.gtri.gpssample.fragments.add_household

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddHouseholdFragment : Fragment()
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!

    private var userId: Int = -1
    private var studyId: Int = -1
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
        _binding = FragmentAddHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        val enumData = sharedViewModel.enumDataViewModel.currentEnumData

        enumData?.value?.let { enum_data ->
            addHouseholdAdapter = AddHouseholdAdapter( study.fields, enum_data )
            binding.recyclerView.adapter = addHouseholdAdapter
            binding.recyclerView.itemAnimator = DefaultItemAnimator()
            binding.recyclerView.layoutManager = LinearLayoutManager(activity )
        }

        binding.deleteButton.setOnClickListener {
            enumData?.value?.let { enum_data ->
                DAO.enumDataDAO.delete( enum_data )
                findNavController().popBackStack()
            }
        }

        binding.saveButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddHouseholdFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}