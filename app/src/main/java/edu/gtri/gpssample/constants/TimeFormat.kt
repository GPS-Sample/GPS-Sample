/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class TimeFormat(val format : String) {
    None("None"),
    twelveHour("12 hour (AM/PM)"),
    twentyFourHour("24 hour"),
}

object TimeFormatConverter
{
    val array : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> TimeFormat.twelveHour.format
            1 -> TimeFormat.twentyFourHour.format
            else -> String()
        }
    }

    fun toIndex(timeFormat : TimeFormat) : Int
    {
        return when(timeFormat)
        {
            TimeFormat.twelveHour -> 1
            TimeFormat.twentyFourHour -> 2
            else -> 0
        }
    }
    fun fromIndex( index : Int) : TimeFormat
    {
        return when(index)
        {
            1 -> TimeFormat.twelveHour
            2 -> TimeFormat.twentyFourHour
            else -> TimeFormat.None
        }
    }

    fun fromString( type : String) : TimeFormat
    {
        return when(type)
        {
            TimeFormat.twelveHour.format -> TimeFormat.twelveHour
            TimeFormat.twentyFourHour.format -> TimeFormat.twentyFourHour
            else -> TimeFormat.None
        }
    }
}