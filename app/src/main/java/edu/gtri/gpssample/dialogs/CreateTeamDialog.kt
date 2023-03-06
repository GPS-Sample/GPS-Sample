package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Team

class CreateTeamDialog
{
    interface CreateTeamDialogDelegate
    {
        fun shouldUpdateTeam( team: Team )
        fun shouldCreateTeamNamed( name: String )
    }

    constructor()
    {
    }

    constructor( context: Context, team: Team?, delegate: CreateTeamDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_create_team, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)
        val editText = view.findViewById<EditText>(R.id.edit_text)

        team?.let {
            editText.setText( it.name )
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

                team?.let {
                    it.name = editText.text.toString()
                    delegate.shouldUpdateTeam( it )
                } ?: delegate.shouldCreateTeamNamed( editText.text.toString())
            }
        }
    }
}