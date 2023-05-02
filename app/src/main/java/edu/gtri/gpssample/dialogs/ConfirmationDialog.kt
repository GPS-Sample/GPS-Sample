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
        fun didSelectLeftButton( tag: Any? )
        fun didSelectRightButton( tag: Any? )
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, message: String?, leftButton: String, rightButton: String, tag: Any?, delegate: ConfirmationDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_confirmation, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val textView = view.findViewById<TextView>(R.id.text_view)
        textView.text = message

        val noButton = view.findViewById<Button>(R.id.left_button)
        noButton.text = leftButton

        noButton.setOnClickListener {
            delegate.didSelectLeftButton(tag)
            alertDialog.dismiss()
        }

        val yesButton = view.findViewById<Button>(R.id.right_button)
        yesButton.text = rightButton

        yesButton.setOnClickListener {
            delegate.didSelectRightButton(tag)
            alertDialog.dismiss()
        }
    }
}