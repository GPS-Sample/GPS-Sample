package edu.gtri.gpssample.fragments.CreateStudy

import android.os.Bundle
import android.util.Log
import android.view.*
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

        // required: configId
        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val configId = arguments!!.getInt( Key.kConfigId.toString(), -1);

        if (configId < 0)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        // optional: studyId
        val studyId = arguments!!.getInt( Key.kStudyId.toString(), -1);

        if (studyId > 0)
        {
            DAO.studyDAO.getStudy( studyId )?.let { study ->
                this.study = study
            }

            if (!this::study.isInitialized)
            {
                Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $studyId not found.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!this::study.isInitialized)
        {
            study = Study( -1, configId, "", false )
            study.id = DAO.studyDAO.createStudy( study )
        }
        else
        {
            binding.titleTextView.text = "Study ${study.name}"
        }

        binding.studyNameEditText.setText( study.name )

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        createStudyAdapter = CreateStudyAdapter( activity!!, listOf<Field>(), listOf<Rule>(), listOf<Filter>())
        createStudyAdapter.didSelectField = this::didSelectField
        createStudyAdapter.didSelectRule = this::didSelectRule
        createStudyAdapter.didSelectFilter = this::didSelectFilter
        createStudyAdapter.shouldAddField = this::shouldAddField
        createStudyAdapter.shouldAddRule = this::shouldAddRule
        createStudyAdapter.shouldAddFilter = this::shouldAddFilter

        binding.expandableListView.setAdapter( createStudyAdapter )

        val user = (activity!!.application as? MainApplication)!!.user

        if (user!!.role == Role.Supervisor.toString())
        {
//            binding.addButton.visibility = View.GONE
            binding.generateBarcodeButton.text = "NEXT"
        }

        binding.generateBarcodeButton.setOnClickListener {

            if (user!!.role == Role.Supervisor.toString())
            {
                val bundle = Bundle()
                bundle.putInt( Key.kStudyId.toString(), study.id )
                findNavController().navigate( R.id.action_navigate_to_DefineEnumerationAreaFragment, bundle )
            }
            else
            {
                if (binding.studyNameEditText.text.toString().isEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, "Please enter a study name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                study.isValid = true
                study.name = binding.studyNameEditText.text.toString()

                DAO.studyDAO.updateStudy( study )

                val bundle = Bundle()
                bundle.putInt( Key.kStudyId.toString(), study.id )
                findNavController().navigate( R.id.action_navigate_to_ManageStudyFragment, bundle )
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (this::study.isInitialized)
        {
            val fields = DAO.fieldDAO.getFields(study.id)
            val rules = DAO.ruleDAO.getRules(study.id)
            val filters = listOf<Filter>()

            createStudyAdapter.updateFieldsRulesFilters( fields, rules, filters )
        }
    }

    fun shouldAddField()
    {
        val bundle = Bundle()
        bundle.putInt( Key.kStudyId.toString(), study.id )
        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    fun shouldAddRule()
    {
        val fields = DAO.fieldDAO.getFields( study.id )

        if (fields.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "You must create at least one field before you can create a rule", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val bundle = Bundle()
            bundle.putInt( Key.kStudyId.toString(), study.id )
            findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
        }
    }

    fun shouldAddFilter()
    {
        val rules = DAO.ruleDAO.getRules( study.id )

        if (rules.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "You must create at least one rule before you can create a filter", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val bundle = Bundle()
            bundle.putInt( Key.kStudyId.toString(), study.id )
            findNavController().navigate( R.id.action_navigate_to_CreateFilterFragment, bundle )
        }
    }

    fun didSelectField( field: Field )
    {
        val bundle = Bundle()
        bundle.putInt( Key.kFieldId.toString(), field.id )
        bundle.putInt( Key.kStudyId.toString(), study.id )

        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
    }

    fun didSelectRule( rule: Rule )
    {
        val bundle = Bundle()
        bundle.putInt( Key.kRuleId.toString(), rule.id )
        bundle.putInt( Key.kStudyId.toString(), study.id )

        findNavController().navigate( R.id.action_navigate_to_CreateRuleFragment, bundle )
    }

    fun didSelectFilter( filter: Filter )
    {
        val bundle = Bundle()
        bundle.putInt( Key.kFilterId.toString(), filter.id )
        bundle.putInt( Key.kStudyId.toString(), study.id )

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
        Log.d( "xxx", item.toString())
        when (item.itemId) {
            android.R.id.home ->
            {
                if (!study.isValid)
                {
                    if (binding.studyNameEditText.text.toString().isNotEmpty())
                    {
                        ConfirmationDialog( activity, "Unsaved Changes", "Would you like to save this study?", saveTag, this)
                        return true
                    }
                }
                return false
            }
//            R.id.action_create_field ->
//            {
//                val bundle = Bundle()
//                bundle.putInt( Key.kStudyId.toString(), study.id )
//                findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
//
//                return true
//            }
//            R.id.action_manage_study -> {
//                val bundle = Bundle()
//                bundle.putInt( Key.kStudyId.toString(), study.id )
//                findNavController().navigate( R.id.action_navigate_to_ManageStudyFragment, bundle )
//            }
            R.id.action_delete_study -> {
                ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this study?", deleteTag, this)
            }
        }

        return false
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
            saveTag -> {
                study.isValid = true
                study.name = binding.studyNameEditText.text.toString()
                DAO.studyDAO.updateStudy( study )
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