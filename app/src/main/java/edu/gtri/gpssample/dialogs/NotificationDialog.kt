package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import edu.gtri.gpssample.R

class NotificationDialog
{
    constructor()
    {
    }

    constructor(context: Context?, title: String?, message: String? )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_notification, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

//        val titleTextView = view.findViewById<TextView>(R.id.title_text_view)
//        titleTextView.text = title

        val messageTextView = view.findViewById<TextView>(R.id.message_text_view)
        messageTextView.text = message

        val okButton = view.findViewById<Button>(R.id.ok_button)

        okButton.setOnClickListener {
            alertDialog.dismiss()
        }
    }
}