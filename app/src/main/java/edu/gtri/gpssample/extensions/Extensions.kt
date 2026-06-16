/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.extensions

import android.database.sqlite.SQLiteStatement
import java.text.DateFormat
import java.util.Date

fun Int.toBoolean() = this == 1

fun Boolean.toInt() = if (this) 1 else 0


fun Date.toLocalizedDateTimeString() : String
{
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT ).format(this )
}

fun String.getSimpleUuid() : String
{
    val parts: List<String> = this.split("-").map { it }

    return if (parts.isNotEmpty()) parts[0].take(4 ) else ""
}

fun SQLiteStatement.bind(index: Int, value: Any?)
{
    when (value)
    {
        null -> bindNull(index)
        is String -> bindString(index, value)
        is Int -> bindLong(index, value.toLong())
        is Long -> bindLong(index, value)
        is Boolean -> bindLong(index, if (value) 1 else 0)
        is Double -> bindDouble(index, value)
        is Float -> bindDouble(index, value.toDouble())
        else -> throw IllegalArgumentException("Unsupported type ${value::class}")
    }
}
