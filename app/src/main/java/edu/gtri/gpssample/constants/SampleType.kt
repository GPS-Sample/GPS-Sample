/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class SampleType(val format : String) {
    None("None"),
    NumberHouseholds("# of Households"),
    PercentHouseholds("% of all Households"),
}

object SampleTypeConverter
{
    val array : Array<String> = Array(2){ i ->
            when(i)
            {
                0 -> SampleType.NumberHouseholds.format
                1 -> SampleType.PercentHouseholds.format
                else -> String()
            }
    }

    fun toIndex(sampleType : SampleType) : Int
    {
        return when(sampleType)
        {
            SampleType.NumberHouseholds -> 1
            SampleType.PercentHouseholds -> 2
            else -> 0
        }
    }

    fun fromIndex( index : Int) : SampleType
    {
        return when(index)
        {
            1 -> SampleType.NumberHouseholds
            2 -> SampleType.PercentHouseholds
            else -> SampleType.None
        }
    }

    fun fromArrayPosition( position : Int) : SampleType
    {
        return when(position)
        {
            1 -> SampleType.NumberHouseholds
            2 -> SampleType.PercentHouseholds
            else -> SampleType.None
        }
    }

    fun toArrayPosition(sampleType : SampleType) : Int
    {
        return when(sampleType)
        {
            SampleType.NumberHouseholds -> 0
            SampleType.PercentHouseholds -> 1
            else -> 0
        }
    }
    fun fromString( type : String) : SampleType
    {
        return when(type)
        {
            SampleType.NumberHouseholds.format -> SampleType.NumberHouseholds
            SampleType.PercentHouseholds.format -> SampleType.PercentHouseholds
            else -> SampleType.None
        }
    }
}