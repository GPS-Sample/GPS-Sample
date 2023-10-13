package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R

class ResetPinDialog
{
    interface ResetPinDialogDelegate
    {
        fun didUpdatePin( pin: String )
    }

    constructor()
    {
    }

    constructor( context: Context, currentPin: String, delegate: ResetPinDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_reset_pin, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val editText1 = view.findViewById<EditText>(R.id.pin1_edit_text)
        val editText2 = view.findViewById<EditText>(R.id.pin2_edit_text)
        val currentPinEditText = view.findViewById<EditText>(R.id.current_pin_edit_text)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val cancelButton = view!!.findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {

            if (currentPinEditText.text.toString().length > 0 && editText1.text.toString().length > 0 && editText2.text.toString().length > 0)
            {
                if (currentPinEditText.text.toString() == currentPin)
                {
                    if (editText1.text.toString() == editText2.text.toString())
                    {
                        alertDialog.dismiss()

                        delegate.didUpdatePin( editText1.text.toString())
                    }
                    else
                    {
                        Toast.makeText( context, context?.getString(R.string.pin_not_match), Toast.LENGTH_SHORT).show()
                    }
                }
                else
                {
                    Toast.makeText( context, context?.getString(R.string.pin_incorrect), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}