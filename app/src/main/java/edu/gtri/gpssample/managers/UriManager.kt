/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.managers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class UriManager
{
    fun createFileFromContentUri(context: Context, fileUri : Uri) : File {

        var fileName : String = ""

        fileUri.let { returnUri ->
            context.contentResolver.query(returnUri,null,null,null)
        }?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }

        //  For extract file mimeType
        val fileType: String? = fileUri.let { returnUri ->
            context.contentResolver.getType(returnUri)
        }

        val iStream : InputStream = context.contentResolver.openInputStream(fileUri)!!
        val outputDir : File = context?.cacheDir!!
        val outputFile : File = File(outputDir,fileName)
        copyStreamToFile(iStream, outputFile)
        iStream.close()
        return  outputFile
    }

    fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }
}