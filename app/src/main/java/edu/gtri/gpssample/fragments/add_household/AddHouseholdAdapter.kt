package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
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
import edu.gtri.gpssample.dialogs.TimePickerDialog
import java.util.*

class AddHouseholdAdapter( var fields : List<Field>, var fieldDataMap: HashMap<Int, FieldData>) :
    RecyclerView.Adapter<AddHouseholdAdapter.ViewHolder>(),
    DatePickerDialog.DatePickerDialogDelegate,
    TimePickerDialog.TimePickerDialogDelegate
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

    fun dateString(date: Date?): String
    {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val day = calendar[Calendar.DAY_OF_MONTH]
        val month = calendar[Calendar.MONTH] + 1
        val year = calendar[Calendar.YEAR]

        return "${month}/${day}/${year}"
    }

    fun timeString(date: Date?): String
    {
        val calendar = Calendar.getInstance()
        calendar.time = date

        var hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]

        var meridiem = "am"

        if (hour >= 12) {
            meridiem = "pm"
            if (hour > 12) {
                hour -= 12
            }
        }

        return String.format("%d:%02d %s", hour, minute, meridiem)
    }

    fun displayDate( date: Date, field: Field, fieldData: FieldData, editText: EditText )
    {
        if (field.date && !field.time)
        {
            editText.setText( dateString( date ))
        }
        else if (!field.date && field.time)
        {
            editText.setText( timeString( date ))
        }
        else
        {
            editText.setText( date.toString())
        }
    }

    override fun didSelectDate(date: Date, field: Field, fieldData: FieldData, editText: EditText)
    {
        if (field.date && !field.time)
        {
            fieldData.dateValue = date.time
            displayDate( date, field, fieldData, editText )
        }
        else
        {
            TimePickerDialog( context!!, "Select Time", date, field, fieldData, editText,this )
        }
    }

    override fun didSelectTime(date: Date, field: Field, fieldData: FieldData, editText: EditText)
    {
        fieldData.dateValue = date.time
        displayDate( date, field, fieldData, editText )
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
                editText.setText( fieldData.textValue )
                editText.doAfterTextChanged {
                    fieldData.textValue = it.toString()
                }
            }

            FieldType.Number -> {
                frameLayout = holder.frameLayout.findViewById(R.id.number_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)

                if (field.integerOnly)
                {
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                    editText.setText( fieldData.numberValue.toInt().toString())
                }
                else
                {
                    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    editText.setText( String.format( "%.2f", fieldData.numberValue ))
                }

                editText.doAfterTextChanged {
                    if (it.toString().isNotEmpty())
                    {
                        fieldData.numberValue = it.toString().toDouble()
                    }
                }
            }

            FieldType.Date -> {
                frameLayout = holder.frameLayout.findViewById(R.id.date_layout)

                var date = Date()
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)

                if (fieldData.dateValue != 0L)
                {
                    date = Date( fieldData.dateValue )
                    displayDate( date, field, fieldData, editText )
                }

                val editView = frameLayout.findViewById<View>(R.id.edit_view)
                editView.setOnClickListener {

                    if (!field.date && field.time)
                    {
                        TimePickerDialog( context!!, "Select Time", date, field, fieldData, editText,this )
                    }
                    else
                    {
                        DatePickerDialog( context!!, "Select Date", date, field, fieldData, editText,this )
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

                spinner.setSelection( fieldData.dropdownIndex )

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
                {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
                    {
                        fieldData.dropdownIndex = position
                    }

                    override fun onNothingSelected(parent: AdapterView<*>)
                    {
                    }
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
                    checkBox.isChecked = fieldData.checkbox1

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        fieldData.checkbox1 = isChecked
                    }
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox2)
                checkBox.visibility = View.GONE
                if (field.option2.length > 0)
                {
                    checkBox.text = field.option2
                    checkBox.visibility = View.VISIBLE
                    checkBox.isChecked = fieldData.checkbox2

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        fieldData.checkbox2 = isChecked
                    }
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox3)
                checkBox.visibility = View.GONE
                if (field.option3.length > 0)
                {
                    checkBox.text = field.option3
                    checkBox.visibility = View.VISIBLE
                    checkBox.isChecked = fieldData.checkbox3

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        fieldData.checkbox3 = isChecked
                    }
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox4)
                checkBox.visibility = View.GONE
                if (field.option4.length > 0)
                {
                    checkBox.text = field.option4
                    checkBox.visibility = View.VISIBLE
                    checkBox.isChecked = fieldData.checkbox4

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        fieldData.checkbox4 = isChecked
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
