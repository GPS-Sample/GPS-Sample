package edu.gtri.gpssample.fragments.CreateRule

import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateRuleBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog

class CreateRuleFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var rule: Rule? = null
    private lateinit var study: Study
    private lateinit var viewModel: CreateRuleViewModel

    private var firstLaunch = true
    private var _binding: FragmentCreateRuleBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateRuleViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateRuleBinding.inflate(inflater, container, false)

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

        val ruleId = arguments!!.getInt( Key.kRuleId.toString(), -1);

        if (ruleId > 0)
        {
            rule = DAO.ruleDAO.getRule( ruleId )
        }

        val fields = DAO.fieldDAO.getFields( studyId )

        val fieldNames = ArrayList<String>()

        for (field in fields)
        {
            fieldNames.add( field.name )
        }

        binding.fieldSpinner.adapter = ArrayAdapter<String>(activity!!, android.R.layout.simple_list_item_1, fieldNames )

        ArrayAdapter.createFromResource(activity!!, R.array.operators, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.operatorSpinner.adapter = adapter
            }

        if (rule != null)
        {
            setHasOptionsMenu( true )

            binding.nameEditText.setText( rule!!.name )
            binding.valueEditText.setText( rule!!.value )

            for (i in fields.indices)
            {
                if (rule!!.fieldId == fields[i].id)
                {
                    binding.fieldSpinner.setSelection(i)
                    break
                }
            }

            val operators = resources.getStringArray(R.array.operators)

            for (i in operators.indices)
            {
                if (rule!!.operator == operators[i])
                {
                    binding.operatorSpinner.setSelection(i)
                    break
                }
            }
        }

        binding.fieldSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
            {
                if (firstLaunch)
                {
                    firstLaunch = false
                }
                else
                {
                    binding.valueEditText.setText("")
                }

                setKeyboardInputType( fields[position] )
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            val name = binding.nameEditText.text.toString()
            val value = binding.valueEditText.text.toString()

            if (name.length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a field name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (value.length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a value.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val field = fields[binding.fieldSpinner.selectedItemPosition]

            val operator = binding.operatorSpinner.selectedItem as String

            if (rule == null)
            {
                rule = Rule( -1, studyId, field.id, name, operator, value )
                rule!!.id = DAO.ruleDAO.createRule( rule!! )
            }

            rule!!.name = name
            rule!!.fieldId = field.id
            rule!!.operator = operator
            rule!!.value = value

            DAO.ruleDAO.updateRule( rule!! )

            findNavController().popBackStack()
        }
    }

    fun setKeyboardInputType( field: Field)
    {
        if (field.type == FieldType.Number.toString())
        {
            if (field.integerOnly)
            {
                binding.valueEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            else
            {
                binding.valueEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }
        else
        {
            binding.valueEditText.inputType = InputType.TYPE_CLASS_TEXT
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_delete_field ->
            {
                ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this rule?", 0, this)
                return true
            }
        }

        return false
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Int )
    {
        rule?.let { rule ->
            DAO.ruleDAO.deleteRule( rule )
        }

        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}