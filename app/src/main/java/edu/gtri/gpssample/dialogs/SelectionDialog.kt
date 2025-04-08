/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R

class SelectionDialog : SelectionDialogAdapter.SelectionDialogAdapterDelegate
{
    interface SelectionDialogDelegate
    {
        fun didMakeSelection( selection: String, tag: Int )
    }

    constructor()
    {
    }

    var tag = 0
    lateinit var alertDialog: AlertDialog
    lateinit var delegate: SelectionDialogDelegate

    constructor( context: Context?, items: List<String>, delegate: SelectionDialogDelegate, tag: Int = 0 )
    {
        this.tag = tag
        this.delegate = delegate

        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_select_map_tiles, null)

        val selectMapTilesDialogAdapter = SelectionDialogAdapter( items, this )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        recyclerView.adapter = selectMapTilesDialogAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.layoutManager = LinearLayoutManager(context)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    override fun adapterDidMakeSelection( selection: String )
    {
        delegate.didMakeSelection( selection, tag )
        alertDialog.dismiss()
    }
}