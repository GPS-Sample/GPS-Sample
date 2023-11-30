package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
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

    constructor( context: Context?, text: String, delegate: BusyIndicatorDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_busy_indicator, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val textView = view.findViewById<TextView>(R.id.text_view)
        textView.text = text

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            delegate.didPressCancelButton()
        }
    }
}