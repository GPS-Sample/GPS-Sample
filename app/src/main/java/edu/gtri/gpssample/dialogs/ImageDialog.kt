/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.widget.*
import edu.gtri.gpssample.R
import java.io.ByteArrayInputStream
import java.util.*

class ImageDialog
{
    interface ImageDialogDelegate
    {
        fun shouldDeleteImage()
    }

    constructor()
    {
    }

    constructor(context: Context?, imageData: String, delegate: ImageDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_image, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
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

        // base64 decode the bitmap
        val byteArray = Base64.getDecoder().decode( imageData )
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val bitmap = BitmapFactory.decodeStream(byteArrayInputStream)

        val imageView = view.findViewById<ImageView>( R.id.image_view )
        imageView.setImageBitmap(bitmap)
    }
}