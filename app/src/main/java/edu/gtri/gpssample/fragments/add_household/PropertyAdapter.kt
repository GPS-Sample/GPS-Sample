/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.FieldDataOption
import edu.gtri.gpssample.database.models.FieldOption
import org.json.JSONObject

class PropertyAdapter( var keys: ArrayList<String>, var values: ArrayList<String>, var editMode: Boolean ) : RecyclerView.Adapter<PropertyAdapter.ViewHolder>()
{
    override fun getItemCount() = keys.size

    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_property, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val key = keys.get(position)
        val value = values.get(position)

        holder.textView.text = key
        holder.editText.setText( value )

        if (!editMode)
        {
            holder.editText.focusable = View.NOT_FOCUSABLE
        }

        holder.editText.addTextChangedListener(object: TextWatcher
        {
            override fun afterTextChanged(s: Editable?)
            {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int)
            {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
            {
                values.set( position, s.toString())
            }
        })
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val textView: TextView = itemView.findViewById(R.id.text_view);
        val editText: EditText = itemView.findViewById(R.id.edit_text);
    }
}