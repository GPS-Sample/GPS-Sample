/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import edu.gtri.gpssample.R

class BusyIndicatorDialog
{
    interface BusyIndicatorDialogDelegate
    {
        fun didPressCancelButton()
    }

    lateinit var alertDialog: AlertDialog
    lateinit var progressTextView: TextView

    constructor( context: Context?, text: String, delegate: BusyIndicatorDialogDelegate?, cancelable: Boolean = true )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_busy_indicator, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val textView = view.findViewById<TextView>(R.id.title_text_view)
        textView.text = text

        progressTextView = view.findViewById<TextView>(R.id.progress_text_view)

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        if (!cancelable)
        {
            cancelButton.visibility = View.GONE
        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            delegate?.didPressCancelButton()
        }
    }

    fun updateProgress( text: String )
    {
        progressTextView.text = text
    }
}