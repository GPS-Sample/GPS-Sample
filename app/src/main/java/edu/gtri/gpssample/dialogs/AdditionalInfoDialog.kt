package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R


class AdditionalInfoDialog
{
    interface AdditionalInfoDialogDelegate
    {
        fun didSelectCancelButton()
        fun didSelectSaveButton( incompleteReason: String, notes: String )
    }

    constructor()
    {
    }

    constructor( context: Context?, incompleteReason: String, notes: String, delegate: AdditionalInfoDialogDelegate )
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

        val incompleteCheckBox = view.findViewById<CheckBox>(R.id.incomplete_check_box)
        val notesTextView = view.findViewById<EditText>(R.id.notes_edit_text)
        val nobodyHomeButton = view.findViewById<RadioButton>(R.id.nobody_home_button)
        val doesNotExistButton = view.findViewById<RadioButton>(R.id.does_not_exist_button)
        val otherButton = view.findViewById<RadioButton>(R.id.other_button)

        incompleteCheckBox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener
        {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
            {
                if (!isChecked)
                {
                    nobodyHomeButton.isChecked = false
                    doesNotExistButton.isChecked = false
                    otherButton.isChecked = false
                }
            }
        })

        incompleteCheckBox.isChecked = incompleteReason.isNotEmpty()
        notesTextView.setText( notes )

        when (incompleteReason)
        {
            "Nobody home" -> nobodyHomeButton.isChecked = true
            "Home does not exist" -> doesNotExistButton.isChecked = true
            "Other" -> otherButton.isChecked = true
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {

            var incompleteReason = ""

            if (incompleteCheckBox.isChecked)
            {
                if (nobodyHomeButton.isChecked)
                {
                    incompleteReason = "Nobody home"
                }
                else if (doesNotExistButton.isChecked)
                {
                    incompleteReason = "Home does not exist"
                }
                else if (otherButton.isChecked)
                {
                    incompleteReason = "Other"
                }
                else
                {
                    Toast.makeText(context!!.applicationContext, "Oops! Please select a reason for incomplete", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            delegate.didSelectSaveButton( incompleteReason, notesTextView.text.toString() )
            alertDialog.dismiss()
        }
    }
}