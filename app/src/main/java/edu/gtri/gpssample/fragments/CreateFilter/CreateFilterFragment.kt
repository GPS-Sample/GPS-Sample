package edu.gtri.gpssample.fragments.CreateFilter

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
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.databinding.FragmentCreateFilterBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.SelectRuleDialog
import edu.gtri.gpssample.fragments.CreateRule.CreateRuleViewModel
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import java.util.*

class CreateFilterFragment : Fragment(), SelectRuleDialog.SelectRuleDialogDelegate, ConfirmationDialog.ConfirmationDialogDelegate
{

    private lateinit var viewModel: CreateRuleViewModel
    private var _binding: FragmentCreateFilterBinding? = null
    private val binding get() = _binding!!
    private lateinit var createFilterAdapter: CreateFilterAdapter
    private lateinit var study_uuid: String
    private lateinit var filter: Filter
    private var sampleSizeIsVisible = true

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

        // required: Study_uuid
        study_uuid = arguments!!.getString( Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        // required: SamplingMethod
        val samplingMethod = arguments!!.getString( Keys.kSamplingMethod.toString(), "");

        if (samplingMethod.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: samplingMethod.", Toast.LENGTH_SHORT).show()
            return
        }

        val samplingMethods by lazy { resources.getStringArray(R.array.samling_methods) }

        if (samplingMethod == samplingMethods[0] || samplingMethod == samplingMethods[1])
        {
            sampleSizeIsVisible = false
            binding.sampleSizeTextView.visibility = View.GONE
            binding.sampleSize1Layout.visibility = View.GONE
            binding.sampleSize2Layout.visibility = View.GONE
            binding.sampleSize3Layout.visibility = View.GONE
        }

        // optional: filterId
        val filter_uuid = arguments!!.getString( Keys.kFilter_uuid.toString(), "");

        if (filter_uuid.isNotEmpty())
        {
            DAO.filterDAO.getFilter( filter_uuid )?.let {
                this.filter = it
            }

            if (!this::filter.isInitialized)
            {
                Toast.makeText(activity!!.applicationContext, "Fatal! Filter with id $filter_uuid not found.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!this::filter.isInitialized)
        {
            filter = Filter( UUID.randomUUID().toString(), study_uuid, "", -1, 0 )
        }
        else
        {
            binding.nameEditText.setText( filter.name )

            if (sampleSizeIsVisible)
            {
                if (filter.sampleSize > 0)
                {
                    when(filter.sampleSizeIndex)
                    {
                        0 -> binding.sampleSize1EditText.setText(filter.sampleSize.toString())
                        1 -> binding.sampleSize2EditText.setText(filter.sampleSize.toString())
                        2 -> binding.sampleSize3EditText.setText(filter.sampleSize.toString())
                    }
                }
            }
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

        binding.sampleSize1EditText.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            binding.sampleSize2EditText.setText("")
            binding.sampleSize3EditText.setText("")
        }

        binding.sampleSize2EditText.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            binding.sampleSize1EditText.setText("")
            binding.sampleSize3EditText.setText("")
        }

        binding.sampleSize3EditText.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            binding.sampleSize1EditText.setText("")
            binding.sampleSize2EditText.setText("")
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

            val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )

            if (filterRules.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "You must add at least one rule", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sampleSizeIsVisible)
            {
                val sample1Size = binding.sampleSize1EditText.text.toString().toIntOrNull()
                val sample2Size = binding.sampleSize2EditText.text.toString().toIntOrNull()
                val sample3Size = binding.sampleSize3EditText.text.toString().toIntOrNull()

                if (sample1Size == null && sample2Size == null && sample3Size == null)
                {
                    Toast.makeText(activity!!.applicationContext, "Please enter a sample size.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                sample1Size?.let { sampleSize ->
                    filter.sampleSize = sampleSize
                    filter.sampleSizeIndex = 0
                }

                sample2Size?.let { sampleSize ->
                    filter.sampleSize = sampleSize
                    filter.sampleSizeIndex = 1
                }

                sample3Size?.let { sampleSize ->
                    filter.sampleSize = sampleSize
                    filter.sampleSizeIndex = 2
                }
            }

            filter.name = binding.nameEditText.text.toString()

            if (DAO.filterDAO.exists( filter.uuid ))
            {
                DAO.filterDAO.updateFilter( filter )
            }
            else
            {
                DAO.filterDAO.createFilter( filter )
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )
        createFilterAdapter.updateFilterRules(filterRules)
    }

    private var selectedFilterRule: FilterRule? = null

    fun shouldEditFilterRule( filterRule: FilterRule )
    {
        SelectRuleDialog( activity!!, study_uuid, filter.uuid, filterRule,this )
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
            val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )
            createFilterAdapter.updateFilterRules( filterRules )
        }
    }

    override fun didDismissSelectRuleDialog()
    {
        val filterRules = DAO.filterRuleDAO.getFilterRules( study_uuid, filter.uuid )

        createFilterAdapter.updateFilterRules( filterRules )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}