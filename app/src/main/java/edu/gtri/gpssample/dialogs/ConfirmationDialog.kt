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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import edu.gtri.gpssample.R

class ConfirmationDialog
{
    interface ConfirmationDialogDelegate
    {
        fun didSelectFirstButton( tag: Any? )
        fun didSelectSecondButton( tag: Any? )
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, message: String?, firstButtonText: String, secondButtonText: String, tag: Any?, delegate: ConfirmationDialogDelegate, layoutVertically: Boolean = false )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_confirmation, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val textView = view.findViewById<TextView>(R.id.text_view)

        if (message != null && message.isNotEmpty())
        {
            textView.text = message
        }
        else
        {
            textView.visibility = View.GONE
        }

        val firstButton: Button
        val secondButton: Button

        if (layoutVertically)
        {
            view.findViewById<LinearLayout>( R.id.horizontal_layout ).visibility = View.GONE
            view.findViewById<LinearLayout>( R.id.vertical_layout ).visibility = View.VISIBLE
            firstButton = view.findViewById<Button>(R.id.top_button)
            secondButton = view.findViewById<Button>(R.id.bottom_button)
        }
        else
        {
            view.findViewById<LinearLayout>( R.id.vertical_layout ).visibility = View.GONE
            view.findViewById<LinearLayout>( R.id.horizontal_layout ).visibility = View.VISIBLE
            firstButton = view.findViewById<Button>(R.id.left_button)
            secondButton = view.findViewById<Button>(R.id.right_button)
        }

        firstButton.text = firstButtonText
        secondButton.text = secondButtonText

        firstButton.setOnClickListener {
            delegate.didSelectFirstButton(tag)
            alertDialog.dismiss()
        }

        secondButton.setOnClickListener {
            delegate.didSelectSecondButton(tag)
            alertDialog.dismiss()
        }
    }
}