/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.configuration

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.models.EnumArea
import java.util.*
import kotlin.collections.ArrayList

class ConfigurationAdapter(var enumAreas: List<EnumArea>?) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
{
    override fun getItemCount() : Int {
        enumAreas?.let {enumAreas ->
            return enumAreas.count()
        }
        return 0
    }

    private lateinit var context: Context
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectEnumArea: ((enumArea: EnumArea) -> Unit)

    fun updateEnumAreas( areas: List<EnumArea>? )
    {
        this.enumAreas = areas
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_enum_area, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val enumArea = enumAreas!!.get(holder.adapterPosition)

        holder.nameTextView.setText( enumArea.name )

        var sampledCount = 0
        var enumerationCount = 0
        var surveyedCount = 0

        for (location in enumArea.locations)
        {
            for (enumItem in location.enumerationItems)
            {
                if (enumItem.enumerationState == EnumerationState.Enumerated || enumItem.enumerationState == EnumerationState.Incomplete)
                {
                    enumerationCount += 1
                }
                if (enumItem.samplingState == SamplingState.Sampled)
                {
                    sampledCount += 1
                }
                if (enumItem.collectionState == CollectionState.Complete)
                {
                    surveyedCount += 1
                }
            }
        }

        holder.enumeratedTextView.text = "$enumerationCount"
        holder.sampledTextView.text = "$sampledCount"
        holder.surveyedTextView.text = "$surveyedCount"

        holder.itemView.setOnClickListener {
            didSelectEnumArea(enumArea)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val enumeratedTextView : TextView = itemView.findViewById(R.id.number_enumerated_text_view)
        val sampledTextView : TextView = itemView.findViewById(R.id.number_sampled_text_view)
        val surveyedTextView : TextView = itemView.findViewById(R.id.number_surveyed_text_view)

    }
}