/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_sample

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
import edu.gtri.gpssample.constants.ReviewStatus
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.utils.CameraUtils
import java.util.*
import kotlin.collections.ArrayList

class CreateSampleAdapter(var locations: List<Location>, val enumAreaName: String) : RecyclerView.Adapter<CreateSampleAdapter.ViewHolder>()
{
    override fun getItemCount() = locations.size

    private lateinit var mContext: Context
    lateinit var didSelectLocation: ((location: Location) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_collection, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    fun updateLocations( locations: List<Location> )
    {
        val filteredLocations = ArrayList<Location>()

        for (location in locations)
        {
            if (location.isVisible)
            {
                filteredLocations.add( location )
            }
        }

        this.locations = filteredLocations

        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val location = locations.get(holder.adapterPosition)

        if (location.enumerationItems.isNotEmpty())
        {
            holder.firstTextView.setText( "${enumAreaName} : ${location.enumerationItems.last().subAddress}" )
        }
        else
        {
            holder.firstTextView.setText( "${enumAreaName} :" )
        }

        holder.secondTextView.setText( location.uuid )

        if (location.distance > 0)
        {
            holder.thirdTextView.setText("${mContext.resources.getString(R.string.distance)}: ${String.format( "%.1f", location.distance )} ${location.distanceUnits}")
        }

        if (location.isLandmark)
        {
            holder.firstTextView.setText( "${location.description}" )

            if (location.imageUuid.isNotEmpty())
            {
                ImageDAO.instance().getImage( location )?.let { image ->
                    CameraUtils.decodeString( image.data )?.let { bitmap ->
                        holder.photoImageView.visibility = View.VISIBLE
                        holder.photoImageView.setImageBitmap( bitmap )
                    }
                }
            }

            if (holder.photoImageView.visibility != View.VISIBLE)
            {
                holder.locationImageView.visibility = View.VISIBLE
            }
        }
        else if (location.enumerationItems.isNotEmpty())
        {
            holder.checkImageView.visibility = View.GONE

            if (location.enumerationItems.size == 1)
            {
                if (location.enumerationItems[0].enumerationState == EnumerationState.Enumerated && location.enumerationItems[0].reviewStatus != ReviewStatus.Exclude)
                {
                    holder.checkImageView.visibility = View.VISIBLE
                }
            }
            else
            {
                for (enumerationItem in location.enumerationItems)
                {
                    if (enumerationItem.enumerationState == EnumerationState.Enumerated && enumerationItem.reviewStatus != ReviewStatus.Exclude)
                    {
                        holder.checkImageView.visibility = View.VISIBLE
                        break
                    }
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
        val locationImageView: ImageView = itemView.findViewById(R.id.location_image_view)
        val photoImageView: ImageView = itemView.findViewById(R.id.photo_image_view)
    }
}