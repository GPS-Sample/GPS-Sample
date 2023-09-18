package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import java.util.*

class BlockFieldAdapter(val config: Config, val fieldDataList: List<FieldData>) :
    RecyclerView.Adapter<BlockFieldAdapter.ViewHolder>(),
    DatePickerDialog.DatePickerDialogDelegate,
    TimePickerDialog.TimePickerDialogDelegate
{
    private var context: Context? = null

    override fun getItemCount() = fieldDataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_block_field, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val frameLayout: FrameLayout = itemView as FrameLayout
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        val fieldData = fieldDataList.get(holder.adapterPosition)

        fieldData.field?.let { field ->

            if (field.id == null)
            {
                return
            }

            holder.itemView.isSelected = false

            layoutNonBlockField( holder, field, fieldData )
        }
    }

    fun layoutNonBlockField( holder: ViewHolder, field: Field, fieldData: FieldData )
    {
        var frameLayout: FrameLayout? = null

        when (field.type) {
            FieldType.Text -> {
                frameLayout = holder.frameLayout.findViewById(R.id.text_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                editText.setText( fieldData.textValue )
                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE
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
                    fieldData.numberValue?.let {
                        editText.setText( it.toInt().toString())
                    }
                }
                else
                {
                    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    fieldData.numberValue?.let {
                        editText.setText( String.format( "%.2f", it ))
                    }
                }

                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

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

                fieldData.dateValue?.let {
                    date = Date( it )
                    displayDate( date, field, fieldData, editText )
                }

                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

                val editView = frameLayout.findViewById<View>(R.id.edit_view)

                editView.setOnClickListener {

                    fieldData.dateValue?.let {
                        date = Date( it )
                    }

                    if (!field.date && field.time)
                    {
                        TimePickerDialog( context!!, context?.getString(R.string.select_time) ?: "Select Time", date, field, fieldData, editText,this )
                    }
                    else
                    {
                        DatePickerDialog( context!!, context?.getString(R.string.select_date) ?: "Select Date", date, field, fieldData, editText,this )
                    }
                }
            }

            FieldType.Dropdown ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.dropdown_layout)

                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

                var data = ArrayList<String>()

                if (field.option1.length > 0) data.add(field.option1)
                if (field.option2.length > 0) data.add(field.option2)
                if (field.option3.length > 0) data.add(field.option3)
                if (field.option4.length > 0) data.add(field.option4)

                val spinner = frameLayout.findViewById<Spinner>(R.id.spinner)
                spinner.adapter = ArrayAdapter<String>(this.context!!, android.R.layout.simple_spinner_dropdown_item, data )

                fieldData.dropdownIndex?.let {
                    spinner.setSelection( it )
                } ?: run {
                    spinner.setSelection( data.size-1 )
                }

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

                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

//                var checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox1)
//                checkBox.visibility = View.GONE
//                if (field.option1.length > 0)
//                {
//                    checkBox.text = field.option1
//                    checkBox.visibility = View.VISIBLE
//                    checkBox.isChecked = fieldData.checkbox1
//
//                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
//                        fieldData.checkbox1 = isChecked
//                    }
//                }
//
//                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox2)
//                checkBox.visibility = View.GONE
//                if (field.option2.length > 0)
//                {
//                    checkBox.text = field.option2
//                    checkBox.visibility = View.VISIBLE
//                    checkBox.isChecked = fieldData.checkbox2
//
//                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
//                        fieldData.checkbox2 = isChecked
//                    }
//                }
//
//                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox3)
//                checkBox.visibility = View.GONE
//                if (field.option3.length > 0)
//                {
//                    checkBox.text = field.option3
//                    checkBox.visibility = View.VISIBLE
//                    checkBox.isChecked = fieldData.checkbox3
//
//                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
//                        fieldData.checkbox3 = isChecked
//                    }
//                }
//
//                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox4)
//                checkBox.visibility = View.GONE
//                if (field.option4.length > 0)
//                {
//                    checkBox.text = field.option4
//                    checkBox.visibility = View.VISIBLE
//                    checkBox.isChecked = fieldData.checkbox4
//
//                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
//                        fieldData.checkbox4 = isChecked
//                    }
//                }
            }
            else -> {}
        }

        frameLayout?.let { layout ->
            layout.visibility = View.VISIBLE
            val titleView = layout.findViewById<TextView>(R.id.title_text_view)
            titleView.text = field.name
        }
    }

    fun dateString(date: Date?): String
    {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val day = calendar[Calendar.DAY_OF_MONTH]
        val month = calendar[Calendar.MONTH] + 1
        val year = calendar[Calendar.YEAR]

        when (config.dateFormat)
        {
            DateFormat.DayMonthYear -> return "${day}/${month}/${year}"
            DateFormat.MonthDayYear -> return "${month}/${day}/${year}"
            DateFormat.YearMonthDay -> return "${year}/${month}/${day}"
            else -> return "${day}/${month}/${year}"
        }
    }

    fun timeString(date: Date?): String
    {
        val calendar = Calendar.getInstance()
        calendar.time = date

        var hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]

        var meridiem = "am"

        if (config.timeFormat == TimeFormat.twelveHour)
        {
            if (hour >= 12) {
                meridiem = "pm"
                if (hour > 12) {
                    hour -= 12
                }
            }
        }

        when (config.timeFormat)
        {
            TimeFormat.twelveHour -> return String.format("%d:%02d %s", hour, minute, meridiem)
            TimeFormat.twentyFourHour -> return String.format("%d:%02d", hour, minute)
            else -> return String.format("%d:%02d %s", hour, minute, meridiem)
        }
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
            TimePickerDialog( context!!, context?.getString(R.string.select_time) ?: "Select Time", date, field, fieldData, editText,this )
        }
    }

    override fun didSelectTime(date: Date, field: Field, fieldData: FieldData, editText: EditText)
    {
        fieldData.dateValue = date.time
        displayDate( date, field, fieldData, editText )
    }
}