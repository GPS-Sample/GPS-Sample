package edu.gtri.gpssample.fragments.perform_enumeration

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationAdapter(var locations: List<Location>) : RecyclerView.Adapter<PerformEnumerationAdapter.ViewHolder>()
{
    override fun getItemCount() = locations.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectLocation: ((location: Location) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateLocations( locations: List<Location> )
    {
        this.locations = locations
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val location = locations.get(holder.adapterPosition)

        holder.nameTextView.setText( location.id.toString())
        holder.dateTextView.setText( Date(location.creationDate).toString())

        holder.itemView.setOnClickListener {
            didSelectLocation(location)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view);
    }
}