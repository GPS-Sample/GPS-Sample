/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.constants

import android.util.Log
import androidx.fragment.app.Fragment

enum class SamplingMethod(val format : String) {
    None("None"),
    SimpleRandom("Simple Random Sampling"),
    Cluster("Cluster Sampling"),
    Subsets("Subsets: May Overlap"),
    Strata("Strata: Mutually Exclusive"),
}

object SamplingMethodConverter
{
    val array : Array<String> = Array(3){ i ->
        when(i)
        {
            0 -> SamplingMethod.Cluster.format
            1 -> SamplingMethod.SimpleRandom.format
            2 -> SamplingMethod.Strata.format
            else -> String()
        }
    }

    fun toIndex(sampleMethod : SamplingMethod) : Int
    {
        return when(sampleMethod)
        {
            SamplingMethod.None         -> 0
            SamplingMethod.SimpleRandom -> 1
            SamplingMethod.Cluster      -> 2
            SamplingMethod.Subsets      -> 3
            SamplingMethod.Strata       -> 4
        }
    }

    fun fromIndex( index : Int) : SamplingMethod
    {
        return when(index)
        {
            0 -> SamplingMethod.None
            1 -> SamplingMethod.SimpleRandom
            2 -> SamplingMethod.Cluster
            3 -> SamplingMethod.Subsets
            4 -> SamplingMethod.Strata
            else -> SamplingMethod.None
        }
    }

    fun fromArrayPosition( position : Int ) : SamplingMethod
    {
        return when(position)
        {
            0 -> SamplingMethod.Cluster
            1 -> SamplingMethod.SimpleRandom
            2 -> SamplingMethod.Strata
            else -> SamplingMethod.None
        }
    }

    fun toArrayPosition(samplingMethod : SamplingMethod) : Int
    {
        return when(samplingMethod)
        {
            SamplingMethod.Cluster      -> 0
            SamplingMethod.SimpleRandom -> 1
            SamplingMethod.Strata       -> 2
            else -> 0
        }
    }

    fun fromString( type : String) : SamplingMethod
    {
        return SamplingMethod.valueOf( type )
    }
}