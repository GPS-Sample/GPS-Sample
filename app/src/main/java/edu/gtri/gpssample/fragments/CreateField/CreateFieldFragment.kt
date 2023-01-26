package edu.gtri.gpssample.fragments.CreateField

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.GPSSampleDAO
import edu.gtri.gpssample.databinding.FragmentCreateFieldBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.models.Field
import edu.gtri.gpssample.models.Study

class CreateFieldFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var field: Field? = null
    private var study: Study? = null

    private lateinit var checkbox1Layout: LinearLayout
    private lateinit var checkbox2Layout: LinearLayout
    private lateinit var checkbox3Layout: LinearLayout
    private lateinit var checkbox4Layout: LinearLayout
    private lateinit var dropdown1Layout: LinearLayout
    private lateinit var dropdown2Layout: LinearLayout
    private lateinit var dropdown3Layout: LinearLayout
    private lateinit var dropdown4Layout: LinearLayout
    private lateinit var checkbox1EditText: EditText
    private lateinit var checkbox2EditText: EditText
    private lateinit var checkbox3EditText: EditText
    private lateinit var checkbox4EditText: EditText
    private lateinit var dropdown1EditText: EditText
    private lateinit var dropdown2EditText: EditText
    private lateinit var dropdown3EditText: EditText
    private lateinit var dropdown4EditText: EditText
    private lateinit var textLayout: LinearLayout
    private lateinit var numberLayout: LinearLayout
    private lateinit var dateLayout: LinearLayout
    private lateinit var checkboxLayout: LinearLayout
    private lateinit var dropdownLayout: LinearLayout
    private var _binding: FragmentCreateFieldBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CreateFieldViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateFieldViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateFieldBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

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

        study = GPSSampleDAO.studyDAO.getStudy( studyId )
        if (study == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $studyId not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val fieldId = arguments!!.getInt( Key.kFieldId.toString(), -1);

        if (fieldId > 0)
        {
            field = GPSSampleDAO.fieldDAO.getField( fieldId )
        }

        textLayout = view.findViewById<LinearLayout>(R.id.layout_field_text)
        numberLayout = view.findViewById<LinearLayout>(R.id.layout_field_number)
        dateLayout = view.findViewById<LinearLayout>(R.id.layout_field_date)
        checkboxLayout = view.findViewById<LinearLayout>(R.id.layout_field_checkbox)
        dropdownLayout = view.findViewById<LinearLayout>(R.id.layout_field_dropdown)

        checkbox1Layout = checkboxLayout.findViewById( R.id.option_1_layout )
        checkbox2Layout = checkboxLayout.findViewById( R.id.option_2_layout )
        checkbox3Layout = checkboxLayout.findViewById( R.id.option_3_layout )
        checkbox4Layout = checkboxLayout.findViewById( R.id.option_4_layout )

        checkbox1EditText = checkboxLayout.findViewById( R.id.option_1_edit_text )
        checkbox2EditText = checkboxLayout.findViewById( R.id.option_2_edit_text )
        checkbox3EditText = checkboxLayout.findViewById( R.id.option_3_edit_text )
        checkbox4EditText = checkboxLayout.findViewById( R.id.option_4_edit_text )

        dropdown1Layout = dropdownLayout.findViewById( R.id.option_1_layout )
        dropdown2Layout = dropdownLayout.findViewById( R.id.option_2_layout )
        dropdown3Layout = dropdownLayout.findViewById( R.id.option_3_layout )
        dropdown4Layout = dropdownLayout.findViewById( R.id.option_4_layout )

        dropdown1EditText = dropdownLayout.findViewById( R.id.option_1_edit_text )
        dropdown2EditText = dropdownLayout.findViewById( R.id.option_2_edit_text )
        dropdown3EditText = dropdownLayout.findViewById( R.id.option_3_edit_text )
        dropdown4EditText = dropdownLayout.findViewById( R.id.option_4_edit_text )

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "CreateFieldFragment", Toast.LENGTH_SHORT).show()
            }
        }

        ArrayAdapter.createFromResource(activity!!, R.array.field_types, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.fieldTypeSpinner.adapter = adapter
            }

        binding.fieldTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
            {
                when(position)
                {
                    0 -> {
                        textLayout.visibility = View.VISIBLE
                        numberLayout.visibility = View.GONE
                        dateLayout.visibility = View.GONE
                        checkboxLayout.visibility = View.GONE
                        dropdownLayout.visibility = View.GONE
                    }
                    1 -> {
                        textLayout.visibility = View.GONE
                        numberLayout.visibility = View.VISIBLE
                        dateLayout.visibility = View.GONE
                        checkboxLayout.visibility = View.GONE
                        dropdownLayout.visibility = View.GONE
                    }
                    2 -> {
                        textLayout.visibility = View.GONE
                        numberLayout.visibility = View.GONE
                        dateLayout.visibility = View.VISIBLE
                        checkboxLayout.visibility = View.GONE
                        dropdownLayout.visibility = View.GONE
                    }
                    3 -> {
                        textLayout.visibility = View.GONE
                        numberLayout.visibility = View.GONE
                        dateLayout.visibility = View.GONE
                        checkboxLayout.visibility = View.VISIBLE
                        dropdownLayout.visibility = View.GONE
                    }
                    4 -> {
                        textLayout.visibility = View.GONE
                        numberLayout.visibility = View.GONE
                        dateLayout.visibility = View.GONE
                        checkboxLayout.visibility = View.GONE
                        dropdownLayout.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        val checkboxAddAnotherButton = checkboxLayout.findViewById<Button>(R.id.add_another_button)

        checkboxAddAnotherButton.setOnClickListener {

            if (checkbox1Layout.visibility == View.GONE)
            {
                checkbox1Layout.visibility = View.VISIBLE
            }
            else if (checkbox2Layout.visibility == View.GONE)
            {
                checkbox2Layout.visibility = View.VISIBLE
            }
            else if (checkbox3Layout.visibility == View.GONE)
            {
                checkbox3Layout.visibility = View.VISIBLE
            }
            else if (checkbox4Layout.visibility == View.GONE)
            {
                checkbox4Layout.visibility = View.VISIBLE
            }
        }

        val dropdownAddAnotherButton = dropdownLayout.findViewById<Button>(R.id.add_another_button)

        dropdownAddAnotherButton.setOnClickListener {

            if (dropdown1Layout.visibility == View.GONE)
            {
                dropdown1Layout.visibility = View.VISIBLE
            }
            else if (dropdown2Layout.visibility == View.GONE)
            {
                dropdown2Layout.visibility = View.VISIBLE
            }
            else if (dropdown3Layout.visibility == View.GONE)
            {
                dropdown3Layout.visibility = View.VISIBLE
            }
            else if (dropdown4Layout.visibility == View.GONE)
            {
                dropdown4Layout.visibility = View.VISIBLE
            }
        }

        val checkbox1Button = checkboxLayout.findViewById<Button>(R.id.option_1_button)
        checkbox1Button.setOnClickListener {

            checkbox1EditText.setText("")
            checkbox1Layout.visibility = View.GONE
        }

        val checkbox2Button = checkboxLayout.findViewById<Button>(R.id.option_2_button)
        checkbox2Button.setOnClickListener {

            checkbox2EditText.setText("")
            checkbox2Layout.visibility = View.GONE
        }

        val checkbox3Button = checkboxLayout.findViewById<Button>(R.id.option_3_button)
        checkbox3Button.setOnClickListener {

            checkbox3EditText.setText("")
            checkbox3Layout.visibility = View.GONE
        }

        val checkbox4Button = checkboxLayout.findViewById<Button>(R.id.option_4_button)
        checkbox4Button.setOnClickListener {

            checkbox4EditText.setText("")
            checkbox4Layout.visibility = View.GONE
        }

        val dropdown1Button = dropdownLayout.findViewById<Button>(R.id.option_1_button)
        dropdown1Button.setOnClickListener {

            dropdown1EditText.setText("")
            dropdown1Layout.visibility = View.GONE
        }

        val dropdown2Button = dropdownLayout.findViewById<Button>(R.id.option_2_button)
        dropdown2Button.setOnClickListener {

            dropdown2EditText.setText("")
            dropdown2Layout.visibility = View.GONE
        }

        val dropdown3Button = dropdownLayout.findViewById<Button>(R.id.option_3_button)
        dropdown3Button.setOnClickListener {

            dropdown3EditText.setText("")
            dropdown3Layout.visibility = View.GONE
        }

        val dropdown4Button = dropdownLayout.findViewById<Button>(R.id.option_4_button)
        dropdown4Button.setOnClickListener {

            dropdown4EditText.setText("")
            dropdown4Layout.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener {

            findNavController().popBackStack()
        }

        if (field != null)
        {
            setHasOptionsMenu( true )

            binding.titleTextView.text = "Field ${field!!.name}"
            binding.fieldNameEditText.setText( field!!.name )

            when( field!!.type )
            {
                FieldType.Text -> {
                    binding.fieldTypeSpinner.setSelection(0)
                    val piiCheckbox = textLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                    val requiredCheckbox = textLayout.findViewById<CheckBox>( R.id.required_checkBox )
                    piiCheckbox.isChecked = field!!.pii
                    requiredCheckbox.isChecked = field!!.required
                }
                FieldType.Number -> {
                    binding.fieldTypeSpinner.setSelection(1)
                    val piiCheckbox = numberLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                    val integerOnlyCheckbox = numberLayout.findViewById<CheckBox>( R.id.integer_only_checkBox )
                    val requiredCheckbox = numberLayout.findViewById<CheckBox>( R.id.required_checkBox )
                    piiCheckbox.isChecked = field!!.pii
                    requiredCheckbox.isChecked = field!!.required
                    integerOnlyCheckbox.isChecked = field!!.integerOnly
                }
                FieldType.Date -> {
                    binding.fieldTypeSpinner.setSelection(2)
                    val piiCheckbox = dateLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                    val requiredCheckbox = dateLayout.findViewById<CheckBox>( R.id.required_checkBox )
                    val dateCheckbox = dateLayout.findViewById<CheckBox>( R.id.date_checkBox )
                    val timeCheckbox = dateLayout.findViewById<CheckBox>( R.id.time_checkBox)
                    piiCheckbox.isChecked = field!!.pii
                    requiredCheckbox.isChecked = field!!.required
                    dateCheckbox.isChecked = field!!.date
                    timeCheckbox.isChecked = field!!.time
                }
                FieldType.Checkbox -> {
                    binding.fieldTypeSpinner.setSelection(3)
                    val piiCheckbox = checkboxLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                    val requiredCheckbox = checkboxLayout.findViewById<CheckBox>( R.id.required_checkBox )
                    piiCheckbox.isChecked = field!!.pii
                    requiredCheckbox.isChecked = field!!.required

                    if (field!!.option1.length > 0)
                    {
                        checkbox1Layout.visibility = View.VISIBLE
                        checkbox1EditText.setText( field!!.option1 )
                    }
                    if (field!!.option2.length > 0)
                    {
                        checkbox2Layout.visibility = View.VISIBLE
                        checkbox2EditText.setText( field!!.option2 )
                    }
                    if (field!!.option3.length > 0)
                    {
                        checkbox3Layout.visibility = View.VISIBLE
                        checkbox3EditText.setText( field!!.option3 )
                    }
                    if (field!!.option4.length > 0)
                    {
                        checkbox4Layout.visibility = View.VISIBLE
                        checkbox4EditText.setText( field!!.option4 )
                    }
                }
                FieldType.Dropdown -> {
                    binding.fieldTypeSpinner.setSelection(4)
                    val piiCheckbox = dropdownLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                    val requiredCheckbox = dropdownLayout.findViewById<CheckBox>( R.id.required_checkBox )
                    piiCheckbox.isChecked = field!!.pii
                    requiredCheckbox.isChecked = field!!.required

                    if (field!!.option1.length > 0)
                    {
                        dropdown1Layout.visibility = View.VISIBLE
                        dropdown1EditText.setText( field!!.option1 )
                    }
                    if (field!!.option2.length > 0)
                    {
                        dropdown2Layout.visibility = View.VISIBLE
                        dropdown2EditText.setText( field!!.option2 )
                    }
                    if (field!!.option3.length > 0)
                    {
                        dropdown3Layout.visibility = View.VISIBLE
                        dropdown3EditText.setText( field!!.option3 )
                    }
                    if (field!!.option4.length > 0)
                    {
                        dropdown4Layout.visibility = View.VISIBLE
                        dropdown4EditText.setText( field!!.option4 )
                    }
                }
            }
        }

        binding.saveButton.setOnClickListener {

            if (binding.fieldNameEditText.text.toString().length > 0)
            {
                val fieldType = FieldType.valueOf(binding.fieldTypeSpinner.selectedItem as String)

                if (field == null)
                {
                    field = Field()
                    field!!.id = GPSSampleDAO.fieldDAO.createField( field!! )
                    field!!.studyId = study!!.id
                }

                field!!.name = binding.fieldNameEditText.text.toString()
                field!!.type = fieldType

                when (fieldType) {
                    FieldType.Text -> {
                        val piiCheckbox = textLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                        val requiredCheckbox = textLayout.findViewById<CheckBox>( R.id.required_checkBox )
                        field!!.pii = piiCheckbox.isChecked
                        field!!.required = requiredCheckbox.isChecked
                    }
                    FieldType.Number -> {
                        val piiCheckbox = numberLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                        val integerOnlyCheckbox = numberLayout.findViewById<CheckBox>( R.id.integer_only_checkBox )
                        val requiredCheckbox = numberLayout.findViewById<CheckBox>( R.id.required_checkBox )
                        field!!.pii = piiCheckbox.isChecked
                        field!!.required = requiredCheckbox.isChecked
                        field!!.integerOnly = integerOnlyCheckbox.isChecked
                    }
                    FieldType.Date -> {
                        val piiCheckbox = dateLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                        val requiredCheckbox = dateLayout.findViewById<CheckBox>( R.id.required_checkBox )
                        val dateCheckbox = dateLayout.findViewById<CheckBox>( R.id.date_checkBox )
                        val timeCheckbox = dateLayout.findViewById<CheckBox>( R.id.time_checkBox)
                        field!!.pii = piiCheckbox.isChecked
                        field!!.required = requiredCheckbox.isChecked
                        field!!.date = dateCheckbox.isChecked
                        field!!.time = timeCheckbox.isChecked
                    }
                    FieldType.Checkbox -> {
                        val editText1 = checkbox1EditText.text.toString()
                        val editText2 = checkbox2EditText.text.toString()
                        val editText3 = checkbox3EditText.text.toString()
                        val editText4 = checkbox4EditText.text.toString()

                        val length = editText1.length + editText2.length + editText3.length + editText4.length

                        if (length == 0)
                        {
                            Toast.makeText(activity!!.applicationContext, "Oops! You must enter at least 1 option", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        field!!.option1 = editText1
                        field!!.option2 = editText2
                        field!!.option3 = editText3
                        field!!.option4 = editText4

                        val piiCheckbox = checkboxLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                        val requiredCheckbox = checkboxLayout.findViewById<CheckBox>( R.id.required_checkBox )
                        field!!.pii = piiCheckbox.isChecked
                        field!!.required = requiredCheckbox.isChecked
                    }
                    FieldType.Dropdown -> {
                        val editText1 = dropdown1EditText.text.toString()
                        val editText2 = dropdown2EditText.text.toString()
                        val editText3 = dropdown3EditText.text.toString()
                        val editText4 = dropdown4EditText.text.toString()

                        val length = editText1.length + editText2.length + editText3.length + editText4.length

                        if (length == 0)
                        {
                            Toast.makeText(activity!!.applicationContext, "Oops! You must enter at least 1 option", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        field!!.option1 = editText1
                        field!!.option2 = editText2
                        field!!.option3 = editText3
                        field!!.option4 = editText4

                        val piiCheckbox = dropdownLayout.findViewById<CheckBox>( R.id.pii_checkBox )
                        val requiredCheckbox = dropdownLayout.findViewById<CheckBox>( R.id.required_checkBox )
                        field!!.pii = piiCheckbox.isChecked
                        field!!.required = requiredCheckbox.isChecked
                    }
                }

                GPSSampleDAO.fieldDAO.updateField( field!! )

                findNavController().popBackStack()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_field, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_delete_field ->
            {
                ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this field?", this)
                return true
            }
        }

        return false
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes()
    {
        GPSSampleDAO.fieldDAO.deleteField( field!! )

        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}