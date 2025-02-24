/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
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
import edu.gtri.gpssample.utils.DateUtils
import java.util.*

class BlockFieldAdapter( val parentFieldIndex: Int, val editMode: Boolean, val config: Config, val fieldDataList: List<FieldData>) :
    RecyclerView.Adapter<BlockFieldAdapter.ViewHolder>(),
    DatePickerDialog.DatePickerDialogDelegate,
    TimePickerDialog.TimePickerDialogDelegate
{
    private var context: Context? = null
    private lateinit var checkboxOptionAdapter: CheckboxOptionAdapter

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

            if (field.uuid.isEmpty())
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

                if (!editMode)
                {
                    editText.inputType = InputType.TYPE_NULL
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

                if (!editMode)
                {
                    editText.inputType = InputType.TYPE_NULL
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

                if (editMode)
                {
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
            }

            FieldType.Dropdown ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.dropdown_layout)

                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

                val data = ArrayList<String>()
                data.add( context!!.getString(R.string.select))

                for (fieldDataOption in fieldData.fieldDataOptions)
                {
                    data.add( fieldDataOption.name )
                }

                val spinner = frameLayout.findViewById<Spinner>(R.id.spinner)
                spinner.adapter = ArrayAdapter<String>(this.context!!, android.R.layout.simple_spinner_dropdown_item, data )

                fieldData.dropdownIndex?.let {
                    spinner.setSelection(it + 1)
                }

                if (editMode)
                {
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
                    {
                        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
                        {
                            if (position == 0)
                            {
                                fieldData.textValue = ""
                                fieldData.dropdownIndex = null
                            }
                            else
                            {
                                fieldData.dropdownIndex = position-1
                                fieldData.textValue = field.fieldOptions[position-1].name
                            }                        }

                        override fun onNothingSelected(parent: AdapterView<*>)
                        {
                        }
                    }
                }
            }

            FieldType.Checkbox ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.checkbox_layout)

                val requiredTextView = frameLayout.findViewById<TextView>(R.id.required_text_view)
                requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

                val recyclerView = frameLayout.findViewById<RecyclerView>(R.id.recycler_view)

                checkboxOptionAdapter = CheckboxOptionAdapter( editMode, fieldData.fieldDataOptions )
                recyclerView.adapter = checkboxOptionAdapter
                recyclerView.itemAnimator = DefaultItemAnimator()
                recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            else -> {}
        }

        frameLayout?.let { layout ->
            layout.visibility = View.VISIBLE
            val titleView = layout.findViewById<TextView>(R.id.title_text_view)
            titleView.text = "${parentFieldIndex}.${field.index}. ${field.name}"
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

    override fun didSelectDate(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        if (field.date && !field.time)
        {
            field.minimum?.let { minimum ->
                if (date.time < minimum)
                {
                    val minDate = DateUtils.dateString( Date( minimum.toLong()), config.dateFormat )
                    Toast.makeText(context!!.applicationContext, "The minimum allowed date is ${minDate}", Toast.LENGTH_LONG).show()
                    return
                }
            }

            field.maximum?.let { maximum ->
                if (date.time > maximum)
                {
                    val maxDate = DateUtils.dateString( Date( maximum.toLong()), config.dateFormat )
                    Toast.makeText(context!!.applicationContext, "The maximum allowed date is ${maxDate}", Toast.LENGTH_LONG).show()
                    return
                }
            }

            fieldData?.let{ fieldData ->
                fieldData.dateValue = date.time
                editText?.let { editText ->
                    displayDate( date, field, fieldData, editText )
                }
            }
        }
        else
        {
            TimePickerDialog( context!!, context?.getString(R.string.select_time) ?: "Select Time", date, field, fieldData, editText,this )
        }
    }

    override fun didSelectTime(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    {
        fieldData?.let { fieldData ->
            fieldData.dateValue = date.time
            editText?.let { editText ->
                displayDate( date, field, fieldData, editText )
            }
        }
    }
}