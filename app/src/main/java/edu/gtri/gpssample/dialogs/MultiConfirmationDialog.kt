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
    constructor( context: Context?, title: String?, message: String?, items: List<String>, tag: Any?, completion: (selection: String, tag: Any?) -> Unit )
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

        if (items.size > 4)
        {
            val fifthButton = view.findViewById<Button>(R.id.fifth_button)
            fifthButton.visibility = View.VISIBLE
            fifthButton.setOnClickListener {
                completion(items[4], tag)
                alertDialog.dismiss()
            }
        }

        val firstButton = view.findViewById<Button>(R.id.first_button)
        val secondButton = view.findViewById<Button>(R.id.second_button)
        val thirdButton = view.findViewById<Button>(R.id.third_button)
        val fourthButton = view.findViewById<Button>(R.id.fourth_button)
        val fifthButton = view.findViewById<Button>(R.id.fifth_button)

        firstButton.visibility = View.GONE
        secondButton.visibility = View.GONE
        thirdButton.visibility = View.GONE
        fourthButton.visibility = View.GONE
        fifthButton.visibility = View.GONE

        if (items.size >= 1)
        {
            firstButton.text = items[0]
            firstButton.visibility = View.VISIBLE
            firstButton.setOnClickListener {
                alertDialog.dismiss()
                completion(items[0], tag)
            }
            if (items.size >= 2)
            {
                secondButton.text = items[1]
                secondButton.visibility = View.VISIBLE
                secondButton.setOnClickListener {
                    alertDialog.dismiss()
                    completion(items[1], tag)
                }
                if (items.size >= 3)
                {
                    thirdButton.text = items[2]
                    thirdButton.visibility = View.VISIBLE
                    thirdButton.setOnClickListener {
                        alertDialog.dismiss()
                        completion(items[2], tag)
                    }
                    if (items.size >= 4)
                    {
                        fourthButton.text = items[3]
                        fourthButton.visibility = View.VISIBLE
                        fourthButton.setOnClickListener {
                            alertDialog.dismiss()
                            completion(items[3], tag)
                        }
                        if (items.size >= 5)
                        {
                            fifthButton.text = items[4]
                            fifthButton.visibility = View.VISIBLE
                            fifthButton.setOnClickListener {
                                alertDialog.dismiss()
                                completion(items[4], tag)
                            }
                        }
                    }
                }
            }
        }
    }
}