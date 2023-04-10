package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.database.models.Field
import kotlinx.coroutines.NonDisposableHandle.parent

class AddHouseholdAdapter(var fields: List<Field>?) : RecyclerView.Adapter<AddHouseholdAdapter.ViewHolder>()
{
    override fun getItemCount() = fields!!.size

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

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        var frameLayout: FrameLayout? = null
        val field = fields!!.get(holder.adapterPosition)

        holder.itemView.isSelected = false

        when (field.type) {
            FieldType.Text -> frameLayout = holder.frameLayout.findViewById(R.id.text_layout)
            FieldType.Number -> frameLayout = holder.frameLayout.findViewById(R.id.number_layout)
            FieldType.Date -> frameLayout = holder.frameLayout.findViewById(R.id.date_layout)
            FieldType.Checkbox ->
            {
                frameLayout = holder.frameLayout.findViewById(R.id.checkbox_layout)

                var checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox1)
                checkBox.visibility = View.GONE
                if (field.option1.length > 0)
                {
                    checkBox.text = field.option1
                    checkBox.visibility = View.VISIBLE
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox2)
                checkBox.visibility = View.GONE
                if (field.option2.length > 0)
                {
                    checkBox.text = field.option2
                    checkBox.visibility = View.VISIBLE
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox3)
                checkBox.visibility = View.GONE
                if (field.option3.length > 0)
                {
                    checkBox.text = field.option3
                    checkBox.visibility = View.VISIBLE
                }

                checkBox = frameLayout.findViewById<CheckBox>(R.id.checkbox4)
                checkBox.visibility = View.GONE
                if (field.option4.length > 0)
                {
                    checkBox.text = field.option4
                    checkBox.visibility = View.VISIBLE
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
