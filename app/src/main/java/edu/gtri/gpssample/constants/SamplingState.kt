/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

enum class SamplingState(val format : String) {
    None("None"),
    NotSampled("NotSampled"),
    Sampled("Sampled"),
    SampledSpare("SampledSpare"),
    SampledSkipped("SampledSkipped"),

}

object SamplingStateConverter
{
    val array : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SamplingState.NotSampled.format
            1 -> SamplingState.Sampled.format
            2 -> SamplingState.SampledSpare.format
            3 -> SamplingState.SampledSkipped.format
            else -> String()
        }
    }

    fun toIndex(samplingState: SamplingState) : Int
    {
        return when(samplingState)
        {
            SamplingState.NotSampled -> 1
            SamplingState.Sampled -> 2
            SamplingState.SampledSpare -> 3
            SamplingState.SampledSkipped -> 4
            else -> 0
        }
    }

    fun fromIndex( index : Int) : SamplingState
    {
        return when(index)
        {
            1 -> SamplingState.NotSampled
            2 -> SamplingState.Sampled
            3 -> SamplingState.SampledSpare
            4 -> SamplingState.SampledSkipped
            else -> SamplingState.None
        }
    }

    fun toArrayPosition(samplingState : SamplingState) : Int
    {
        return when(samplingState)
        {
            SamplingState.NotSampled -> 0
            SamplingState.Sampled -> 1
            SamplingState.SampledSpare -> 2
            SamplingState.SampledSkipped -> 3
            else -> 0
        }
    }
    fun fromString( type : String) : SamplingState
    {
        return when(type)
        {
            SamplingState.NotSampled.format -> SamplingState.NotSampled
            SamplingState.Sampled.format -> SamplingState.Sampled
            SamplingState.SampledSpare.format -> SamplingState.SampledSpare
            SamplingState.SampledSkipped.format -> SamplingState.SampledSkipped
            else -> SamplingState.None
        }
    }
}