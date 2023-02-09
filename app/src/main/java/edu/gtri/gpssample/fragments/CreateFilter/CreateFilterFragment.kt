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

        createFilterAdapter = CreateFilterAdapter(listOf<FilterRule>())
        createFilterAdapter.shouldEditFilterRule = this::shouldEditFilterRule
        createFilterAdapter.shouldDeleteFilterRule = this::shouldDeleteFilterRule

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = createFilterAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addRuleButton.setOnClickListener {
            SelectRuleDialog( activity!!, studyId, null, this )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        val filterRules = DAO.filterRuleDAO.getFilterRules( study.id )

        createFilterAdapter.updateFilterRules(filterRules)
    }

    private var selectedFilterRule: FilterRule? = null

    fun shouldEditFilterRule( filterRule: FilterRule )
    {
        SelectRuleDialog( activity!!, study!!.id, filterRule,this )
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
            val filterRules = DAO.filterRuleDAO.getFilterRules( study.id )
            createFilterAdapter.updateFilterRules( filterRules )
        }
    }

    override fun didDismissSelectRuleDialog()
    {
        val filterRules = DAO.filterRuleDAO.getFilterRules( study.id )

        createFilterAdapter.updateFilterRules( filterRules )
    }
}