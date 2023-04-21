package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.dialogs.DatePickerDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddHouseholdAdapter( var fields : List<Field>, var fieldDataMap: HashMap<Int, FieldData>) : RecyclerView.Adapter<AddHouseholdAdapter.ViewHolder>(), DatePickerDialog.DatePickerDialogDelegate
{
    override fun getItemCount() = fields.size

    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_household, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val frameLayout: FrameLayout = itemView as FrameLayout
    }

    override fun didSelectDate(date: Date )
    {
        Log.d( "xxx", date.toString() )
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        var frameLayout: FrameLayout? = null
        val field = fields.get(holder.adapterPosition)

        if (field.id == null)
        {
            return
        }

        val fieldData = fieldDataMap[field.id!!]

        if (fieldData == null)
        {
            return
        }

        holder.itemView.isSelected = false

        when (field.type) {
            FieldType.Text -> {
                frameLayout = holder.frameLayout.findViewById(R.id.text_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                editText.setText( fieldData.response1 )
                editText.doAfterTextChanged {
                    fieldData.response1 = it.toString()
                }
            }

            FieldType.Number -> {
                frameLayout = holder.frameLayout.findViewById(R.id.number_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                editText.setText( fieldData.response1 )
                editText.doAfterTextChanged {
                    fieldData.response1 = it.toString()
                }
            }

            FieldType.Date -> {
                frameLayout = holder.frameLayout.findViewById(R.id.date_layout)

                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                editText.setText( fieldData.response1 )

                val editView = frameLayout.findViewById<View>(R.id.edit_view)
                editView.setOnClickListener {

                    DatePickerDialog( context!!, "Select a Date", Date(), this)
                }
            }

            FieldType.Checkbox ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.checkbox_layout)
                var checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox1)
                checkBox.visibility = View.GONE
                if (field.option1.length > 0)
                {
                    checkBox.text = field.option1
                    checkBox.visibility = View.VISIBLE

                    if (fieldData.response1 == field.option1)
                    {
                        checkBox.isChecked = true
                    }

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            fieldData.response1 = field.option1
                        } else {
                            fieldData.response1 = ""
                        }
                    }
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox2)
                checkBox.visibility = View.GONE
                if (field.option2.length > 0)
                {
                    checkBox.text = field.option2
                    checkBox.visibility = View.VISIBLE

                    if (fieldData.response2 == field.option2)
                    {
                        checkBox.isChecked = true
                    }

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            fieldData.response2 = field.option2
                        } else {
                            fieldData.response2 = ""
                        }
                    }
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox3)
                checkBox.visibility = View.GONE
                if (field.option3.length > 0)
                {
                    checkBox.text = field.option3
                    checkBox.visibility = View.VISIBLE

                    if (fieldData.response3 == field.option3)
                    {
                        checkBox.isChecked = true
                    }

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            fieldData.response3 = field.option3
                        } else {
                            fieldData.response3 = ""
                        }
                    }
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox4)
                checkBox.visibility = View.GONE
                if (field.option4.length > 0)
                {
                    checkBox.text = field.option4
                    checkBox.visibility = View.VISIBLE

                    if (fieldData.response4 == field.option4)
                    {
                        checkBox.isChecked = true
                    }

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            fieldData.response4 = field.option4
                        } else {
                            fieldData.response4 = ""
                        }
                    }
                }
            }

            FieldType.Dropdown ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.dropdown_layout)

                var data = ArrayList<String>()

                if (field.option1.length > 0) data.add(field.option1)
                if (field.option2.length > 0) data.add(field.option2)
                if (field.option3.length > 0) data.add(field.option3)
                if (field.option4.length > 0) data.add(field.option4)

                val spinner = frameLayout.findViewById<Spinner>(R.id.spinner)
                spinner.adapter = ArrayAdapter<String>(this.context!!, android.R.layout.simple_spinner_dropdown_item, data )

                if (fieldData.response1 == field.option1)
                    spinner.setSelection(0)
                else if (fieldData.response1 == field.option2)
                    spinner.setSelection(1)
                else if (fieldData.response1 == field.option3)
                    spinner.setSelection(2)
                else if (fieldData.response1 == field.option4)
                    spinner.setSelection(3)

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
                {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
                    {
                        when( position )
                        {
                            0 -> fieldData.response1 = field.option1
                            1 -> fieldData.response1 = field.option2
                            2 -> fieldData.response1 = field.option3
                            3 -> fieldData.response1 = field.option4
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>)
                    {
                    }
                }
            }
            else -> {}
        }

        frameLayout?.let { layout ->
            layout.visibility = View.VISIBLE
            val titleView = layout.findViewById<TextView>(R.id.title_text_view)
            titleView.text = field.name
        }
    }
}
