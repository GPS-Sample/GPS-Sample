package edu.gtri.gpssample.constants

enum class SamplingState(val format : String) {
    None("None"),
    NotSampled("Not Sampled"),
    Sampled("Sampled"),
    Resampled("Resampled"),
    Invalid("Invalid"),

}

object SamplingStateConverter
{
    val array : Array<String> = Array(4){ i ->
        when(i)
        {
            0 -> SamplingState.NotSampled.format
            1 -> SamplingState.Sampled.format
            2 -> SamplingState.Resampled.format
            3 -> SamplingState.Invalid.format
            else -> String()
        }
    }

    fun toIndex(samplingState: SamplingState) : Int
    {
        return when(samplingState)
        {
            SamplingState.NotSampled -> 1
            SamplingState.Sampled -> 2
            SamplingState.Resampled -> 3
            SamplingState.Invalid -> 4
            else -> 0
        }
    }

    fun fromIndex( index : Int) : SamplingState
    {
        return when(index)
        {
            1 -> SamplingState.NotSampled
            2 -> SamplingState.Sampled
            3 -> SamplingState.Resampled
            4 -> SamplingState.Invalid
            else -> SamplingState.None
        }
    }

    fun toArrayPosition(samplingState : SamplingState) : Int
    {
        return when(samplingState)
        {
            SamplingState.NotSampled -> 0
            SamplingState.Sampled -> 1
            SamplingState.Resampled -> 2
            SamplingState.Invalid -> 3
            else -> 0
        }
    }
    fun fromString( type : String) : SamplingState
    {
        return when(type)
        {
            SamplingState.NotSampled.format -> SamplingState.NotSampled
            SamplingState.Sampled.format -> SamplingState.Sampled
            SamplingState.Resampled.format -> SamplingState.Resampled
            SamplingState.Invalid.format -> SamplingState.Invalid
            else -> SamplingState.None
        }
    }
}