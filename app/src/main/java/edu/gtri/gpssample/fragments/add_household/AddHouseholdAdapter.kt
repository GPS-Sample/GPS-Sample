package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
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
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import edu.gtri.gpssample.fragments.createfield.CreateFieldCheckboxAdapter
import java.util.*

class AddHouseholdAdapter(val config: Config, val enumerationItem: EnumerationItem, val fieldList: List<Field>, val filteredFieldDataList: List<FieldData>) :
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

        fieldData.field?.let { field ->

            if (field.id == null)
            {
                return
            }

            holder.itemView.isSelected = false

            if (field.fieldBlockContainer)
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
            editText.setText( it.toInt().toString())
        }

        val requiredTextView = numberLayout.findViewById<TextView>(R.id.required_text_view)
        requiredTextView.visibility = if (field.required) View.VISIBLE else View.GONE

        editText.doAfterTextChanged {
            if (it.toString().isNotEmpty())
            {
                fieldData.numberValue = it.toString().toDouble()
                layoutBlockAdapter( fieldData, field, blockLayout )
            }
        }

        val titleView: TextView = numberLayout.findViewById<TextView>(R.id.title_text_view)
        titleView.text = field.name

        layoutBlockAdapter( fieldData, field, blockLayout )
    }

    fun layoutBlockAdapter( fieldData: FieldData, field: Field, blockLayout: LinearLayout )
    {
        fieldData.numberValue?.let {
            val numberOfBlocks = it.toInt()

            if (numberOfBlocks > 0)
            {
                field.fieldBlockUUID?.let { fieldBlockUUID ->
                    val blockFields = getBlockFields( fieldBlockUUID )
                    val listOfLists = ArrayList<ArrayList<FieldData>>()

                    for (blockNumber in 1..numberOfBlocks)
                    {
                        val blockFieldDataList = ArrayList<FieldData>()

                        // look for existing block FieldData items
                        for (blockFieldData in enumerationItem.fieldDataList)
                        {
                            blockFieldData.field?.fieldBlockUUID?.let { uuid ->
                                if (uuid == fieldBlockUUID && blockFieldData.blockNumber == blockNumber)
                                {
                                    blockFieldDataList.add(blockFieldData)
                                }
                            }
                        }

                        // check to see if the block size changed

//                        if (blockFieldDataList.isNotEmpty() && blockFieldDataList.size !=numberOfBlocks)
//                        {
//                            for (bfd in blockFieldDataList)
//                            {
//                                enumerationItem.fieldDataList.remove( bfd )
//                            }
//
//                            blockFieldDataList.clear()
//                        }

                        if (blockFieldDataList.isEmpty())
                        {
                            for (blockField in blockFields)
                            {
                                val blockFieldData = FieldData(blockField,blockNumber)

                                enumerationItem.fieldDataList.add(blockFieldData)

                                blockFieldDataList.add(blockFieldData)
                            }
                        }

                        listOfLists.add( blockFieldDataList )
                    }

                    blockAdapter = BlockAdapter( config, listOfLists )

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

                val data = ArrayList<String>()

                for (fieldDataOption in fieldData.fieldDataOptions)
                {
                    data.add( fieldDataOption.name )
                }

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

                val recyclerView = frameLayout.findViewById<RecyclerView>(R.id.recycler_view)

                checkboxOptionAdapter = CheckboxOptionAdapter( fieldData.fieldDataOptions )
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

    fun getBlockFields( uuid: String ) : ArrayList<Field>
    {
        val filteredFieldList = ArrayList<Field>()

        for (field in fieldList)
        {
            if (!field.fieldBlockContainer)
            {
                field.fieldBlockUUID?.let { fieldBlockUUID ->
                    if (uuid == field.fieldBlockUUID)
                    {
                        filteredFieldList.add( field )
                    }
                }
            }
        }

        return filteredFieldList
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
