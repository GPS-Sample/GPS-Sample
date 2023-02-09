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

class CreateFilterFragment : Fragment(), SelectRuleDialog.SelectRuleDialogDelegate, ConfirmationDialog.ConfirmationDialogDelegate
{

    private lateinit var viewModel: CreateRuleViewModel
    private var _binding: FragmentCreateFilterBinding? = null
    private val binding get() = _binding!!
    private lateinit var createFilterAdapter: CreateFilterAdapter
    private lateinit var study: Study
    private lateinit var filter: Filter

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

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        val studyId = arguments!!.getInt( Key.kStudyId.toString(), -1);

        if (studyId < 0)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.studyDAO.getStudy( studyId )?.let { study ->
            this.study = study
        }

        if (!this::study.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $studyId not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // clean up leftover filters & filterRules

        val invalidFilters = DAO.filterDAO.getInvalidFilters( study!!.id )

        for (invalidFilter in invalidFilters)
        {
            DAO.filterRuleDAO.deleteFilterRules( study!!.id, invalidFilter!!.id )
            DAO.filterDAO.deleteFilter( invalidFilter )
        }

        // optional: filterId
        val filterId = arguments!!.getInt( Key.kFilterId.toString(), -1);

        if (filterId > 0)
        {
            DAO.filterDAO.getFilter( filterId )?.let { filter ->
                this.filter = filter
            }

            if (!this::filter.isInitialized)
            {
                Toast.makeText(activity!!.applicationContext, "Fatal! Filter with id $filterId not found.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!this::filter.isInitialized)
        {
            filter = Filter( -1, studyId, "", false )
            filter.id = DAO.filterDAO.createFilter( filter )
        }

        createFilterAdapter = CreateFilterAdapter(listOf<FilterRule>())
        createFilterAdapter.shouldEditFilterRule = this::shouldEditFilterRule
        createFilterAdapter.shouldDeleteFilterRule = this::shouldDeleteFilterRule

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = createFilterAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addRuleButton.setOnClickListener {
            SelectRuleDialog( activity!!, study!!.id, filter!!.id, null, this )
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

            val filterRules = DAO.filterRuleDAO.getFilterRules( study.id, filter.id )

            if (filterRules.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "You must add at least one rule", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            filter.name = binding.nameEditText.text.toString()
            filter.isValid = true

            DAO.filterDAO.updateFilter( filter )

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        val filterRules = DAO.filterRuleDAO.getFilterRules( study.id, filter.id )

        createFilterAdapter.updateFilterRules(filterRules)
    }

    private var selectedFilterRule: FilterRule? = null

    fun shouldEditFilterRule( filterRule: FilterRule )
    {
        SelectRuleDialog( activity!!, study!!.id, filter!!.id, filterRule,this )
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
            val filterRules = DAO.filterRuleDAO.getFilterRules( study.id, filter.id )
            createFilterAdapter.updateFilterRules( filterRules )
        }
    }

    override fun didDismissSelectRuleDialog()
    {
        val filterRules = DAO.filterRuleDAO.getFilterRules( study.id, filter.id )

        createFilterAdapter.updateFilterRules( filterRules )
    }
}