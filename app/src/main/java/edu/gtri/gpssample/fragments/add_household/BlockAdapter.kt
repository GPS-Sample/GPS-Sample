/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.add_household

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.dialogs.DatePickerDialog
import edu.gtri.gpssample.dialogs.TimePickerDialog
import java.util.*

class BlockAdapter( val editMode: Boolean, val config: Config, val listOfLists: ArrayList<ArrayList<FieldData>>) : RecyclerView.Adapter<BlockAdapter.ViewHolder>()
{
    private var context: Context? = null
    private lateinit var blockFieldAdapter: BlockFieldAdapter

    override fun getItemCount() = listOfLists.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_field_block, parent, false))

        viewHolder.itemView.isSelected = false

        return viewHolder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val linearLayout: LinearLayout = itemView as LinearLayout
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val fieldDataList = listOfLists.get(holder.adapterPosition)

        val titleTextView = holder.linearLayout.findViewById<TextView>(R.id.title_text_view)
        titleTextView.text = "Item # ${position+1}"

        blockFieldAdapter = BlockFieldAdapter( editMode, config, fieldDataList )

        val recyclerView: RecyclerView = holder.linearLayout.findViewById(R.id.recycler_view)
        recyclerView.adapter = blockFieldAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );
    }
}
