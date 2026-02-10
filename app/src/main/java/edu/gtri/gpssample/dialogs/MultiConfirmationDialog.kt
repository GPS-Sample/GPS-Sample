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

class MultiConfirmationDialog
{
    interface MulitConfirmationDialogDelegate
    {
        fun didSelectMultiButton( selection: String, tag: Any? )
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, message: String?, items: List<String>, tag: Any?, delegate: MulitConfirmationDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_multi_confirmation, null)

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
        val thirdButton: Button
        val fourthButton: Button

        if (items.size > 4)
        {
            val fifthButton = view.findViewById<Button>(R.id.fifth_button)
            fifthButton.visibility = View.VISIBLE
            fifthButton.setOnClickListener {
                delegate.didSelectMultiButton(items[4], tag)
                alertDialog.dismiss()
            }
        }

        firstButton = view.findViewById<Button>(R.id.first_button)
        secondButton = view.findViewById<Button>(R.id.second_button)
        thirdButton = view.findViewById<Button>(R.id.third_button)
        fourthButton = view.findViewById<Button>(R.id.fourth_button)

        firstButton.text = items[0]
        secondButton.text = items[1]
        thirdButton.text = items[2]
        fourthButton.text = items[3]

        firstButton.setOnClickListener {
            delegate.didSelectMultiButton(items[0], tag)
            alertDialog.dismiss()
        }

        secondButton.setOnClickListener {
            delegate.didSelectMultiButton(items[1], tag)
            alertDialog.dismiss()
        }

        thirdButton.setOnClickListener {
            delegate.didSelectMultiButton(items[2], tag)
            alertDialog.dismiss()
        }

        fourthButton.setOnClickListener {
            delegate.didSelectMultiButton(items[3], tag)
            alertDialog.dismiss()
        }
    }
}