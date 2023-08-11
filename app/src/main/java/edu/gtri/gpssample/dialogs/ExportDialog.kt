package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.provider.MediaStore.Audio.Radio
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R

class ExportDialog
{
    interface ExportDialogDelegate
    {
        fun shouldExport( fileName: String, configuration: Boolean, qrCode: Boolean )
    }

    constructor()
    {
    }

    constructor( context: Context?, configFileName: String, enumFileName: String, delegate: ExportDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_export, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        val fileNameEditText = view.findViewById<EditText>( R.id.file_name_edit_text )
        val configButton = view.findViewById<RadioButton>( R.id.config_button )
        val qrButton = view.findViewById<RadioButton>( R.id.qr_button )

        fileNameEditText.setText( configFileName )

        configButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b)
            {
                fileNameEditText.setText( configFileName )
            }
            else
            {
                fileNameEditText.setText( enumFileName )
            }
        }

        val exportButton = view.findViewById<Button>(R.id.export_button)

        exportButton.setOnClickListener {

            if (fileNameEditText.text.toString().length == 0)
            {
                Toast.makeText(context!!.applicationContext, context?.getString(R.string.enter_file_name), Toast.LENGTH_SHORT).show()

            }
            else
            {
                delegate.shouldExport( fileNameEditText.text.toString(), configButton.isChecked, qrButton.isChecked )
                alertDialog.dismiss()
            }
        }
    }
}