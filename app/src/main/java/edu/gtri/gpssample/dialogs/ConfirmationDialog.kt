package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import edu.gtri.gpssample.R

class ConfirmationDialog
{
    interface ConfirmationDialogDelegate
    {
        fun didAnswerNo()
        fun didAnswerYes()
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, message: String?, delegate: ConfirmationDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_confirmation, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val textView = view!!.findViewById<TextView>(R.id.text_view)
        textView.text = message

        val noButton = view!!.findViewById<Button>(R.id.no_button)

        noButton.setOnClickListener {
            delegate.didAnswerNo()
            alertDialog.dismiss()
        }

        val yesButton = view!!.findViewById<Button>(R.id.yes_button)

        yesButton.setOnClickListener {
            delegate.didAnswerYes()
            alertDialog.dismiss()
        }
    }
}