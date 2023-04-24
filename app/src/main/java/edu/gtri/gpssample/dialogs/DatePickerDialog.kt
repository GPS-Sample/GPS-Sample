package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldData
import java.util.*

class DatePickerDialog
{
    interface DatePickerDialogDelegate
    {
        fun didSelectDate(date: Date, field: Field, fieldData: FieldData, editText: EditText )
    }

    constructor(context: Context, title: String, date: Date, field: Field, fieldData: FieldData, editText: EditText, delegate: DatePickerDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_date_picker, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val datePicker = view.findViewById<View>(R.id.date_picker) as DatePicker

        val calendar = Calendar.getInstance()
        calendar.time = date

        datePicker.updateDate(
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )

        val titleView = view.findViewById<TextView>(R.id.title_text_view)
        titleView.text = title

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val saveButton = view.findViewById<View>(R.id.save_button) as Button

        saveButton.setOnClickListener(View.OnClickListener {
            val calendar = Calendar.getInstance()
            calendar[Calendar.YEAR] = datePicker.year
            calendar[Calendar.MONTH] = datePicker.month
            calendar[Calendar.DAY_OF_MONTH] = datePicker.dayOfMonth
            val date = calendar.time
            delegate.didSelectDate(date,field,fieldData,editText)
            alertDialog.dismiss()
        })

        val cancelButton = view.findViewById<View>(R.id.cancel_button) as Button

        cancelButton.setOnClickListener(View.OnClickListener {
            alertDialog.dismiss()
        })
    }
}