package edu.gtri.gpssample.constants

enum class SampleType(val format : String) {
    None("None"),
    NumberHouseholds("# of Households"),
    PercentHouseholds("% of all Households"),
    PercentTotal("% of total population"),

}

object SampleTypeConverter
{
    fun toArray() : Array<String>
    {
        return  Array(4){ i ->
            when(i)
            {
                0 -> SampleType.NumberHouseholds.format
                1 -> SampleType.PercentHouseholds.format
                2 -> SampleType.PercentTotal.format
                else -> String()
            }
        }
    }
    fun toIndex(sampleType : SampleType) : Int
    {
        return when(sampleType)
        {
            SampleType.NumberHouseholds -> 1
            SampleType.PercentHouseholds -> 2
            SampleType.PercentTotal -> 3
            else -> 0
        }
    }
    fun fromIndex( index : Int) : SampleType
    {
        return when(index)
        {
            1 -> SampleType.NumberHouseholds
            2 -> SampleType.PercentHouseholds
            3 -> SampleType.PercentTotal
            else -> SampleType.None
        }
    }
}