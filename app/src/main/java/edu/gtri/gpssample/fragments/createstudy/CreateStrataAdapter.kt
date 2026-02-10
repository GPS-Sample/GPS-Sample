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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Strata
import edu.gtri.gpssample.database.models.Study
import java.util.*
import kotlin.collections.ArrayList

class CreateStrataAdapter( var stratas: List<Strata>, var delegate: CreateStrataAdapterDelegate ) : RecyclerView.Adapter<CreateStrataAdapter.ViewHolder>()
{
    interface CreateStrataAdapterDelegate
    {
        fun strataEditButtonPressed( strata: Strata )
        fun strataDeleteButtonPressed( strata: Strata )
    }

    override fun getItemCount() = stratas.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectStrata: ((strata: Strata) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_strata, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateStratas( stratas: List<Strata> )
    {

        this.stratas = stratas
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val strata = stratas.get(holder.adapterPosition)

        holder.strataNameEditText.setText( strata.name )
        holder.sampleSizeEditText.setText( strata.sampleSize.toString())
        holder.sampleTypeTextView.setText( strata.sampleType.format )

        holder.editButton.setOnClickListener {
            delegate.strataEditButtonPressed( strata )
        }

        holder.deleteButton.setOnClickListener {
            delegate.strataDeleteButtonPressed( strata )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val editButton= itemView.findViewById<ImageView>(R.id.edit_image_view )
        val deleteButton= itemView.findViewById<ImageView>(R.id.delete_image_view )
        val strataNameEditText: EditText = itemView.findViewById(R.id.strata_name_edit_text);
        val sampleSizeEditText: EditText = itemView.findViewById(R.id.sample_size_edit_text);
        val sampleTypeTextView: TextView = itemView.findViewById(R.id.sample_type_text_view);
    }
}