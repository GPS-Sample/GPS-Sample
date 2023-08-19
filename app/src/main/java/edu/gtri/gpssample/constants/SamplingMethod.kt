package edu.gtri.gpssample.constants

import androidx.fragment.app.Fragment
import edu.gtri.gpssample.R

enum class SamplingMethod(val format : String) {
    None("None"),
    SimpleRandom("Simple Random Sampling"),
    Cluster("Cluster Sampling"),
    Subsets("Subsets: May Overlap"),
    Strata("Strata: Mutually Exclusive"),
}

object SamplingMethodConverter
{
    val array : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SamplingMethod.SimpleRandom.format
            1 -> SamplingMethod.Cluster.format
            2 -> SamplingMethod.Subsets.format
            3 -> SamplingMethod.Strata.format
            else -> String()
        }
    }

    fun toIndex(sampleMethod : SamplingMethod) : Int
    {
        return when(sampleMethod)
        {
            SamplingMethod.SimpleRandom -> 1
            SamplingMethod.Cluster -> 2
            SamplingMethod.Subsets -> 3
            SamplingMethod.Strata -> 4
            else -> 0
        }
    }

    fun fromIndex( index : Int) : SamplingMethod
    {
        return when(index)
        {
            1 -> SamplingMethod.SimpleRandom
            2 -> SamplingMethod.Cluster
            3 -> SamplingMethod.Subsets
            4 -> SamplingMethod.Strata
            else -> SamplingMethod.None
        }
    }

    fun fromArrayPosition( position : Int) : SamplingMethod
    {
        return when(position)
        {
            0 -> SamplingMethod.SimpleRandom
            1 -> SamplingMethod.Cluster
            2 -> SamplingMethod.Subsets
            3 -> SamplingMethod.Strata
            else -> SamplingMethod.None
        }
    }

    fun toArrayPosition(samplingMethod : SamplingMethod) : Int
    {
        return when(samplingMethod)
        {
            SamplingMethod.SimpleRandom -> 0
            SamplingMethod.Cluster -> 1
            SamplingMethod.Subsets -> 2
            SamplingMethod.Strata -> 3
            else -> 0
        }
    }
    fun fromString( type : String) : SamplingMethod
    {
        return when(type)
        {
            SamplingMethod.SimpleRandom.format -> SamplingMethod.SimpleRandom
            SamplingMethod.Cluster.format -> SamplingMethod.Cluster
            SamplingMethod.Subsets.format -> SamplingMethod.Subsets
            SamplingMethod.Strata.format -> SamplingMethod.Strata
            else -> SamplingMethod.None
        }
    }

    fun internationalString(samplingMethod: SamplingMethod, fragment: Fragment) : String
    {
        return when(samplingMethod)
        {
            SamplingMethod.SimpleRandom-> fragment.getString(R.string.simple_random)
            SamplingMethod.Cluster -> fragment.getString(R.string.cluster_sampling)
            SamplingMethod.Subsets -> fragment.getString(R.string.subset_overlap)
            SamplingMethod.Strata -> fragment.getString(R.string.strata_exclusive)
            else -> ""
        }
    }
}