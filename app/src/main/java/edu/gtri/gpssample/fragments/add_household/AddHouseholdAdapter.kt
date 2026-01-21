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
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import edu.gtri.gpssample.fragments.createfield.CreateFieldCheckboxAdapter
import edu.gtri.gpssample.utils.DateUtils
import java.util.*

class AddHouseholdAdapter( val editMode: Boolean, val config: Config, val enumerationItem: EnumerationItem, val fields: List<Field>, val filteredFieldDataList: List<FieldData>) :
    RecyclerView.Adapter<AddHouseholdAdapter.ViewHolder>(),
    DatePickerDialog.DatePickerDialogDelegate,
    TimePickerDialog.TimePickerDialogDelegate
{
    private var context: Context? = null

    private lateinit var blockAdapter: BlockAdapter
    private lateinit var checkboxOptionAdapter: CheckboxOptionAdapter

    override fun getItemCount() = filteredFieldDataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_household, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val frameLayout: FrameLayout = itemView as FrameLayout
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        val fieldData = filteredFieldDataList.get(holder.adapterPosition)

        DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { field ->

            holder.itemView.isSelected = false

            if (field.fields != null)
            {
                layoutBlockField( holder, field, fieldData )
            }
            else
            {
                layoutNonBlockField( holder, field, fieldData )
            }
        }
    }

    fun layoutBlockField( holder: ViewHolder, field: Field, fieldData: FieldData )
    {
        val blockLayout: LinearLayout = holder.frameLayout.findViewById(R.id.block_layout)
        val numberLayout: FrameLayout = blockLayout.findViewById(R.id.block_number_layout)
        val editText = numberLayout.findViewById<EditText>(R.id.edit_text)

        blockLayout.visibility = View.VISIBLE
        numberLayout.visibility = View.VISIBLE

        editText.inputType = InputType.TYPE_CLASS_NUMBER
        fieldData.numberValue?.let {
            editText.setText( it.toLong().toString())
        }

        val requiredTextView = numberLayout.findViewById<TextView>(R.id.required_text_view)
        requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

        editText.doAfterTextChanged {
            if (it.toString().isNotEmpty())
            {
                val newNumberValue = it.toString().toDouble()

                fieldData.numberValue?.let { oldNumberValue ->
                    if (newNumberValue < oldNumberValue)
                    {
                        val removeList = ArrayList<FieldData>()

                        for (blockFieldData in enumerationItem.fieldDataList)
                        {
                            DAO.fieldDAO.getField( blockFieldData.fieldUuid )?.let { blockField ->
                                blockField.parentUUID?.let { uuid ->
                                    if (uuid == field.uuid)
                                    {
                                        blockFieldData.blockNumber?.let { blockNumber ->
                                            if (blockNumber > newNumberValue)
                                            {
                                                removeList.add( blockFieldData )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for (removeField in removeList)
                        {
                            enumerationItem.fieldDataList.remove( removeField )
                        }
                    }
                }

                fieldData.numberValue = newNumberValue
                layoutBlockAdapter( fieldData, field, blockLayout )
            }
        }

        val titleView: TextView = numberLayout.findViewById<TextView>(R.id.title_text_view)
        titleView.text = "${field.index}. ${field.name}"

        layoutBlockAdapter( fieldData, field, blockLayout )
    }

    fun layoutBlockAdapter( fieldData: FieldData, field: Field, blockLayout: LinearLayout )
    {
        fieldData.numberValue?.let {
            val numberOfBlocks = it.toInt()

            if (numberOfBlocks > 0)
            {
                field.fields?.let { blockFields ->
                    val listOfLists = ArrayList<ArrayList<FieldData>>()

                    for (blockNumber in 1..numberOfBlocks)
                    {
                        var blockFieldDataList = ArrayList<FieldData>()

                        // look for existing block FieldData items
                        for (blockFieldData in enumerationItem.fieldDataList)
                        {
                            DAO.fieldDAO.getField( blockFieldData.fieldUuid )?.let { blockField ->
                                blockField.parentUUID?.let { uuid ->
                                    if (uuid == field.uuid && blockFieldData.blockNumber == blockNumber)
                                    {
                                        blockFieldDataList.add(blockFieldData)
                                    }
                                }
                            }
                        }

                        if (blockFieldDataList.isNotEmpty())
                        {
                            blockFieldDataList = ArrayList<FieldData>(blockFieldDataList.sortedBy {it.creationDate})
                        }
                        else
                        {
                            var count = 0
                            val creationDate = Date().time

                            for (blockField in blockFields)
                            {
                                val blockFieldData = FieldData(creationDate + count, blockField.uuid, blockNumber )

                                count += 1

                                enumerationItem.fieldDataList.add(blockFieldData)

                                blockFieldDataList.add(blockFieldData)

                                if (blockField.type == FieldType.Checkbox || blockField.type == FieldType.Dropdown)
                                {
                                    // create a fiedDataOption for each fieldOption
                                    for (fieldOption in blockField.fieldOptions)
                                    {
                                        val fieldDataOption = FieldDataOption(fieldOption.name, false)
                                        blockFieldData.fieldDataOptions.add(fieldDataOption)
                                    }
                                }
                            }
                        }

                        listOfLists.add( blockFieldDataList )
                    }

                    blockAdapter = BlockAdapter( field.index, editMode, config, listOfLists )

                    val recyclerView: RecyclerView = blockLayout.findViewById(R.id.recycler_view)
                    recyclerView.adapter = blockAdapter
                    recyclerView.itemAnimator = DefaultItemAnimator()
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );
                }
            }
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
                        editText.setText( it.toLong().toString())
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

                if (!editMode)
                {
                    editText.inputType = InputType.TYPE_NULL
                }

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
                    spinner.setSelection( it+1 )
                }

                if (!editMode)
                {
                    spinner.isEnabled = false
                }
                else
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
                            }
                        }

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
            titleView.text = "${field.index}. ${field.name}"
        }
    }

    fun displayDate( date: Date, field: Field, fieldData: FieldData, editText: EditText )
    {
        if (field.date && !field.time)
        {
            editText.setText( DateUtils.dateString( date, config.dateFormat ))
        }
        else if (field.time && !field.date)
        {
            editText.setText( DateUtils.timeString( date, config.timeFormat ))
        }
        else
        {
            editText.setText( DateUtils.dateTimeString( date, config.dateFormat, config.timeFormat ))
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

            fieldData?.let { fieldData ->
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
