/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.perform_multi_collection

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location

class PerformMultiCollectionAdapter( var enumerationItems: List<EnumerationItem>, val enumAreaName: String ) : RecyclerView.Adapter<PerformMultiCollectionAdapter.ViewHolder>()
{
    override fun getItemCount() = enumerationItems.size

    private lateinit var context: Context
    lateinit var didSelectEnumerationItem: ((enumerationItem: EnumerationItem) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false

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

        holder.dateTextView.setText( enumerationItem.uuid )

        if (enumerationItem.subAddress.isNotEmpty())
        {
            holder.nameTextView.setText( "${enumAreaName} : ${enumerationItem.subAddress}" )
        }

        if (enumerationItem.collectionState == CollectionState.Complete)
        {
            holder.checkImageView.visibility = View.VISIBLE
        }
        else
        {
            holder.checkImageView.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            didSelectEnumerationItem(enumerationItem)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        val checkImageView: ImageView = itemView.findViewById(R.id.check_image_view)
    }
}