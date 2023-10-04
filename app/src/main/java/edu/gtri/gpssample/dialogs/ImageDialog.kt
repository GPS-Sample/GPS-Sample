package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R

class ImageDialog
{
    interface ImageDialogDelegate
    {
        fun shouldDeleteImage()
    }

    constructor()
    {
    }

    constructor(context: Context?, imageFileName: String, delegate: ImageDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_image, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val dismissButton = view.findViewById<Button>(R.id.dismiss_button)
        val deleteButton = view.findViewById<Button>(R.id.delete_button)

        dismissButton.setOnClickListener {
            alertDialog.dismiss()
        }

        deleteButton.setOnClickListener {
            delegate.shouldDeleteImage()
            alertDialog.dismiss()
        }

        val imageView = view.findViewById<ImageView>( R.id.image_view )
        val bitmap = BitmapFactory.decodeFile(imageFileName)
        imageView.setImageBitmap(bitmap)
    }
}