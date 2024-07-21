package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import edu.gtri.gpssample.R

class DropdownDialog
{
    interface DropdownDialogDelegate
    {
        fun didSelectSaveButton( json: String, response: String )
        fun didSelectCancelButton( json: String )
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, items: ArrayList<String>, json: String, delegate: DropdownDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_dropdown, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val spinner = view.findViewById<Spinner>(R.id.spinner)
        spinner.adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, items )

        val firstButton = view.findViewById<Button>(R.id.left_button)
        val secondButton = view.findViewById<Button>(R.id.right_button)

        firstButton.setOnClickListener {
            delegate.didSelectCancelButton( json )
            alertDialog.dismiss()
        }

        secondButton.setOnClickListener {
            delegate.didSelectSaveButton( json, spinner.selectedItem as String )
            alertDialog.dismiss()
        }
    }
}