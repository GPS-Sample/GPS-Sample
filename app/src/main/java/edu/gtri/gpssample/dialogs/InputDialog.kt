package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import edu.gtri.gpssample.R

class InputDialog
{
    interface InputDialogDelegate
    {
        fun didPressQrButton() {}

        fun didCancelText( tag: Any? )
        fun didEnterText( text: String, tag: Any? )
    }

    var editText: EditText? = null

    constructor()
    {
    }

    constructor( context: Context, qrVisible: Boolean, title: String, text: String?, leftButton: String, rightButton: String, tag: Any?, delegate: InputDialogDelegate, required: Boolean = true, inputNumber: Boolean = true )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_input, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val textView = view.findViewById<TextView>(R.id.title_text_view)
        textView.text = title

        editText = view.findViewById<EditText>(R.id.edit_text)

        if (inputNumber)
        {
            editText!!.inputType = InputType.TYPE_CLASS_NUMBER
        }

        text?.let {
            editText!!.setText( it )
        }

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        if (qrVisible)
        {
            val imageButton = view!!.findViewById<ImageButton>(R.id.qr_button)
            imageButton.visibility = View.VISIBLE
            imageButton.setOnClickListener {
                delegate.didPressQrButton()
            }
        }

        val cancelButton = view!!.findViewById<Button>(R.id.cancel_button)
        cancelButton.text = leftButton

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            delegate.didCancelText( tag )
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)
        saveButton.text = rightButton

        saveButton.setOnClickListener {

            if (!required || (required && editText!!.text.toString().length > 0))
            {
                alertDialog.dismiss()

                delegate.didEnterText( editText!!.text.toString(), tag )
            }
        }
    }

}