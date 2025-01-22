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
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R

class CheckboxDialogAdapter(var items: List<String> ) : RecyclerView.Adapter<CheckboxDialogAdapter.ViewHolder>()
{
    override fun getItemCount() = items.size

    val selections = ArrayList<String>()

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_checkbox_option, parent, false))

        viewHolder.itemView.isSelected = false

        if (selections.isEmpty())
        {
            for (item in items)
            {
                selections.add( "" )
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val item = items.get(holder.adapterPosition)

        holder.checkBox.text = item
        holder.checkBox.isSelected = false

        holder.checkBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (isChecked)
                {
                    selections[position] = item
                }
                else
                {
                    selections[position] = ""
                }
            }
        })
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
    }
}