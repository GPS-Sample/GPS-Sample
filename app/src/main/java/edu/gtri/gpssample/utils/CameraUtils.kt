/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64

object CameraUtils
{
    fun getRotationAngle(context: Context, uri: Uri) : Float
    {
        var angle = 0f

        var inputStream: InputStream? = null

        try
        {
            inputStream = context.getContentResolver().openInputStream(uri)
            inputStream?.let {
                val exifInterface: ExifInterface = ExifInterface(it)

                val orientation: Int = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270f
                }
            }
        }
        catch (e: IOException)
        {
        }

        return angle
    }

    fun rotate( bitmap: Bitmap, degrees: Float ): Bitmap
    {
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(degrees.toFloat()) },
            true
        )
    }

    fun encodeBitmap( bitmap: Bitmap ) : String?
    {
        try
        {
            var width = bitmap.width.toDouble()
            var height = bitmap.height.toDouble()
            val aspectRatio = width / height

            width = 200.0
            height = width / aspectRatio

            val bm = Bitmap.createScaledBitmap( bitmap, width.toInt(), height.toInt(), false )

            val byteArrayOutputStream = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            return Base64.getEncoder().encodeToString(byteArray)
        }
        catch (e: Exception)
        {
            Log.d( "xxx", e.stackTrace.toString())
        }

        return null
    }

    fun decodeString( imageData: String ) : Bitmap
    {
        val byteArray = Base64.getDecoder().decode( imageData )
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        return BitmapFactory.decodeStream(byteArrayInputStream)
    }
}