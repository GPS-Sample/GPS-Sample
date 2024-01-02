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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.databinding.FragmentCreateRuleBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import edu.gtri.gpssample.utils.DateUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class CreateRuleFragment : Fragment(),
    DatePickerDialog.DatePickerDialogDelegate,
    TimePickerDialog.TimePickerDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate
{
    private var rule: Rule? = null
    private var _binding: FragmentCreateRuleBinding? = null
    private val binding get() = _binding!!

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

        // for an existing rule, figure out which field was selected

        sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
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
                            }
                            else if (field.type == FieldType.Dropdown)
                            {
                                binding.textValueEditText.visibility = View.GONE
                                binding.dropdownValueSpinner.visibility = View.VISIBLE
                                binding.dateValueTextView.visibility = View.GONE

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

                            break
                        }
                    }
                }
            }
        }

        sharedViewModel.createRuleModel.currentRule?.value?.operator?.let { operator ->
            sharedViewModel.createRuleModel.ruleOperationPosition.value = OperatorConverter.toIndex( operator )
        }

        binding.fieldSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                sharedViewModel.createRuleModel.currentRule?.value?.let{ rule ->
                    sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                        val field = study.fields[position]
                        rule.field = field

                        if (field.type == FieldType.Text || field.type == FieldType.Number)
                        {
                            binding.textValueEditText.visibility = View.VISIBLE
                            binding.dropdownValueSpinner.visibility = View.GONE
                            binding.dateValueTextView.visibility = View.GONE
                        }
                        else if (field.type == FieldType.Dropdown)
                        {
                            binding.textValueEditText.visibility = View.GONE
                            binding.dropdownValueSpinner.visibility = View.VISIBLE
                            binding.dateValueTextView.visibility = View.GONE

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
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        binding.dateValueTextView.setOnClickListener {
            sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
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
        }

        binding.dropdownValueSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                sharedViewModel.createRuleModel.currentRule?.value?.let{ rule ->
                    rule.field?.let { field ->
                        val fieldOption = field.fieldOptions[position]
                        rule.value = fieldOption.name
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        binding.deleteImageView.setOnClickListener {
            sharedViewModel.createStudyModel.currentStudy?.value?.let  { study ->
                sharedViewModel.createRuleModel.deleteSelectedRule( study )
                findNavController().popBackStack()
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            sharedViewModel.addRule()
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateRuleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun setKeyboardInputType( field: Field)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_delete ->
            {
                ConfirmationDialog( activity,  resources.getString(R.string.please_confirm), resources.getString(R.string.delete_rule_message),
                     resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
                return true
            }
        }

        return false
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        rule?.let { rule ->
            DAO.ruleDAO.deleteRule( rule )
        }

        findNavController().popBackStack()
    }

    override fun didSelectDate(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        sharedViewModel.createRuleModel.currentRule?.value?.let{ rule ->
            val unixTime = date.time
            rule.value = unixTime.toString()

            Log.d( "xxx", Date( unixTime ).toString())

            binding.dateValueTextView.setText( DateUtils.dateString( Date( unixTime ), config.dateFormat ))

            if (field.time)
            {
                TimePickerDialog( context!!, context?.getString(R.string.select_time) ?: "Select Time", date, field, fieldData, editText,this )
            }
        }
    }

    override fun didSelectTime(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        sharedViewModel.createRuleModel.currentRule?.value?.let{ rule ->
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
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}