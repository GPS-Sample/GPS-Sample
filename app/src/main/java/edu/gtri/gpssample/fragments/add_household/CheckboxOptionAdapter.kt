package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.FieldDataOption
import edu.gtri.gpssample.database.models.FieldOption

class CheckboxOptionAdapter( var fieldDataOptions: List<FieldDataOption>) : RecyclerView.Adapter<CheckboxOptionAdapter.ViewHolder>()
{
    override fun getItemCount() = fieldDataOptions.size

    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_checkbox_option, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    fun updateFieldDataOptions( fieldDataOptions: List<FieldDataOption> )
    {
        this.fieldDataOptions = fieldDataOptions
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox);
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val fieldDataOption = fieldDataOptions.get(position)

        holder.checkbox.text = fieldDataOption.name
        holder.checkbox.isChecked = fieldDataOption.value

        holder.checkbox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                fieldDataOption.value = isChecked
            }
        })
    }
}