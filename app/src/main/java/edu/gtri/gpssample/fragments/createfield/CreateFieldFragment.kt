package edu.gtri.gpssample.fragments.createfield

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.io.Resources
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentCreateFieldBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.FieldOption
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import edu.gtri.gpssample.fragments.add_household.AddHouseholdAdapter
import edu.gtri.gpssample.utils.DateUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class CreateFieldFragment : Fragment(), InputDialog.InputDialogDelegate, DatePickerDialog.DatePickerDialogDelegate
{
    private var _binding: FragmentCreateFieldBinding? = null
    private val binding get() = _binding!!

    private lateinit var field: Field
    private lateinit var study: Study
    private lateinit var checkboxLayout: LinearLayout
    private lateinit var checkboxRecyclerView: RecyclerView
    private lateinit var dropdownRecyclerView: RecyclerView
    private lateinit var createFieldCheckboxAdapter: CreateFieldCheckboxAdapter
    private lateinit var createFieldDropdownAdapter: CreateFieldDropdownAdapter
    private lateinit var dropdownLayout: LinearLayout
    private lateinit var sharedViewModel : ConfigurationViewModel

    private val kCheckboxTag = 1
    private val kDropdownTag = 2
    private var isBlockField = false

    val fieldTypes : Array<String>
        get() {
            val array: Array<String> = Array(5)
            { i ->
                when (i) {
                    0 -> getString(R.string.text)
                    1 -> getString(R.string.number)
                    2 -> getString(R.string.date)
                    3 -> getString(R.string.checkbox)
                    4 -> getString(R.string.dropdown)
                    else -> String()
                }
            }
            return array
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        sharedViewModel.createFieldModel.fieldTypes = fieldTypes

        sharedViewModel.createFieldModel.currentField?.value?.type?.let { fieldType ->
            sharedViewModel.createFieldModel.fieldTypePosition?.value = FieldTypeConverter.toIndex( fieldType )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateFieldBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createFieldFragment = this@CreateFieldFragment
            this.executePendingBindings()
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        }

        sharedViewModel.createFieldModel.currentField?.value?.let { field ->
            this.field = field
        }

        field.parentUUID?.let {
            isBlockField = true
            binding.blockButtonLayout.visibility = View.VISIBLE
            binding.normalButtonLayout.visibility = View.GONE
        }

        field.fields?.let {
            binding.fieldBlockContainerCheckBox.isChecked = true
        }

        binding.fieldIndexEditText.setText( "${field.index}.")

        binding.fieldBlockContainerCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (isChecked)
                {
                    if (field.fields == null)
                    {
                        field.fields = ArrayList<Field>()
                    }
                    binding.saveButton.text = resources.getString(R.string.next)
                }
                else
                {
                    field.fields = null
                    binding.saveButton.text = resources.getString(R.string.save)
                }
            }
        })

        binding.deleteImageView.setOnClickListener {
            sharedViewModel.createStudyModel.currentStudy?.value?.let  { study ->

                sharedViewModel.createFieldModel.deleteCurrentField( study )
                findNavController().popBackStack()
            }
        }

        checkboxLayout = view.findViewById(R.id.layout_field_checkbox)
        checkboxRecyclerView = checkboxLayout.findViewById<RecyclerView>( R.id.checkbox_recycler_view )
        createFieldCheckboxAdapter = CreateFieldCheckboxAdapter( field.fieldOptions )
        createFieldCheckboxAdapter.shouldDeleteCheckboxFieldOption = this::shouldDeleteCheckboxFieldOption
        checkboxRecyclerView.adapter = createFieldCheckboxAdapter
        checkboxRecyclerView.layoutManager = LinearLayoutManager(activity)

        dropdownLayout = view.findViewById(R.id.layout_field_dropdown)
        dropdownRecyclerView = dropdownLayout.findViewById<RecyclerView>( R.id.dropdown_recycler_view )
        createFieldDropdownAdapter = CreateFieldDropdownAdapter( field.fieldOptions )
        createFieldDropdownAdapter.shouldDeleteDropdownFieldOption = this::shouldDeleteDropdownFieldOption
        dropdownRecyclerView.adapter = createFieldDropdownAdapter
        dropdownRecyclerView.layoutManager = LinearLayoutManager(activity)

        val minimumNumberCheckBox = view.findViewById<CheckBox>(R.id.minimum_number_checkBox)
        val maximumNumberCheckBox = view.findViewById<CheckBox>(R.id.maximum_number_checkBox)
        val minimumNumberEditText = view.findViewById<EditText>(R.id.minimum_number_edit_text)
        val maximumNumberEditText = view.findViewById<EditText>(R.id.maximum_number_edit_text)

        minimumNumberCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (isChecked)
                {
                    minimumNumberEditText.visibility = View.VISIBLE
                }
                else
                {
                    minimumNumberEditText.visibility = View.GONE
                }
            }
        })

        maximumNumberCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (isChecked)
                {
                    maximumNumberEditText.visibility = View.VISIBLE
                }
                else
                {
                    maximumNumberEditText.visibility = View.GONE
                }
            }
        })

        val minimumDateCheckBox = view.findViewById<CheckBox>(R.id.minimum_date_checkBox)
        val maximumDateCheckBox = view.findViewById<CheckBox>(R.id.maximum_date_checkBox)
        val minimumDateLayout = view.findViewById<LinearLayout>(R.id.minimum_date_layout)
        val maximumDateLayout = view.findViewById<LinearLayout>(R.id.maximum_date_layout)
        val minimumDateEditText = view.findViewById<EditText>(R.id.minimum_date_edit_text)
        val maximumDateEditText = view.findViewById<EditText>(R.id.maximum_date_edit_text)
        val minimumDateCalendarButton = view.findViewById<Button>(R.id.minimum_date_calendar_button)
        val maximumDateCalendarButton = view.findViewById<Button>(R.id.maximum_date_calendar_button)

        val dateCheckBox = view.findViewById<CheckBox>(R.id.date_checkBox)

        dateCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                minimumDateEditText.setText("")
                maximumDateEditText.setText("")

                if (isChecked)
                {
                    field.date = true
                    minimumDateCheckBox.visibility = View.VISIBLE
                    maximumDateCheckBox.visibility = View.VISIBLE
                }
                else
                {
                    field.date = false
                    minimumDateCheckBox.isChecked = false
                    maximumDateCheckBox.isChecked = false
                    minimumDateCheckBox.visibility = View.GONE
                    maximumDateCheckBox.visibility = View.GONE
                }
            }
        })

        minimumDateCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (isChecked)
                {
                    minimumDateLayout.visibility = View.VISIBLE
                }
                else
                {
                    minimumDateLayout.visibility = View.GONE
                }
            }
        })

        maximumDateCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (isChecked)
                {
                    maximumDateLayout.visibility = View.VISIBLE
                }
                else
                {
                    maximumDateLayout.visibility = View.GONE
                }
            }
        })

        minimumDateCalendarButton.setOnClickListener {
            DatePickerDialog( context!!, context?.getString(R.string.select_date) ?: "Select Date", Date(), field, null, minimumDateEditText,this )
        }

        maximumDateCalendarButton.setOnClickListener {
            DatePickerDialog( context!!, context?.getString(R.string.select_date) ?: "Select Date", Date(), field, null, maximumDateEditText,this )
        }

        // respond to changes to the FieldType dropdown
        sharedViewModel.createFieldModel.fieldType.observe( this, androidx.lifecycle.Observer { fieldType ->

            val textLayout = view.findViewById<LinearLayout>(R.id.layout_field_text)
            val numberLayout = view.findViewById<LinearLayout>(R.id.layout_field_number)
            val dateLayout = view.findViewById<LinearLayout>(R.id.layout_field_date)

            binding.fieldBlockContainerCheckBox.visibility = View.GONE

            minimumNumberCheckBox.isChecked = false
            maximumNumberCheckBox.isChecked = false
            minimumNumberEditText.visibility = View.GONE
            maximumNumberEditText.visibility = View.GONE

            minimumDateCheckBox.isChecked = false
            maximumDateCheckBox.isChecked = false
            minimumDateLayout.visibility = View.GONE
            maximumDateLayout.visibility = View.GONE

            minimumDateCheckBox.visibility = View.GONE
            maximumDateCheckBox.visibility = View.GONE

            when (fieldType)
            {
                FieldType.Text -> {
                    textLayout.visibility = View.VISIBLE
                    numberLayout.visibility = View.GONE
                    dateLayout.visibility = View.GONE
                    checkboxLayout.visibility = View.GONE
                    dropdownLayout.visibility = View.GONE
                }
                FieldType.Number -> {
                    if (!isBlockField)
                    {
                        binding.fieldBlockContainerCheckBox.visibility = View.VISIBLE
                    }
                    textLayout.visibility = View.GONE
                    numberLayout.visibility = View.VISIBLE
                    dateLayout.visibility = View.GONE
                    checkboxLayout.visibility = View.GONE
                    dropdownLayout.visibility = View.GONE

                    field.minimum?.let {
                        minimumNumberCheckBox.isChecked = true
                        minimumNumberEditText.visibility = View.VISIBLE
                        minimumNumberEditText.setText(it.toInt().toString())
                    }

                    field.maximum?.let {
                        maximumNumberCheckBox.isChecked = true
                        maximumNumberEditText.visibility = View.VISIBLE
                        maximumNumberEditText.setText(it.toInt().toString())
                    }
                }
                FieldType.Date -> {
                    textLayout.visibility = View.GONE
                    numberLayout.visibility = View.GONE
                    dateLayout.visibility = View.VISIBLE
                    checkboxLayout.visibility = View.GONE
                    dropdownLayout.visibility = View.GONE

                    field.minimum?.let {
                        val date = Date( it.toLong())
                        minimumDateCheckBox.isChecked = true
                        minimumDateCheckBox.visibility = View.VISIBLE
                        minimumDateLayout.visibility = View.VISIBLE
                        sharedViewModel.currentConfiguration?.value?.let { config ->
                            minimumDateEditText.setText( DateUtils.dateString( date, config.dateFormat ))
                        }
                    }

                    field.maximum?.let {
                        val date = Date( it.toLong())
                        maximumDateCheckBox.isChecked = true
                        maximumDateCheckBox.visibility = View.VISIBLE
                        maximumDateLayout.visibility = View.VISIBLE
                        sharedViewModel.currentConfiguration?.value?.let { config ->
                            maximumDateEditText.setText( DateUtils.dateString( date, config.dateFormat ))
                        }
                    }
                }
                FieldType.Checkbox -> {
                    textLayout.visibility = View.GONE
                    numberLayout.visibility = View.GONE
                    dateLayout.visibility = View.GONE
                    checkboxLayout.visibility = View.VISIBLE
                    dropdownLayout.visibility = View.GONE
                }
                FieldType.Dropdown -> {
                    textLayout.visibility = View.GONE
                    numberLayout.visibility = View.GONE
                    dateLayout.visibility = View.GONE
                    checkboxLayout.visibility = View.GONE
                    dropdownLayout.visibility = View.VISIBLE
                }
                else -> {}
            }
        })

        val checkboxAddAnotherButton = checkboxLayout.findViewById<Button>(R.id.add_another_button)

        checkboxAddAnotherButton.setOnClickListener {
            InputDialog( activity!!, false, resources.getString(R.string.option_item_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kCheckboxTag, this@CreateFieldFragment )
        }

        val dropdownAddAnotherButton = dropdownLayout.findViewById<Button>(R.id.add_another_button)

        dropdownAddAnotherButton.setOnClickListener {
            InputDialog( activity!!, false, resources.getString(R.string.option_item_name), "", resources.getString(R.string.cancel), resources.getString(R.string.save), kDropdownTag, this@CreateFieldFragment )
        }

        binding.saveButton.setOnClickListener {
            if (field.type == FieldType.Date && !field.date && !field.time)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString( R.string.select_date_type), Toast.LENGTH_LONG).show()
            }
            else
            {
                minimumNumberEditText.text.toString().toDoubleOrNull()?.let {
                    field.minimum = it
                }

                maximumNumberEditText.text.toString().toDoubleOrNull()?.let {
                    field.maximum = it
                }

                if (minimumDateEditText.tag is Double)
                {
                    field.minimum = minimumDateEditText.tag as Double
                }

                if (maximumDateEditText.tag is Double)
                {
                    field.maximum = maximumDateEditText.tag as Double
                }

                if (field.minimum != null && field.maximum !=null && field.minimum!! > field.maximum!!)
                {
                    Toast.makeText(activity!!.applicationContext, resources.getString( R.string.min_greater_than_max ), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (!study.fields.contains (field))
                {
                    study.fields.add( field )
                }

                if (field.fields == null)
                {
                    findNavController().popBackStack()
                }
                else
                {
                    sharedViewModel.createFieldModel.setParentField( field )

                    field.fields?.let { fields ->
                        val childField = Field( field.uuid, fields.size+1,"", FieldType.Text, false, false, false, false, false, false, null, null )
                        fields.add( childField )
                        sharedViewModel.createFieldModel.setCurrentField( childField )
                        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment )
                    }
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addAnotherButton.setOnClickListener {
            if (binding.fieldNameEditText.text.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString( R.string.enter_name), Toast.LENGTH_LONG).show()
            }
            else
            {
                sharedViewModel.createFieldModel.parentField?.value?.let { parentField ->

                    parentField.fields?.let { fields ->
                        val childField = Field( parentField.uuid, fields.size+1, "", FieldType.Text, false, false, false, false, false, false, null, null )
                        fields.add( childField )
                        sharedViewModel.createFieldModel.setCurrentField( childField )
                        findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment )
                    }
                }
            }
        }

        binding.endBlockButton.setOnClickListener {
            findNavController().popBackStack( R.id.CreateStudyFragment, false )
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateFieldFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun didCancelText( tag: Any? )
    {
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        tag?.let {
            val tag = it as Int
            if (tag == kCheckboxTag)
            {
                val fieldOption = FieldOption( name )
                field.fieldOptions.add( fieldOption )
                createFieldCheckboxAdapter.updateFieldOptions( field.fieldOptions )
            }
            else if (tag == kDropdownTag)
            {
                val fieldOption = FieldOption( name )
                field.fieldOptions.add( fieldOption )
                createFieldDropdownAdapter.updateFieldOptions( field.fieldOptions )
            }
        }
    }

    private fun shouldDeleteCheckboxFieldOption(fieldOption: FieldOption)
    {
        field.fieldOptions.remove(fieldOption)
        createFieldCheckboxAdapter.updateFieldOptions( field.fieldOptions )
    }

    private fun shouldDeleteDropdownFieldOption(fieldOption: FieldOption)
    {
        field.fieldOptions.remove(fieldOption)
        createFieldDropdownAdapter.updateFieldOptions( field.fieldOptions )
    }

    override fun didSelectDate(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        sharedViewModel.currentConfiguration?.value?.let { config ->
            editText?.let { editText ->
                if (editText.id == R.id.minimum_date_edit_text)
                {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    calendar[Calendar.HOUR] = 0
                    calendar[Calendar.MINUTE] = 0
                    calendar[Calendar.SECOND] = 0
                    calendar[Calendar.AM_PM] = Calendar.AM
                    editText.tag = calendar.time.time.toDouble()
                }
                else if (editText.id == R.id.maximum_date_edit_text)
                {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    calendar[Calendar.HOUR] = 11
                    calendar[Calendar.MINUTE] = 59
                    calendar[Calendar.SECOND] = 59
                    calendar[Calendar.AM_PM] = Calendar.PM
                    editText.tag = calendar.time.time.toDouble()
                }

                editText.setText( DateUtils.dateString( date, config.dateFormat ))
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}