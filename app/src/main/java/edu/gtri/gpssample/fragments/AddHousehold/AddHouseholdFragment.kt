package edu.gtri.gpssample.fragments.AddHousehold

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.NavPlan
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.fragments.CreateSample.CreateSampleAdapter

class AddHouseholdFragment : Fragment()
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!

    private lateinit var study_uuid: String
    private lateinit var viewModel: AddHouseholdViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddHouseholdViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentAddHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        // required: Study_uuid
        study_uuid = arguments!!.getString( Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        //val fields = DAO.fieldDAO.getFields( study_uuid )

      //  addHouseholdAdapter = AddHouseholdAdapter( fields )

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = addHouseholdAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.cancelButton.setOnClickListener {
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