package edu.gtri.gpssample.fragments.createstudy

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

enum class DeleteMode(val value : Int)
{
    deleteStudyTag (1),
    deleteFieldTag (2),
    deleteRuleTag (3),
    deleteFilterTag (4),
    saveTag (4)

}

class CreateStudyFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var quickStart = false
    private lateinit var study: Study
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var createStudyAdapter: CreateStudyAdapter
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
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

        createStudyAdapter = CreateStudyAdapter( activity!!, listOf<Field>(), listOf<Rule>(), listOf<Filter>())
        createStudyAdapter.didSelectField = this::didSelectField
        createStudyAdapter.didSelectRule = this::didSelectRule
        createStudyAdapter.didSelectFilter = this::didSelectFilter
        createStudyAdapter.shouldAddField = this::shouldAddField
        createStudyAdapter.shouldAddRule = this::shouldAddRule
        createStudyAdapter.shouldAddFilter = this::shouldAddFilter

        createStudyAdapter.didDeleteField = this::didDeleteField
        createStudyAdapter.didDeleteRule = this::didDeleteRule
        createStudyAdapter.didDeleteFilter = this::didDeleteFilter



        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createStudyFragment = this@CreateStudyFragment
        }
        val quick_start = arguments?.getBoolean( Keys.kQuickStart.toString(), false )

        quick_start?.let {
            quickStart = it
        }


        ArrayAdapter.createFromResource(activity!!, R.array.samling_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.samplingMethodSpinner.adapter = adapter
            }

        binding.expandableListView.setAdapter( createStudyAdapter )

        binding.saveButton.setOnClickListener {
            updateStudy()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateStudyFragment.value.toString() + ": " + this.javaClass.simpleName

        sharedViewModel.createStudyModel.currentStudy?.value?.let{ study->
            createStudyAdapter.updateFieldsRulesFilters( study.fields, study.rules, study.filters )
        }
    }

    private fun shouldAddField()
    {
        val bundle = Bundle()
        sharedViewModel.createFieldModel.createNewField()
        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    private fun shouldAddRule()
    {
        val bundle = Bundle()
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.fields.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "You must create at least one field before you can create a rule", Toast.LENGTH_SHORT).show()
            }else
            {
                sharedViewModel.createRuleModel.createNewRule()
                findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
            }

        }

    }

    private fun shouldAddFilter()
    {
        val bundle = Bundle()
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.rules.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "You must create at least one rule before you can create a filter", Toast.LENGTH_SHORT).show()
            }else
            {
                sharedViewModel.createFilterModel.createNewFiler()
                findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
            }
        }
    }

    private fun didSelectField( field: Field )
    {
        val bundle = Bundle()
        sharedViewModel.createFieldModel.setSelectedField(field)

        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    private fun didSelectRule( rule: Rule )
    {
        val bundle = Bundle()
        sharedViewModel.setSelectedRule(rule)
        findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
    }

    private fun didSelectFilter( filter: Filter )
    {
        val bundle = Bundle()
        bundle.putString( Keys.kFilter_uuid.toString(), filter.uuid )
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
        bundle.putString( Keys.kSamplingMethod.toString(), binding.samplingMethodSpinner.selectedItem as String)

        findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
    }


    private fun didDeleteField( field: Field )
    {
        sharedViewModel.createFieldModel.setSelectedField(field)
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this field?",
            DeleteMode.deleteFieldTag.value, this)
    }

    private fun didDeleteRule( rule: Rule )
    {
        sharedViewModel.deleteRule(rule)
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this rule?",
            DeleteMode.deleteRuleTag.value, this)
    }

    private fun didDeleteFilter( filter: Filter )
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            android.R.id.home -> // intercept the back button press
            {
                sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->

                    if (study.name.isNotEmpty() || study.fields.size > 0 || study.rules.size > 0 || study.filters.size > 0)
                    {
                        ConfirmationDialog( activity, "Unsaved Changes", "Would you like to save this study?", DeleteMode.saveTag.value, this)
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

    private fun updateStudy()
    {
        val name = binding.studyNameEditText.text.toString()
        if (name.length == 0)
        {
            Toast.makeText(activity!!.applicationContext, "Please enter a name.", Toast.LENGTH_SHORT).show()
            return
        }
        sharedViewModel.addStudy()
        val bundle = Bundle()
        findNavController().navigate( R.id.action_navigate_to_CreateConfigurationFragment, bundle )
    }

    override fun didAnswerNo()
    {
        findNavController().popBackStack()
    }

    override fun didAnswerYes( tag: Int )
    {
        when( tag )
        {
            DeleteMode.deleteStudyTag.value -> {
                sharedViewModel.deleteCurrentStudy()
                findNavController().popBackStack()
            }
            DeleteMode.deleteFieldTag.value -> {

                sharedViewModel.deleteSelectedField()
                sharedViewModel.createStudyModel.currentStudy?.value?.let{ study->
                    createStudyAdapter.updateFieldsRulesFilters( study.fields, study.rules, study.filters )
                }
            }
            DeleteMode.deleteRuleTag.value -> {
                sharedViewModel.deleteSelectedRule()
                sharedViewModel.createStudyModel.currentStudy?.value?.let{ study->
                    createStudyAdapter.updateFieldsRulesFilters( study.fields, study.rules, study.filters )
                }
            }
            DeleteMode.deleteFilterTag.value -> {

            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}