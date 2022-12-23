package edu.gtri.gpssample.fragments.CreateStudy

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.models.FieldModel

class CreateStudyAdapter(var fields: List<FieldModel>?) : RecyclerView.Adapter<CreateStudyAdapter.ViewHolder>()
{
    override fun getItemCount() = fields!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var selectedItemCallback: ((fieldModel: FieldModel, shouldDismissKeyboard: Boolean) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateFields( studies: List<FieldModel> )
    {
        this.fields = fields
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val fieldModel = fields!!.get(holder.adapterPosition)

        holder.nameTextView.setText( fieldModel.name )

        holder.itemView.setOnClickListener {
            selectedItemCallback.invoke(fieldModel, true)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
    }
}