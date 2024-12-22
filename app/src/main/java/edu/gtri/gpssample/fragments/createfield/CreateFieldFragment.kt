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
import edu.gtri.gpssample.database.models.FieldOption
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.fragments.ManageStudies.CreateFilterAdapter
import edu.gtri.gpssample.fragments.add_household.AddHouseholdAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class CreateFieldFragment : Fragment(), InputDialog.InputDialogDelegate

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

        // respond to changes to the FieldType dropdown
        sharedViewModel.createFieldModel.fieldType.observe( this, androidx.lifecycle.Observer { fieldType ->

            val textLayout = view.findViewById<LinearLayout>(R.id.layout_field_text)
            val numberLayout = view.findViewById<LinearLayout>(R.id.layout_field_number)
            val dateLayout = view.findViewById<LinearLayout>(R.id.layout_field_date)

            binding.fieldBlockContainerCheckBox.visibility = View.GONE

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
                }
                FieldType.Date -> {
                    textLayout.visibility = View.GONE
                    numberLayout.visibility = View.GONE
                    dateLayout.visibility = View.VISIBLE
                    checkboxLayout.visibility = View.GONE
                    dropdownLayout.visibility = View.GONE
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
                        val childField = Field( field.uuid, fields.size+1,"", FieldType.Text, false, false, false, false, false, false )
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
                        val childField = Field( parentField.uuid, fields.size+1, "", FieldType.Text, false, false, false, false, false, false )
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

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}