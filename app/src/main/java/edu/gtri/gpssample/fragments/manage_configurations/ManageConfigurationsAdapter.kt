/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.manage_configurations

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.extensions.toLocalizedDateTimeString
import java.util.*

class ManageConfigurationsAdapter(var configurations: List<Config>?) : RecyclerView.Adapter<ManageConfigurationsAdapter.ViewHolder>()
{
    override fun getItemCount() = configurations!!.size

    lateinit var didSelectConfig: ((config: Config) -> Unit)
    lateinit var shouldCloneConfig: ((config: Config) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_configurations, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    fun updateConfigurations( configurations: List<Config> )
    {
        this.configurations = configurations
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val config = configurations!!.get(holder.adapterPosition)

        holder.nameTextView.setText( config.name )
        val dateText = holder.itemView.resources.getString( R.string.created ) + " " + Date(config.creationDate).toLocalizedDateTimeString()

        holder.dateTextView.setText( dateText )
        holder.itemView.setOnClickListener {
            didSelectConfig(config)
        }

        holder.cloneImageView.setOnClickListener {
            shouldCloneConfig( config )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view);
        val cloneImageView: ImageView = itemView.findViewById(R.id.clone_image_view);
    }
}