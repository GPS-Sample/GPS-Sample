/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R

class SelectionDialogAdapter(var items: List<String>, var delegate: SelectionDialogAdapterDelegate) : RecyclerView.Adapter<SelectionDialogAdapter.ViewHolder>()
{
    interface SelectionDialogAdapterDelegate
    {
        fun adapterDidMakeSelection( selection: String )
    }

    override fun getItemCount() = items.size

    var selection = ""

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_text, parent, false))

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        val item = items.get(holder.adapterPosition)

        holder.textView.text = item

        holder.textView.setOnClickListener {
            selection = item
            delegate.adapterDidMakeSelection( selection )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val textView: TextView = itemView.findViewById(R.id.text_view)
    }
}