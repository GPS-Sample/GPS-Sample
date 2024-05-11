package edu.gtri.gpssample.fragments.createrule

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentCreateRuleBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import edu.gtri.gpssample.fragments.add_household.CheckboxOptionAdapter
import edu.gtri.gpssample.utils.DateUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class CreateRuleFragment : Fragment(),
    DatePickerDialog.DatePickerDialogDelegate,
    TimePickerDialog.TimePickerDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentCreateRuleBinding? = null
    private val binding get() = _binding!!

    private lateinit var rule: Rule
    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        sharedViewModel.createRuleModel.fragment = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateRuleBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createRuleFragment = this@CreateRuleFragment
            this.executePendingBindings()
        }

        sharedViewModel.currentConfiguration?.value?.let { config ->
            this.config = config
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        }

        sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
            this.rule = rule
        }

        // for an existing rule, figure out which field was selected

        rule.field?.let{ field->
            for (i in 0..study.fields.size-1)
            {
                if (study.fields[i].uuid == field.uuid)
                {
                    sharedViewModel.createRuleModel.ruleFieldPosition.value = i

                    if (field.type == FieldType.Text || field.type == FieldType.Number)
                    {
                        binding.textValueEditText.visibility = View.VISIBLE
                        binding.dropdownValueSpinner.visibility = View.GONE
                        binding.dateValueTextView.visibility = View.GONE
                        binding.checkboxValueRecyclerView.visibility = View.GONE
                    }
                    else if (field.type == FieldType.Dropdown)
                    {
                        binding.textValueEditText.visibility = View.GONE
                        binding.dropdownValueSpinner.visibility = View.VISIBLE
                        binding.dateValueTextView.visibility = View.GONE
                        binding.checkboxValueRecyclerView.visibility = View.GONE

                        val items = ArrayList<String>()

                        for (j in 0..field.fieldOptions.size-1)
                        {
                            val fieldOption = field.fieldOptions[j]
                            items.add( fieldOption.name )

                            if (rule.value == fieldOption.name)
                            {
                                sharedViewModel.createRuleModel.dropdownPosition.value = j
                            }
                        }

                        binding.dropdownValueSpinner.adapter = ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_dropdown_item, items )
                    }
                    else if (field.type == FieldType.Date)
                    {
                        binding.textValueEditText.visibility = View.GONE
                        binding.dropdownValueSpinner.visibility = View.GONE
                        binding.dateValueTextView.visibility = View.VISIBLE
                        binding.checkboxValueRecyclerView.visibility = View.GONE

                        rule.value.toLongOrNull()?.let { unixTime ->
                            if (field.date && !field.time)
                            {
                                binding.dateValueTextView.setText( DateUtils.dateString( Date( unixTime ), config.dateFormat ))
                            }
                            else if (field.time && !field.date)
                            {
                                binding.dateValueTextView.setText( DateUtils.timeString( Date( unixTime ), config.timeFormat ))
                            }
                            else
                            {
                                binding.dateValueTextView.setText( DateUtils.dateTimeString( Date( unixTime ), config.dateFormat, config.timeFormat))
                            }
                        }
                    }
                    else if (field.type == FieldType.Checkbox)
                    {
                        binding.textValueEditText.visibility = View.GONE
                        binding.dropdownValueSpinner.visibility = View.GONE
                        binding.dateValueTextView.visibility = View.GONE
                        binding.checkboxValueRecyclerView.visibility = View.VISIBLE

                        binding.checkboxValueRecyclerView.adapter = CheckboxOptionAdapter( true, rule.fieldDataOptions )
                    }

                    break
                }
            }
        }

        if (rule.uuid.isEmpty())
        {
            val field = study.fields[0]
            if (field.type == FieldType.Checkbox)
            {
                rule.operator = Operator.Contains
            }
        }

        rule.operator?.let { operator ->
            sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( operator )
        }

        binding.checkboxValueRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.checkboxValueRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.checkboxValueRecyclerView.recycledViewPool.setMaxRecycledViews(0, 0 )

        binding.fieldSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                val field = study.fields[position]
                rule.field = field
                setKeyboardInputType( field )
                rule.operator = Operator.Equal
                sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( Operator.Equal )

                if (field.type == FieldType.Text || field.type == FieldType.Number)
                {
                    binding.textValueEditText.visibility = View.VISIBLE
                    binding.dropdownValueSpinner.visibility = View.GONE
                    binding.dateValueTextView.visibility = View.GONE
                    binding.checkboxValueRecyclerView.visibility = View.GONE
                }
                else if (field.type == FieldType.Dropdown)
                {
                    binding.textValueEditText.visibility = View.GONE
                    binding.dropdownValueSpinner.visibility = View.VISIBLE
                    binding.dateValueTextView.visibility = View.GONE
                    binding.checkboxValueRecyclerView.visibility = View.GONE

                    val items = ArrayList<String>()

                    for (j in 0..field.fieldOptions.size-1)
                    {
                        val fieldOption = field.fieldOptions[j]
                        items.add( fieldOption.name )

                        if (rule.value == fieldOption.name)
                        {
                            sharedViewModel.createRuleModel.dropdownPosition.value = j
                        }
                    }

                    binding.dropdownValueSpinner.adapter = ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_dropdown_item, items )
                }
                else if (field.type == FieldType.Date)
                {
                    binding.textValueEditText.visibility = View.GONE
                    binding.dropdownValueSpinner.visibility = View.GONE
                    binding.dateValueTextView.visibility = View.VISIBLE
                    binding.checkboxValueRecyclerView.visibility = View.GONE
                }
                else if (field.type == FieldType.Checkbox)
                {
                    binding.textValueEditText.visibility = View.GONE
                    binding.dropdownValueSpinner.visibility = View.GONE
                    binding.dateValueTextView.visibility = View.GONE
                    binding.checkboxValueRecyclerView.visibility = View.VISIBLE

                    if (rule.fieldDataOptions.isEmpty())
                    {
                        for (fieldOption in field.fieldOptions)
                        {
                            val fieldDataOption = FieldDataOption( fieldOption.name, false )
                            rule.fieldDataOptions.add( fieldDataOption )
                        }
                    }

                    rule.operator = Operator.Contains
                    sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( Operator.Contains )

                    binding.checkboxValueRecyclerView.adapter = CheckboxOptionAdapter( true, rule.fieldDataOptions )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        binding.dateValueTextView.setOnClickListener {
            rule.field?.let { field ->
                val date = Date()
                if (field.time && !field.date)
                {
                    TimePickerDialog(context!!, context?.getString(R.string.select_time) ?: "Select Time", date, field, null, null, this)
                }
                else
                {
                    DatePickerDialog(context!!, context?.getString(R.string.select_date) ?: "Select Date", date, field, null, null, this)
                }
            }
        }

        binding.dropdownValueSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                rule.field?.let { field ->
                    val fieldOption = field.fieldOptions[position]
                    rule.value = fieldOption.name
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity,  resources.getString(R.string.please_confirm), resources.getString(R.string.delete_rule_message), resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            if (rule.name.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, context?.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            rule.operator?.let { operator ->
                rule.field?.let { field ->
                    when (field.type)
                    {
                        FieldType.Text,
                        FieldType.Checkbox ->
                        {
                            when (operator)
                            {
                                Operator.LessThan,
                                Operator.GreaterThan,
                                Operator.LessThanOrEqual,
                                Operator.GreaterThanOrEqual ->
                                {
                                    Toast.makeText(activity!!.applicationContext, context?.getString(R.string.invalid_operator), Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                else -> {}
                            }
                        }

                        FieldType.Number,
                        FieldType.Date ->
                        {
                            if (operator == Operator.Contains)
                            {
                                Toast.makeText(activity!!.applicationContext, context?.getString(R.string.invalid_operator), Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                        }

                        FieldType.Dropdown ->
                        {
                            when (operator)
                            {
                                Operator.Contains,
                                Operator.LessThan,
                                Operator.GreaterThan,
                                Operator.LessThanOrEqual,
                                Operator.GreaterThanOrEqual ->
                                {
                                    Toast.makeText(activity!!.applicationContext, context?.getString(R.string.invalid_operator), Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                else -> {}
                            }
                        }

                        else -> {}
                    }
                }
            }

            sharedViewModel.addRule()
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateRuleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun setKeyboardInputType( field: Field )
    {
        if (field.type == FieldType.Number)
        {
            if (field.integerOnly)
            {
                binding.textValueEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            else
            {
                binding.textValueEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }
        else
        {
            binding.textValueEditText.inputType = InputType.TYPE_CLASS_TEXT
        }
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        sharedViewModel.createRuleModel.deleteSelectedRule( study )
        findNavController().popBackStack()
    }

    override fun didSelectDate(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        val unixTime = date.time
        rule.value = unixTime.toString()

        binding.dateValueTextView.setText( DateUtils.dateString( Date( unixTime ), config.dateFormat ))

        if (field.time)
        {
            TimePickerDialog( context!!, context?.getString(R.string.select_time) ?: "Select Time", date, field, fieldData, editText,this )
        }
    }

    override fun didSelectTime(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        val unixTime = date.time
        rule.value = unixTime.toString()

        if (field.date && field.time)
        {
            binding.dateValueTextView.setText( DateUtils.dateTimeString( Date( unixTime ), config.dateFormat, config.timeFormat))
        }
        else if (field.time)
        {
            binding.dateValueTextView.setText( DateUtils.timeString( Date( unixTime ), config.timeFormat))
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}