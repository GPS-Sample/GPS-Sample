/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class DistanceFormat(val format : String) {
    None("None"),
    Meters("Meters / Kilometers"),
    Feet("Feet / Miles"),
}


object DistanceFormatConverter
{
    val array : Array<String> = Array(2){ i ->
        when(i)
        {
            0 -> DistanceFormat.Meters.format
            1 -> DistanceFormat.Feet.format
            else -> String()
        }
    }

    fun toIndex(distanceFormat : DistanceFormat) : Int
    {
        return when(distanceFormat)
        {
            DistanceFormat.Meters -> 1
            DistanceFormat.Feet -> 2
            else -> 0
        }
    }

    fun fromIndex( index : Int) : DistanceFormat
    {
        return when(index)
        {
            1 -> DistanceFormat.Meters
            2 -> DistanceFormat.Feet
            else -> DistanceFormat.None
        }
    }
    fun fromString( type : String) : DistanceFormat
    {
        return when(type)
        {
            DistanceFormat.Meters.format -> DistanceFormat.Meters
            DistanceFormat.Feet.format -> DistanceFormat.Feet
            else -> DistanceFormat.None
        }
    }
}