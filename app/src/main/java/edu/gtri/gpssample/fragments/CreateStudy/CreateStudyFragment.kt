package edu.gtri.gpssample.fragments.CreateStudy

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.Role
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

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        // required: configId
        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val config_uuid = arguments!!.getString( Key.kConfig_uuid.toString(), "");

        if (config_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        // optional: studyId
        val study_uuid = arguments!!.getString( Key.kStudy_uuid.toString(), "");

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

        ArrayAdapter.createFromResource(activity!!, R.array.samling_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.samplingMethodSpinner.adapter = adapter
            }

        val samplingMethods by lazy { resources.getStringArray(R.array.samling_methods) }

        if (!this::study.isInitialized)
        {
            study = Study( UUID.randomUUID().toString(), config_uuid, "", samplingMethods[0] )
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
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
            {
                when( position )
                {
                    0 -> { // simple random sampling
                        binding.sampleSize1Layout.visibility = View.VISIBLE
                        binding.sampleSize2Layout.visibility = View.VISIBLE
                        binding.sampleSize3Layout.visibility = View.VISIBLE
                        binding.sampleSize1TextView.setText( "# of Households in all clusters")
                    }
                    1 -> { // cluster sampling
                        binding.sampleSize1Layout.visibility = View.VISIBLE
                        binding.sampleSize2Layout.visibility = View.GONE
                        binding.sampleSize3Layout.visibility = View.GONE
                        binding.sampleSize1TextView.setText( "# of Households per cluster")
                    }
                    else -> { // subset or strata sampling
                        binding.sampleSize1Layout.visibility = View.GONE
                        binding.sampleSize2Layout.visibility = View.GONE
                        binding.sampleSize3Layout.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            updateStudy()
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

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
        bundle.putString( Key.kStudy_uuid.toString(), study.uuid )
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
            bundle.putString( Key.kStudy_uuid.toString(), study.uuid )
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
            bundle.putString( Key.kStudy_uuid.toString(), study.uuid )
            findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
        }
    }

    fun didSelectField( field: Field )
    {
        val bundle = Bundle()
        bundle.putString( Key.kField_uuid.toString(), field.uuid )
        bundle.putString( Key.kStudy_uuid.toString(), study.uuid )

        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    fun didSelectRule( rule: Rule )
    {
        val bundle = Bundle()
        bundle.putString( Key.kRule_uuid.toString(), rule.uuid )
        bundle.putString( Key.kStudy_uuid.toString(), study.uuid )

        findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
    }

    fun didSelectFilter( filter: Filter )
    {
        val bundle = Bundle()
        bundle.putString( Key.kFilter_uuid.toString(), filter.uuid )
        bundle.putString( Key.kStudy_uuid.toString(), study.uuid )

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
//            android.R.id.home -> // intercept the back button press
//            {
//                if (!study.isValid)
//                {
//                    if (binding.studyNameEditText.text.toString().isNotEmpty())
//                    {
//                        ConfirmationDialog( activity, "Unsaved Changes", "Would you like to save this study?", saveTag, this)
//                        return true
//                    }
//                }
//                else
//                {
//                    updateStudy()
//                }
//
//                return false
//            }

            R.id.action_manage_study -> {
                manageStudy()
            }

            R.id.action_delete_study -> {
                ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this study?", deleteTag, this)
            }
        }

        return false
    }

    fun manageStudy()
    {
        val user = (activity!!.application as? MainApplication)!!.user

        if (user!!.role == Role.Supervisor.toString())
        {
            val bundle = Bundle()
            bundle.putString( Key.kStudy_uuid.toString(), study.uuid )
            findNavController().navigate( R.id.action_navigate_to_DefineEnumerationAreaFragment, bundle )
        }
        else
        {
            if (binding.studyNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a study name", Toast.LENGTH_SHORT).show()
                return
            }

            val bundle = Bundle()
            bundle.putString( Key.kStudy_uuid.toString(), study.uuid )
            findNavController().navigate( R.id.action_navigate_to_ManageStudyFragment, bundle )
        }
    }

    fun updateStudy()
    {
        study.name = binding.studyNameEditText.text.toString()
        study.samplingMethod = binding.samplingMethodSpinner.selectedItem as String

        if (DAO.studyDAO.exists( study.uuid ))
        {
            DAO.studyDAO.updateStudy( study )
        }
        else
        {
            DAO.studyDAO.createStudy( study )
        }
    }

    override fun didAnswerNo()
    {
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