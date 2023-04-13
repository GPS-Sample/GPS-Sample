package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.cardview.widget.CardView
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.adapters.AdapterViewBindingAdapter.OnItemSelected
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import kotlinx.coroutines.NonDisposableHandle.parent

class AddHouseholdAdapter(var fields : List<Field>, var enumData: EnumData) : RecyclerView.Adapter<AddHouseholdAdapter.ViewHolder>()
{
    override fun getItemCount() = fields.size

    private var enum_data_id = 0
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        enumData.id?.let {
            enum_data_id = it
        }

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_household, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val frameLayout: FrameLayout = itemView as FrameLayout
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        var frameLayout: FrameLayout? = null
        val field = fields.get(holder.adapterPosition)
        var field_id : Int = 0
        field.id?.let {
            field_id = it
        }

        holder.itemView.isSelected = false

        when (field.type) {
            FieldType.Text -> {
                frameLayout = holder.frameLayout.findViewById(R.id.text_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                var fieldData = DAO.fieldDataDAO.getFieldData(field_id, enum_data_id)
                editText.setText( fieldData.response1 )
                editText.doAfterTextChanged {
                    fieldData.response1 = it.toString()
                    DAO.fieldDataDAO.updateFieldData( fieldData )
                }
            }

            FieldType.Number -> {
                frameLayout = holder.frameLayout.findViewById(R.id.number_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                var fieldData = DAO.fieldDataDAO.getFieldData(field_id, enum_data_id)
                editText.setText( fieldData.response1 )
                editText.doAfterTextChanged {
                    fieldData.response1 = it.toString()
                    DAO.fieldDataDAO.updateFieldData( fieldData )
                }
            }

            FieldType.Date -> {
                frameLayout = holder.frameLayout.findViewById(R.id.date_layout)
                val editText = frameLayout.findViewById<EditText>(R.id.edit_text)
                var fieldData = DAO.fieldDataDAO.getFieldData(field_id, enum_data_id)
                editText.setText( fieldData.response1 )
                editText.doAfterTextChanged {
                    fieldData.response1 = it.toString()
                    DAO.fieldDataDAO.updateFieldData( fieldData )
                }
            }

            FieldType.Checkbox ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.checkbox_layout)
                var fieldData = DAO.fieldDataDAO.getFieldData(field_id, enum_data_id)

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

                        DAO.fieldDataDAO.updateFieldData(fieldData)
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

                        DAO.fieldDataDAO.updateFieldData(fieldData)
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

                        DAO.fieldDataDAO.updateFieldData(fieldData)
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

                        DAO.fieldDataDAO.updateFieldData(fieldData)
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

                var fieldData = DAO.fieldDataDAO.getFieldData(field_id, enum_data_id)

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

                        DAO.fieldDataDAO.updateFieldData( fieldData )
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
