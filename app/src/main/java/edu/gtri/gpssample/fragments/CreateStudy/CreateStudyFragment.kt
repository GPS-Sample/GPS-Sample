package edu.gtri.gpssample.fragments.CreateStudy

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import java.util.*

class CreateStudyFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var quickStart = false
    private lateinit var study: Study
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var createStudyAdapter: CreateStudyAdapter
    private lateinit var viewModel: CreateStudyViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateStudyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentCreateStudyBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        // required: configId
        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val config_uuid = arguments!!.getString( Keys.kConfig_uuid.toString(), "");

        if (config_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        // optional: studyId
        val study_uuid = arguments!!.getString( Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isNotEmpty())
        {
            DAO.studyDAO.getStudy( study_uuid )?.let { study ->
                this.study = study
            }

            if (!this::study.isInitialized)
            {
                Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $study_uuid not found.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val quick_start = arguments?.getBoolean( Keys.kQuickStart.toString(), false )

        quick_start?.let {
            quickStart = it
        }

        if (quickStart)
        {
            binding.saveButton.setText( "NEXT" )
        }

        ArrayAdapter.createFromResource(activity!!, R.array.samling_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.samplingMethodSpinner.adapter = adapter
            }

        val samplingMethods by lazy { resources.getStringArray(R.array.samling_methods) }

        if (!this::study.isInitialized)
        {
            study = Study( UUID.randomUUID().toString(), config_uuid, "", samplingMethods[0], -1, 0 )
        }
        else
        {
            for (i in samplingMethods.indices)
            {
                if (samplingMethods[i] == study.samplingMethod)
                {
                    binding.samplingMethodSpinner.setSelection(i)
                }
            }
        }

        if (study.sampleSize > 0)
        {
            when(study.sampleSizeIndex)
            {
                0 -> binding.sampleSize1EditText.setText(study.sampleSize.toString())
                1 -> binding.sampleSize2EditText.setText(study.sampleSize.toString())
                2 -> binding.sampleSize3EditText.setText(study.sampleSize.toString())
            }
        }

        binding.studyNameEditText.setText( study.name )

        createStudyAdapter = CreateStudyAdapter( activity!!, listOf<Field>(), listOf<Rule>(), listOf<Filter>())
        createStudyAdapter.didSelectField = this::didSelectField
        createStudyAdapter.didSelectRule = this::didSelectRule
        createStudyAdapter.didSelectFilter = this::didSelectFilter
        createStudyAdapter.shouldAddField = this::shouldAddField
        createStudyAdapter.shouldAddRule = this::shouldAddRule
        createStudyAdapter.shouldAddFilter = this::shouldAddFilter

        binding.expandableListView.setAdapter( createStudyAdapter )

        binding.samplingMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                when( position )
                {
                    0 -> { // simple random sampling
                        binding.sampleSize1Layout.visibility = View.VISIBLE
                        binding.sampleSize2Layout.visibility = View.VISIBLE
                        binding.sampleSize3Layout.visibility = View.VISIBLE
                        binding.sampleSize1TextView.setText( "# of Households in all clusters")
                        binding.sampleSizeTextView.visibility = View.VISIBLE
                    }
                    1 -> { // cluster sampling
                        binding.sampleSize1Layout.visibility = View.VISIBLE
                        binding.sampleSize2Layout.visibility = View.GONE
                        binding.sampleSize3Layout.visibility = View.GONE
                        binding.sampleSize1TextView.setText( "# of Households per cluster")
                        binding.sampleSizeTextView.visibility = View.VISIBLE
                    }
                    else -> { // subset or strata sampling
                        binding.sampleSize1Layout.visibility = View.GONE
                        binding.sampleSize2Layout.visibility = View.GONE
                        binding.sampleSize3Layout.visibility = View.GONE
                        binding.sampleSizeTextView.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
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

        binding.saveButton.setOnClickListener {
            updateStudy()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateStudyFragment.value.toString() + ": " + this.javaClass.simpleName

        if (this::study.isInitialized)
        {
            val fields = DAO.fieldDAO.getFields(study.uuid)
            val rules = DAO.ruleDAO.getRules(study.uuid)
            val filters = DAO.filterDAO.getFilters(study.uuid)

            createStudyAdapter.updateFieldsRulesFilters( fields, rules, filters )
        }
    }

    fun shouldAddField()
    {
        val bundle = Bundle()
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    fun shouldAddRule()
    {
        val fields = DAO.fieldDAO.getFields( study.uuid )

        if (fields.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "You must create at least one field before you can create a rule", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val bundle = Bundle()
            bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
            findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
        }
    }

    fun shouldAddFilter()
    {
        val rules = DAO.ruleDAO.getRules( study.uuid )

        if (rules.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "You must create at least one rule before you can create a filter", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val bundle = Bundle()
            bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
            bundle.putString( Keys.kSamplingMethod.toString(), binding.samplingMethodSpinner.selectedItem as String)
            findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
        }
    }

    fun didSelectField( field: Field )
    {
        val bundle = Bundle()
        bundle.putString( Keys.kField_uuid.toString(), field.uuid )
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )

        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    fun didSelectRule( rule: Rule )
    {
        val bundle = Bundle()
        bundle.putString( Keys.kRule_uuid.toString(), rule.uuid )
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )

        findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
    }

    fun didSelectFilter( filter: Filter )
    {
        val bundle = Bundle()
        bundle.putString( Keys.kFilter_uuid.toString(), filter.uuid )
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
        bundle.putString( Keys.kSamplingMethod.toString(), binding.samplingMethodSpinner.selectedItem as String)

        findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_study, menu)
    }

    val deleteTag = 1
    val saveTag = 2

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            android.R.id.home -> // intercept the back button press
            {
                if (DAO.studyDAO.doesNotExist( study.uuid ))
                {
                    val fields = DAO.fieldDAO.getFields(study.uuid)
                    val rules = DAO.ruleDAO.getRules(study.uuid)
                    val filters = DAO.filterDAO.getFilters(study.uuid)

                    if (binding.studyNameEditText.text.toString().isNotEmpty() || fields.size > 0 || rules.size > 0 || filters.size > 0)
                    {
                        ConfirmationDialog( activity, "Unsaved Changes", "Would you like to save this study?", saveTag, this)
                        return true
                    }
                }
            }

            R.id.action_home -> {
                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
            }

            R.id.action_manage_enumeration_areas -> {
                if (binding.studyNameEditText.text.toString().isEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, "Please enter a study name", Toast.LENGTH_SHORT).show()
                    return true
                }

                val bundle = Bundle()
                bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
                findNavController().navigate( R.id.action_navigate_to_ManageEnumerationAreasFragment, bundle )
            }
        }

        return false
    }

    fun manageSamples()
    {
    }

    fun updateStudy()
    {
        val name = binding.studyNameEditText.text.toString()

        if (name.length == 0)
        {
            Toast.makeText(activity!!.applicationContext, "Please enter a name.", Toast.LENGTH_SHORT).show()
            return
        }

        val samplingMethod = binding.samplingMethodSpinner.selectedItem as String
        val samplingMethods by lazy { resources.getStringArray(R.array.samling_methods) }

        if (samplingMethod == samplingMethods[0] || samplingMethod == samplingMethods[1])
        {
            val sample1Size = binding.sampleSize1EditText.text.toString().toIntOrNull()
            val sample2Size = binding.sampleSize2EditText.text.toString().toIntOrNull()
            val sample3Size = binding.sampleSize3EditText.text.toString().toIntOrNull()

            if (sample1Size == null && sample2Size == null && sample3Size == null)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a sample size.", Toast.LENGTH_SHORT).show()
                return
            }

            sample1Size?.let { sampleSize ->
                study.sampleSize = sample1Size
                study.sampleSizeIndex = 0
            }

            sample2Size?.let { sampleSize ->
                study.sampleSize = sampleSize
                study.sampleSizeIndex = 1
            }

            sample3Size?.let { sampleSize ->
                study.sampleSize = sampleSize
                study.sampleSizeIndex = 2
            }
        }

        study.name = name
        study.samplingMethod = binding.samplingMethodSpinner.selectedItem as String

        if (DAO.studyDAO.exists( study.uuid ))
        {
            DAO.studyDAO.updateStudy( study )
        }
        else
        {
            DAO.studyDAO.createStudy( study )
        }

        if (quickStart)
        {
            val bundle = Bundle()
            bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
            findNavController().navigate( R.id.action_navigate_to_ManageEnumerationAreasFragment, bundle )
        }
        else
        {
            findNavController().popBackStack()
        }
    }

    override fun didAnswerNo()
    {
        findNavController().popBackStack()
    }

    override fun didAnswerYes( tag: Int )
    {
        when( tag )
        {
            deleteTag -> {
                DAO.studyDAO.deleteStudy( study )
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}