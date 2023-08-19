package edu.gtri.gpssample.fragments.createfield

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*

class CreateFieldFragment : Fragment()
{
    private var _binding: FragmentCreateFieldBinding? = null
    private val binding get() = _binding!!

    private lateinit var checkboxLayout: LinearLayout
    private lateinit var checkbox1Layout: LinearLayout
    private lateinit var checkbox2Layout: LinearLayout
    private lateinit var checkbox3Layout: LinearLayout
    private lateinit var checkbox4Layout: LinearLayout

    private lateinit var dropdownLayout: LinearLayout
    private lateinit var dropdown1Layout: LinearLayout
    private lateinit var dropdown2Layout: LinearLayout
    private lateinit var dropdown3Layout: LinearLayout
    private lateinit var dropdown4Layout: LinearLayout

    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        sharedViewModel.createFieldModel.fragment = this
        sharedViewModel.createFieldModel.tempField = MutableLiveData( sharedViewModel.createFieldModel.currentField?.value?.copy())

        sharedViewModel.createFieldModel.tempField?.value?.type?.let { fieldType ->
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

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createFieldFragment = this@CreateFieldFragment
            this.executePendingBindings()
        }

        binding.deleteImageView.setOnClickListener {
            sharedViewModel.createStudyModel.currentStudy?.value?.let  { study ->
                sharedViewModel.createFieldModel.deleteSelectedField( study )
                findNavController().popBackStack()
            }
        }

        checkboxLayout = view.findViewById<LinearLayout>(R.id.layout_field_checkbox)
        dropdownLayout = view.findViewById<LinearLayout>(R.id.layout_field_dropdown)

        checkbox1Layout = checkboxLayout.findViewById( R.id.option_1_layout )
        checkbox2Layout = checkboxLayout.findViewById( R.id.option_2_layout )
        checkbox3Layout = checkboxLayout.findViewById( R.id.option_3_layout )
        checkbox4Layout = checkboxLayout.findViewById( R.id.option_4_layout )

        dropdown1Layout = dropdownLayout.findViewById( R.id.option_1_layout )
        dropdown2Layout = dropdownLayout.findViewById( R.id.option_2_layout )
        dropdown3Layout = dropdownLayout.findViewById( R.id.option_3_layout )
        dropdown4Layout = dropdownLayout.findViewById( R.id.option_4_layout )

        // respond to changes to the FieldType dropdown
        sharedViewModel.createFieldModel.fieldType.observe( this, androidx.lifecycle.Observer { fieldType ->

            val textLayout = view.findViewById<LinearLayout>(R.id.layout_field_text)
            val numberLayout = view.findViewById<LinearLayout>(R.id.layout_field_number)
            val dateLayout = view.findViewById<LinearLayout>(R.id.layout_field_date)
            val checkboxLayout = view.findViewById<LinearLayout>(R.id.layout_field_checkbox)
            val dropdownLayout = view.findViewById<LinearLayout>(R.id.layout_field_dropdown)

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

                    sharedViewModel.createFieldModel.tempField?.value?.option1?.let { text ->
                        if (text.isNotEmpty())
                        {
                            checkbox1Layout.visibility = View.VISIBLE
                        }
                    }
                    sharedViewModel.createFieldModel.tempField?.value?.option2?.let { text ->
                        if (text.isNotEmpty())
                        {
                            checkbox2Layout.visibility = View.VISIBLE
                        }
                    }
                    sharedViewModel.createFieldModel.tempField?.value?.option3?.let { text ->
                        if (text.isNotEmpty())
                        {
                            checkbox3Layout.visibility = View.VISIBLE
                        }
                    }
                    sharedViewModel.createFieldModel.tempField?.value?.option4?.let { text ->
                        if (text.isNotEmpty())
                        {
                            checkbox4Layout.visibility = View.VISIBLE
                        }
                    }
                }
                FieldType.Dropdown -> {
                    textLayout.visibility = View.GONE
                    numberLayout.visibility = View.GONE
                    dateLayout.visibility = View.GONE
                    checkboxLayout.visibility = View.GONE
                    dropdownLayout.visibility = View.VISIBLE

                    sharedViewModel.createFieldModel.tempField?.value?.option1?.let { text ->
                        if (text.isNotEmpty())
                        {
                            dropdown1Layout.visibility = View.VISIBLE
                        }
                    }
                    sharedViewModel.createFieldModel.tempField?.value?.option2?.let { text ->
                        if (text.isNotEmpty())
                        {
                            dropdown2Layout.visibility = View.VISIBLE
                        }
                    }
                    sharedViewModel.createFieldModel.tempField?.value?.option3?.let { text ->
                        if (text.isNotEmpty())
                        {
                            dropdown3Layout.visibility = View.VISIBLE
                        }
                    }
                    sharedViewModel.createFieldModel.tempField?.value?.option4?.let { text ->
                        if (text.isNotEmpty())
                        {
                            dropdown4Layout.visibility = View.VISIBLE
                        }
                    }
                }
                else -> {}
            }
        })

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

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            // TODO: add validation code

            sharedViewModel.createFieldModel.tempField?.value?.let { field ->
                sharedViewModel.createFieldModel.currentField?.value?.name = field.name
                sharedViewModel.createFieldModel.currentField?.value?.type = field.type
                sharedViewModel.createFieldModel.currentField?.value?.pii = field.pii
                sharedViewModel.createFieldModel.currentField?.value?.required = field.required
                sharedViewModel.createFieldModel.currentField?.value?.integerOnly = field.integerOnly
                sharedViewModel.createFieldModel.currentField?.value?.date = field.date
                sharedViewModel.createFieldModel.currentField?.value?.time = field.time
                sharedViewModel.createFieldModel.currentField?.value?.option1 = field.option1
                sharedViewModel.createFieldModel.currentField?.value?.option2 = field.option2
                sharedViewModel.createFieldModel.currentField?.value?.option3 = field.option3
                sharedViewModel.createFieldModel.currentField?.value?.option4 = field.option4
            }

            sharedViewModel.addField()

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateFieldFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}