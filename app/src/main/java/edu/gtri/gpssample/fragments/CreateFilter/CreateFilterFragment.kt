package edu.gtri.gpssample.fragments.CreateFilter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateFilterBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.SelectRuleDialog
import edu.gtri.gpssample.fragments.CreateRule.CreateRuleViewModel
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import edu.gtri.gpssample.fragments.ManageStudies.ManageStudiesAdapter
import java.util.*

class CreateFilterFragment : Fragment(), SelectRuleDialog.SelectRuleDialogDelegate, ConfirmationDialog.ConfirmationDialogDelegate
{

    private lateinit var viewModel: CreateRuleViewModel
    private var _binding: FragmentCreateFilterBinding? = null
    private val binding get() = _binding!!
    private lateinit var createFilterAdapter: CreateFilterAdapter
    private lateinit var study_uuid: String
    private lateinit var filter_uuid: String
    private var filter: Filter? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateRuleViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateFilterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        study_uuid = arguments!!.getString( Key.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

//        DAO.studyDAO.getStudy( study_uuid )?.let { study ->
//            this.study = study
//        }
//
//        if (!this::study.isInitialized)
//        {
//            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $study_uuid not found.", Toast.LENGTH_SHORT).show()
//            return
//        }

        // optional: filterId
        filter_uuid = arguments!!.getString( Key.kFilter_uuid.toString(), "");

        if (filter_uuid.isNotEmpty())
        {
            filter = DAO.filterDAO.getFilter( filter_uuid )

            // TODO: Null Check
        }

//        if (!this::filter.isInitialized)
//        {
//            filter = Filter( UUID.randomUUID().toString(), study_uuid, "" )
//            DAO.filterDAO.createFilter( filter )
//        }
//        else
//        {
//            binding.nameEditText.setText( filter.name )
//        }

        filter?.let {
            binding.nameEditText.setText( it.name )
        }

        createFilterAdapter = CreateFilterAdapter(listOf<FilterRule>())
        createFilterAdapter.shouldEditFilterRule = this::shouldEditFilterRule
        createFilterAdapter.shouldDeleteFilterRule = this::shouldDeleteFilterRule

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = createFilterAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addRuleButton.setOnClickListener {
            SelectRuleDialog( activity!!, study_uuid, filter!!.uuid, null, this )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            if (binding.nameEditText.text.toString().length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter_uuid )

            if (filterRules.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "You must add at least one rule", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (filter == null)
            {
                filter = Filter( UUID.randomUUID().toString(), study_uuid, "" )
                DAO.filterDAO.createFilter( filter!! )
            }

            filter!!.name = binding.nameEditText.text.toString()

            DAO.filterDAO.updateFilter( filter!! )

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (this::study_uuid.isInitialized && this::filter_uuid.isInitialized)
        {
            val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter_uuid )
            createFilterAdapter.updateFilterRules(filterRules)
        }
    }

    private var selectedFilterRule: FilterRule? = null

    fun shouldEditFilterRule( filterRule: FilterRule )
    {
        SelectRuleDialog( activity!!, study_uuid, filter_uuid, filterRule,this )
    }

    fun shouldDeleteFilterRule( filterRule: FilterRule )
    {
        selectedFilterRule = filterRule
        ConfirmationDialog( activity!!, "Please Confirm", "Are you sure you want to delete this Filter Rule?", 0, this )
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Int )
    {
        selectedFilterRule?.let {
            DAO.filterRuleDAO.deleteFilterRule( it )
            val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter_uuid )
            createFilterAdapter.updateFilterRules( filterRules )
        }
    }

    override fun didDismissSelectRuleDialog()
    {
        val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter_uuid )

        createFilterAdapter.updateFilterRules( filterRules )
    }
}