package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import edu.gtri.gpssample.R

class AdditionalInfoDialog
{
    interface AdditionalInfoDialogDelegate
    {
        fun didSelectCancelButton()
        fun didSelectSaveButton( nobodyHome: Boolean, homeDoesNotExist: Boolean, notes: String )
    }

    constructor()
    {
    }

    constructor( context: Context?, delegate: AdditionalInfoDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_additional_info, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        cancelButton.setOnClickListener {
            delegate.didSelectCancelButton()
            alertDialog.dismiss()
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {
            val nobodyHomeCheckBox = view.findViewById<CheckBox>(R.id.nobody_home_check_box)
            val homeDoesNotExistCheckBox = view.findViewById<CheckBox>(R.id.home_does_not_exist_check_box)
            val notesTextView = view.findViewById<EditText>(R.id.notes_edit_text)

            delegate.didSelectSaveButton( nobodyHomeCheckBox.isChecked, homeDoesNotExistCheckBox.isChecked, notesTextView.text.toString() )
            alertDialog.dismiss()
        }
    }
}