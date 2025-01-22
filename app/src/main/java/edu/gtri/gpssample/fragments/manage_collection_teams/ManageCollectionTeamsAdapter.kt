/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.manage_collection_teams

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.CollectionTeam
import java.util.*
import kotlin.collections.ArrayList

class ManageCollectionTeamsAdapter(var collectionTeams: List<CollectionTeam>?) : RecyclerView.Adapter<ManageCollectionTeamsAdapter.ViewHolder>()
{
    override fun getItemCount() = collectionTeams!!.size

    private lateinit var context: Context
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectTeam: ((collectionTeam: CollectionTeam) -> Unit)
    lateinit var shouldDeleteTeam: ((collectionTeam: CollectionTeam) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_team, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateTeams(collectionTeams: List<CollectionTeam> )
    {
        this.collectionTeams = collectionTeams
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val team = collectionTeams!!.get(holder.adapterPosition)

        holder.nameTextView.setText( team.name )
        holder.imageView.visibility = View.VISIBLE
        holder.dateTextView.setText( Date(team.creationDate).toString())

        holder.imageView.setOnClickListener {
            shouldDeleteTeam(team)
        }

        holder.itemView.setOnClickListener {
            didSelectTeam(team)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.check_image_view)
    }
}