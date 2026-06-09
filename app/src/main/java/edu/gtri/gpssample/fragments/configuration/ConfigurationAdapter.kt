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
import edu.gtri.gpssample.database.ConfigDAO.EnumAreaSummary
import edu.gtri.gpssample.database.models.EnumArea
import java.util.*
import kotlin.collections.ArrayList

class ConfigurationAdapter(var enumAreas: List<EnumArea>?, var enumAreaSummaries: List<EnumAreaSummary>) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
{
    override fun getItemCount() : Int {
        enumAreas?.let {enumAreas ->
            return enumAreas.count()
        }
        return 0
    }

    private lateinit var context: Context
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

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val enumArea = enumAreas!!.get(holder.adapterPosition)

        holder.nameTextView.setText( enumArea.name )

        enumAreaSummaries.find { it.enumAreaUuid == enumArea.uuid } ?.let {
            holder.enumeratedTextView.text = "${it.enumeratedCount}"
            holder.sampledTextView.text = "${it.sampledCount}"
            holder.surveyedTextView.text = "${it.surveyedCount}"
        }

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