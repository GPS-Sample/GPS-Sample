package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import edu.gtri.gpssample.R

class InfoDialog
{
    interface InfoDialogDelegate
    {
        fun didSelectOkButton( tag: Any? )
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, message: String?,
                 button: String, tag: Any?, delegate: InfoDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_info, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val textView = view.findViewById<TextView>(R.id.text_view)
        textView.text = message

        val okButton = view.findViewById<Button>(R.id.ok_button)
        okButton.text = button

        okButton.setOnClickListener {
            delegate.didSelectOkButton(tag)
            alertDialog.dismiss()
        }

    }
}