/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createconfiguration

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Study
import java.util.*
import kotlin.collections.ArrayList

class ManageStudiesAdapter(var studies: List<Study>?) : RecyclerView.Adapter<ManageStudiesAdapter.ViewHolder>()
{
    override fun getItemCount() = studies!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectStudy: ((study: Study) -> Unit)
    lateinit var shouldDeleteStudy: ((study: Study) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateStudies( studies: List<Study>? )
    {

        this.studies = studies
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val study = studies!!.get(holder.adapterPosition)

        holder.nameTextView.setText( study.name )
        holder.dateTextView.setText( Date( study.creationDate ).toString())

        holder.itemView.setOnClickListener {
            didSelectStudy(study)
        }

//        holder.deleteImageView.setOnClickListener {
//            shouldDeleteStudy(study)
//        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view);
    }
}