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
import edu.gtri.gpssample.database.EnumAreaDAO.EnumAreaSummary

class ConfigurationAdapter(var enumAreaSummaries: List<EnumAreaSummary>?) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
{
    override fun getItemCount() : Int
    {
        enumAreaSummaries?.let {
            return it.count()
        }

        return 0
    }

    private lateinit var context: Context
    lateinit var didSelectEnumArea: ((uuid: String) -> Unit)

    fun updateEnumAreas( summaries: List<EnumAreaSummary>? )
    {
        this.enumAreaSummaries = summaries
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

        val enumAreaSummary = enumAreaSummaries!!.get(holder.adapterPosition)

        holder.nameTextView.setText( enumAreaSummary.name )
        holder.enumeratedTextView.text = enumAreaSummary.enumeratedCount.toString()
        holder.sampledTextView.text = enumAreaSummary.sampledCount.toString()
        holder.surveyedTextView.text = enumAreaSummary.surveyedCount.toString()

        holder.itemView.setOnClickListener {
            didSelectEnumArea(enumAreaSummary.uuid)
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