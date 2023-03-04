package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R

class CreateTeamDialog
{
    interface CreateTeamDialogDelegate
    {
        fun shouldCreateTeamNamed( name: String )
    }

    constructor()
    {
    }

    constructor(context: Context, delegate: CreateTeamDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_create_team, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select Rule").setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val cancelButton = view!!.findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {

            val editText = view.findViewById<EditText>(R.id.edit_text)

            if (editText.text.toString().length > 0)
            {
                alertDialog.dismiss()
                delegate.shouldCreateTeamNamed( editText.text.toString())
            }
        }
    }
}