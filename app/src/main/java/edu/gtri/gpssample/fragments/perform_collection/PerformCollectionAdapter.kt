package edu.gtri.gpssample.fragments.perform_collection

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import java.util.*
import kotlin.collections.ArrayList

class PerformCollectionAdapter(var enumerationItems: List<EnumerationItem>) : RecyclerView.Adapter<PerformCollectionAdapter.ViewHolder>()
{
    override fun getItemCount() = enumerationItems.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectEnumerationItem: ((enumerationItem: EnumerationItem) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateEnumerationItems( enumerationItems: List<EnumerationItem> )
    {
        this.enumerationItems = enumerationItems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val enumerationItem = enumerationItems.get(holder.adapterPosition)

        holder.nameTextView.setText( enumerationItem.uuid )
        holder.dateTextView.setText( enumerationItem.subAddress )

        // temp debug data, delete this!
        holder.dateTextView.setText( enumerationItem.locationId.toString())

        holder.itemView.setOnClickListener {
            didSelectEnumerationItem( enumerationItem )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view);
    }
}