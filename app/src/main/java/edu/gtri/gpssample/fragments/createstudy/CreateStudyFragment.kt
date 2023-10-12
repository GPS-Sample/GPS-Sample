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
        sharedViewModel.createStudyModel.fragment = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
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

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createStudyFragment = this@CreateStudyFragment
        }

        ArrayAdapter.createFromResource(activity!!, R.array.samling_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.samplingMethodSpinner.adapter = adapter
            }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        } ?: run {
            binding.deleteImageView.visibility= View.GONE
        }

        binding.expandableListView.setAdapter( createStudyAdapter )
        binding.expandableListView.setChildDivider( getResources().getDrawable(R.color.clear))


        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString( R.string.delete_study_message ),
                resources.getString(R.string.no), resources.getString(R.string.yes), DeleteMode.deleteStudyTag.value, this)
        }

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
        sharedViewModel.createFieldModel.createNewField()
        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment )
    }

    private fun shouldAddRule()
    {
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.fields.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.create_field_rule_message), Toast.LENGTH_SHORT).show()
            }else
            {
                sharedViewModel.createRuleModel.createNewRule()
                findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment )
            }
        }
    }

    private fun shouldAddFilter()
    {
        val bundle = Bundle()
        sharedViewModel.createStudyModel.currentStudy?.value?.let{study ->
            if(study.rules.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.create_rule_filter_message), Toast.LENGTH_SHORT).show()
            }else
            {
                sharedViewModel.createFilterModel.createNewFilter()
                sharedViewModel.createFilterModel.createFilterAdapter.updateRules(null)
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
        sharedViewModel.createFilterModel.setSelectedFilter(filter)
        findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
    }

    private fun didDeleteField( field: Field )
    {
        sharedViewModel.createFieldModel.setSelectedField(field)
        ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_field_message),
            resources.getString(R.string.no), resources.getString(R.string.yes),DeleteMode.deleteFieldTag.value, this)
    }

    private fun didDeleteRule( rule: Rule )
    {
        sharedViewModel.deleteRule(rule)
        ConfirmationDialog( activity, resources.getString( R.string.please_confirm),  resources.getString(R.string.delete_rule_message),
            resources.getString(R.string.no), resources.getString(R.string.yes), DeleteMode.deleteRuleTag.value, this)
    }

    private fun didDeleteFilter( filter: Filter )
    {
        findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment)
    }

    fun manageSamples()
    {
    }

    private fun updateStudy()
    {
        if (study.name.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (study.sampleSize == 0)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.sample_size_error), Toast.LENGTH_SHORT).show()
            return
        }

        if (study.sampleType == SampleType.PercentTotal && study.totalPopulationSize == 0)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.total_population_size_error), Toast.LENGTH_SHORT).show()
            return
        }

        sharedViewModel.addStudy()

        findNavController().popBackStack()
    }

    override fun didSelectLeftButton(tag: Any?)
    {
//        findNavController().popBackStack()
    }

    override fun didSelectRightButton(tag: Any?)
    {
        val t = tag as Int

        when( t )
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