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
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.database.models.Strata

class AddStrataDialog
{
    enum class ButtonPress {
        Cancel,
        Save,
    }

    var strataNameEditText: EditText
    var sampleSizeEditText: EditText
    var strataSpinner: Spinner

    constructor(context: Context, strata: Strata, completion: ((buttonPressed: ButtonPress )->Unit))
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_add_strata, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        strataNameEditText = view.findViewById<EditText>(R.id.strata_name_edit_text )
        sampleSizeEditText = view.findViewById<EditText>(R.id.sample_size_edit_text )
        strataSpinner = view.findViewById<Spinner>(R.id.strata_spinner )

        strataNameEditText.setText( strata.name )
        sampleSizeEditText.setText( strata.sampleSize.toString())

        val position = if (strata.sampleType == SampleType.NumberHouseholds) 0 else 1

        strataSpinner.setSelection( position )

        val items = arrayListOf( context.resources.getString( R.string.numberhouseholds), context.resources.getString(R.string.percenthouseholds ))

        strataSpinner.adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items)

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        val saveButton = view.findViewById<Button>(R.id.save_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            completion( ButtonPress.Cancel )
        }

        saveButton.setOnClickListener {
            strata.name = strataNameEditText.text.toString()
            strata.sampleSize = sampleSizeEditText.text.toString().toInt()
            strata.sampleType = if (strataSpinner.selectedItemId == 0L) SampleType.NumberHouseholds else SampleType.PercentHouseholds
            alertDialog.dismiss()
            completion( ButtonPress.Save )
        }
    }
}