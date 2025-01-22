/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createfield

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.FieldOption

class CreateFieldDropdownAdapter(var fieldOptions: List<FieldOption>) : RecyclerView.Adapter<CreateFieldDropdownAdapter.ViewHolder>()
{
    override fun getItemCount() = fieldOptions.size

    private var context: Context? = null
    private var allHolders = ArrayList<ViewHolder>()

    lateinit var shouldDeleteDropdownFieldOption: ((fieldOption: FieldOption) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_field_option, parent, false))

        viewHolder.itemView.isSelected = false

        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateFieldOptions( fieldOptions: List<FieldOption> )
    {
        this.fieldOptions = fieldOptions
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var optionEditText: EditText = itemView.findViewById(R.id.option_edit_text);
        val optionDeleteButton: Button = itemView.findViewById(R.id.option_delete_button);
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val fieldOption = fieldOptions.get(position)

        holder.optionEditText.setText( fieldOption.name )

        holder.optionDeleteButton.setOnClickListener {
            shouldDeleteDropdownFieldOption( fieldOption )
        }
    }
}