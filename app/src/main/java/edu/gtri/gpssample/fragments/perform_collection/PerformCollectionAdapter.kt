/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_collection

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.maps.extension.style.expressions.dsl.generated.distance
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.utils.CameraUtils
import java.util.*
import kotlin.collections.ArrayList

class PerformCollectionAdapter(var enumerationItems: List<EnumerationItem>, var locations: List<Location>, val enumAreaName: String) : RecyclerView.Adapter<PerformCollectionAdapter.ViewHolder>()
{
    override fun getItemCount() = enumerationItems.size + locations.size

    var items = ArrayList<Any>()
    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectItem: ((item: Any) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_collection, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateItems( enumerationItems: List<EnumerationItem>, locations: List<Location> )
    {
        val filteredLocations = ArrayList<Location>()

        for (location in locations)
        {
            if (location.isVisible)
            {
                filteredLocations.add( location )
            }
        }

        val filteredEnumerationItems = ArrayList<EnumerationItem>()

        for (enumerationItem in enumerationItems)
        {
            if (enumerationItem.isVisible)
            {
                filteredEnumerationItems.add( enumerationItem )
            }
        }

        this.locations = filteredLocations
        this.enumerationItems = filteredEnumerationItems

        items.clear()
        items.addAll( enumerationItems )
        items.addAll( locations )
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val item = items.get( holder.adapterPosition )

        if (item is Location)
        {
            holder.firstTextView.setText( "${item.description}" )
            holder.secondTextView.setText( item.uuid )

            ImageDAO.instance().getImage( item )?.let { image ->
                CameraUtils.decodeString( image.data )?.let { bitmap ->
                    holder.photoImageView.visibility = View.VISIBLE
                    holder.photoImageView.setImageBitmap( bitmap )
                }
            }

            if (holder.photoImageView.visibility != View.VISIBLE)
            {
                holder.locationImageView.visibility = View.VISIBLE
            }

            if (item.distance > 0)
            {
                holder.thirdTextView.setText("Distance: ${String.format( "%.1f", item.distance )} ${item.distanceUnits}")
            }
        }
        else if (item is EnumerationItem)
        {
            holder.secondTextView.setText( item.uuid )

            if (item.subAddress.isNotEmpty())
            {
                holder.firstTextView.setText( "${enumAreaName} : ${item.subAddress}" )
            }

            holder.thirdTextView.setText("")

            if (item.distance > 0)
            {
                holder.thirdTextView.setText("Distance: ${String.format( "%.1f", item.distance )} ${item.distanceUnits}")
            }

            if (item.collectionState == CollectionState.Complete)
            {
                holder.checkImageView.visibility = View.VISIBLE
            }
            else
            {
                holder.checkImageView.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            didSelectItem( item )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val firstTextView: TextView = itemView.findViewById(R.id.first_text_view);
        val secondTextView: TextView = itemView.findViewById(R.id.second_text_view);
        val thirdTextView: TextView = itemView.findViewById(R.id.third_text_view);
        val checkImageView: ImageView = itemView.findViewById(R.id.check_image_view)
        val locationImageView: ImageView = itemView.findViewById(R.id.location_image_view)
        val photoImageView: ImageView = itemView.findViewById(R.id.photo_image_view)
    }
}