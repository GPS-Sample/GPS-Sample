/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import java.util.*

class TimePickerDialog
{
    interface TimePickerDialogDelegate
    {
        fun didSelectTime(date: Date, field: Field, fieldData: FieldData?, editText: EditText?)
    }

    constructor(context: Context, title: String, date: Date, field: Field, fieldData: FieldData?, editText: EditText?, delegate: TimePickerDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_time_picker, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val timePicker = view.findViewById<TimePicker>(R.id.time_picker)

        val calendar = Calendar.getInstance()
        calendar.time = date

        timePicker.hour = calendar[Calendar.HOUR_OF_DAY]
        timePicker.minute = calendar[Calendar.MINUTE]

        val titleView = view.findViewById<TextView>(R.id.title_text_view)
        titleView.text = title

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val saveButton = view.findViewById<View>(R.id.save_button) as Button

        saveButton.setOnClickListener(View.OnClickListener {

            calendar[Calendar.HOUR_OF_DAY] = timePicker.hour
            calendar[Calendar.MINUTE] = timePicker.minute
            val date = calendar.time
            delegate.didSelectTime(date,field,fieldData,editText)
            alertDialog.dismiss()
        })

        val cancelButton = view.findViewById<View>(R.id.cancel_button) as Button

        cancelButton.setOnClickListener(View.OnClickListener {
            alertDialog.dismiss()
        })
    }
}