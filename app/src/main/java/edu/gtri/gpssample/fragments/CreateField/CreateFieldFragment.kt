package edu.gtri.gpssample.fragments.CreateField

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentCreateFieldBinding
import edu.gtri.gpssample.models.FieldModel

class CreateFieldFragment : Fragment()
{
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
        setHasOptionsMenu( true )

        _binding = FragmentCreateFieldBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        textLayout = view.findViewById<LinearLayout>(R.id.layout_field_text)
        numberLayout = view.findViewById<LinearLayout>(R.id.layout_field_number)
        dateLayout = view.findViewById<LinearLayout>(R.id.layout_field_date)
        checkboxLayout = view.findViewById<LinearLayout>(R.id.layout_field_checkbox)
        dropdownLayout = view.findViewById<LinearLayout>(R.id.layout_field_dropdown)

        checkbox1EditText = checkboxLayout.findViewById( R.id.option_1_edit_text )
        checkbox2EditText = checkboxLayout.findViewById( R.id.option_2_edit_text )
        checkbox3EditText = checkboxLayout.findViewById( R.id.option_3_edit_text )
        checkbox4EditText = checkboxLayout.findViewById( R.id.option_4_edit_text )

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

            if (checkbox1EditText.visibility == View.GONE)
            {
                checkbox1EditText.visibility = View.VISIBLE
            }
            else if (checkbox2EditText.visibility == View.GONE)
            {
                checkbox2EditText.visibility = View.VISIBLE
            }
            else if (checkbox3EditText.visibility == View.GONE)
            {
                checkbox3EditText.visibility = View.VISIBLE
            }
            else if (checkbox4EditText.visibility == View.GONE)
            {
                checkbox4EditText.visibility = View.VISIBLE
            }
        }

        val dropdownAddAnotherButton = dropdownLayout.findViewById<Button>(R.id.add_another_button)

        dropdownAddAnotherButton.setOnClickListener {

            if (dropdown1EditText.visibility == View.GONE)
            {
                dropdown1EditText.visibility = View.VISIBLE
            }
            else if (dropdown2EditText.visibility == View.GONE)
            {
                dropdown2EditText.visibility = View.VISIBLE
            }
            else if (dropdown3EditText.visibility == View.GONE)
            {
                dropdown3EditText.visibility = View.VISIBLE
            }
            else if (dropdown4EditText.visibility == View.GONE)
            {
                dropdown4EditText.visibility = View.VISIBLE
            }
        }

        binding.cancelButton.setOnClickListener {

            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            if (binding.fieldNameEditText.text.toString().length > 0)
            {
                val fieldModel = FieldModel()
                fieldModel.name = binding.fieldNameEditText.text.toString()
                (activity!!.application as MainApplication).fields.add( fieldModel )

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