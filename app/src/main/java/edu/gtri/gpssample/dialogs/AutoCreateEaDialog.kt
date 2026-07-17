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
import edu.gtri.gpssample.R

class AutoCreateEaDialog
{
    data class Result (
        var didCancel: Boolean = false,
        var name: String,
        var width: Double,
        var numLocations: Int
    )

    constructor( context: Context, completion: ((result: Result)->Unit))
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_auto_create_ea, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        val saveButton = view.findViewById<Button>(R.id.save_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            completion( Result(true, "", 0.0, 0 ))
        }

        saveButton.setOnClickListener {
            val nameEditText = view.findViewById<EditText>(R.id.name_edit_text )
            val name = nameEditText.text.toString()

            if (name.isEmpty())
            {
                Toast.makeText( context, "Please enter the name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val widthEditText = view.findViewById<EditText>(R.id.width_edit_text )
            val width = widthEditText.text.toString().toDoubleOrNull()

            if (width == null)
            {
                Toast.makeText( context, "Please enter the width", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val numHhsEditText = view.findViewById<EditText>(R.id.num_hhs_edit_text )
            val numHhs = numHhsEditText.text.toString().toIntOrNull()

            if (numHhs == null)
            {
                Toast.makeText( context, "Please enter the number of HHs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            alertDialog.dismiss()
            completion( Result( false, name, width!!, numHhs!! ))
        }
    }
}