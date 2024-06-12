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
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import java.util.*
import kotlin.collections.ArrayList

class PerformEnumerationAdapter(var locations: List<Location>, val enumAreaName: String) : RecyclerView.Adapter<PerformEnumerationAdapter.ViewHolder>()
{
    override fun getItemCount() = locations.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectLocation: ((location: Location) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_collection, parent, false))

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

        if (location.enumerationItems.isNotEmpty() && location.enumerationItems.last().subAddress.isNotEmpty())
        {
            holder.firstTextView.setText( "${enumAreaName} : ${location.enumerationItems.last().subAddress}" )
        }

        holder.secondTextView.setText( location.uuid )

        if (location.distance > 0)
        {
            holder.thirdTextView.setText("Distance: ${String.format( "%.1f", location.distance )} ${location.distanceUnits}")
        }

        if (location.isLandmark)
        {
            holder.firstTextView.setText( "${location.description}" )
        }
        else if (location.enumerationItems.size > 0)
        {
            if (!location.isMultiFamily)
            {
                if (location.enumerationItems[0].enumerationState == EnumerationState.Enumerated)
                {
                    holder.checkImageView.visibility = View.VISIBLE
                }
                else
                {
                    holder.checkImageView.visibility = View.GONE
                }
            }
            else
            {
                var isComplete = true

                for (enumerationItem in location.enumerationItems)
                {
                    isComplete = isComplete && (enumerationItem.enumerationState == EnumerationState.Enumerated)
                }

                if (isComplete)
                {
                    holder.checkImageView.visibility = View.VISIBLE
                }
                else
                {
                    holder.checkImageView.visibility = View.GONE
                }
            }
        }

        holder.itemView.setOnClickListener {
            didSelectLocation(location)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val firstTextView: TextView = itemView.findViewById(R.id.first_text_view);
        val secondTextView: TextView = itemView.findViewById(R.id.second_text_view);
        val thirdTextView: TextView = itemView.findViewById(R.id.third_text_view);
        val checkImageView: ImageView = itemView.findViewById(R.id.check_image_view)
    }
}