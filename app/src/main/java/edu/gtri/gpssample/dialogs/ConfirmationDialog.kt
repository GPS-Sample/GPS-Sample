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
    constructor()
    {
    }

    enum class ButtonPress {
        Left,
        Right,
        None
    }

    constructor( context: Context?, title: String?, message: String?, leftButtonText: String, rightButtonText: String, tag: Any?, layoutVertically: Boolean, completion: (( buttonPressed: ButtonPress, tag: Any? )->Unit))
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_confirmation, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
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

        val leftButton: Button
        val rightButton: Button

        if (layoutVertically)
        {
            view.findViewById<LinearLayout>( R.id.horizontal_layout ).visibility = View.GONE
            view.findViewById<LinearLayout>( R.id.vertical_layout ).visibility = View.VISIBLE
            leftButton = view.findViewById<Button>(R.id.top_button)
            rightButton = view.findViewById<Button>(R.id.bottom_button)
        }
        else
        {
            view.findViewById<LinearLayout>( R.id.vertical_layout ).visibility = View.GONE
            view.findViewById<LinearLayout>( R.id.horizontal_layout ).visibility = View.VISIBLE
            leftButton = view.findViewById<Button>(R.id.left_button)
            rightButton = view.findViewById<Button>(R.id.right_button)
        }

        leftButton.text = leftButtonText
        rightButton.text = rightButtonText

        leftButton.setOnClickListener {
            completion( ButtonPress.Left, tag )
            alertDialog.dismiss()
        }

        rightButton.setOnClickListener {
            completion( ButtonPress.Right, tag )
            alertDialog.dismiss()
        }

        alertDialog.setOnCancelListener {
            completion( ButtonPress.None, tag )
        }
    }
}