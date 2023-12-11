package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import edu.gtri.gpssample.R

class PerformCollectionHelpDialog
{
    constructor(context: Context? )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_perform_collection_help, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()
    }
}