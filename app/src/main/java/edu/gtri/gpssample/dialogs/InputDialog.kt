package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R

class InputDialog
{
    interface InputDialogDelegate
    {
        fun didEnterText( text: String, tag: Any? )
    }

    constructor()
    {
    }

    constructor( context: Context, title: String, text: String?, tag: Any?, delegate: InputDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_input, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val textView = view.findViewById<TextView>(R.id.title_text_view)
        textView.text = title

        val editText = view.findViewById<EditText>(R.id.edit_text)

        text?.let {
            editText.setText( it )
        }

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val cancelButton = view!!.findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {

            if (editText.text.toString().length > 0)
            {
                alertDialog.dismiss()

                delegate.didEnterText( editText.text.toString(), tag )
            }
        }
    }
}