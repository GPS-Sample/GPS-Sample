package edu.gtri.gpssample.fragments.perform_enumeration


import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.database.models.FieldData
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationAdapter(var enumDataList: List<EnumData>?) : RecyclerView.Adapter<PerformEnumerationAdapter.ViewHolder>()
{
    override fun getItemCount() = enumDataList!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectEnumData: ((enumData: EnumData) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateEnumDataList( enumDataList: List<EnumData> )
    {
        this.enumDataList = enumDataList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val enumData = enumDataList!!.get(holder.adapterPosition)

        holder.nameTextView.setText( enumData.id.toString())
        holder.dateTextView.setText( Date(enumData.creationDate).toString())

        holder.itemView.setOnClickListener {
            didSelectEnumData(enumData)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view);
    }
}