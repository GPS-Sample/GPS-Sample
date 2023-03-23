package edu.gtri.gpssample.constants

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
            else -> SamplingMethod.None
        }
    }
    fun fromString( type : String) : SamplingMethod
    {
        return when(type)
        {
            SamplingMethod.SimpleRandom.format -> SamplingMethod.SimpleRandom
            SamplingMethod.Cluster.format -> SamplingMethod.Cluster
            SamplingMethod.Subsets.format -> SamplingMethod.Subsets
            else -> SamplingMethod.None
        }
    }
}