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
import android.view.View
import android.widget.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Strata
import edu.gtri.gpssample.dialogs.ConfirmationDialog.ButtonPress
import java.util.ArrayList

class DropdownDialog
{
    constructor( context: Context?, title: String?, items: ArrayList<String>, completion: ((selection: String )->Unit) )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_dropdown, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val spinner = view.findViewById<Spinner>(R.id.spinner)
        spinner.adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, items )

        val firstButton = view.findViewById<Button>(R.id.left_button)
        val secondButton = view.findViewById<Button>(R.id.right_button)

        firstButton.setOnClickListener {
            completion( "" )
            alertDialog.dismiss()
        }

        secondButton.setOnClickListener {
            completion( spinner.selectedItem as String )
            alertDialog.dismiss()
        }
    }

    constructor( context: Context?, title: String?, stratas: ArrayList<Strata>, tag: Any?, completion: ((selection: Strata? )->Unit))
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_dropdown, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val spinner = view.findViewById<Spinner>(R.id.spinner)
        val items = ArrayList<String>()

        for (strata in stratas)
        {
            items.add( strata.name )
        }

        spinner.adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, items )

        val firstButton = view.findViewById<Button>(R.id.left_button)
        val secondButton = view.findViewById<Button>(R.id.right_button)

        firstButton.setOnClickListener {
            completion( null )
            alertDialog.dismiss()
        }

        secondButton.setOnClickListener {
            for (strata in stratas)
            {
                if (spinner.selectedItem.toString() == strata.name)
                {
                    completion( strata )
                    break
                }
            }

            alertDialog.dismiss()
        }
    }
}